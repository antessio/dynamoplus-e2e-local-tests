package antessio.dynamoplus;

import antessio.dynamoplus.http.HttpConfiguration;
import antessio.dynamoplus.sdk.SDK;
import antessio.dynamoplus.sdk.SdkBuilder;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationHttpSignature;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Clients {
    private SDK adminClient;
    public static final HttpConfiguration HTTP_CONFIGURATION = new HttpConfiguration(30000, 30000, 30000);
    private String clientIdApiKeyReadOnly;
    private String clientIdHttpSignature;
    private String privateKey;
    private String publicKey;
    private String keyId;
    private String clientIdApiKey;
    private String clientIdHttpSignatureReadOnly;

    private Clients() throws NoSuchAlgorithmException {
        clientIdApiKeyReadOnly = "client-id-api-key-read-only";
        clientIdApiKey = "client-id-api-key";
        keyId = "api-key";
        clientIdHttpSignatureReadOnly = "client-id-http-signature-read-only";
        clientIdHttpSignature = "client-id-http-signature";
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        Key pub = kp.getPublic();
        Key pvt = kp.getPrivate();
        Base64.Encoder encoder = Base64.getEncoder();
        privateKey = "-----BEGIN RSA PRIVATE KEY-----\n" +
                encoder.encodeToString(pvt.getEncoded()) +
                "\n-----END RSA PRIVATE KEY-----\n";
        publicKey = "-----BEGIN RSA PUBLIC KEY-----\n" +
                encoder.encodeToString(pub.getEncoded()) +
                "\n-----END RSA PUBLIC KEY-----\n";
        String host = Optional.ofNullable(System.getenv("dynamoplus.host")).orElse("http://localhost:3000");
        System.out.println("host = " + host);
        adminClient = new SdkBuilder(host)
                .withHttpConfiguration(HTTP_CONFIGURATION)
                .withCredentialsProvider(
                        new SdkBuilder.CredentialsProviderBuilder()
                                .withBasicAuthCredentialsProviderBuilder()
                                .withUsername("root")
                                .withPassword("12345")
                                .build())
                .build();
    }

    private static Clients instance;

    public static Clients getIntance() {
        if (instance == null) {
            try {
                instance = new Clients();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    public String getClientIdApiKeyReadOnly() {
        return clientIdApiKeyReadOnly;
    }

    public String getClientIdHttpSignature() {
        return clientIdHttpSignature;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getClientIdApiKey() {
        return clientIdApiKey;
    }

    public String getClientIdHttpSignatureReadOnly() {
        return clientIdHttpSignatureReadOnly;
    }

    public SDK getAdminClient() {
        return adminClient;
    }


    public SDK createClientApiKey(String clientId, String apiKey, List<ClientScope> scopes) {
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientId, scopes, apiKey, Collections.emptyList());
        adminClient.createClientAuthorizationApiKey(clientAuthorization);
        String host = Optional.ofNullable(System.getenv("dynamoplus.host")).orElse("http://localhost:3000");
        return new SdkBuilder(host)
                .withHttpConfiguration(HTTP_CONFIGURATION)
                .withCredentialsProvider(
                        new SdkBuilder.CredentialsProviderBuilder()
                                .withApiKeyCredentialsProviderBuilder()
                                .withClientId(clientId)
                                .withApiKey(apiKey)
                                .build())
                .build();
    }

    public SDK createHttpSignature(String clientId, List<ClientScope> scopes) {
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientId, scopes, publicKey);
        adminClient.createClientAuthorizationHttpSignature(clientAuthorization);
        String host = Optional.ofNullable(System.getenv("dynamoplus.host")).orElse("http://localhost:3000");
        return new SdkBuilder(host)
                .withHttpConfiguration(HTTP_CONFIGURATION)
                .withCredentialsProvider(
                        new SdkBuilder.CredentialsProviderBuilder()
                                .withHttpSignatureCredentialsProviderBuilder()
                                .withKeyId(clientId)
                                .withPrivateKey(privateKey)
                                .build())
                .build();
    }
}
