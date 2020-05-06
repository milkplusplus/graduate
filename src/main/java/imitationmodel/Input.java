package imitationmodel;

import lombok.Value;

@Value
public class Input {
    Integer numberOfRequests;
    Integer queueSize;
    Double probabilityOfLoss;
    Distribution incomingDistribution;
    Distribution servingDistribution;
}
