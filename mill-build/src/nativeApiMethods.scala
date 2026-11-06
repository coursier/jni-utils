package millbuild

import millbuild.NativeApiGen.{JType, Method}

object NativeApiMethods {

  /**
   * The methods of `coursier.jniutils.NativeApi`.
   *
   * To add a native function:
   * - add an abstract method to `NativeApi`,
   * - add it here too, so that the `NativeApi` implementations, the JNI headers, and the C
   *   functions forwarding the calls of the relocated implementations, get generated,
   * - implement `Java_coursier_jniutils_DefaultNativeApi_<name>Native` in
   *   `windows-jni-utils/src/main/c`, including the generated `coursier_jniutils_DefaultNativeApi.h`.
   */
  val methods: Seq[Method] = Seq(
    Method("terminalSize", Nil, JType.String),
    Method("enableAnsiOutput", Nil, JType.String),
    Method("terminalSizeForStdHandle", Seq("stdHandle" -> JType.Int), JType.String),
    Method("enableAnsiOutputForStdHandle", Seq("stdHandle" -> JType.Int), JType.String),

    Method("GetUserEnvironmentVariable", Seq("key" -> JType.String), JType.String),
    Method("SetUserEnvironmentVariable", Seq("key" -> JType.String, "value" -> JType.String), JType.String),
    Method("DeleteUserEnvironmentVariable", Seq("key" -> JType.String), JType.String),

    Method("GetKnownFolderPath", Seq("rfid" -> JType.String), JType.String),

    Method("GetModuleFileName", Nil, JType.String),

    Method("GetOEMCodePage", Nil, JType.Int),
    Method("GetACP", Nil, JType.Int),
    Method("GetConsoleOutputCP", Nil, JType.Int)
  )
}
