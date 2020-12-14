package network.ycc.waterdog.pe;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSVerifierFactory;
import com.nimbusds.jose.jwk.Curve;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;

import network.ycc.waterdog.pe.packet.PEHandshake;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PEEncryptionUtils {
    public static final String MOJANG_KEY = "MHYwEAYHKoZIzj0CAQYFK4EEACIDYgAE8ELkixyLcwlZryUQcu1TvPOmI2B7vX83ndnWRUaXm74wFfa5f/lwQNTfrLVHa2PmenpGI6JhIMUJaWZrjmMj90NoKNFSNBuKdm8rYiXsfaz3K36x/1U26HpG0ZxK/V1V";

    static final Gson GSON;
    static final DefaultJWSVerifierFactory jwsVerifierFactory;
    static final KeyPair keyPair;
    static final KeyFactory keyfactory;
    static final PublicKey mojangPublicKey;

    static {
        GSON = new GsonBuilder().setPrettyPrinting().create();
        jwsVerifierFactory = new DefaultJWSVerifierFactory();
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
            gen.initialize(Curve.P_384.toECParameterSpec());
            keyPair = gen.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Unable to generate private keypair", e);
        }
        try {
            keyfactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to init key factory", e);
        }
        try {
            mojangPublicKey = parseKey(MOJANG_KEY);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException("Unable to init mojang key", e);
        }
    }

    public static void parseHandshake(PEHandshake handshake) {
        final ByteBuf buf = Unpooled.wrappedBuffer(handshake.getHandshakeData());
        final int protocolVersion = buf.readInt() - ProtocolConstants.PE_PROTOCOL_OFFSET;
        final ByteBuf loginData = buf.readSlice(DefinedPacket.readVarInt(buf));
        final ByteBuf chainDataBytes = loginData.readSlice(loginData.readIntLE());
        final ByteBuf jwsDataBytes = loginData.readSlice(loginData.readIntLE());
        final Pair<PublicKey, JsonObject> chainData = extractChainData(GSON.fromJson(
                new InputStreamReader(new ByteBufInputStream(chainDataBytes)),
                new TypeToken<Map<String, List<String>>>() {}.getType()
        ));
        final PublicKey key = chainData.getLeft();
        final JsonObject identityData = chainData.getRight();
        final String jwsString = jwsDataBytes.readCharSequence(jwsDataBytes.readableBytes(), Charsets.UTF_8).toString();
        final JWSObject additionalData;
        try {
            additionalData = JWSObject.parse(jwsString);
        } catch (ParseException e) {
            throw new DecoderException("Failed to parse jws string: " + jwsString, e);
        }
        final JsonObject clientInfo = GSON.fromJson(additionalData.getPayload().toString(), JsonObject.class);
        final String serverAddr = clientInfo.get("ServerAddress").getAsString();
        Preconditions.checkArgument(serverAddr != null, "ServerAddress is missing");
        final HostAndPort hostAndPort = HostAndPort.fromString(serverAddr);

        clientInfo.addProperty("Waterdog_OriginalUUID", getString(identityData, "identity"));

        handshake.setClientInfo(clientInfo);
        handshake.setUsername(getString(identityData, "displayName"));
        if (BungeeCord.getInstance().config.isReplaceUsernameSpaces()) {
            handshake.setUsername(handshake.getUsername().replaceAll(" ", "_"));
        }
        handshake.setUuid(UUID.fromString(getString(identityData, "identity")));
        handshake.setLoginUUID(handshake.getUuid());
        handshake.setAuthorized(key != null);
        if (handshake.isAuthorized()) {
            handshake.setPublicKey(key);
            handshake.setXuid(new BigInteger(getString(identityData, "XUID")));
            if (BungeeCord.getInstance().config.isUseXUIDForUUID()) {
                handshake.setUuid(new UUID(0, handshake.getXuid().longValue()));
            }
            Preconditions.checkArgument(handshake.getXuid().signum() == 1, "XUID is negative or zero");
        }
        handshake.setHost(hostAndPort.getHost());
        if (hostAndPort.hasPort()) {
            handshake.setPort(hostAndPort.getPort());
        }
        handshake.setProtocolVersion(protocolVersion);
        handshake.setRequestedProtocol(2);
    }

    public static void createHandshake(PEHandshake handshake) {
        final ByteBuf buf = Unpooled.buffer(512);
        final byte[] identityData = createIdentityData(handshake.getUsername(),
                handshake.getUuid(), handshake.getXuid()).getBytes(StandardCharsets.UTF_8);
        final byte[] clientData = createClientData(handshake.getClientInfo(),
                handshake.getHost(), handshake.getPort(), handshake.getUuid()).getBytes(StandardCharsets.UTF_8);
        buf.writeInt(handshake.getProtocolVersion() + ProtocolConstants.PE_PROTOCOL_OFFSET);
        DefinedPacket.writeVarInt(identityData.length + clientData.length + 8, buf);
        buf.writeIntLE(identityData.length);
        buf.writeBytes(identityData);
        buf.writeIntLE(clientData.length);
        buf.writeBytes(clientData);
        handshake.setHandshakeData(new byte[buf.readableBytes()]);
        buf.readBytes(handshake.getHandshakeData());
    }

    ////// READING

    static Pair<PublicKey, JsonObject> extractChainData(Map<String, List<String>> maindata) {
        final List<String> chain = maindata.get("chain");
        try {
            boolean foundMojangKey = false;
            boolean signatureValid = false;
            PublicKey key = mojangPublicKey;
            for (String element : chain) {
                final JWSObject jwsobject = JWSObject.parse(element);
                if (!foundMojangKey && jwsobject.getHeader().getX509CertURL().toString().equals(MOJANG_KEY)) {
                    foundMojangKey = true;
                    signatureValid = true;
                }
                if (foundMojangKey && !verify(jwsobject, key)) {
                    signatureValid = false;
                }
                final JsonObject jsonobject = GSON.fromJson(jwsobject.getPayload().toString(), JsonObject.class);
                key = parseKey(getString(jsonobject, "identityPublicKey"));
                if (jsonobject.has("extraData")) {
                    return new ImmutablePair<>(signatureValid ? key : null, getJsonObject(jsonobject, "extraData"));
                }
            }
        } catch (InvalidKeySpecException | JOSEException e) {
            throw new DecoderException("Unable to decode login chain", e);
        } catch (ParseException e) {
            throw new DecoderException("Unable to parse: " + maindata, e);
        }
        throw new DecoderException("Unable to find extraData");
    }

    static String getString(JsonObject jsonObject, String name) {
        if (jsonObject.has(name)) {
            return getAsString(jsonObject.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find a string");
    }

    static String getAsString(JsonElement jsonElement, String name) {
        if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a string, was " + toString(jsonElement));
    }

    static JsonObject getAsJsonObject(JsonElement jsonElement, String name) {
        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject();
        }
        throw new JsonSyntaxException("Expected " + name + " to be a JsonObject, was " + toString(jsonElement));
    }

    static JsonObject getJsonObject(JsonObject jsonObject, String name) {
        if (jsonObject.has(name)) {
            return getAsJsonObject(jsonObject.get(name), name);
        }
        throw new JsonSyntaxException("Missing " + name + ", expected to find an Object");
    }

    static String toString(JsonElement jsonElement) {
        final String abbreviateMiddle = String.valueOf(jsonElement);
        if (jsonElement == null) {
            return "null (missing)";
        }
        if (jsonElement.isJsonNull()) {
            return "null (json)";
        }
        if (jsonElement.isJsonArray()) {
            return "an array (" + abbreviateMiddle + ")";
        }
        if (jsonElement.isJsonObject()) {
            return "an object (" + abbreviateMiddle + ")";
        }
        if (jsonElement.isJsonPrimitive()) {
            final JsonPrimitive asJsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (asJsonPrimitive.isNumber()) {
                return "a number (" + abbreviateMiddle + ")";
            }
            if (asJsonPrimitive.isBoolean()) {
                return "a boolean (" + abbreviateMiddle + ")";
            }
        }
        return abbreviateMiddle;
    }

    static boolean verify(JWSObject object, PublicKey key) throws JOSEException {
        return object.verify(jwsVerifierFactory.createJWSVerifier(object.getHeader(), key));
    }

    static public PublicKey parseKey(String key) throws InvalidKeySpecException {
        return keyfactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(key)));
    }

    ////// WRITING

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    @SuppressWarnings("serial")
    static String createIdentityData(String username, UUID uuid, BigInteger xuid) {
        final Map<String, List<String>> chainmap = new HashMap<>();
        final long iat = System.currentTimeMillis() / 1000;
        final long exp = iat + 24 * 3600;
        final JsonObject extraData = new JsonObject();
        final JsonObject dataChain = new JsonObject();
        extraData.addProperty("identity", uuid.toString());
        extraData.addProperty("displayName", username);
        extraData.addProperty("XUID", xuid == null ? "" : xuid.toString());
        dataChain.addProperty("nbf", iat - 3600);
        dataChain.addProperty("exp", exp);
        dataChain.addProperty("iat", iat);
        dataChain.addProperty("iss", "self");
        dataChain.addProperty("certificateAuthority", true);
        dataChain.add("extraData", extraData);
        dataChain.addProperty("randomNonce", UUID.randomUUID().getLeastSignificantBits());
        dataChain.addProperty("identityPublicKey", Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
        chainmap.put("chain", Collections.singletonList(encodeJWT(dataChain)));
        return GSON.toJson(chainmap, new TypeToken<Map<String, List<String>>>() {}.getType());
    }

    static String encodeJWT(JsonObject payload) {
        try {
            final JWSObject jwsobject = new JWSObject(
                    new JWSHeader.Builder(JWSAlgorithm.ES384)
                            .x509CertURL(new URI(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())))
                            .build(),
                    new Payload(GSON.toJson(payload))
            );
            jwsobject.sign(new ECDSASigner(keyPair.getPrivate(), Curve.P_384));
            return jwsobject.serialize();
        } catch (Exception e) {
            throw new EncoderException("Unable to encode jwt", e);
        }
    }

    static String createClientData(JsonObject baseClientInfo, String host, int port, UUID uuid) {
        final JsonObject out = new JsonObject();
        baseClientInfo.entrySet().forEach(entry -> out.add(entry.getKey(), entry.getValue()));
        out.addProperty("ServerAddress", host + ":" + port);
        out.addProperty("SelfSignedId", uuid.toString());
        return encodeJWT(out);
    }
}
