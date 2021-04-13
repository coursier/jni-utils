package coursier.jniutils;

public final class WindowsKnownFolders {

    public static String knownFolderPath(String rfid) {
        return NativeApi.get().GetKnownFolderPath(rfid);
    }
}
