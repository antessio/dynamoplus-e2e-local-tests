package antessio.dynamoplus;

import antessio.dynamoplus.authentication.provider.apikey.ApiKeyCredentialsProviderBuilder;
import antessio.dynamoplus.authentication.provider.basic.BasicAuthCredentialsProvider;
import antessio.dynamoplus.authentication.provider.httpsignature.HttpSignatureCredentialsProvider;
import antessio.dynamoplus.authentication.provider.httpsignature.HttpSignatureCredentialsProviderBuilder;
import antessio.dynamoplus.http.HttpConfiguration;
import antessio.dynamoplus.http.okhttp.OkHttpSdkHttpClient;
import antessio.dynamoplus.sdk.SDK;
import antessio.dynamoplus.sdk.SDKV2;
import antessio.dynamoplus.sdk.SdkBuilder;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationHttpSignature;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;

import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Clients {
    private SDKV2 adminClient;
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
        privateKey = "-----BEGIN PRIVATE KEY-----\n" +
                "MIICeAIBADANBgkqhkiG9w0BAQEFAASCAmIwggJeAgEAAoGBAMIUQ0bDffIaKHL3\n" +
                "akONlCGXQLfqs8mP4K99ILz6rbyHEDXrVAU1R3XfC4JNRyrRB3aqwF7/aEXJzYMI\n" +
                "kmDSHUvvz7pnhQxHsQ5yl91QT0d/eb+Gz4VRHjm4El4MrUdIUcPxscoPqS/wU8Z8\n" +
                "lOi1z7bGMnChiL7WGqnV8h6RrGzJAgMBAAECgYEAlHxmQJS/HmTO/6612XtPkyei\n" +
                "t1PVO+hdckZcrtln5S68w1QJ03ZA9ziwGIBBa8vDVxIq3kOwpnxQROlg/Lyk9iec\n" +
                "MTPZ0NiJp7D37ESm5vJ5bagfhnHvXCoG04qSrCtdr+nN2mK5xFGOTq8TphjsQEGz\n" +
                "+Du5qdWkaJs5UASyofUCQQDsOSNUfbxYNSB/Weq9+fYqPoJPuchwTeMYmxlnvOVm\n" +
                "YGYcUM40wtStdH9mbelHmbS0KYGprlEr3m7jXaO3V08jAkEA0lPe/ymeS2HjxtCj\n" +
                "98p6Xq4RjJuhG0Dn+4e4eRnoVAXs5SQaiByZImW451zm3qEjVWwufRBkSNBkwQ5a\n" +
                "v7ApIwJBAILiRckSwcC97vug/oe0b8iISfuSnJRdE28WwMTRzOkkkG8v9pEVQnG5\n" +
                "Er3WOGMLrywDs2wowaDk5dvkjkmPfrECQQCAhPtoU5gEXAaBABCRY0ou/JKApsBl\n" +
                "FN4sFpykcy5B2XUN92e28DKqkBnSVjREqZYbpoUpqpB85coLJahSJWSdAkBeuWDJ\n" +
                "IVyL/a54qUgTVCoiItJnxXw6WkUtGdvWnMjtTXJBedMAQVgznrTImXNSk5vVXhxJ\n" +
                "wZ3frm2JIy/Es69M\n" +
                "-----END PRIVATE KEY-----";
        publicKey = "-----BEGIN PUBLIC KEY-----\n" +
                "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDCFENGw33yGihy92pDjZQhl0C3\n" +
                "6rPJj+CvfSC8+q28hxA161QFNUd13wuCTUcq0Qd2qsBe/2hFyc2DCJJg0h1L78+6\n" +
                "Z4UMR7EOcpfdUE9Hf3m/hs+FUR45uBJeDK1HSFHD8bHKD6kv8FPGfJTotc+2xjJw\n" +
                "oYi+1hqp1fIekaxsyQIDAQAB\n" +
                "-----END PUBLIC KEY-----";
        String host = Optional.ofNullable(System.getenv("DYNAMOPLUS_HOST")).orElse("http://localhost:3000");
        String root = Optional.ofNullable(System.getenv("DYNAMOPLUS_ROOT")).orElse("root");
        String password = Optional.ofNullable(System.getenv("DYNAMOPLUS_PASSWORD")).orElse("12345");
        System.out.println("host = " + host);
        System.out.println("root = " + root);
        adminClient = new SdkBuilder(host, new OkHttpSdkHttpClient(HTTP_CONFIGURATION, new BasicAuthCredentialsProvider(root, password))).buildV2();
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

    public SDKV2 getAdminClient() {
        return adminClient;
    }


    public SDKV2 createClientApiKey(String clientId, String apiKey, List<ClientScope> scopes) {
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientId, scopes, apiKey, Collections.emptyList());
        adminClient.createClientAuthorizationApiKey(clientAuthorization);
        String host = Optional.ofNullable(System.getenv("DYNAMOPLUS_HOST")).orElse("http://localhost:3000");
        return new SdkBuilder(
                host,
                new OkHttpSdkHttpClient(HTTP_CONFIGURATION, new ApiKeyCredentialsProviderBuilder()
                        .withClientId(clientId)
                        .withApiKey(apiKey)
                        .build())).buildV2();
    }

    public SDKV2 createHttpSignature(String clientId, List<ClientScope> scopes) {
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientId, scopes, publicKey);
        adminClient.createClientAuthorizationHttpSignature(clientAuthorization);
        String host = Optional.ofNullable(System.getenv("DYNAMOPLUS_HOST")).orElse("http://localhost:3000");
        return new SdkBuilder(
                host,
                new OkHttpSdkHttpClient(HTTP_CONFIGURATION, new HttpSignatureCredentialsProviderBuilder()
                        .withKeyId(clientId)
                        .withPrivateKey(getPrivateKey())
                        .build()))
                .buildV2();
    }
}
