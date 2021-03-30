package coursier.jniutils.windowsknownfolders;

import coursier.jniutils.LoadWindowsLibrary;

public final class WindowsKnownFolders {

    public static String knownFolderPath(String rfid) {
        LoadWindowsLibrary.ensureInitialized();
        return GetKnownFolderPath(rfid);
    }

    private static native String GetKnownFolderPath(String rfid);
}
