package coursier.jniutils;

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
            instance = NativeCalls.nativeApi();
        }
        return instance;
    }

    public static void set(NativeApi nativeApi) {
        instance = nativeApi;
    }
}
