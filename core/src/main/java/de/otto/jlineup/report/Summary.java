package de.otto.jlineup.report;

public record Summary(boolean error, double differenceSum, double differenceMax, int acceptedDifferentPixels) {
}
