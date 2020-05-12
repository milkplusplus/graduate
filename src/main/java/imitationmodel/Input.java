package imitationmodel;

import lombok.Value;
import org.apache.log4j.Logger;

@Value
public class Input {
    Integer numberOfRequests;
    Double probabilityOfLoss;
    Distribution incomingDistribution;
    Distribution servingDistribution;
    Logger log;
}
