package coursier.jniutils.windowsenvironmentvariables;

import coursier.jniutils.LoadWindowsLibrary;
import coursier.jniutils.LoadWindowsLibrary;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Arrays;

public final class WindowsEnvironmentVariables {

    private static native byte[] GetUserEnvironmentVariableNative(String key);
    private static native String SetUserEnvironmentVariableNative(String key, byte[] value);

    public static String get(String key) throws IOException {
        return get(key, Charset.defaultCharset());
    }

    public static String get(String key, Charset charset) throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String value = new String(GetUserEnvironmentVariableNative(key), charset);
        if (value.startsWith("E"))
            throw new IOException(
                    "Error getting user environment variable " + key + ": " + value.substring("E".length()));
        return value.substring(1);
    }

    public static void set(String key, String value) throws IOException {
        set(key, value, Charset.defaultCharset());
    }

    public static void set(String key, String value, Charset charset) throws IOException {
        byte[] b = value.getBytes(charset);
        // Copying things to a NULL-terminated array
        byte[] b0 = Arrays.copyOf(b, b.length + 1);
        String ret = SetUserEnvironmentVariableNative(key, b0);
        if (ret.startsWith("E"))
            throw new IOException(
                    "Error setting user environment variable " + key + ": " + value.substring("E".length()));
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Before: " + get("FOO"));
        set("FOO", "Time €€ éé çç is " + LocalDateTime.now().toString());
        System.out.println("After: " + get("FOO"));
    }
}
