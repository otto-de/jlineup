package de.otto.jlineup.report;

import java.util.List;

public record UrlReport(List<ScreenshotComparisonResult> comparisonResults, Summary summary){

}
