package coursier.jniutils;

import java.io.IOException;

/**
 * Windows console helpers, acting on the console of the standard output of the current process,
 * or of its standard error for the {@code stderr…} methods.
 */
public final class WindowsAnsiTerminal {

    // Values of the nStdHandle parameter of GetStdHandle
    private static final int STD_ERROR_HANDLE = -12;

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
        return parseTerminalSize(NativeApi.get().terminalSize());
    }

    /**
     * Same as {@link #terminalSize()}, but for the console of the standard error.
     *
     * @throws IOException if the standard error isn't a console (when it's redirected, say),
     *                     or if the process has no standard error
     */
    public static TerminalSize stderrTerminalSize() throws IOException {
        return parseTerminalSize(NativeApi.get().terminalSizeForStdHandle(STD_ERROR_HANDLE));
    }

    private static TerminalSize parseTerminalSize(String value) throws IOException {
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
        return parseEnableAnsiOutput(NativeApi.get().enableAnsiOutput());
    }

    /**
     * Same as {@link #enableAnsiOutput()}, but for the console of the standard error.
     *
     * Useful when the standard output is redirected but the standard error isn't, as the
     * console mode is changed through the handle of the standard error.
     *
     * @return whether the console mode needed to be changed or not
     * @throws IOException if the standard error isn't a console (when it's redirected, say),
     *                     or its mode can't be changed
     */
    public static boolean enableStderrAnsiOutput() throws IOException {
        return parseEnableAnsiOutput(NativeApi.get().enableAnsiOutputForStdHandle(STD_ERROR_HANDLE));
    }

    private static boolean parseEnableAnsiOutput(String value) throws IOException {
        if (value.startsWith("error:"))
            throw new IOException("Error enabling ANSI output: " + value.substring("error:".length()));
        return Boolean.parseBoolean(value);
    }

}
