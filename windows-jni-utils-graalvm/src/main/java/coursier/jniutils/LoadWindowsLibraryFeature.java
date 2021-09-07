package coursier.jniutils;

import com.oracle.svm.core.annotate.AutomaticFeature;
import com.oracle.svm.core.jdk.Resources;
import com.oracle.svm.core.jni.JNIRuntimeAccess;
import com.oracle.svm.util.ReflectionUtil;
import org.graalvm.nativeimage.hosted.Feature;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

@AutomaticFeature
public final class LoadWindowsLibraryFeature implements Feature {

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> cls = access.findClassByName(LoadWindowsLibrary.class.getName());
        Method method = ReflectionUtil.lookupMethod(cls, "ensureInitialized");
        access.registerReachabilityHandler(c -> registerDllResource(cls), method);
    }

    private final AtomicBoolean registered = new AtomicBoolean();
    private void registerDllResource(Class<?> cls) {
        if (!registered.getAndSet(true)) {
            JNIRuntimeAccess.register(cls);
            String resource = LoadWindowsLibrary.dllResourcePath();
            InputStream is = cls.getClassLoader().getResourceAsStream(resource);
            if (is == null)
                throw new RuntimeException("Could not find resource " + resource);
            Resources.registerResource(resource, is);
        }
    }
}
