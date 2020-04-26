package imitationmodel;

import lombok.EqualsAndHashCode;
import lombok.Value;

import static imitationmodel.DistributionType.WEIBULL;

@Value
@EqualsAndHashCode(callSuper = true)
public class WeibullDistribution extends Distribution {

    Double shape;
    Double scale;
    org.apache.commons.math3.distribution.WeibullDistribution innerDistribution;

    public WeibullDistribution(Double shape, Double scale) {
        super(WEIBULL);
        this.shape = shape;
        this.scale = scale;
        this.innerDistribution = new org.apache.commons.math3.distribution.WeibullDistribution(shape, scale);
    }

    @Override
    public Double calculateNext() {
        return innerDistribution.sample();
    }

    @Override
    public String toString() {
        return String.format("Weibull distribution with shape = %s, scale = %s", shape, scale);
    }
}
