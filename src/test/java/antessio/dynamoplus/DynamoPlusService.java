package antessio.dynamoplus;

import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
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

    private void cleanupCollection(String collectionName, Class collectionCls, Supplier<String> idSupplier) {
        PaginatedResult<?> result = sdk.getAll(collectionName, 20, null, collectionCls);
        result
                .getData()
                .forEach(b -> sdk.deleteDocument(idSupplier.get(), collectionName));
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


    public Index createIndex(List<String> conditions, Collection collection) {
        return sdk.createIndex(new IndexBuilder()
                .uid(UUID.randomUUID())
                .collection(collection)
                .orderingKey(null)
                .conditions(conditions)
                .createIndex()
        );

    }


    public void setup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);

        Collection category = this.getOrCreateCollection("name", CATEGORY_COLLECTION_NAME);
        Collection book = this.getOrCreateCollection("isbn", BOOK_COLLECTION_NAME);
        this.createIndex(Collections.singletonList("name"), category);
        this.createIndex(Collections.singletonList("author"), book);
        this.createIndex(Collections.singletonList("title"), book);
        this.createIndex(Collections.singletonList("category.name"), book);
    }

    public void cleanup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);
        cleanupCollection(CATEGORY_COLLECTION_NAME, Category.class, () -> "name");
        cleanupCollection(BOOK_COLLECTION_NAME, Category.class, () -> "isbn");
    }
}
