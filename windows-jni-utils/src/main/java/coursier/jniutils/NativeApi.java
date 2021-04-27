package coursier.jniutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class NativeApi {

    public abstract String terminalSize();

    public abstract String enableAnsiOutput();

    public abstract byte[] GetUserEnvironmentVariable(byte[] key);
    public abstract byte[] SetUserEnvironmentVariable(byte[] key, byte[] value);
    public abstract byte[] DeleteUserEnvironmentVariable(byte[] key);

    public abstract String GetKnownFolderPath(String rfid);


    private static NativeApi instance = null;
    public static NativeApi get() {
        if (instance == null) {
            // Equivalent to
            //     instance = NativeCalls.nativeApi();
            // but without referencing coursier.jniutils.NativeCalls from here, so
            // that if this code gets proguarded, the JVM doesn't try (and fail) to link the
            // native methods of coursier.jniutils.NativeCalls. Users need setup a different
            // NativeCalls instance themselves in that case (from one of the other modules).
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String className = "coursier";
            className = className + ".";
            className = className + "jniutils";
            className = className + ".";
            className = className + "NativeCalls";
            try {
                Class<?> cls = cl.loadClass(className);
                Method m = cls.getMethod("nativeApi");
                Object v = m.invoke(null);
                instance = (NativeApi) v;
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException("No NativeApi instance available. Set up one with coursier.jniutils.NativeCalls.setup()", e);
            }
        }
        if (instance == null)
            throw new RuntimeException("No NativeApi instance available. Set up one with coursier.jniutils.NativeCalls.setup()");
        return instance;
    }

    public static void set(NativeApi nativeApi) {
        instance = nativeApi;
    }
}
