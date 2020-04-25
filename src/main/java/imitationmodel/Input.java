package imitationmodel;

import lombok.Value;

@Value
public class Input {
    Integer numberOfRequests;
    Distribution incomingDistribution;
    Distribution servingDistribution;
}
