/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class net_java_games_input_RawInputEventQueue */

#ifndef _Included_net_java_games_input_RawInputEventQueue
#define _Included_net_java_games_input_RawInputEventQueue
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     net_java_games_input_RawInputEventQueue
 * Method:    nPoll
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_net_java_games_input_RawInputEventQueue_nPoll
  (JNIEnv *, jobject, jlong);

/*
 * Class:     net_java_games_input_RawInputEventQueue
 * Method:    nRegisterDevices
 * Signature: (IJ[Lnet/java/games/input/RawDeviceInfo;)V
 */
JNIEXPORT void JNICALL Java_net_java_games_input_RawInputEventQueue_nRegisterDevices
  (JNIEnv *, jclass, jint, jlong, jobjectArray);

#ifdef __cplusplus
}
#endif
#endif
