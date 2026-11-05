#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <windows.h>
#include "coursier_jniutils_DefaultNativeApi.h"

static void _Java_coursier_jniutils_DefaultNativeApi_Terminal_throwOutOfMemory(JNIEnv *env) {
  jclass cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
  if (cls != NULL)
    (*env)->ThrowNew(env, cls, "Cannot allocate terminal message buffer");
}

/* Returns NULL, with an exception pending, if the message buffer can't be allocated. */
jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError
  (JNIEnv *env, const char *origin) {

  DWORD err = GetLastError();
  char dummy[1];
  const int len = snprintf(dummy, 1, "error:%s error %lu", origin, err);
  if (len < 0)
    return (*env)->NewStringUTF(env, "error:cannot format error message");
  char *buf = malloc((size_t) len + 1);
  if (buf == NULL) {
    _Java_coursier_jniutils_DefaultNativeApi_Terminal_throwOutOfMemory(env);
    return NULL;
  }
  snprintf(buf, (size_t) len + 1, "error:%s error %lu", origin, err);
  const jstring result = (*env)->NewStringUTF(env, buf);
  free(buf);
  return result;
}

/* GetStdHandle returns NULL, without setting the last error, when the process has
   no standard output at all (e.g. a GUI process started without one). */
static jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError
  (JNIEnv *env, HANDLE handle) {

  if (handle == NULL)
    return (*env)->NewStringUTF(env, "error:GetStdHandle: no standard output handle");
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetStdHandle");
}

jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_format
  (JNIEnv *env, const char *format, ...) {

  char dummy[1];
  va_list ap;
  va_start(ap, format);
  const int len = vsnprintf(dummy, 1, format, ap);
  va_end(ap);
  if (len < 0)
    return (*env)->NewStringUTF(env, "error:cannot format output");
  char *buf = malloc((size_t) len + 1);
  if (buf == NULL) {
    _Java_coursier_jniutils_DefaultNativeApi_Terminal_throwOutOfMemory(env);
    return NULL;
  }
  va_start(ap, format);
  vsnprintf(buf, (size_t) len + 1, format, ap);
  va_end(ap);
  const jstring result = (*env)->NewStringUTF(env, buf);
  free(buf);
  return result;
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_terminalSizeNative
  (JNIEnv *env, jclass class) {

  HANDLE handle = GetStdHandle(STD_OUTPUT_HANDLE);
  if (handle == INVALID_HANDLE_VALUE || handle == NULL) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError(env, handle);
  }

  CONSOLE_SCREEN_BUFFER_INFO info;
  memset(&info, 0, sizeof(CONSOLE_SCREEN_BUFFER_INFO));
  BOOL res = GetConsoleScreenBufferInfo(handle, &info);
  if (!res) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetConsoleScreenBufferInfo");
  }

  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_format(env, "%hu:%hu", info.dwSize.X, info.dwSize.Y);
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_enableAnsiOutputNative
  (JNIEnv *env, jclass class) {

  HANDLE handle = GetStdHandle(STD_OUTPUT_HANDLE);
  if (handle == INVALID_HANDLE_VALUE || handle == NULL) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError(env, handle);
  }

  DWORD mode = 0;
  BOOL res = GetConsoleMode(handle, &mode);
  if (!res) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetConsoleMode");
  }

  char *ret = "false";
  if (!(mode & ENABLE_VIRTUAL_TERMINAL_PROCESSING)) {
    res = SetConsoleMode(handle, mode | ENABLE_VIRTUAL_TERMINAL_PROCESSING);
    if (!res) {
      return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "SetConsoleMode");
    }
    ret = "true";
  }

  return (*env)->NewStringUTF(env, ret);
}
