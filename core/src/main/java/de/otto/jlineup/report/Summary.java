package de.otto.jlineup.report;

public class Summary {

    public final boolean error;
    public final double differenceSum;
    public final double differenceMax;
    public final int acceptedDifferentPixels;

    public Summary(boolean error, double difference, double differenceMax, int acceptedDifferentPixels) {
        this.error = error;
        this.differenceSum = difference;
        this.differenceMax = differenceMax;
        this.acceptedDifferentPixels = acceptedDifferentPixels;
    }
}
