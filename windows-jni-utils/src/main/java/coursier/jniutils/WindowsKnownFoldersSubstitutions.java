package coursier.jniutils;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;

@TargetClass(className = "coursier.jniutils.WindowsKnownFolders")
@Platforms({Platform.DARWIN.class, Platform.LINUX.class})
final class WindowsKnownFoldersSubstitutions {

    @Substitute
    public static String knownFolderPath(String rfid) {
        throw new RuntimeException("Not available on this platform");
    }

}
