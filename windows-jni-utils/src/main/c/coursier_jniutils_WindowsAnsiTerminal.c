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
   no such standard handle at all (e.g. a GUI process started without one). */
static jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError
  (JNIEnv *env, DWORD stdHandle, HANDLE handle) {

  if (handle == NULL) {
    const char *msg = "error:GetStdHandle: no standard handle";
    if (stdHandle == STD_OUTPUT_HANDLE)
      msg = "error:GetStdHandle: no standard output handle";
    else if (stdHandle == STD_ERROR_HANDLE)
      msg = "error:GetStdHandle: no standard error handle";
    else if (stdHandle == STD_INPUT_HANDLE)
      msg = "error:GetStdHandle: no standard input handle";
    return (*env)->NewStringUTF(env, msg);
  }
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

/* stdHandle is one of the STD_*_HANDLE values accepted by GetStdHandle */
static jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_size
  (JNIEnv *env, DWORD stdHandle) {

  HANDLE handle = GetStdHandle(stdHandle);
  if (handle == INVALID_HANDLE_VALUE || handle == NULL) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError(env, stdHandle, handle);
  }

  CONSOLE_SCREEN_BUFFER_INFO info;
  memset(&info, 0, sizeof(CONSOLE_SCREEN_BUFFER_INFO));
  BOOL res = GetConsoleScreenBufferInfo(handle, &info);
  if (!res) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetConsoleScreenBufferInfo");
  }

  /* dwSize is the size of the whole screen buffer, whose height includes the scrollback
     (often 9001 lines); srWindow is the visible part of it, with inclusive bounds. */
  const int width = info.srWindow.Right - info.srWindow.Left + 1;
  const int height = info.srWindow.Bottom - info.srWindow.Top + 1;
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_format(env, "%d:%d", width, height);
}

/* stdHandle is one of the STD_*_HANDLE values accepted by GetStdHandle */
static jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_enableAnsiOutput
  (JNIEnv *env, DWORD stdHandle) {

  HANDLE handle = GetStdHandle(stdHandle);
  if (handle == INVALID_HANDLE_VALUE || handle == NULL) {
    return _Java_coursier_jniutils_DefaultNativeApi_Terminal_stdHandleError(env, stdHandle, handle);
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

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_terminalSizeNative
  (JNIEnv *env, jclass class) {
  UNREFERENCED_PARAMETER(class);
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_size(env, STD_OUTPUT_HANDLE);
}

/* The STD_*_HANDLE values are ((DWORD) -10) and the like, passed as negative ints from Java */
JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_terminalSizeForStdHandleNative
  (JNIEnv *env, jclass class, jint stdHandle) {
  UNREFERENCED_PARAMETER(class);
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_size(env, (DWORD) stdHandle);
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_enableAnsiOutputNative
  (JNIEnv *env, jclass class) {
  UNREFERENCED_PARAMETER(class);
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_enableAnsiOutput(env, STD_OUTPUT_HANDLE);
}

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_enableAnsiOutputForStdHandleNative
  (JNIEnv *env, jclass class, jint stdHandle) {
  UNREFERENCED_PARAMETER(class);
  return _Java_coursier_jniutils_DefaultNativeApi_Terminal_enableAnsiOutput(env, (DWORD) stdHandle);
}
