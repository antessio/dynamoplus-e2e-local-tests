package antessio.dynamoplus;

import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.Either;
import antessio.dynamoplus.sdk.PaginatedResult;
import antessio.dynamoplus.sdk.SDK;
import antessio.dynamoplus.sdk.SdkException;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.collection.CollectionBuilder;
import antessio.dynamoplus.sdk.domain.system.index.Index;
import antessio.dynamoplus.sdk.domain.system.index.IndexBuilder;

import java.util.*;
import java.util.function.Supplier;

public class DynamoPlusService {

    private SDK sdk;


    private static DynamoPlusService instance;

    public static DynamoPlusService getInstance() {
        if (instance == null) {
            instance = new DynamoPlusService(Clients.getIntance().getAdminClient());
        }
        return instance;
    }

    private DynamoPlusService(SDK sdk) {
        this.sdk = sdk;
    }

    private void cleanupCollection(String collectionName, Class collectionCls, Supplier<String> idSupplier) {
        Either<PaginatedResult<?>, SdkException> eitherBooksOrError = sdk.queryAll(collectionName, 20, null, collectionCls);
        eitherBooksOrError
                .mapOk(PaginatedResult::getData)
                .ok()
                .orElse(Collections.emptyList())
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
                .orElseGet(() -> {
                    Either<Collection, SdkException> result = sdk.createCollection(getCollection(idKey, collectionName));
                    return result.ok().orElseThrow(() -> result.error().orElseThrow(() -> new RuntimeException("")));
                });

    }

    private Optional<Collection> findCollectionByName(String collectionName) {
        return sdk.getAllCollections().mapOk(PaginatedResult::getData).ok().orElse(Collections.emptyList())
                .stream()
                .filter(c -> c.getName().equals(collectionName))
                .findFirst();
    }


    public Index createIndex(String name, List<String> conditions, Collection collection) {
        Either<Index, SdkException> result = sdk.createIndex(new IndexBuilder()
                .uid(UUID.randomUUID())
                .collection(collection)
                .name(name)
                .orderingKey(null)
                .conditions(conditions)
                .createIndex()
        );
        return result.ok().orElseThrow(() -> result.error().orElseThrow(() -> new RuntimeException("")));

    }


    public void setup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);

        Collection category = this.getOrCreateCollection("name", CATEGORY_COLLECTION_NAME);
        Collection book = this.getOrCreateCollection("isbn", BOOK_COLLECTION_NAME);
        this.createIndex("category__name", Collections.singletonList("name"), category);
        this.createIndex("book__author", Collections.singletonList("author"), book);
        this.createIndex("book__title", Collections.singletonList("title"), book);
        this.createIndex("book__category.name", Collections.singletonList("category.name"), book);
    }

    public void cleanup(String suffix) {
        String CATEGORY_COLLECTION_NAME = String.format("%s_%s", "category", suffix);
        String BOOK_COLLECTION_NAME = String.format("%s_%s", "book", suffix);
        cleanupCollection(CATEGORY_COLLECTION_NAME, Category.class, () -> "id");
        cleanupCollection(BOOK_COLLECTION_NAME, Category.class, () -> "isbn");
    }
}
