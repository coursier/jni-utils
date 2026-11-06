package coursier.jniutils;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public final class WindowsCodePages {

    /**
     * The system OEM code page.
     *
     * This is the code page a freshly opened console gets, and therefore the one cmd.exe
     * reads batch files with unless something called chcp. It is not the ANSI code page
     * that sun.jnu.encoding reflects, and the two commonly differ (say 437 and 1252).
     */
    public static int oem() {
        return NativeApi.get().GetOEMCodePage();
    }

    /**
     * The system ANSI code page.
     *
     * This is the code page of the "A" variants of the Windows API functions, which
     * sun.jnu.encoding reflects (but that property is frozen at build time in a GraalVM
     * native image), say 1252.
     */
    public static int ansi() {
        return NativeApi.get().GetACP();
    }

    /**
     * The output code page of the console of the current process, or 0 if the process has
     * no console.
     *
     * This is the OEM code page by default, unless something called chcp (or
     * SetConsoleOutputCP).
     */
    public static int consoleOutput() {
        return NativeApi.get().GetConsoleOutputCP();
    }

    /**
     * The {@link Charset} for a Windows code page, or null if this JVM has no charset for it.
     *
     * Beware that in a GraalVM native image, only the charsets included at build time can be
     * looked up here - pass -H:+AddAllCharsets if arbitrary code pages need to resolve.
     */
    public static Charset charsetFor(int codePage) {
        switch (codePage) {
            case 65001: return StandardCharsets.UTF_8;
            case 1200:  return StandardCharsets.UTF_16LE;
            case 1201:  return StandardCharsets.UTF_16BE;
            default:
        }
        // Java has no single naming scheme for code page charsets: 1252 is windows-1252,
        // 437 is IBM437, 932 is windows-31j (aliased MS932), 949 is x-windows-949...
        // The Microsoft spellings come first on purpose. Several code pages also resolve
        // under a "cp" or "IBM" name, but to IBM's variant of the encoding rather than the
        // one Windows actually uses - "cp932" is x-IBM942C, not windows-31j.
        String[] candidates = {
            "windows-" + codePage,
            "MS" + codePage,
            "x-windows-" + codePage,
            "cp" + codePage,
            "IBM" + codePage
        };
        for (String name : candidates) {
            try {
                return Charset.forName(name);
            } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                // try the next spelling
            }
        }
        return null;
    }

    /** The {@link Charset} for {@link #oem()}, or null if this JVM has no charset for it. */
    public static Charset oemCharset() {
        return charsetFor(oem());
    }

    /** The {@link Charset} for {@link #ansi()}, or null if this JVM has no charset for it. */
    public static Charset ansiCharset() {
        return charsetFor(ansi());
    }

    /**
     * The {@link Charset} for {@link #consoleOutput()}, or null if the process has no console,
     * or if this JVM has no charset for its code page.
     */
    public static Charset consoleOutputCharset() {
        int codePage = consoleOutput();
        return codePage == 0 ? null : charsetFor(codePage);
    }
}
