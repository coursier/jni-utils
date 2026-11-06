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
