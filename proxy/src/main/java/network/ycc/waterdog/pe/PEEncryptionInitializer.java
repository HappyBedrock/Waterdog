package network.ycc.waterdog.pe;

import com.google.gson.JsonObject;

import com.nimbusds.jose.JWSObject;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.util.ReferenceCountUtil;

import lombok.Getter;

import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.jni.cipher.NativeCipher;
import net.md_5.bungee.netty.PipelineUtils;

import network.ycc.waterdog.pe.packet.PEEncryptionRequest;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.SecretKeySpec;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Base64;

public class PEEncryptionInitializer extends ChannelInitializer<Channel> {
    public static final int HASH_LENGTH = 8;
    private static final SecureRandom numberGenerator = new SecureRandom();

    @Getter
    private final PEEncryptionRequest request;
    private final byte[] keyBytes;
    private final SecretKeySpec serverKey;
    private ByteBuf hashBuffer;
    private ByteBuf keyBuffer;
    private int hashBufferReader;

    public PEEncryptionInitializer(PublicKey publicKey) throws NoSuchAlgorithmException, InvalidKeyException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final JsonObject additionalData = new JsonObject();
        final String saltString = createSalt();
        final KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(PEEncryptionUtils.getKeyPair().getPrivate());
        ka.doPhase(publicKey, true);
        digest.update(Base64.getDecoder().decode(saltString));
        digest.update(ka.generateSecret());
        additionalData.addProperty("salt", saltString);
        keyBytes = digest.digest();
        serverKey = new SecretKeySpec(keyBytes, "AES");
        request = new PEEncryptionRequest(PEEncryptionUtils.encodeJWT(additionalData));
    }

    public PEEncryptionInitializer(PEEncryptionRequest request) throws NoSuchAlgorithmException, ParseException,
            InvalidKeySpecException, InvalidKeyException {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final JWSObject additionalData = JWSObject.parse(request.getJwtData());
        final PublicKey publicKey = PEEncryptionUtils.parseKey(additionalData.getHeader().getX509CertURL().toString());
        final String saltString = additionalData.getPayload().toJSONObject().getAsString("salt");
        final KeyAgreement ka = KeyAgreement.getInstance("ECDH");
        ka.init(PEEncryptionUtils.getKeyPair().getPrivate());
        ka.doPhase(publicKey, true);
        digest.update(Base64.getDecoder().decode(saltString));
        digest.update(ka.generateSecret());
        keyBytes = digest.digest();
        serverKey = new SecretKeySpec(keyBytes, "AES");
        this.request = request;
    }

    protected static String createSalt() {
        final byte[] out = new byte[32];
        numberGenerator.nextBytes(out);
        return Base64.getEncoder().encodeToString(out);
    }

    protected void initChannel(Channel ch) throws Exception {
        hashBuffer = ch.alloc().directBuffer(32, 32);
        keyBuffer = ch.alloc().directBuffer(32, 32).writeBytes(keyBytes);
        hashBufferReader = hashBuffer.readerIndex();

        ch.pipeline().addBefore(PEDecompressor.NAME, PipelineUtils.DECRYPT_HANDLER,
                new PECipherDecoder());
        ch.pipeline().addBefore(PECompressor.NAME, PipelineUtils.ENCRYPT_HANDLER,
                new PECipherEncoder());
    }

    protected void destroy() {
        hashBuffer.release();
        keyBuffer.release();
    }

    protected class PECipherDecoder extends ChannelInboundHandlerAdapter {
        private long counter = 0;
        private final BungeeCipher cipher;

        public PECipherDecoder() throws GeneralSecurityException {
            cipher = EncryptionUtil.getCipher(false, serverKey);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            cipher.free();
            destroy();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                //TODO: chunked in-place cipher seems to break, maybe not the hash?
                final ByteBuf data = condenseMaybe((ByteBuf) msg, ctx.alloc());
                //final ByteBuf data = (ByteBuf) msg;
                try {
                    final int readerIndex = data.readerIndex();
                    final int writerIndex = data.writerIndex();
                    PENativeExt.cipherInPlace(cipher, data);
                    createHash(cipher, data.readerIndex(readerIndex).writerIndex(writerIndex - HASH_LENGTH), counter++);
                    data.writerIndex(writerIndex);
                    while (hashBuffer.isReadable()) {
                        if (data.readByte() != hashBuffer.readByte()) {
                            throw new DecoderException("Corrupt encrypted data!");
                        }
                    }
                    data.readerIndex(readerIndex).writerIndex(writerIndex - HASH_LENGTH);
                    ctx.fireChannelRead(data.retain());
                } finally {
                    ReferenceCountUtil.safeRelease(data);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        protected ByteBuf condenseMaybe(ByteBuf buf, ByteBufAllocator alloc) {
            if (buf.nioBufferCount() > 1) {
                final ByteBuf out = alloc.directBuffer(buf.readableBytes());
                out.writeBytes(buf);
                buf.release();
                return out;
            } else {
                return buf;
            }
        }
    }

    protected class PECipherEncoder extends ChannelOutboundHandlerAdapter {
        private long counter = 0;
        private final BungeeCipher cipher;

        public PECipherEncoder() throws GeneralSecurityException {
            cipher = EncryptionUtil.getCipher(true, serverKey);
        }

        @Override
        public void handlerRemoved(ChannelHandlerContext ctx) {
            cipher.free();
        }

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            if (msg instanceof ByteBuf) {
                final ByteBuf data = (ByteBuf) msg;
                final CompositeByteBuf out = ctx.alloc().compositeDirectBuffer(2);
                final ByteBuf hashChunk = ctx.alloc().directBuffer(32, 32);
                try {
                    final int readerIndex = data.readerIndex();
                    createHash(cipher, data, counter++, hashChunk);
                    data.readerIndex(readerIndex);
                    PENativeExt.cipherInPlace(cipher, data);
                    PENativeExt.cipherInPlace(cipher, hashChunk);
                    out.addComponent(true, data.retain());
                    out.addComponent(true, hashChunk.retain());
                    ctx.write(out.retain(), promise);
                } finally {
                    ReferenceCountUtil.safeRelease(msg);
                    ReferenceCountUtil.safeRelease(out);
                    ReferenceCountUtil.safeRelease(hashChunk);
                }
            } else {
                ctx.write(msg, promise);
            }
        }
    }

    protected void createHash(BungeeCipher cipher, ByteBuf in, long counter) {
        hashBuffer.readerIndex(hashBufferReader);
        hashBuffer.writerIndex(hashBufferReader);

        createHash(cipher, in, counter, hashBuffer);
    }

    protected void createHash(BungeeCipher cipher, ByteBuf in, long counter, ByteBuf out) {
        final int readerIndex = out.readerIndex();
        if (cipher instanceof NativeCipher) {
            PENativeExt.staticPEHash((NativeCipher) cipher, counter, in, keyBuffer, out);
        } else {
            cipher.staticPEHash(counter, in, keyBuffer, out);
        }

        out.writerIndex(readerIndex + HASH_LENGTH); //need only the first 8 bytes
    }
}
