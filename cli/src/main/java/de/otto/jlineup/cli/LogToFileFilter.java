package de.otto.jlineup.cli;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

@SuppressWarnings("unused")
public class LogToFileFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent iLoggingEvent) {
        if (iLoggingEvent.getMDCPropertyMap().containsKey("reportlogname")) {
            return FilterReply.NEUTRAL;
        }
        return FilterReply.DENY;
    }
}
