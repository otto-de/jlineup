package de.otto.jlineup.cli.graalvm;

import com.oracle.svm.core.annotate.AutomaticFeature;
import org.graalvm.nativeimage.hosted.Feature;


@SuppressWarnings("unused")
@AutomaticFeature
public class RuntimeReflectionRegistrationFeature implements Feature {

    public void beforeAnalysis(BeforeAnalysisAccess access) {
//        try {
//            RuntimeReflection.register(HttpCheckConfig.class.getDeclaredConstructor());
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
    }

    @Override
    public void duringSetup(DuringSetupAccess access) {
    }
}