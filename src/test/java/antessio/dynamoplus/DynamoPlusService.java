package antessio.dynamoplus;

import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorization;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationApiKey;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientAuthorizationHttpSignature;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.collection.CollectionBuilder;
import antessio.dynamoplus.sdk.domain.system.index.Index;
import antessio.dynamoplus.sdk.domain.system.index.IndexBuilder;

import java.util.*;
import java.util.function.Supplier;

public class DynamoPlusService {

    private SDKV2 sdk;


    private static DynamoPlusService instance;

    public static DynamoPlusService getInstance() {
        if (instance == null) {
            instance = new DynamoPlusService(Clients.getIntance().getAdminClient());
        }
        return instance;
    }

    private DynamoPlusService(SDKV2 sdk) {
        this.sdk = sdk;
    }

    public void cleanupCollection(String collectionName, Class collectionCls, Supplier<String> idSupplier) {
        PaginatedResult<?> result = sdk.getAll(collectionName, 20, null, collectionCls);
        result
                .getData()
                .forEach(b -> sdk.deleteDocument(idSupplier.get(), collectionName));
        while (result.getHasMore() && !result.getData().isEmpty()) {
            result
                    .getData()
                    .forEach(b -> sdk.deleteDocument(idSupplier.get(), collectionName));
            result = sdk.getAll(collectionName, 20, null, collectionCls);

        }
    }

    private Collection getCollection(String idKey, String collectionName) {
        return new CollectionBuilder()
                .idKey(idKey)
                .name(collectionName)
                .fields(Collections.emptyList())
                .createCollection();
    }

    public Collection getOrCreateCollection(String idKey, String collectionName) {

        return findCollectionByName(collectionName)
                .orElseGet(() -> sdk.createCollection(getCollection(idKey, collectionName)));

    }

    private Optional<Collection> findCollectionByName(String collectionName) {
        return sdk.getAllCollections().getData()
                .stream()
                .filter(c -> c.getName().equals(collectionName))
                .findFirst();
    }


    public Index createIndex(List<String> conditions, Collection collection, String orderingKey) {
        return sdk.createIndex(new IndexBuilder()
                .collection(collection)
                .orderingKey(orderingKey)
                .conditions(conditions)
                .build()
        );

    }


    public void setup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);

        Collection category = this.getOrCreateCollection("name", CATEGORY_COLLECTION_NAME);
        Collection book = this.getOrCreateCollection("isbn", BOOK_COLLECTION_NAME);
        this.createIndex(Collections.singletonList("name"), category, null);
        this.createIndex(Collections.singletonList("author"), book, null);
        this.createIndex(Collections.singletonList("title"), book, null);
        this.createIndex(Arrays.asList("category.name", "rating"), book, "rating");
        this.createIndex(Collections.singletonList("category.name"), book, null);
    }

    public void cleanup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);
        cleanupCollection(CATEGORY_COLLECTION_NAME, Category.class, () -> "name");
        cleanupCollection(BOOK_COLLECTION_NAME, Category.class, () -> "isbn");
    }

    public void deleteAllIndexesByCollection(String collectionName) {

        PaginatedResult<Index> result = sdk.getIndexByCollectionName(collectionName, 20, null);
        result
                .getData()
                .forEach(sdk::deleteIndex);
        while (result.getHasMore() && !result.getData().isEmpty()) {
            result
                    .getData()
                    .forEach(sdk::deleteIndex);
            result = sdk.getIndexByCollectionName(collectionName, 20, null);
        }
    }

    public void deleteCollection(String collectionName) {
        Optional.ofNullable(sdk.getCollection(collectionName))
                .ifPresent(sdk::deleteCollection);
    }

    public ClientAuthorizationHttpSignature getClientAuthorizationHttpSignature(String clientId) {
        return sdk.getClientAuthorizationHttpSignature(clientId);
    }

    public ClientAuthorizationApiKey getClientAuthorizationApiKey(String clientId) {
        return sdk.getClientAuthorizationApiKey(clientId);
    }
}
