package coursier.jniutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;

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


    private static NativeApi instance = null;

    /**
     * The implementation to use.
     *
     * That's the one passed to {@link #set}, if any, else the first {@code NativeApi} service
     * found by {@link ServiceLoader}, else the first {@link LowPriorityNativeApi} one.
     *
     * This loads the native library, if it's not loaded already.
     */
    public static NativeApi get() {
        Class<NativeApi> nativeApiClass = NativeApi.class;
        Class<LowPriorityNativeApi> lowPriorityNativeApiClass = LowPriorityNativeApi.class;

        if (instance == null) {
            LoadWindowsLibrary.ensureInitialized();
            Iterator<NativeApi> iterator = ServiceLoader.load(nativeApiClass).iterator();
            if (iterator.hasNext())
                instance = iterator.next();
        }

        if (instance == null) {
            Iterator<LowPriorityNativeApi> lowPriorityIterator = ServiceLoader.load(lowPriorityNativeApiClass).iterator();
            if (lowPriorityIterator.hasNext())
                instance = lowPriorityIterator.next();
        }

        if (instance == null)
            throw new RuntimeException("No NativeApi instance available. Could not load a Service for " + nativeApiClass + " or " + lowPriorityNativeApiClass);

        return instance;
    }

    /** Forces the implementation returned by {@link #get}, or resets it if passed null. */
    public static void set(NativeApi nativeApi) {
        instance = nativeApi;
    }
}
