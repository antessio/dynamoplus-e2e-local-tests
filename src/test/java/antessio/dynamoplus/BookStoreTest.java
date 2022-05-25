package antessio.dynamoplus;

import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.PaginatedResult;
import antessio.dynamoplus.sdk.SDKV2;
import antessio.dynamoplus.sdk.SdkException;
import antessio.dynamoplus.sdk.SdkHttpException;
import antessio.dynamoplus.sdk.domain.conditions.Eq;
import antessio.dynamoplus.sdk.domain.conditions.PredicateBuilder;
import antessio.dynamoplus.sdk.domain.conditions.Range;
import antessio.dynamoplus.sdk.domain.document.query.Query;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import antessio.dynamoplus.sdk.domain.system.index.IndexConfiguration;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

public abstract class BookStoreTest {
    private final static String NUMBERS = "0123456789";

    protected static final String CATEGORY_BASE_COLLECTION_NAME = "category";
    protected static final String BOOK_BASE_COLLECTION_NAME = "book";
    protected static final Category PULP = Category.builder().id(UUID.randomUUID().toString()).name("Pulp").build();
    protected static final Category THRILLER = Category.builder().id(UUID.randomUUID().toString()).name("Thriller").build();
    public static final String CHUCK_PALHANIUK = "Chuck Palhaniuk";

    protected static Map<String, SDKV2> registeredClients = new HashMap<>();

    protected static final String CLIENT_ID_BOOK_READ_ONLY = "client-id-books-readonly";
    protected static final String CLIENT_ID_BOOK_READ_WRITE = "client-id-books-rw";
    protected static final String CLIENT_ID_CATEGORY_READ_ONLY = "client-id-category-readonly";
    protected static final String CLIENT_ID_CATEGORY_READ_WRITE = "client-id-cateogry-rw";

    protected abstract SDKV2 getClient(String clientId, List<ClientScope> scopes);


