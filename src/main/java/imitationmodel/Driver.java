package imitationmodel;

import com.beust.jcommander.JCommander;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.StringJoiner;
import java.util.TreeMap;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.util.Collections.max;

public class Driver {

    public static void main(String[] args) {
        execute(getInput(args));
    }

    static Input getInput(String[] args) {
        RawInput rawInput = new RawInput();
        JCommander.newBuilder().addObject(rawInput).build().parse(args);

        Input input = new Input(
                rawInput.getNumberOfRequests(),
                rawInput.getQueueSize(),
                rawInput.getProbabilityOfLoss(),
                rawInput.createIncomingDistribution(),
                rawInput.createServingDistribution());

        System.out.println("Input parameters:");
        System.out.println("Number of requests = " + input.getNumberOfRequests());
        System.out.println("Incoming distribution is " + input.getIncomingDistribution().toString());
        System.out.println("Serving distribution is " + input.getServingDistribution().toString());
        return input;
    }

    private static void execute(Input input) {
        final int numberOfChannels = 1;
        int numberOfFreeChannels = numberOfChannels;
        int currentQueueSize = 0;

        Queue<Double> incomingQueue = getIncomingQueue(input);
        Queue<Double> servingQueue = new PriorityQueue<>();

        List<Integer> queueLengthsList = new ArrayList<>();
        Map<Integer, Integer> queueLengthsMap = new HashMap<>();

        while (!incomingQueue.isEmpty() || !servingQueue.isEmpty() || numberOfChannels != numberOfFreeChannels) {
            Double currentIncomingValue = incomingQueue.peek();
            Double currentServingValue = servingQueue.peek();

            if (currentIncomingValue != null && currentServingValue != null) {
                if (currentIncomingValue < currentServingValue) {
                    incomingQueue.remove();
                    if (numberOfFreeChannels > 0) {
                        servingQueue.add(currentIncomingValue + input.getServingDistribution().calculateNext());
                        numberOfFreeChannels--;
                    } else {
                        currentQueueSize++;
                        if (currentQueueSize == input.getQueueSize()) {
                            throw new RuntimeException("Queue size is more than " + input.getQueueSize());
                        }
                    }
                } else {
                    servingQueue.remove();
                    if (currentQueueSize == 0) {
                        numberOfFreeChannels++;
                    } else {
                        servingQueue.add(currentServingValue + input.getServingDistribution().calculateNext());
                        currentQueueSize--;
                    }
                }
            }
            if (currentIncomingValue != null && currentServingValue == null) {
                incomingQueue.remove();
                servingQueue.add(currentIncomingValue + input.getServingDistribution().calculateNext());
                numberOfFreeChannels--;
            }
            if (currentIncomingValue == null && currentServingValue != null) {
                servingQueue.remove();
                if (currentQueueSize == 0) {
                    numberOfFreeChannels++;
                } else {
                    servingQueue.add(currentServingValue + input.getServingDistribution().calculateNext());
                    currentQueueSize--;
                }
            }
            queueLengthsList.add(currentQueueSize);
            queueLengthsMap.merge(currentQueueSize, 1, Integer::sum);
        }

        calculateStatistics(input, queueLengthsList, queueLengthsMap);
    }

    private static Queue<Double> getIncomingQueue(Input input) {
        Queue<Double> incomingQueue = new ArrayDeque<>();
        Double currentValue = 0.0;

        for (int i = 0; i < input.getNumberOfRequests(); i++) {
            currentValue += input.getIncomingDistribution().calculateNext();
            incomingQueue.add(currentValue);
        }
        return incomingQueue;
    }

    private static void calculateStatistics(Input input, List<Integer> queueLengthsList, Map<Integer, Integer> queueLengthsMap) {
        System.out.println("Statistics:");
        queueLengthsMap.forEach((k, v) -> System.out.println("Queue size " + k + " was " + v + " times"));
        System.out.println("Maximum queue length is " + max(queueLengthsMap.keySet()));
        System.out.println("Average queue length is " + getAverageQueueLength(queueLengthsMap, input.getNumberOfRequests()));

        printListValues("List of queue lengths:", queueLengthsList);

        List<Integer> maxLengths = getMaxLengths(queueLengthsList);
        printListValues("List of max queue lengths:", maxLengths);

        long numberOfNotNullValuesInQueue = queueLengthsList.stream().filter(v -> v != 0).count();
        double averageK = (double) numberOfNotNullValuesInQueue / maxLengths.size();
        System.out.println("Number of not null values in queue: " + numberOfNotNullValuesInQueue);
        System.out.println("Number of values in max lengths list: " + maxLengths.size());
        System.out.println("Average K: " + averageK);

        List<Integer> maxValuesOfSubsequenceOfMaxLengths = getMaxValuesOfSubsequenceOfMaxLengths(maxLengths);
        printListValues("Max values of subsequence of max lengths list:", maxValuesOfSubsequenceOfMaxLengths);
        System.out.println("Number of values in max values of subsequence of max lengths list: " + maxValuesOfSubsequenceOfMaxLengths.size());

        calculateQueueSize(maxValuesOfSubsequenceOfMaxLengths, averageK, input.getProbabilityOfLoss());
    }

