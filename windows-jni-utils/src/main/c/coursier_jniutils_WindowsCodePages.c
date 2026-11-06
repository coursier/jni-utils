#define WINVER 0x0600
#define _WIN32_WINNT 0x0600
#define UNICODE

#include <windows.h>
#include "coursier_jniutils_DefaultNativeApi.h"

/* The system OEM code page, which is what a freshly opened console - and so cmd.exe
   reading a batch file - uses. Looked up at run time on purpose: the JDK properties that
   describe encodings (file.encoding, sun.jnu.encoding, native.encoding) are frozen at
   build time in a GraalVM native image, and none of them reports the OEM code page anyway. */
JNIEXPORT jint JNICALL Java_coursier_jniutils_DefaultNativeApi_GetOEMCodePageNative
  (JNIEnv *env, jclass class) {
  UNREFERENCED_PARAMETER(env);
  UNREFERENCED_PARAMETER(class);
  return (jint) GetOEMCP();
}

/* The system ANSI code page, used by the "A" variants of the Windows API functions. It's
   the one sun.jnu.encoding reflects, but that property is frozen in native images. */
JNIEXPORT jint JNICALL Java_coursier_jniutils_DefaultNativeApi_GetACPNative
  (JNIEnv *env, jclass class) {
  UNREFERENCED_PARAMETER(env);
  UNREFERENCED_PARAMETER(class);
  return (jint) GetACP();
}

/* The output code page of the console of the current process, as changed by chcp say.
   GetConsoleOutputCP returns 0 if the process has no console. */
JNIEXPORT jint JNICALL Java_coursier_jniutils_DefaultNativeApi_GetConsoleOutputCPNative
  (JNIEnv *env, jclass class) {
  UNREFERENCED_PARAMETER(env);
  UNREFERENCED_PARAMETER(class);
  return (jint) GetConsoleOutputCP();
}
