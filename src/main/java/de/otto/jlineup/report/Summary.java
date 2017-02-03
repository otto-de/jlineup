package de.otto.jlineup.report;

public class Summary {

    public final boolean error;
    public final double differenceSum;
    public final double differenceMax;

    public Summary(boolean error, double difference, double differenceMax) {
        this.error = error;
        this.differenceSum = difference;
        this.differenceMax = differenceMax;
    }
}
