package net.md_5.bungee.jni.cipher;

class NativeCipherImpl
{

    native long init(boolean forEncryption, byte[] key);

    native void free(long ctx);

    native void cipher(long ctx, long in, long out, int length);

    // Waterdog start
    native void update(long ctx, byte in);
    native void updateLongLE(long ctx, long in);
    native void update(long ctx, long in, int length);

    native void digest(long ctx, long out);

    native static void staticPEHash(long counter, long in, int inLength, long key, long out);
    // Waterdog end
}
