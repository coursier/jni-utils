package coursier.jniutils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;

public abstract class NativeApi {

    public abstract String terminalSize();

    public abstract String enableAnsiOutput();

    public abstract byte[] GetUserEnvironmentVariable(byte[] key);
    public abstract byte[] SetUserEnvironmentVariable(byte[] key, byte[] value);
    public abstract byte[] DeleteUserEnvironmentVariable(byte[] key);

    public abstract String GetKnownFolderPath(String rfid);


    private static NativeApi instance = null;

    public static NativeApi get() {
        Class<NativeApi> nativeApiClass = NativeApi.class;
        Class<LowPriorityNativeApi> lowPriorityNativeApiClass = LowPriorityNativeApi.class;

        if (instance == null) {
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

    public static void set(NativeApi nativeApi) {
        instance = nativeApi;
    }
}
