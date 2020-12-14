// Support for CentOS 6
#if __linux__ && !__arm__ && !__aarch64__ // Waterfall
__asm__(".symver memcpy,memcpy@GLIBC_2.2.5");
#endif // Waterfall

#include <stdlib.h>
#include <string.h>

#include <mbedtls/aes.h>
#include "net_md_5_bungee_jni_cipher_NativeCipherImpl.h"

typedef unsigned char byte;

// Waterdog start
#include <mbedtls/sha256.h>

#ifdef __APPLE__
    #define htole64(x) x
#else
    #include <endian.h>
#endif
// Waterdog end

struct crypto_context {
    int mode;
    mbedtls_aes_context cipher;
    byte *key;
    mbedtls_sha256_context sha256; // Waterdog
};

jlong JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_init(JNIEnv* env, jobject obj, jboolean forEncryption, jbyteArray key) {
    jsize keyLen = env->GetArrayLength(key);
    jbyte *keyBytes = env->GetByteArrayElements(key, NULL);

    crypto_context *crypto = (crypto_context*) malloc(sizeof (crypto_context));
    mbedtls_aes_init(&crypto->cipher);

    mbedtls_aes_setkey_enc(&crypto->cipher, (byte*) keyBytes, keyLen * 8);

    crypto->key = (byte*) malloc(keyLen);
    memcpy(crypto->key, keyBytes, keyLen);

    crypto->mode = (forEncryption) ? MBEDTLS_AES_ENCRYPT : MBEDTLS_AES_DECRYPT;

    // Waterdog start
    mbedtls_sha256_init(&crypto->sha256);
    mbedtls_sha256_starts_ret(&crypto->sha256, 0);
    // Waterdog end

    env->ReleaseByteArrayElements(key, keyBytes, JNI_ABORT);
    return (jlong) crypto;
}

void Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_free(JNIEnv* env, jobject obj, jlong ctx) {
    crypto_context *crypto = (crypto_context*) ctx;

    mbedtls_aes_free(&crypto->cipher);
    free(crypto->key);
    free(crypto);
}

void Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_cipher(JNIEnv* env, jobject obj, jlong ctx, jlong in, jlong out, jint length) {
    crypto_context *crypto = (crypto_context*) ctx;

    mbedtls_aes_crypt_cfb8(&crypto->cipher, crypto->mode, length, crypto->key, (byte*) in, (byte*) out);
}

// Waterdog start
#define SHA256_CTX(ctx) (&((crypto_context *)ctx)->sha256)

jint throwException(JNIEnv *env, const char* message, int err) {
    // These can't be static for some unknown reason
    jclass exceptionClass = env->FindClass("net/md_5/bungee/jni/NativeCodeException");
    jmethodID exceptionInitID = env->GetMethodID(exceptionClass, "<init>", "(Ljava/lang/String;I)V");

    jstring jMessage = env->NewStringUTF(message);

    jthrowable throwable = (jthrowable) env->NewObject(exceptionClass, exceptionInitID, jMessage, err);
    return env->Throw(throwable);
}

void JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_staticPEHash(JNIEnv *env, jclass clazz,
        jlong counter, jlong in, jint inLength, jlong key, jlong out) {
    int ret;
    mbedtls_sha256_context ctx;

    counter = htole64(counter);

    //do everything in one call using stack allocation
    mbedtls_sha256_init(&ctx);
    if((ret = mbedtls_sha256_starts_ret(&ctx, 0)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)&counter, 8)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)in, inLength)) != 0) goto err;
    if((ret = mbedtls_sha256_update_ret(&ctx, (byte *)key, 32)) != 0) goto err;
    if((ret = mbedtls_sha256_finish_ret(&ctx, (byte *)out)) != 0) goto err;

    return;

err:
    throwException(env, "Could not complete static SHA256 hash: ", ret);
}

void JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_update__JB(JNIEnv *env, jobject thiz, jlong ctx, jbyte in) {
    int ret = mbedtls_sha256_update_ret(SHA256_CTX(ctx), (byte *)&in, 1);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_updateLongLE(JNIEnv *env, jobject thiz, jlong ctx, jlong in) {
    in = htole64(in);

    int ret = mbedtls_sha256_update_ret(SHA256_CTX(ctx), (byte *)&in, 8);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_update__JJI(JNIEnv *env, jobject thiz, jlong ctx, jlong in, jint len) {
    int ret = mbedtls_sha256_update_ret(SHA256_CTX(ctx), (byte *)in, len);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not update SHA256 hash: ", ret);
}

void JNICALL Java_net_md_15_bungee_jni_cipher_NativeCipherImpl_digest(JNIEnv *env, jobject thiz, jlong ctx, jlong out) {
    int ret;

    ret = mbedtls_sha256_finish_ret(SHA256_CTX(ctx), (byte *)out);
    if (ret != 0) goto err;

    ret = mbedtls_sha256_starts_ret(SHA256_CTX(ctx), 0);
    if (ret != 0) goto err;

    return;

err:
    throwException(env, "Could not finish SHA256 hash: ", ret);
}
// Waterdog end
