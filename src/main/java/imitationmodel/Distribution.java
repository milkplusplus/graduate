package imitationmodel;

import lombok.Getter;

@Getter
public abstract class Distribution {

    DistributionType distributionType;

    public Distribution(DistributionType distributionType) {
        this.distributionType = distributionType;
    }

    public abstract Double calculateNext();
}
