package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.collection.CollectionBuilder;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;


import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HttpSignatureClientTest {

    public static final String SUFFIX = "__http_signature_test";
    public static final String CATEGORY_COLLECTION_NAME = String.format("category_%s", SUFFIX);
    public static final String BOOK_COLLECTION_NAME = String.format("book_%s", SUFFIX);

    private final static String NUMBERS = "0123456789";
    public static final String CHUCK_PALHANIUK = "Chuck Palhaniuk";

    private static SDK clientReadWrite;
    private static SDK clientReadOnly;
    private static Category PULP;
    private static Category THRILLER;


    @BeforeAll
    public static void init() {
        long now = System.currentTimeMillis();
        SDK adminClient = Clients.getIntance().getAdminClient();
        List<ClientScope> scopes = ClientScope.READ_WRITE.stream()
                .map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType))
                .collect(toList());
        clientReadWrite = Clients.getIntance().createHttpSignature(
                "client-id-books-rw-" + now,
                scopes);
        clientReadOnly = Clients.getIntance().createHttpSignature(
                "client-id-books-readonly-" + now,
                ClientScope.READ.stream()
                        .map(clientScopeType -> new ClientScope(BOOK_COLLECTION_NAME, clientScopeType))
                        .collect(toList()));

        DynamoPlusService.getInstance().setup(SUFFIX);
        Either<List<Category>, SdkException> eitherCategories = adminClient.queryAll(CATEGORY_COLLECTION_NAME, null, null, Category.class)
                .mapOk(PaginatedResult::getData);
        Supplier<RuntimeException> unableToRunTheTest = () -> eitherCategories.error().map(RuntimeException::new).orElseGet(() -> new RuntimeException("unable to run the test"));
        List<Category> categories = eitherCategories.ok().orElseThrow(unableToRunTheTest);
        PULP = categories
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase("Pulp"))
                .findFirst()
                .orElseGet(() -> createCategory(adminClient, CATEGORY_COLLECTION_NAME, "Pulp"));
        THRILLER = categories
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase("Thriller"))
                .findFirst()
                .orElseGet(() ->
                        Optional.ofNullable(createCategory(adminClient, CATEGORY_COLLECTION_NAME, "Thriller")
                        ).orElseThrow(unableToRunTheTest));
        Either<PaginatedResult<Book>, SdkException> eitherBooksOrError = adminClient.queryAll(BOOK_COLLECTION_NAME, 20, null, Book.class);
        eitherBooksOrError
                .mapOk(PaginatedResult::getData)
                .ok()
                .orElse(Collections.emptyList())
                .forEach(b -> adminClient.deleteDocument(b.getIsbn(), BOOK_COLLECTION_NAME));
    }

    private static Category createCategory(SDK adminClient, String collectionName, String thriller) {
        Either<Category, SdkException> eitherCategory = adminClient.createDocument(collectionName, Category.builder().name(thriller).build(), Category.class);
        return eitherCategory
                .ok()
                .orElseThrow(() -> eitherCategory.error().map(RuntimeException::new).orElseGet(() -> new RuntimeException("unable to create a document of category " + collectionName)));
    }

//    private static Collection createCollectionIfNotExists(SDK adminClient, String collectionName) {
//        return adminClient.getCollection(collectionName)
//                .ok()
//                .orElseGet(() -> adminClient.createCollection(new CollectionBuilder().idKey("name").name(collectionName).createCollection())
//                        .ok()
//                        .orElseThrow(() -> new RuntimeException("Unable to create the collection"))
//                );
//    }

    @AfterAll
    public static void clean() {
        DynamoPlusService.getInstance().cleanup(SUFFIX);
    }

    @DisplayName("Test create documents")
    @Test
    @Order(1)
    void createDocuments() {
        testCreateBook(PULP, "Fight Club", "Chuck Palhaniuk");
        testCreateBook(PULP, "Choke", "Chuck Palhaniuk");
        testCreateBook(THRILLER, "Män som hatar kvinnor", "Stieg Larsson");
        testCreateBook(PULP, "Pulp", "Charles Bukowski");
        testCreateBook(PULP, "Filth", "Irvine Welsh");
    }

    @DisplayName("Test query all documents")
    @Test
    @Order(2)
    void queryAllCategories() {
        Either<PaginatedResult<Book>, SdkException> result = clientReadWrite.queryAll(
                BOOK_COLLECTION_NAME,
                null,
                null,
                Book.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(r -> r.getData().size() == 5, "expected size 5")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.ok().map(PaginatedResult::getData).get())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Fight Club", CHUCK_PALHANIUK),
                        tuple("Choke", CHUCK_PALHANIUK),
                        tuple("Män som hatar kvinnor", "Stieg Larsson"),
                        tuple("Pulp", "Charles Bukowski"),
                        tuple("Filth", "Irvine Welsh")
                );
    }

    @DisplayName("Test query books by category")
    @Test
    @Order(3)
    void queryBooksByCategory() {
        Either<PaginatedResult<Book>, SdkException> result = clientReadWrite.queryByIndex(
                BOOK_COLLECTION_NAME,
                "book__category.name",
                new QueryBuilder<Book>()
                        .matches(Book.builder().category(Category.builder().name(THRILLER.getName()).build()).build())
                        .build(),
                Book.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(r -> r.getData().size() == 1, "expected size 1")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.ok().map(PaginatedResult::getData).get())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Män som hatar kvinnor", "Stieg Larsson")
                );
    }

    @DisplayName("Test query books by author")
    @Test
    @Order(3)
    void queryBooksByAuthor() {
        Either<PaginatedResult<Book>, SdkException> result = clientReadWrite.queryByIndex(
                BOOK_COLLECTION_NAME,
                "book__author",
                new QueryBuilder<Book>()
                        .matches(Book.builder().author(CHUCK_PALHANIUK).build())
                        .build(),
                Book.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(r -> r.getData().size() == 2, "expected size 3 ")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.ok().map(PaginatedResult::getData).get())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Fight Club", CHUCK_PALHANIUK),
                        tuple("Choke", CHUCK_PALHANIUK)
                );
    }

    @DisplayName("Test create forbidden")
    @Test
    @Order(3)
    void createForbidden() {
        Either<Book, SdkException> documentResult1 = clientReadOnly.createDocument(BOOK_COLLECTION_NAME,
                Book.builder()
                        .isbn(getRandomIsbn())
                        .title("Survivor")
                        .author(CHUCK_PALHANIUK)
                        .category(PULP)
                        .build(),
                Book.class);
        assertThat(documentResult1.error())
                .get()
                .matches(e -> e instanceof SdkHttpException)
                .extracting(e -> (SdkHttpException) e)
                .matches(e -> e.getHttpCode() == 403);
    }

    @DisplayName("Test query forbidden")
    @Test
    @Order(4)
    void queryForbidden() {

        Either<PaginatedResult<Category>, SdkException> result = clientReadWrite.queryAll(CATEGORY_COLLECTION_NAME, null, null, Category.class);
        assertThat(result.error())
                .get()
                .matches(e -> e instanceof SdkHttpException)
                .extracting(e -> (SdkHttpException) e)
                .matches(e -> e.getHttpCode() == 403);
    }


    private void testCreateBook(Category category, String title, String author) {
        Either<Book, SdkException> documentResult2 = clientReadWrite.createDocument(BOOK_COLLECTION_NAME,
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

}
