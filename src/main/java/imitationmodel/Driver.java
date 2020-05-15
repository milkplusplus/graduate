package imitationmodel;

import com.beust.jcommander.JCommander;
import org.apache.log4j.Logger;

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
        Input input = getInput(args);

        List<Integer> queueLengthsList = performSimulation(input, 1_000_000);
        Map<Integer, Integer> queueLengthsMap = new HashMap<>();
        queueLengthsList.forEach(value -> queueLengthsMap.merge(value, 1, Integer::sum));

        int calculatedQueueSize = calculateStatistics(input, queueLengthsList, queueLengthsMap);

        performSimulation(input, calculatedQueueSize);
    }

    static Input getInput(String[] args) {
        RawInput rawInput = new RawInput();
        JCommander.newBuilder().addObject(rawInput).build().parse(args);

        Input input = new Input(
                rawInput.getNumberOfRequests(),
                rawInput.getProbabilityOfLoss(),
                rawInput.createIncomingDistribution(),
                rawInput.createServingDistribution(),
                getAppropriateLogger(rawInput.getLogType(), rawInput.getLogDestination()));

        Logger log = input.getLog();

        log.warn("\nInput parameters:");
        log.warn("Number of requests = " + input.getNumberOfRequests());
        log.warn("Probability of loss = " + input.getProbabilityOfLoss());
        log.warn("Incoming distribution is " + input.getIncomingDistribution().toString());
        log.warn("Serving distribution is " + input.getServingDistribution().toString());
        return input;
    }

    private static List<Integer> performSimulation(Input input, int queueSize) {
        final int numberOfChannels = 1;
        int numberOfFreeChannels = numberOfChannels;
        int currentQueueSize = 0;
        int lostRequests = 0;

        Queue<Double> incomingQueue = getIncomingQueue(input);
        Queue<Double> servingQueue = new PriorityQueue<>();

        List<Integer> queueLengthsList = new ArrayList<>();

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
                        if (currentQueueSize == queueSize) {
                            lostRequests++;
                        } else {
                            currentQueueSize++;
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
        }

        if (lostRequests > 0) {
            input.getLog().warn("Number of lost requests is " + lostRequests + ", probability of lost is "
                    + ((double) lostRequests) / input.getNumberOfRequests());
        }

        return queueLengthsList;
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

    private static int calculateStatistics(Input input, List<Integer> queueLengthsList, Map<Integer, Integer> queueLengthsMap) {
        Logger log = input.getLog();

        log.warn("\nStatistics:");
        queueLengthsMap.forEach((k, v) -> log.info("Queue size " + k + " was " + v + " times"));
        log.warn("Maximum queue length is " + max(queueLengthsMap.keySet()));
        log.warn("Average queue length is " + getAverageQueueLength(queueLengthsMap, input.getNumberOfRequests()));

        printListValues("List of queue lengths:", queueLengthsList, log);

        List<Integer> maxLengths = getMaxLengths(queueLengthsList);
        printListValues("List of max queue lengths:", maxLengths, log);

        long numberOfNotNullValuesInQueue = queueLengthsList.stream().filter(v -> v != 0).count();
        double averageK = (double) numberOfNotNullValuesInQueue / maxLengths.size();
        log.warn("Number of not null values in queue: " + numberOfNotNullValuesInQueue);
        log.warn("Number of values in max lengths list: " + maxLengths.size());
        log.warn("Average K: " + averageK);

        List<Integer> maxValuesOfSubsequenceOfMaxLengths = getMaxValuesOfSubsequenceOfMaxLengths(maxLengths);
        printListValues("Max values of subsequence of max lengths list:", maxValuesOfSubsequenceOfMaxLengths, log);
        log.warn("Number of values in max values of subsequence of max lengths list: " + maxValuesOfSubsequenceOfMaxLengths.size());

        return calculateQueueSize(maxValuesOfSubsequenceOfMaxLengths, averageK, input.getProbabilityOfLoss(), log);
    }

    private static double getAverageQueueLength(Map<Integer, Integer> queueLengthMap, Integer numberOfRequests) {
        return queueLengthMap.entrySet()
                             .stream()
                             .mapToDouble(entry -> entry.getKey() * ((double) entry.getValue() / (2 * numberOfRequests)))
                             .sum();
    }

    private static int calculateQueueSize(List<Integer> maxLengths, double averageK, double probabilityOfLoss, Logger log) {
        Map<Integer, Double> histogramMap = createHistogramMap(maxLengths, log);

        double averageX = histogramMap.keySet().stream().mapToDouble(Integer::doubleValue).sum() / histogramMap.size();
        double averageU = histogramMap.values().stream().mapToDouble(Double::doubleValue).sum() / histogramMap.size();
        double sumOfAllXU = histogramMap.entrySet().stream().map(entry -> entry.getKey() * entry.getValue()).mapToDouble(Double::doubleValue).sum();
        double sumOfSquaresOfU = histogramMap.values().stream().mapToDouble(Double::doubleValue).map(v -> v * v).sum();

        double coefficientA = (sumOfAllXU - histogramMap.size() * averageX * averageU) / (sumOfSquaresOfU - histogramMap.size() * averageU * averageU);
        double coefficientB = averageX - coefficientA * averageU;
        double coefficientR = exp(0.5 / coefficientA);

        double rawN = coefficientA * log(coefficientA * coefficientR / (50 * averageK * probabilityOfLoss)) + coefficientB - 1;
        int ceilN = (int) Math.ceil(rawN);

        log.warn("X->U values are " + histogramMap);
        log.warn("Coefficient A is " + coefficientA);
        log.warn("Coefficient B is " + coefficientB);
        log.warn("Coefficient R is " + coefficientR);
        log.info("Entered probability of lost is " + probabilityOfLoss);

        log.warn("Calculated N size is " + ceilN);

        return ceilN;
    }

    private static Map<Integer, Double> createHistogramMap(List<Integer> maxLengths, Logger log) {
        Map<Integer, Double> histogramMap = new TreeMap<>();
        maxLengths.forEach(value -> histogramMap.merge(value, 1.0, Double::sum));

        log.warn("Values for histogram are " + histogramMap);

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

    private static void printListValues(String title, List<?> list, Logger log) {
        log.info(title);
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < list.size(); i++) {
            joiner.add(list.get(i).toString());
            if (i % 50 == 49 || (i == list.size() - 1)) {
                log.info(joiner.toString());
                joiner = new StringJoiner(", ");
            }
        }
    }

    private static Logger getAppropriateLogger(String logType, String logDestination) {
        if ("full".equalsIgnoreCase(logType)) {
            if ("console".equalsIgnoreCase(logDestination)) {
                return Logger.getLogger("FullConsole");
            } else {
                return Logger.getLogger("FullFile");
            }
        } else {
            if ("console".equalsIgnoreCase(logDestination)) {
                return Logger.getLogger("MainConsole");
            } else {
                return Logger.getLogger("MainFile");
            }
        }
    }
}
