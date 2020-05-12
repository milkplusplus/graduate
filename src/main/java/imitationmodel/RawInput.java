package imitationmodel;

import com.beust.jcommander.Parameter;
import lombok.Data;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Arrays;
import java.util.List;

import static imitationmodel.DistributionType.POISSON;
import static imitationmodel.DistributionType.WEIBULL;
import static java.lang.Double.valueOf;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

@Data
public class RawInput {

    @Parameter(names = "-numberOfRequests", required = true)
    Integer numberOfRequests;
    @Parameter(names = "-probabilityOfLoss", required = true)
    Double probabilityOfLoss;
    @Parameter(names = "-incomingDistribution", required = true)
    String incomingDistribution;
    @Parameter(names = "-incomingDistributionParameters", required = true)
    String incomingDistributionParameters;
    @Parameter(names = "-servingDistribution", required = true)
    String servingDistribution;
    @Parameter(names = "-servingDistributionParameters", required = true)
    String servingDistributionParameters;
    @Parameter(names = "-logType")
    String logType;
    @Parameter(names = "-logDestination")
    String logDestination;

    public Distribution createIncomingDistribution() {
        return getDistributionByRawValues(incomingDistribution, incomingDistributionParameters);
    }

    public Distribution createServingDistribution() {
        return getDistributionByRawValues(servingDistribution, servingDistributionParameters);
    }

    private static Distribution getDistributionByRawValues(String distribution, String distributionParameters) {
        DistributionType distributionType = getDistributionTypeByCharacter(distribution);

        if (POISSON.equals(distributionType)) {
            if (isParsable(distributionParameters)) {
                return new PoissonDistribution(valueOf(distributionParameters));
            } else {
                throw new IllegalArgumentException(
                        "distributionParameters should contain lambda value, but contains " + distributionParameters);
            }
        }

        if (WEIBULL.equals(distributionType)) {
            String[] parametersArray = distributionParameters.split(",");

            List<Double> parametersList = Arrays.stream(parametersArray)
                                                .filter(NumberUtils::isParsable)
                                                .map(Double::valueOf)
                                                .collect(toList());

            if (parametersList.size() != parametersArray.length || parametersList.size() != 2) {
                throw new IllegalArgumentException(
                        "distributionParameters should contain comma-separated numbers, but contains " + distributionParameters);
            }
            return new WeibullDistribution(parametersList.get(0), parametersList.get(1));
        }

        throw new UnsupportedOperationException("There is no such distribution type " + distribution);
    }

    private static DistributionType getDistributionTypeByCharacter(String character) {
        switch (character) {
            case "M":
                return POISSON;
            case "W":
                return WEIBULL;
            default:
                throw new UnsupportedOperationException("There is no distribution for character " + character);
        }
    }
}
