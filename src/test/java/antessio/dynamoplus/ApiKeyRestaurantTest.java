package antessio.dynamoplus;


import antessio.dynamoplus.sdk.SDKV2;
import antessio.dynamoplus.sdk.SdkHttpException;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Optional;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiKeyRestaurantTest extends RestaurantTest {

    public static final String SUFFIX = "__api_key_test";


    @BeforeAll
    public static void init() {
        initialize(SUFFIX);
    }

    @AfterAll
    public static void clean() {
        cleanup(SUFFIX);
    }


    @Override
    protected SDKV2 getClient(String clientId, List<ClientScope> scopes) {
        final String cId = clientId + SUFFIX;
        if (registeredClients.containsKey(cId)) {
            return registeredClients.get(cId);
        } else {
            SDKV2 clientReadWrite = Optional.ofNullable(getClientAuthorizationApiKey(cId))
                    .map(Clients.getIntance()::createClientApiKey)
                    .orElseGet(() -> Clients.getIntance().createClientApiKey(
                            cId,
                            cId,
                            scopes));
            registeredClients.put(cId, clientReadWrite);
            return clientReadWrite;
        }
    }

    private ClientAuthorizationApiKey getClientAuthorizationApiKey(String clientId) {
        try {
            return DynamoPlusService.getInstance().getClientAuthorizationApiKey(clientId);
        } catch (SdkHttpException e) {
            return null;
        }
    }


    @Override
    protected String getSuffix() {
        return SUFFIX;
    }

}
