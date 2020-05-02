package antessio.dynamoplus;

import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorization;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationHttpSignature;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import antessio.dynamoplus.sdk.domain.system.collection.*;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.index.Index;
import antessio.dynamoplus.sdk.domain.system.index.IndexBuilder;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminTest {

    public static final String CATEGORY_COLLECTION_NAME = String.format("category_%s", AdminTest.class.getName());
    public static final String BOOK_COLLECTION_NAME = String.format("book_%s", AdminTest.class.getName());

    private SDKV2 sdk = Clients.getIntance().getAdminClient();


    @DisplayName("Test create collections")
    @Test
    @Order(1)
    void testCreateCollections() {

        Collection categoryCollection = getCollection("name", CATEGORY_COLLECTION_NAME);
        Collection bookCollection = getCollectionBuilder("isbn", BOOK_COLLECTION_NAME)
                .fields(Arrays.asList(
                        buildAttributeNotNull("author", CollectionAttributeType.STRING).build(),
                        buildAttributeNotNull("title", CollectionAttributeType.STRING).build(),
                        buildAttributeNotNull("category", CollectionAttributeType.OBJECT)
                                .attributes(Collections.singletonList(
                                        buildAttributeNotNull("name", CollectionAttributeType.STRING).build()
                                ))
                                .build()
                ))
                .createCollection();

        Collection collectionResult = sdk.createCollection(categoryCollection);
        assertCollectionMatches(collectionResult, CATEGORY_COLLECTION_NAME, "name");

        Collection bookResult = sdk.createCollection(bookCollection);
        assertCollectionMatches(bookResult, BOOK_COLLECTION_NAME, "isbn");

    }


    private AttributeBuilder buildAttributeNotNull(String name, CollectionAttributeType type) {
        return new AttributeBuilder()
                .attributeName(name)
                .attributeType(type)
                .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL));
    }


    @DisplayName("Test create indexes")
    @Test
    @Order(2)
    void testCreateIndexes() {
        testIndex(CATEGORY_COLLECTION_NAME, "category__name", Collections.singletonList("name"), getCollection("name", CATEGORY_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__author", Collections.singletonList("author"), getCollection("isbn", BOOK_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__title", Collections.singletonList("title"), getCollection("isbn", BOOK_COLLECTION_NAME));
        testIndex(BOOK_COLLECTION_NAME, "book__category.name", Collections.singletonList("category.name"), getCollection("isbn", BOOK_COLLECTION_NAME));
    }


    @DisplayName("Create client authorization API key read only")
    @Test
    @Order(3)
    void createClientAuthorizationApiKeyReadOnly() {
        List<ClientScope> scopes = ClientScope.READ.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType)).collect(Collectors.toList());
        String clientIdApiKeyReadOnly = Clients.getIntance().getClientIdApiKeyReadOnly();
        String keyId = Clients.getIntance().getKeyId();
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientIdApiKeyReadOnly, scopes, keyId, Collections.emptyList());
        testClientAuthorizationApiKey(clientAuthorization);
    }

    @DisplayName("Create client authorization API key")
    @Test
    @Order(4)
    void createClientAuthorizationApiKey() {
        List<ClientScope> scopes = Stream.concat(
                ClientScope.READ_WRITE.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType)),
                ClientScope.READ.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType))
        ).collect(Collectors.toList());
        String clientIdApiKey = Clients.getIntance().getClientIdApiKey();
        String keyId = Clients.getIntance().getKeyId();
        ClientAuthorizationApiKey clientAuthorization = new ClientAuthorizationApiKey(clientIdApiKey, scopes, keyId, Collections.emptyList());
        testClientAuthorizationApiKey(clientAuthorization);
    }


    @DisplayName("Create client authorization http signature read only")
    @Test
    @Order(5)
    void createClientAuthorizationHttpSignatureReadOnly() {
        List<ClientScope> scopes = ClientScope.READ.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType)).collect(Collectors.toList());
        String clientIdHttpSignatureReadOnly = Clients.getIntance().getClientIdHttpSignatureReadOnly();
        String publicKey = Clients.getIntance().getPublicKey();
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientIdHttpSignatureReadOnly, scopes, publicKey);
        testClientAuthorizationHttpSignature(clientAuthorization);
    }

    @DisplayName("Create client authorization http signature")
    @Test
    @Order(6)
    void createClientAuthorizationHttpSignature() {
        List<ClientScope> scopes = Stream.concat(
                ClientScope.READ_WRITE.stream().map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType)),
                ClientScope.READ.stream().map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType))
        ).collect(Collectors.toList());
        String clientIdHttpSignature = Clients.getIntance().getClientIdHttpSignature();
        String publicKey = Clients.getIntance().getPublicKey();
        ClientAuthorizationHttpSignature clientAuthorization = new ClientAuthorizationHttpSignature(clientIdHttpSignature, scopes, publicKey);
        testClientAuthorizationHttpSignature(clientAuthorization);
    }

    @DisplayName("Get client api key")
    @Test
    @Order(7)
    void getClientApiKey() {
        String clientIdApiKey = Clients.getIntance().getClientIdApiKey();
        ClientAuthorizationApiKey result = sdk.getClientAuthorizationApiKey(clientIdApiKey);
        assertThat(result)
                .matches(c -> c.getType().equals(ClientAuthorization.ClientAuthorizationType.api_key));
    }


    private void testClientAuthorizationApiKey(ClientAuthorizationApiKey clientAuthorization) {
        ClientAuthorizationApiKey result = sdk.createClientAuthorizationApiKey(clientAuthorization);
        assertThat(result)
                .matches(c -> c.getClientId().equals(clientAuthorization.getClientId()));
    }

    private void testClientAuthorizationHttpSignature(ClientAuthorizationHttpSignature clientAuthorization) {
        ClientAuthorizationHttpSignature result = sdk.createClientAuthorizationHttpSignature(clientAuthorization);
        assertThat(result)
                .matches(c -> c.getClientId().equals(clientAuthorization.getClientId()));
    }


    private void testIndex(String category, String name, List<String> conditions, Collection collection) {
        Index resultCreateIndex1 = sdk.createIndex(new IndexBuilder()
                .uid(UUID.randomUUID())
                .collection(collection)
                .orderingKey(null)
                .conditions(conditions)
                .createIndex()
        );
        assertIndexMatches(resultCreateIndex1, category, name);
    }


    private Collection getCollection(String idKey, String collectionName) {
        return getCollectionBuilder(idKey, collectionName)
                .createCollection();
    }

    private CollectionBuilder getCollectionBuilder(String idKey, String collectionName) {
        return new CollectionBuilder()
                .idKey(idKey)
                .name(collectionName)
                .fields(Collections.emptyList());
    }

    private void assertIndexMatches(Index indexResult, String collectionName, String indexName) {
        assertThat(indexResult)
                .matches(i -> i.getUid() != null, "uid must be present")
                .matches(i -> i.getCollection().getName().equals(collectionName), "collection name must match")
        //.matches(i -> i.getName().equals(indexName), "index name must match")
        ;
    }

    private void assertCollectionMatches(Collection collectionResult, String collectionName, String idKey) {
        assertThat(collectionResult)
                .matches(c -> c.getName().equals(collectionName), "collection name must match")
                .matches(c -> c.getIdKey().equals(idKey), "id name must match");
    }
}
