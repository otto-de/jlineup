package de.otto.jlineup.web.configuration;

import de.otto.edison.status.domain.StatusDetail;
import de.otto.edison.status.indicator.StatusDetailIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.otto.edison.status.domain.Status.OK;
import static de.otto.edison.status.domain.StatusDetail.statusDetail;

@Component
public class LambdaStatusDetailIndicator implements StatusDetailIndicator {

    private final JLineupWebProperties properties;

    public LambdaStatusDetailIndicator(JLineupWebProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<StatusDetail> statusDetails() {
        JLineupWebLambdaProperties lambda = properties.getLambda();

        Map<String, String> details = new LinkedHashMap<>();
        details.put("Default function", lambda.getFunctionName());

        if (lambda.getFunctionNameBase() != null) {
            details.put("Base function", lambda.getFunctionNameBase());
        }
        if (lambda.getFunctionNameChromeHeadless() != null) {
            details.put("Chrome Headless function", lambda.getFunctionNameChromeHeadless());
        }
        if (lambda.getFunctionNameFirefoxHeadless() != null) {
            details.put("Firefox Headless function", lambda.getFunctionNameFirefoxHeadless());
        }
        if (lambda.getFunctionNameWebkitHeadless() != null) {
            details.put("Webkit Headless function", lambda.getFunctionNameWebkitHeadless());
        }

        return List.of(statusDetail("Lambda Functions", OK, "Configured", details));
    }
}
