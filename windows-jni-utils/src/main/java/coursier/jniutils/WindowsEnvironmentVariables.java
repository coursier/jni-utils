package coursier.jniutils;

import java.io.IOException;

public final class WindowsEnvironmentVariables {

    public static String get(String key) throws IOException {
        String value = CString.fromC(NativeApi.get().GetUserEnvironmentVariable(CString.toC(key)));
        if (value == null)
            return null;
        if (value.startsWith("E"))
            throw new IOException(
                    "Error getting user environment variable " + key + ": " + value.substring("E".length()));
        return value.substring(1);
    }

    public static void set(String key, String value) throws IOException {
        String ret = CString.fromC(NativeApi.get().SetUserEnvironmentVariable(CString.toC(key), CString.toC(value)));
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error setting user environment variable " + key + ": " + ret.substring("E".length()));
    }

    public static void delete(String key) throws IOException {
        String ret = CString.fromC(NativeApi.get().DeleteUserEnvironmentVariable(CString.toC(key)));
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error deleting user environment variable " + key + ": " + ret.substring("E".length()));
    }
}
