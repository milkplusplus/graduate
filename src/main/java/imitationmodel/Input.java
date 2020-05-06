package imitationmodel;

import lombok.Value;

@Value
public class Input {
    Integer numberOfRequests;
    Double probabilityOfLoss;
    Distribution incomingDistribution;
    Distribution servingDistribution;
}
