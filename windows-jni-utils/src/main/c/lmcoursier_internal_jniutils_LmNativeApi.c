#include "lmcoursier_internal_jniutils_LmNativeApi.h"
#include "coursier_jniutils_DefaultNativeApi.h"

JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_terminalSizeNative
  (JNIEnv *env, jclass class) {
  return Java_coursier_jniutils_DefaultNativeApi_terminalSizeNative(env, class);
}

JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_enableAnsiOutputNative
  (JNIEnv *env, jclass class) {
  return Java_coursier_jniutils_DefaultNativeApi_enableAnsiOutputNative(env, class);
}

JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_GetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {
  return Java_coursier_jniutils_DefaultNativeApi_GetUserEnvironmentVariableNative(env, class, key);
}

JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_SetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key, jbyteArray value) {
  return Java_coursier_jniutils_DefaultNativeApi_SetUserEnvironmentVariableNative(env, class, key, value);
}

JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_DeleteUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {
  return Java_coursier_jniutils_DefaultNativeApi_DeleteUserEnvironmentVariableNative(env, class, key);
}

JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_LmNativeApi_GetKnownFolderPathNative
  (JNIEnv *env, jclass class, jstring rfid) {
  return Java_coursier_jniutils_DefaultNativeApi_GetKnownFolderPathNative(env, class, rfid);
}
