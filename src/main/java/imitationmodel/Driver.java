package imitationmodel;

import com.beust.jcommander.JCommander;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import static java.util.Collections.max;

public class Driver {

    public static void main(String[] args) {
        execute(getInput(args));
    }

    static Input getInput(String[] args) {
        RawInput rawInput = new RawInput();
        JCommander.newBuilder().addObject(rawInput).build().parse(args);

        Input input = new Input(rawInput.getNumberOfRequests(),
                rawInput.createIncomingDistribution(),
                rawInput.createServingDistribution());

        System.out.println("Input parameters:");
        System.out.println("Number of requests = " + input.getNumberOfRequests());
        System.out.println("Incoming distribution is " + input.getIncomingDistribution().toString());
        System.out.println("Serving distribution is " + input.getServingDistribution().toString());
        return input;
    }

    private static void execute(Input input) {
        final int queueSize = 1_000_000;
        final int numberOfChannels = 1;
        int numberOfFreeChannels = numberOfChannels;
        int currentQueueSize = 0;

        Queue<Double> incomingQueue = getIncomingQueue(input);
        Queue<Double> servingQueue = new PriorityQueue<>();
        Map<Integer, Integer> queueLengthMap = new HashMap<>();

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
                        if (currentQueueSize == queueSize) {
                            throw new RuntimeException("Queue size is more than " + queueSize);
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
            queueLengthMap.merge(currentQueueSize, 1, Integer::sum);
        }

        System.out.println("Statistics:");
        queueLengthMap.forEach((k, v) -> System.out.println("Queue size " + k + " was " + v + " times"));
        System.out.println("Maximum queue length is " + max(queueLengthMap.keySet()));
        System.out.println("Average queue length is " + getAverageQueueLength(queueLengthMap, input.getNumberOfRequests()));
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

    private static double getAverageQueueLength(Map<Integer, Integer> queueLengthMap, Integer numberOfRequests) {
        return queueLengthMap.entrySet()
                             .stream()
                             .mapToDouble(entry -> entry.getKey() * ((double) entry.getValue() / (2 * numberOfRequests)))
                             .sum();
    }
}
