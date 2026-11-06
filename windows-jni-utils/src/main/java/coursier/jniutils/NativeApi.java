package coursier.jniutils;

/**
 * The native methods of this library.
 *
 * Users shouldn't need to call these directly, and should rather use the classes wrapping
 * them, like {@link WindowsAnsiTerminal} or {@link WindowsEnvironmentVariables}.
 *
 * Several implementations exist, whose native methods are bound to the same DLL: one per
 * project shading this library, as shading changes the names of the classes the JNI functions
 * are looked up from.
 */
public abstract class NativeApi {

    public abstract String terminalSize();

    public abstract String enableAnsiOutput();

    public abstract String GetUserEnvironmentVariable(String key);
    public abstract String SetUserEnvironmentVariable(String key, String value);
    public abstract String DeleteUserEnvironmentVariable(String key);

    public abstract String GetKnownFolderPath(String rfid);

    public abstract String GetModuleFileName();

    public abstract int GetOEMCodePage();


    private static volatile NativeApi instance = null;
    private static final Object lock = new Object();

    /**
     * The implementations provided by the modules meant for the projects shading this library,
     * picked by {@link #get} over {@link DefaultNativeApi} if they're around. When this library
     * is shaded, the native methods of the relocated {@link DefaultNativeApi} can't be bound
     * anymore, but those of these implementations can (their packages aren't relocated).
     */
    private static final String[] alternativeImplementations = {
        "coursier.bootstrap.launcher.jniutils.BootstrapNativeApi",
        "lmcoursier.internal.jniutils.LmNativeApi",
        "coursierapi.internal.jniutils.ApiInternalNativeApi"
    };

    /**
     * The implementation to use.
     *
     * That's the one passed to {@link #set}, if any, else the first of the alternative
     * implementations (bootstrap, lmcoursier, coursierapi) that can be found in the class
     * loader of this class, else {@link DefaultNativeApi}.
     *
     * This loads the native library, if it's not loaded already.
     */
    public static NativeApi get() {
        NativeApi instance0 = instance;
        if (instance0 != null)
            return instance0;

        synchronized (lock) {
            instance0 = instance;
            if (instance0 == null) {
                NativeApi found = null;
                for (String className : alternativeImplementations) {
                    found = instantiate(className);
                    if (found != null)
                        break;
                }
                // Instantiating an implementation loads the DLL, under a name specific to
                // that implementation (see LoadWindowsLibrary.ensureInitialized(String))
                if (found == null)
                    found = new DefaultNativeApi();

                instance = found;
                instance0 = found;
            }
            return instance0;
        }
    }

    /**
     * Instantiates the {@code NativeApi} implementation named className, if it can be found in
     * the class loader of this class, or returns null.
     */
    private static NativeApi instantiate(String className) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className, false, NativeApi.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError ex) {
            return null;
        }
        if (!NativeApi.class.isAssignableFrom(clazz))
            return null;
        try {
            return (NativeApi) clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Error instantiating " + className, ex);
        }
    }

    /** Forces the implementation returned by {@link #get}, or resets it if passed null. */
    public static void set(NativeApi nativeApi) {
        instance = nativeApi;
    }
}
