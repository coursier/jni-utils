package coursier.jniutils;

public final class ModuleFileName {

    public static String get() {
        return NativeApi.get().GetModuleFileName();
    }
}
