package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;


import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
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

    private static SDKV2 clientReadWrite;
    private static SDKV2 clientReadOnly;
    private static Category PULP;
    private static Category THRILLER;


    @BeforeAll
    public static void init() {
        long now = System.currentTimeMillis();
        SDKV2 adminClient = Clients.getIntance().getAdminClient();
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
        List<Category> categories = adminClient.queryAll(CATEGORY_COLLECTION_NAME, null, null, Category.class).getData();
        PULP = categories
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase("Pulp"))
                .findFirst()
                .orElseGet(() -> createCategory(adminClient, CATEGORY_COLLECTION_NAME, "Pulp"));
        THRILLER = categories
                .stream()
                .filter(c -> c.getName().equalsIgnoreCase("Thriller"))
                .findFirst()
                .orElseGet(() -> createCategory(adminClient, CATEGORY_COLLECTION_NAME, "Thriller"));
        adminClient.queryAll(BOOK_COLLECTION_NAME, 20, null, Book.class)
                .getData()
                .forEach(b -> adminClient.deleteDocument(b.getIsbn(), BOOK_COLLECTION_NAME));
    }

    private static Category createCategory(SDKV2 adminClient, String collectionName, String thriller) {
        return adminClient.createDocument(collectionName, Category.builder().name(thriller).build(), Category.class);
    }


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
        PaginatedResult<Book> result = clientReadWrite.queryAll(
                BOOK_COLLECTION_NAME,
                null,
                null,
                Book.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 5, "expected size 5")
                .matches(r -> r.getLastKey() == null, "expected no other results");

        assertThat(result.getData())
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
        PaginatedResult<Book> result = clientReadWrite.queryByIndex(
                BOOK_COLLECTION_NAME,
                "book__category.name",
                new QueryBuilder<Book>()
                        .matches(Book.builder().category(Category.builder().name(THRILLER.getName()).build()).build())
                        .build(),
                Book.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 1, "expected size 1")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.getData())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Män som hatar kvinnor", "Stieg Larsson")
                );
    }

    @DisplayName("Test query books by author")
    @Test
    @Order(3)
    void queryBooksByAuthor() {
        PaginatedResult<Book> result = clientReadWrite.queryByIndex(
                BOOK_COLLECTION_NAME,
                "book__author",
                new QueryBuilder<Book>()
                        .matches(Book.builder().author(CHUCK_PALHANIUK).build())
                        .build(),
                Book.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 2, "expected size 3 ")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.getData())
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
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> clientReadOnly.createDocument(BOOK_COLLECTION_NAME,
                        Book.builder()
                                .isbn(getRandomIsbn())
                                .title("Survivor")
                                .author(CHUCK_PALHANIUK)
                                .category(PULP)
                                .build(),
                        Book.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);

    }

    @DisplayName("Test query forbidden")
    @Test
    @Order(4)
    void queryForbidden() {
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> clientReadWrite.queryAll(CATEGORY_COLLECTION_NAME, null, null, Category.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
    }


    private void testCreateBook(Category category, String title, String author) {
        Book documentResult2 = clientReadWrite.createDocument(BOOK_COLLECTION_NAME,
                Book.builder()
                        .isbn(getRandomIsbn())
                        .title(title)
                        .author(author)
                        .category(category)
                        .build(),
                Book.class);
        assertThat(documentResult2)
                .matches(c -> c.getTitle().equals(title), "title must match")
                .matches(c -> c.getAuthor().equals(author), "author must match");
    }

    private String getRandomIsbn() {
        return IntStream.range(0, 13)
                .mapToObj(i -> new Random().nextInt(NUMBERS.length()))
                .map(NUMBERS::charAt)
                .map(c -> c + "")
                .collect(Collectors.joining());
    }

}
