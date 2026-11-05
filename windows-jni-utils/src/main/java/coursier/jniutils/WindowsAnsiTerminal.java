package coursier.jniutils;

import java.io.IOException;

/** Windows console helpers, acting on the console of the standard output of the current process. */
public final class WindowsAnsiTerminal {

    /**
     * The size of the visible console window, in characters.
     *
     * This is the visible part of the console screen buffer, so its height is the number of
     * visible lines, not including the scrollback.
     *
     * @throws IOException if the standard output isn't a console (when it's redirected, say),
     *                     or if the process has no standard output
     */
    public static TerminalSize terminalSize() throws IOException {
        String value = NativeApi.get().terminalSize();
        if (value.startsWith("error:"))
            throw new IOException("Error getting terminal size: " + value.substring("error:".length()));
        String[] elems = value.split(":", 2);
        if (elems.length != 2)
            throw new RuntimeException("Unexpected output from native method: '" + value + "'");
        return new TerminalSize(Integer.parseInt(elems[0]), Integer.parseInt(elems[1]));
    }

    /**
     * Enables the processing of ANSI escape sequences by the console
     * (ENABLE_VIRTUAL_TERMINAL_PROCESSING), if it isn't already.
     *
     * @return whether the console mode needed to be changed or not
     * @throws IOException if the standard output isn't a console (when it's redirected, say),
     *                     or its mode can't be changed
     */
    public static boolean enableAnsiOutput() throws IOException {
        String value = NativeApi.get().enableAnsiOutput();
        if (value.startsWith("error:"))
            throw new IOException("Error enabling ANSI output: " + value.substring("error:".length()));
        return Boolean.parseBoolean(value);
    }

}
