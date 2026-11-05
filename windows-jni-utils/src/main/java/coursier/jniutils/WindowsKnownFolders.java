package coursier.jniutils;

public final class WindowsKnownFolders {

    /**
     * The path of a known folder, as returned by SHGetKnownFolderPath.
     *
     * @param rfid a KNOWNFOLDERID GUID, with its surrounding braces, like
     *             "{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}" for FOLDERID_RoamingAppData
     * @return the path of the folder, or null if rfid is null or malformed, or the folder can't be found
     */
    public static String knownFolderPath(String rfid) {
        return NativeApi.get().GetKnownFolderPath(rfid);
    }
}
