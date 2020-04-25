package imitationmodel;

import lombok.EqualsAndHashCode;
import lombok.Value;

import static imitationmodel.DistributionType.WEIBULL;

@Value
@EqualsAndHashCode(callSuper = true)
public class WeibullDistribution extends Distribution {

    Double c;
    Double b;

    public WeibullDistribution(Double c, Double b) {
        super(WEIBULL);
        this.c = c;
        this.b = b;
    }

    @Override
    public int calculateNext() {
        return 0;
    }

    @Override
    public String toString() {
        return String.format("Weibull distribution with c = %s, b = %s", c, b);
    }
}
