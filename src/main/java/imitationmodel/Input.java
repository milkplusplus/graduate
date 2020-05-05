package imitationmodel;

import lombok.Value;

@Value
public class Input {
    Integer numberOfRequests;
    Integer queueSize;
    Distribution incomingDistribution;
    Distribution servingDistribution;
}
