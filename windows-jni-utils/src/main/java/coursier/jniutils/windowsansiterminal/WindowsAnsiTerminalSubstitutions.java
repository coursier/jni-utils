package coursier.jniutils.windowsansiterminal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

import java.io.IOException;

@TargetClass(className = "coursier.jniutils.windowsansiterminal.WindowsAnsiTerminal")
@Platforms({Platform.DARWIN.class, Platform.LINUX.class})
final class WindowsAnsiTerminalSubstitutions {

    @Substitute
    public static TerminalSize terminalSize() throws IOException {
        throw new RuntimeException("Not available on this platform");
    }

    @Substitute
    public static boolean enableAnsiOutput() throws IOException {
        throw new RuntimeException("Not available on this platform");
    }

}
