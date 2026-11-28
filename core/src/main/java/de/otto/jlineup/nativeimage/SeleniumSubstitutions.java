package de.otto.jlineup.nativeimage;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import java.lang.reflect.Method;

/**
 * Since 4.53.0 Selenium compiles bean getters with LambdaMetafactory at runtime, which cannot define classes
 * in a native image. Selenium falls back to reflective getters for methods it considers not linkable, so
 * in the native image all methods are reported as not linkable.
 */
@TargetClass(className = "org.openqa.selenium.json.SimplePropertyDescriptor")
final class SeleniumSubstitutions {

    @Substitute
    static boolean isLinkableFromSelenium(Method method) {
        return false;
    }
}
