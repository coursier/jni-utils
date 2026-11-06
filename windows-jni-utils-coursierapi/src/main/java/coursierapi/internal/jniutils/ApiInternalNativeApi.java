package coursierapi.internal.jniutils;

import coursier.jniutils.LoadWindowsLibrary;
import coursier.jniutils.NativeApi;

public final class ApiInternalNativeApi extends NativeApi {

    static native String terminalSizeNative();

    static native String enableAnsiOutputNative();

    static native String GetUserEnvironmentVariableNative(String key);
    static native String SetUserEnvironmentVariableNative(String key, String value);
    static native String DeleteUserEnvironmentVariableNative(String key);

    static native String GetKnownFolderPathNative(String rfid);

    static native String GetModuleFileNameNative();

    static native int GetOEMCodePageNative();

    public String terminalSize() {
        return terminalSizeNative();
    }

    public String enableAnsiOutput() {
        return enableAnsiOutputNative();
    }

    public String GetUserEnvironmentVariable(String key) {
        return GetUserEnvironmentVariableNative(key);
    }
    public String SetUserEnvironmentVariable(String key, String value) {
        return SetUserEnvironmentVariableNative(key, value);
    }
    public String DeleteUserEnvironmentVariable(String key) {
        return DeleteUserEnvironmentVariableNative(key);
    }

    public String GetKnownFolderPath(String rfid) {
        return GetKnownFolderPathNative(rfid);
    }

    public String GetModuleFileName() {
        return GetModuleFileNameNative();
    }

    public int GetOEMCodePage() {
        return GetOEMCodePageNative();
    }

    static {
        LoadWindowsLibrary.ensureInitialized("coursierapi");
    }

    public static void setup() {
        NativeApi.set(new ApiInternalNativeApi());
    }
}
