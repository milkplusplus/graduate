package imitationmodel;

import com.beust.jcommander.JCommander;

public class Driver {

    public static void main(String[] args) {
        Input input = getInput(args);
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
}
