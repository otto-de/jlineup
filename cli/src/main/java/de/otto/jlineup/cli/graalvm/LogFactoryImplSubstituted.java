package de.otto.jlineup.cli.graalvm;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.apache.commons.logging.impl.SimpleLog;

@SuppressWarnings("unused")
@TargetClass(LogFactoryImpl.class)
public final class LogFactoryImplSubstituted {

    @Substitute
    private Log discoverLogImplementation(String logCategory) {
        return new SimpleLog(logCategory);
    }
}
