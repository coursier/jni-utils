package coursier.jniutils;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import java.io.IOException;
import java.nio.charset.Charset;

@TargetClass(className = "coursier.jniutils.WindowsEnvironmentVariables")
@Platforms({Platform.DARWIN.class, Platform.LINUX.class})
final class WindowsEnvironmentVariablesSubstitutions {

    @Substitute
    public static String get(String key, Charset charset) throws IOException {
        throw new RuntimeException("Not available on this platform");
    }

    @Substitute
    public static void set(String key, String value, Charset charset) throws IOException {
        throw new RuntimeException("Not available on this platform");
    }

}
