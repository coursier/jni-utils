package coursier.jniutils;

public final class ModuleFileName {

    /**
     * The path of the executable of the current process, as returned by GetModuleFileName,
     * like C:\...\bin\java.exe (or the native executable, from a GraalVM native image).
     *
     * Paths longer than MAX_PATH are truncated. On failure, this returns an error message
     * prefixed with "error:" rather than throwing.
     */
    public static String get() {
        return NativeApi.get().GetModuleFileName();
    }
}