    protected static void initialize(String suffix) {
        Collection category = DynamoPlusService.getInstance().getOrCreateCollection("name", getCollectionName(CATEGORY_BASE_COLLECTION_NAME, suffix));
        Collection book = DynamoPlusService.getInstance().getOrCreateCollection("isbn", getCollectionName(BOOK_BASE_COLLECTION_NAME, suffix));
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("name"), category, null, IndexConfiguration.OPTIMIZE_WRITE);
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("author"), book, null, IndexConfiguration.OPTIMIZE_WRITE);
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("title"), book, null, IndexConfiguration.OPTIMIZE_READ);
        DynamoPlusService.getInstance().createIndex(Arrays.asList("category.name", "rating"), book, "rating", IndexConfiguration.OPTIMIZE_WRITE);
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("category.name"), book, null, IndexConfiguration.OPTIMIZE_READ);

    }

    protected List<ClientScope> getClientScopeRW(String collectionName) {
        return ClientScope.READ_WRITE.stream()
                .map(clientScopeType -> new ClientScope(collectionName, clientScopeType))
                .collect(Collectors.toList());
    }

    protected List<ClientScope> getClientScopeR(String collectionName) {
        return ClientScope.READ.stream()
                .map(clientScopeType -> new ClientScope(collectionName, clientScopeType))
                .collect(Collectors.toList());
    }

    protected static void cleanup(String suffix) {
//        DynamoPlusService.getInstance().cleanupCollection(getCollectionName(CATEGORY_BASE_COLLECTION_NAME, suffix), Category.class, () -> "name");
//        DynamoPlusService.getInstance().cleanupCollection(getCollectionName(BOOK_BASE_COLLECTION_NAME, suffix), Category.class, () -> "isbn");
//        DynamoPlusService.getInstance().deleteAllIndexesByCollection(getCollectionName(CATEGORY_BASE_COLLECTION_NAME, suffix));
//        DynamoPlusService.getInstance().deleteAllIndexesByCollection(getCollectionName(BOOK_BASE_COLLECTION_NAME, suffix));
//        DynamoPlusService.getInstance().deleteCollection(getCollectionName(CATEGORY_BASE_COLLECTION_NAME, suffix));
//        DynamoPlusService.getInstance().deleteCollection(getCollectionName(BOOK_BASE_COLLECTION_NAME, suffix));

    }

    protected static String getCollectionName(String baseCollection, String suffix) {
        return String.format("%s_%s", baseCollection, suffix);
    }

    protected void testCreateCategory(Category category, String suffix) {
        String collectionName = getCollectionName(CATEGORY_BASE_COLLECTION_NAME, suffix);
        Category documentResult1 = getClient(CLIENT_ID_CATEGORY_READ_WRITE, getClientScopeRW(collectionName)).createDocument(
                collectionName,
                category,
                Category.class);
        assertThat(documentResult1)
                .matches(c -> c.getName().equals(category.getName()), "name must match");
    }

    protected void testCreateBook(Category category, String title, String author, int rating) {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        Book documentResult2 = getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName)).createDocument(collectionName,
                Book.builder()
                        .isbn(getRandomIsbn())
                        .title(title)
                        .author(author)
                        .category(category)
                        .rating(String.format("%02d", rating))
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


    protected abstract String getSuffix();

    @DisplayName("Test create categories")
    @Test
    @Order(100)
    void createCategories() {
        testCreateCategory(PULP, getSuffix());
        testCreateCategory(THRILLER, getSuffix());
    }

    @DisplayName("Test create books")
    @Test
    @Order(101)
    void createBooks() {
        testCreateBook(PULP, "Fight Club", "Chuck Palhaniuk", 8);
        testCreateBook(PULP, "Choke", "Chuck Palhaniuk", 7);
        testCreateBook(THRILLER, "Män som hatar kvinnor", "Stieg Larsson", 7);
        testCreateBook(PULP, "Pulp", "Charles Bukowski", 5);
        testCreateBook(PULP, "Filth", "Irvine Welsh", 6);
    }

    @DisplayName("Test query all categories")
    @Test
    @Order(200)
    void queryAllCategories() {
        String collectionName = getCollectionName(CATEGORY_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Category> result = getClient(CLIENT_ID_CATEGORY_READ_WRITE, getClientScopeRW(collectionName))
                .getAll(
                        collectionName,
                        null,
                        null,
                        Category.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 2, "expected size 2")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results")
                .matches(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName()) || c.getName().equals(THRILLER.getName())), "expected category found");
    }

    @DisplayName("Test query all books")
    @Test
    @Order(201)
    void getAllBooks() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Book> result = getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName))
                .getAll(
                        collectionName,
                        null,
                        null,
                        Book.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 5, "expected size 5")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");

        AssertionsForInterfaceTypes.assertThat(result.getData())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Fight Club", CHUCK_PALHANIUK),
                        tuple("Choke", CHUCK_PALHANIUK),
                        tuple("Män som hatar kvinnor", "Stieg Larsson"),
                        tuple("Pulp", "Charles Bukowski"),
                        tuple("Filth", "Irvine Welsh")
                );
    }

    @DisplayName("Test query category by name")
    @Test
    @Order(300)
    void queryCategoriesByName() {
        String collectionName = getCollectionName(CATEGORY_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Category> result = getClient(CLIENT_ID_CATEGORY_READ_WRITE, getClientScopeRW(collectionName)).query(
                collectionName,
                new Query(new PredicateBuilder()
                        .withEq("name", "Pulp")),
                Category.class,
                null,
                null);
        assertThat(result)
                .matches(r -> r.getData().size() == 1, "expected size 1")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results")
                .matches(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName())), "expected category found")
        ;
    }

    @DisplayName("Test query books by author")
    @Test
    @Order(301)
    void queryBooksByAuthor() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Book> result = getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName)).query(
                collectionName,
                new Query(new PredicateBuilder().withEq("author", CHUCK_PALHANIUK)),
                Book.class, null, null);
        assertThat(result)
                .matches(r -> r.getData().size() == 2, "expected size 2 ")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
        AssertionsForInterfaceTypes.assertThat(result.getData())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Fight Club", CHUCK_PALHANIUK),
                        tuple("Choke", CHUCK_PALHANIUK)
                );
    }

    @DisplayName("Test query books by category")
    @Test
    @Order(302)
    void queryBooksByCategory() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Book> result = getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName)).query(
                collectionName,
                new Query(new PredicateBuilder().withEq("category.name", THRILLER.getName())),
                Book.class,
                null,
                null);
        assertThat(result)
                .matches(r -> r.getData().size() == 1, "expected size 1")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
        AssertionsForInterfaceTypes.assertThat(result.getData())
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Män som hatar kvinnor", "Stieg Larsson")
                );
    }

    @DisplayName("Test query books by category")
    @Test
    @Order(303)
    void queryBooksByCategoryAndRating() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Book> result = getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName)).query(
                collectionName,
                new Query(new PredicateBuilder()
                        .withAnd(Arrays.asList(
                                new Eq("category.name", PULP.getName()),
                                new Range("rating", "07", "09")
                        ))),
                Book.class,
                null,
                null);
        assertThat(result)
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
        AssertionsForInterfaceTypes.assertThat(result.getData())
                .hasSize(2)
                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
                .contains(
                        tuple("Fight Club", CHUCK_PALHANIUK),
                        tuple("Choke", CHUCK_PALHANIUK)
                );
    }

    @DisplayName("Test create forbidden")
    @Test
    @Order(400)
    void createCategoryForbidden() {

        String collectionName = getCollectionName(CATEGORY_BASE_COLLECTION_NAME, getSuffix());
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> getClient(CLIENT_ID_CATEGORY_READ_ONLY, getClientScopeR(collectionName)).createDocument(collectionName,
                        PULP,
                        Category.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);

    }

    @DisplayName("Test query book forbidden")
    @Test
    @Order(500)
    void queryBookForbidden() {
        String collectionName = getCollectionName(CATEGORY_BASE_COLLECTION_NAME, getSuffix());
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> getClient(CLIENT_ID_BOOK_READ_WRITE, getClientScopeRW(collectionName)).getAll(collectionName, null, null, Book.class))
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
    }


    @DisplayName("Test create book forbidden")
    @Test
    @Order(501)
    void createBookForbidden() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());

        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> getClient(CLIENT_ID_BOOK_READ_ONLY, getClientScopeR(collectionName)).createDocument(collectionName,
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

    @DisplayName("Test query category forbidden")
    @Test
    @Order(5002)
    void queryCategoryForbidden() {
        String collectionName = getCollectionName(BOOK_BASE_COLLECTION_NAME, getSuffix());
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> getClient(CLIENT_ID_CATEGORY_READ_WRITE, getClientScopeRW(collectionName)).getAll(
                        collectionName, null, null, Category.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
    }


}
