package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.util.*;
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
    private final static String NUMBERS = "0123456789";

    private static SDK clientReadWrite;
    private static SDK clientReadOnly;
    private static Category PULP;
    private static Category THRILLER;


    @BeforeAll
    public static void init() {
        long now = System.currentTimeMillis();
        List<ClientScope> scopes = ClientScope.READ_WRITE.stream()
                .map(clientScopeType -> new ClientScope("book", clientScopeType))
                .collect(toList());
        clientReadWrite = Clients.getIntance().createHttpSignature(
                "client-id-books-rw-" + now,
                scopes);
        clientReadOnly = Clients.getIntance().createHttpSignature(
                "client-id-books-readonly-" + now,
                ClientScope.READ.stream()
                        .map(clientScopeType -> new ClientScope("book", clientScopeType))
                        .collect(toList()));
        SDK clientReadOnlyCategory = Clients.getIntance().createHttpSignature(
                "client-id-books-readonly-" + now,
                ClientScope.READ.stream()
                        .map(clientScopeType -> new ClientScope("category", clientScopeType))
                        .collect(toList()));
        Supplier<RuntimeException> unableToRunTheTest = () -> new RuntimeException("unable to run the test");
        List<Category> categories = clientReadOnlyCategory.queryAll("categories", null, null, Category.class)
                .ok()
                .map(PaginatedResult::getData)
                .orElseThrow(unableToRunTheTest);
        PULP = categories.stream().filter(c -> c.getName().equalsIgnoreCase("pulp")).findFirst().orElseThrow(unableToRunTheTest);
        THRILLER = categories.stream().filter(c -> c.getName().equalsIgnoreCase("thriller")).findFirst().orElseThrow(unableToRunTheTest);

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
                "book",
                null,
                null,
                Book.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(r -> r.getData().size() == 5, "expected size 5")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.ok().map(PaginatedResult::getData).get())
                .extracting(b -> tuple(b.getAuthor(), b.getTitle()))
                .contains(
                        tuple("Fight Club", "Chuck Palhaniuk"),
                        tuple("Choke", "Chuck Palhaniuk"),
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
                "book",
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
                .extracting(b -> tuple(b.getAuthor(), b.getTitle()))
                .contains(
                        tuple("Män som hatar kvinnor", "Stieg Larsson")
                );
    }

    @DisplayName("Test query books by author")
    @Test
    @Order(3)
    void queryBooksByAuthor() {
        Either<PaginatedResult<Book>, SdkException> result = clientReadWrite.queryByIndex(
                "book",
                "book__author",
                new QueryBuilder<Book>()
                        .matches(Book.builder().author("Chuck Palhaniuk").build())
                        .build(),
                Book.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .get()
                .matches(r -> r.getData().size() == 3, "expected size 3")
                .matches(r -> r.getLastKey() == null, "expected no other results");
        assertThat(result.ok().map(PaginatedResult::getData).get())
                .extracting(b -> tuple(b.getAuthor(), b.getTitle()))
                .contains(
                        tuple("Fight Club", "Chuck Palhaniuk"),
                        tuple("Choke", "Chuck Palhaniuk"),
                        tuple("Pulp", "Charles Bukowski")
                );
    }

    @DisplayName("Test create forbidden")
    @Test
    @Order(3)
    void createForbidden() {
        Either<Book, SdkException> documentResult1 = clientReadOnly.createDocument("book",
                Book.builder()
                        .isbn(getRandomIsbn())
                        .title("Survivor")
                        .author("Chuck Palhaniuk")
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

        Either<PaginatedResult<Category>, SdkException> result = clientReadWrite.queryAll("category", null, null, Category.class);
        assertThat(result.error())
                .get()
                .matches(e -> e instanceof SdkHttpException)
                .extracting(e -> (SdkHttpException) e)
                .matches(e -> e.getHttpCode() == 403);
    }


    private void testCreateBook(Category category, String title, String author) {
        Either<Book, SdkException> documentResult2 = clientReadWrite.createDocument("book",
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
