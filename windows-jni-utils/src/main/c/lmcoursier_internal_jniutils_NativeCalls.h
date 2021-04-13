/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class lmcoursier_internal_jniutils_NativeCalls */

#ifndef _Included_lmcoursier_internal_jniutils_NativeCalls
#define _Included_lmcoursier_internal_jniutils_NativeCalls
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    terminalSizeNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_terminalSizeNative
  (JNIEnv *, jclass);

/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    enableAnsiOutputNative
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_enableAnsiOutputNative
  (JNIEnv *, jclass);

/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    GetUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_GetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    SetUserEnvironmentVariableNative
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_SetUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    DeleteUserEnvironmentVariableNative
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_DeleteUserEnvironmentVariableNative
  (JNIEnv *, jclass, jbyteArray);

/*
 * Class:     lmcoursier_internal_jniutils_NativeCalls
 * Method:    GetKnownFolderPathNative
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_lmcoursier_internal_jniutils_NativeCalls_GetKnownFolderPathNative
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
