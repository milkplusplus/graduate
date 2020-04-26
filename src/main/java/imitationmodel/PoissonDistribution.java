package imitationmodel;

import lombok.EqualsAndHashCode;
import lombok.Value;

import static imitationmodel.DistributionType.POISSON;
import static java.lang.Math.log;
import static java.lang.Math.random;

@Value
@EqualsAndHashCode(callSuper = true)
public class PoissonDistribution extends Distribution {

    Double lambda;

    public PoissonDistribution(Double lambda) {
        super(POISSON);
        this.lambda = lambda;
    }

    @Override
    public Double calculateNext() {
        return -log(1 - random()) / lambda;
    }

    @Override
    public String toString() {
        return String.format("Poisson distribution with lambda = %s", lambda);
    }
}
