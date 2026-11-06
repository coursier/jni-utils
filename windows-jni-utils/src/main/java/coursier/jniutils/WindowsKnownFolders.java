package coursier.jniutils;

public final class WindowsKnownFolders {

    // KNOWNFOLDERID values, as defined in KnownFolders.h, to be passed to knownFolderPath.
    // Typical paths are given for reference only: users and administrators can relocate most
    // of these folders.

    /** FOLDERID_RoamingAppData, typically %USERPROFILE%\AppData\Roaming (%APPDATA%) */
    public static final String ROAMING_APP_DATA = "{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}";
    /** FOLDERID_LocalAppData, typically %USERPROFILE%\AppData\Local (%LOCALAPPDATA%) */
    public static final String LOCAL_APP_DATA = "{F1B32785-6FBA-4FCF-9D55-7B8E7F157091}";
    /** FOLDERID_LocalAppDataLow, typically %USERPROFILE%\AppData\LocalLow */
    public static final String LOCAL_APP_DATA_LOW = "{A520A1A4-1780-4FF6-BD18-167343C5AF16}";
    /** FOLDERID_ProgramData, typically %ALLUSERSPROFILE% (C:\ProgramData) */
    public static final String PROGRAM_DATA = "{62AB5D82-FDC1-4DC3-A9DD-070D1D495D97}";
    /** FOLDERID_Profile, typically %USERPROFILE% */
    public static final String PROFILE = "{5E6C858F-0E22-4760-9AFE-EA3317B67173}";
    /** FOLDERID_Desktop, typically %USERPROFILE%\Desktop */
    public static final String DESKTOP = "{B4BFCC3A-DB2C-424C-B029-7FE99A87C641}";
    /** FOLDERID_Documents, typically %USERPROFILE%\Documents */
    public static final String DOCUMENTS = "{FDD39AD0-238F-46AF-ADB4-6C85480369C7}";
    /** FOLDERID_Downloads, typically %USERPROFILE%\Downloads */
    public static final String DOWNLOADS = "{374DE290-123F-4565-9164-39C4925E467B}";
    /**
     * FOLDERID_ProgramFiles, typically %ProgramFiles%. From a 32-bit process on a 64-bit
     * system, this is the x86 Program Files folder (C:\Program Files (x86)).
     */
    public static final String PROGRAM_FILES = "{905E63B6-C1BF-494E-B29C-65B732D3D21A}";
    /**
     * FOLDERID_ProgramFilesX64, typically C:\Program Files. Not supported from 32-bit
     * processes: knownFolderPath returns null for it there.
     */
    public static final String PROGRAM_FILES_X64 = "{6D809377-6AF0-444B-8957-A3773F02200E}";
    /** FOLDERID_ProgramFilesX86, typically C:\Program Files (x86) on 64-bit systems */
    public static final String PROGRAM_FILES_X86 = "{7C5A40EF-A0FB-4BFC-874A-C0F2E0B9FA8E}";
    /** FOLDERID_System, typically %windir%\system32 */
    public static final String SYSTEM = "{1AC14E77-02E7-4E5D-B744-2EB1AE5198B7}";
    /** FOLDERID_Windows, typically %windir% (C:\Windows) */
    public static final String WINDOWS = "{F38BF404-1D43-42F2-9305-67DE0B28FC23}";
    /** FOLDERID_UserProgramFiles, typically %LOCALAPPDATA%\Programs */
    public static final String USER_PROGRAM_FILES = "{5CD7AEE2-2219-4A67-B85D-6C9CE15660CB}";
    /** FOLDERID_Startup, typically %APPDATA%\Microsoft\Windows\Start Menu\Programs\StartUp */
    public static final String STARTUP = "{B97D20BB-F46A-4C97-BA10-5E3608430854}";
    /** FOLDERID_StartMenu, typically %APPDATA%\Microsoft\Windows\Start Menu */
    public static final String START_MENU = "{625B53C3-AB48-4EC1-BA1F-A1EF4146FC19}";
    /** FOLDERID_Programs, typically %APPDATA%\Microsoft\Windows\Start Menu\Programs */
    public static final String PROGRAMS = "{A77F5D77-2E2B-44C3-A6A2-ABA601054A51}";

    /**
     * The path of a known folder, as returned by SHGetKnownFolderPath.
     *
     * @param rfid a KNOWNFOLDERID GUID, with its surrounding braces, like
     *             "{3EB685DB-65F9-4CF6-A03A-E3EF65729F3D}" for FOLDERID_RoamingAppData
     *             (see the constants of this class)
     * @return the path of the folder, or null if rfid is null or malformed, or the folder can't be found
     */
    public static String knownFolderPath(String rfid) {
        return NativeApi.get().GetKnownFolderPath(rfid);
    }

    /** The path of FOLDERID_RoamingAppData, or null if it can't be found */
    public static String roamingAppData() {
        return knownFolderPath(ROAMING_APP_DATA);
    }

    /** The path of FOLDERID_LocalAppData, or null if it can't be found */
    public static String localAppData() {
        return knownFolderPath(LOCAL_APP_DATA);
    }

    /** The path of FOLDERID_ProgramData, or null if it can't be found */
    public static String programData() {
        return knownFolderPath(PROGRAM_DATA);
    }

    /** The path of FOLDERID_Profile, or null if it can't be found */
    public static String profile() {
        return knownFolderPath(PROFILE);
    }
}
