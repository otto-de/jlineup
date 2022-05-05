package de.otto.jlineup.report;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Summary summary = (Summary) o;
        return error == summary.error && Double.compare(summary.differenceSum, differenceSum) == 0 && Double.compare(summary.differenceMax, differenceMax) == 0 && acceptedDifferentPixels == summary.acceptedDifferentPixels;
    }

    @Override
    public int hashCode() {
        return Objects.hash(error, differenceSum, differenceMax, acceptedDifferentPixels);
    }

    @Override
    public String toString() {
        return "Summary{" +
                "error=" + error +
                ", differenceSum=" + differenceSum +
                ", differenceMax=" + differenceMax +
                ", acceptedDifferentPixels=" + acceptedDifferentPixels +
                '}';
    }
}
