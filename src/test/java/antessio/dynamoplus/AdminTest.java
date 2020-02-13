package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.http.HttpConfiguration;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.collection.CollectionBuilder;
import antessio.dynamoplus.sdk.domain.system.index.Index;
import antessio.dynamoplus.sdk.domain.system.index.IndexBuilder;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;


import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AdminTest {
    private final static String NUMBERS = "0123456789";
    public static final HttpConfiguration HTTP_CONFIGURATION = new HttpConfiguration(30000, 30000, 30000);
    private SDK dynamoplusSdk;
    public static final Category PULP = Category.builder().id(UUID.randomUUID().toString()).name("Pulp").build();
    public static final Category THRILLER = Category.builder().id(UUID.randomUUID().toString()).name("Thriller").build();

    @BeforeEach
    void setUp() {
        String host = Optional.ofNullable(System.getenv("dynamoplus.host")).orElse("http://localhost:3000");
        System.out.println("host = " + host);
        dynamoplusSdk = new SdkBuilder(host)
                .withHttpConfiguration(HTTP_CONFIGURATION)
                .withCredentialsProvider(
                        new SdkBuilder.CredentialsProviderBuilder()
                                .withBasicAuthCredentialsProviderBuilder()
                                .withUsername("root")
                                .withPassword("12345")
                                .build())
                .build();
    }

    @DisplayName("Test create collections")
    @Test
    @Order(1)
    void testCreateCollections() {

        Collection categoryCollection = getCollection("id", "category");
        Collection bookCollection = getCollection("isbn", "book");

        Either<Collection, SdkException> collectionResult = dynamoplusSdk.createCollection(categoryCollection);
        assertCollectionMatches(collectionResult, "category", "id");

        Either<Collection, SdkException> bookResult = dynamoplusSdk.createCollection(bookCollection);
        assertCollectionMatches(bookResult, "book", "isbn");

    }


    @DisplayName("Test create indexes")
    @Test
    @Order(2)
    void testCreateIndexes() {
        testIndex("category", "category__name", Collections.singletonList("name"), getCollection("id", "category"));
        testIndex("book", "book__author", Collections.singletonList("author"), getCollection("id", "book"));
        testIndex("book", "book__title", Collections.singletonList("title"), getCollection("id", "book"));
        testIndex("book", "book__category.name", Collections.singletonList("category.name"), getCollection("id", "book"));
    }

    @DisplayName("Test create documents")
    @Test
    @Order(3)
    void createDocuments() {
        testCreateCategory(PULP);
        testCreateCategory(THRILLER);
        testCreateBook(PULP, "Fight Club", "Chuck Palhaniuk");
        testCreateBook(PULP, "Choke", "Chuck Palhaniuk");
        testCreateBook(THRILLER, "Män som hatar kvinnor", "Stieg Larsson");
        testCreateBook(PULP, "Pulp", "Charles Bukowski");
        testCreateBook(PULP, "Filth", "Irvine Welsh");
    }

    @DisplayName("Test query documents")
    @Test
    @Order(4)
    void queryDocuments() {
        Either<PaginatedResult<Category>, SdkException> result = dynamoplusSdk.queryByIndex(
                "category",
                "category__name",
                new QueryBuilder<Category>()
                        .matches(Category.builder().name("Pulp").build())
                        .build(),
                Category.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(r -> r.getData().size() == 1, "expected size 1"))
                .hasValueSatisfying(new Condition<>(r -> r.getLastKey() == null, "expected no other results"))
                .hasValueSatisfying(new Condition<>(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName())), "expected category found"));
    }


    private void testCreateCategory(Category category) {
        Either<Category, SdkException> documentResult1 = dynamoplusSdk.createDocument("category",
                category,
                Category.class);
        documentResult1.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(documentResult1.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(c -> c.getName().equals(category.getName()), "name must match"));
    }

    private void testCreateBook(Category category, String title, String author) {
        Either<Book, SdkException> documentResult2 = dynamoplusSdk.createDocument("book",
                Book.builder()
                        .isbn(getRandomIsbn())
                        .title(title)
                        .author(author)
                        .category(category)
                        .build(),
                Book.class);
        documentResult2.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(documentResult2.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(c -> c.getTitle().equals(title), "title must match"))
                .hasValueSatisfying(new Condition<>(c -> c.getAuthor().equals(author), "author must match"));
    }

    private String getRandomIsbn() {
        return IntStream.range(0, 13)
                .mapToObj(i -> new Random().nextInt(NUMBERS.length()))
                .map(NUMBERS::charAt)
                .map(c -> c + "")
                .collect(Collectors.joining());
    }

    private void testIndex(String category, String name, List<String> conditions, Collection collection) {
        Either<Index, SdkException> resultCreateIndex1 = dynamoplusSdk.createIndex(new IndexBuilder()
                .uid(UUID.randomUUID())
                .collection(collection)
                .name(name)
                .orderingKey(null)
                .conditions(conditions)
                .createIndex()
        );
        assertIndexMatches(resultCreateIndex1, category, name);
    }


    private Collection getCollection(String idKey, String collectionName) {
        return new CollectionBuilder()
                .idKey(idKey)
                .name(collectionName)
                .fields(Collections.emptyList())
                .createCollection();
    }

    private void assertIndexMatches(Either<Index, SdkException> indexResult, String collectionName, String indexName) {
        indexResult.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(indexResult.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(i -> i.getUid() != null, "uid must be present"))
                .hasValueSatisfying(new Condition<>(i -> i.getCollection().getName().equals(collectionName), "collection name must match"))
                .hasValueSatisfying(new Condition<>(i -> i.getName().equals(indexName), "index name must match"));
    }

    private void assertCollectionMatches(Either<Collection, SdkException> collectionResult, String book, String isbn) {
        collectionResult.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(collectionResult.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(c -> c.getName().equals(book), "collection name must match"))
                .hasValueSatisfying(new Condition<>(c -> c.getIdKey().equals(isbn), "id name must match"));
    }
}
