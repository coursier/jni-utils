package coursier.jniutils.windowsenvironmentvariables;

import coursier.jniutils.CString;
import coursier.jniutils.LoadWindowsLibrary;

import java.io.IOException;

public final class WindowsEnvironmentVariables {

    private static native byte[] GetUserEnvironmentVariableNative(byte[] key);
    private static native byte[] SetUserEnvironmentVariableNative(byte[] key, byte[] value);
    private static native byte[] DeleteUserEnvironmentVariableNative(byte[] key);

    public static String get(String key) throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String value = CString.fromC(GetUserEnvironmentVariableNative(CString.toC(key)));
        if (value == null)
            return null;
        if (value.startsWith("E"))
            throw new IOException(
                    "Error getting user environment variable " + key + ": " + value.substring("E".length()));
        return value.substring(1);
    }

    public static void set(String key, String value) throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String ret = CString.fromC(SetUserEnvironmentVariableNative(CString.toC(key), CString.toC(value)));
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error setting user environment variable " + key + ": " + ret.substring("E".length()));
    }

    public static void delete(String key) throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String ret = CString.fromC(DeleteUserEnvironmentVariableNative(CString.toC(key)));
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error deleting user environment variable " + key + ": " + ret.substring("E".length()));
    }
}
