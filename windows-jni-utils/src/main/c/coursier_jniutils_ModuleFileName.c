#define WINVER 0x0600
#define _WIN32_WINNT 0x0600

#include <windows.h>
#include <libloaderapi.h>
#include <stdlib.h>
#include "coursier_jniutils_DefaultNativeApi.h"

jstring _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(JNIEnv *, const char *);

// Windows paths can't be longer than that (including the terminating NUL)
#define MODULE_FILE_NAME_MAX_SIZE 32768

JNIEXPORT jstring JNICALL Java_coursier_jniutils_DefaultNativeApi_GetModuleFileNameNative
  (JNIEnv *env, jclass cls) {

  DWORD size = MAX_PATH;
  DWORD len;
  WCHAR *buf;
  jstring res;

  for (;;) {
    buf = (WCHAR *) malloc(size * sizeof(WCHAR));
    if (buf == NULL)
      return (*env)->NewStringUTF(env, "error:GetModuleFileName: cannot allocate memory");

    len = GetModuleFileNameW(NULL, buf, size);
    if (len == 0) {
      // compute the error message before free, so that GetLastError isn't clobbered
      res = _Java_coursier_jniutils_DefaultNativeApi_Terminal_lastError(env, "GetModuleFileName");
      free(buf);
      return res;
    }

    // If the buffer is too small, GetModuleFileNameW returns size, and truncates the path
    if (len < size) {
      res = (*env)->NewString(env, (const jchar *) buf, (jsize) len);
      free(buf);
      return res;
    }

    free(buf);

    if (size >= MODULE_FILE_NAME_MAX_SIZE)
      return (*env)->NewStringUTF(env, "error:GetModuleFileName: path too long");

    size *= 2;
    if (size > MODULE_FILE_NAME_MAX_SIZE)
      size = MODULE_FILE_NAME_MAX_SIZE;
  }
}

