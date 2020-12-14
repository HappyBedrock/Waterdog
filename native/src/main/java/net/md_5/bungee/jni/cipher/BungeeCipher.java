package net.md_5.bungee.jni.cipher;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;

/**
 * Class to expose cipher methods from either native or fallback Java cipher.
 */
public interface BungeeCipher
{

    void init(boolean forEncryption, SecretKey key) throws GeneralSecurityException;

    void free();

    void cipher(ByteBuf in, ByteBuf out) throws GeneralSecurityException;

    ByteBuf cipher(ChannelHandlerContext ctx, ByteBuf in) throws GeneralSecurityException;

    // Waterdog start
    void update(byte in);
    void updateLongLE(long in);
    void update(ByteBuf in);

    void digest(ByteBuf out);

    void staticPEHash(long counter, ByteBuf in, ByteBuf key, ByteBuf out);
    // Waterdog end
}