    private static double getAverageQueueLength(Map<Integer, Integer> queueLengthMap, Integer numberOfRequests) {
        return queueLengthMap.entrySet()
                             .stream()
                             .mapToDouble(entry -> entry.getKey() * ((double) entry.getValue() / (2 * numberOfRequests)))
                             .sum();
    }

    private static void calculateQueueSize(List<Integer> maxLengths, double averageK, double probabilityOfLoss) {
        Map<Integer, Double> histogramMap = createHistogramMap(maxLengths);

        double averageX = histogramMap.keySet().stream().mapToDouble(Integer::doubleValue).sum() / histogramMap.size();
        double averageU = histogramMap.values().stream().mapToDouble(Double::doubleValue).sum() / histogramMap.size();
        double sumOfAllXU = histogramMap.entrySet().stream().map(entry -> entry.getKey() * entry.getValue()).mapToDouble(Double::doubleValue).sum();
        double sumOfSquaresOfU = histogramMap.values().stream().mapToDouble(Double::doubleValue).map(v -> v * v).sum();

        double coefficientA = (sumOfAllXU - histogramMap.size() * averageX * averageU) / (sumOfSquaresOfU - histogramMap.size() * averageU * averageU);
        double coefficientB = averageX - coefficientA * averageU;
        double coefficientR = exp(0.5 / coefficientA);

        double rawN = coefficientA * log(coefficientA * coefficientR / (50 * averageK * probabilityOfLoss)) + coefficientB - 1;
        int ceilN = (int) Math.ceil(rawN);

        System.out.println("X->U values are " + histogramMap);
        System.out.println("Coefficient A is " + coefficientA);
        System.out.println("Coefficient B is " + coefficientB);
        System.out.println("Coefficient R is " + coefficientR);

        System.out.println("Calculated N size is " + ceilN);
    }

    private static Map<Integer, Double> createHistogramMap(List<Integer> maxLengths) {
        Map<Integer, Double> histogramMap = new TreeMap<>();
        maxLengths.forEach(value -> histogramMap.merge(value, 1.0, Double::sum));

        System.out.println("Values for histogram are " + histogramMap);

        double previousValue = 0.0;
        for (Map.Entry<Integer, Double> entry : histogramMap.entrySet()) {
            previousValue += entry.getValue() / (maxLengths.size() + 1);
            entry.setValue(-log(-log(previousValue)));
        }

        return histogramMap;
    }

    private static List<Integer> getMaxLengths(List<Integer> queueLengthList) {
        List<Integer> maxLengthsList = new ArrayList<>();

        Integer currentMaxValue = 0;
        for (Integer value : queueLengthList) {
            if (value > currentMaxValue) {
                currentMaxValue = value;
            } else {
                if (value == 0 && currentMaxValue != 0) {
                    maxLengthsList.add(currentMaxValue);
                    currentMaxValue = 0;
                }
            }
        }

        return maxLengthsList;
    }

    private static List<Integer> getMaxValuesOfSubsequenceOfMaxLengths(List<Integer> maxLengthsList) {
        if (maxLengthsList.size() < 1000) {
            return new ArrayList<>(maxLengthsList);
        }

        List<Integer> result = new ArrayList<>();

        Integer currentMaxValue = 0;
        for (int i = 0; i < maxLengthsList.size(); i++) {
            Integer value = maxLengthsList.get(i);

            if (value > currentMaxValue) {
                currentMaxValue = value;
            }

            if (i % 50 == 49 || (i == maxLengthsList.size() - 1)) {
                result.add(currentMaxValue);
                currentMaxValue = 0;
            }
        }

        return result;
    }

    private static void printListValues(String title, List<?> list) {
        System.out.println(title);
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < list.size(); i++) {
            joiner.add(list.get(i).toString());
            if (i % 50 == 49 || (i == list.size() - 1)) {
                System.out.println(joiner.toString());
                joiner = new StringJoiner(", ");
            }
        }
    }
}
