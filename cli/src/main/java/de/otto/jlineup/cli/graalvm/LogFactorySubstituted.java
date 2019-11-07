package de.otto.jlineup.cli.graalvm;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.LogFactoryImpl;

@SuppressWarnings("unused")
@TargetClass(LogFactory.class)
public final class LogFactorySubstituted {

    @Substitute
    protected static LogFactory newFactory(final String factoryClass,
                                           final ClassLoader classLoader,
                                           final ClassLoader contextClassLoader) {
        return new LogFactoryImpl();
    }
}
