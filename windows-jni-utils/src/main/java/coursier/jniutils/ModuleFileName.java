package coursier.jniutils;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class ModuleFileName {

    /**
     * The path of the executable of the current process, as returned by GetModuleFileName,
     * like C:\...\bin\java.exe (or the native executable, from a GraalVM native image).
     *
     * @throws UncheckedIOException if GetModuleFileName fails
     */
    public static String get() {
        String value = NativeApi.get().GetModuleFileName();
        if (value.startsWith("error:"))
            throw new UncheckedIOException(
                new IOException("Error getting module file name: " + value.substring("error:".length()))
            );
        return value;
    }
}
