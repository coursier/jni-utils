package coursier.jniutils.windowsansiterminal;

import coursier.jniutils.LoadWindowsLibrary;

import java.io.IOException;

public final class WindowsAnsiTerminal {

    private static native String terminalSizeNative();

    private static native String enableAnsiOutputNative();

    public static TerminalSize terminalSize() throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String value = terminalSizeNative();
        if (value.startsWith("error:"))
            throw new IOException("Error getting terminal size: " + value.substring("error:".length()));
        String[] elems = value.split(":", 2);
        if (elems.length != 2)
            throw new RuntimeException("Unexpected output from native method: '" + value + "'");
        return new TerminalSize(Integer.parseInt(elems[0]), Integer.parseInt(elems[1]));
    }

    /**
     *
     * @return Whether the console mode needed to be changed or not
     * @throws IOException
     */
    public static boolean enableAnsiOutput() throws IOException {
        LoadWindowsLibrary.ensureInitialized();
        String value = enableAnsiOutputNative();
        if (value.startsWith("error:"))
            throw new IOException("Error enabling ANSI output: " + value.substring("error:".length()));
        return Boolean.parseBoolean(value);
    }

}
