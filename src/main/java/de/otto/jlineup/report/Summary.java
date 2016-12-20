package de.otto.jlineup.report;

public class Summary {

    public final boolean error;
    public final double difference;

    public Summary(boolean error, double difference) {
        this.error = error;
        this.difference = difference;
    }
}
