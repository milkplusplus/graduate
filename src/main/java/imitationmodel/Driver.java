package imitationmodel;

import com.beust.jcommander.JCommander;

import java.util.ArrayDeque;
import java.util.PriorityQueue;
import java.util.Queue;

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
        final int queueSize = 100_000_000;
        final int numberOfChannels = 1;
        int numberOfFreeChannels = numberOfChannels;
        int currentQueueSize = 0;

        Queue<Double> incomingQueue = getIncomingQueue(input);
        Queue<Double> servingQueue = new PriorityQueue<>();

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
        }
        System.out.println("END");
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
}
