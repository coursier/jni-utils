#include "coursier_bootstrap_launcher_jniutils_NativeCalls.h"
#include "coursier_jniutils_NativeCalls.h"

JNIEXPORT jstring JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_terminalSizeNative
  (JNIEnv *env, jclass class) {
  return Java_coursier_jniutils_NativeCalls_terminalSizeNative(env, class);
}

JNIEXPORT jstring JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_enableAnsiOutputNative
  (JNIEnv *env, jclass class) {
  return Java_coursier_jniutils_NativeCalls_enableAnsiOutputNative(env, class);
}

JNIEXPORT jbyteArray JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_GetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {
  return Java_coursier_jniutils_NativeCalls_GetUserEnvironmentVariableNative(env, class, key);
}

JNIEXPORT jbyteArray JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_SetUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key, jbyteArray value) {
  return Java_coursier_jniutils_NativeCalls_SetUserEnvironmentVariableNative(env, class, key, value);
}

JNIEXPORT jbyteArray JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_DeleteUserEnvironmentVariableNative
  (JNIEnv *env, jclass class, jbyteArray key) {
  return Java_coursier_jniutils_NativeCalls_DeleteUserEnvironmentVariableNative(env, class, key);
}

JNIEXPORT jstring JNICALL Java_coursier_bootstrap_launcher_jniutils_NativeCalls_GetKnownFolderPathNative
  (JNIEnv *env, jclass class, jstring rfid) {
  return Java_coursier_jniutils_NativeCalls_GetKnownFolderPathNative(env, class, rfid);
}
