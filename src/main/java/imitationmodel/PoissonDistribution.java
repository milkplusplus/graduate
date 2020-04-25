package imitationmodel;

import lombok.EqualsAndHashCode;
import lombok.Value;

import static imitationmodel.DistributionType.POISSON;

@Value
@EqualsAndHashCode(callSuper = true)
public class PoissonDistribution extends Distribution {

    Double lambda;

    public PoissonDistribution(Double lambda) {
        super(POISSON);
        this.lambda = lambda;
    }

    @Override
    public int calculateNext() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("Poisson distribution with lambda = %s", lambda);
    }
}
