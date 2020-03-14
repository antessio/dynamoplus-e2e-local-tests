package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiKeyClientTest {

    public static final String SUFFIX = "__api_key_test";
    public static final String CATEGORY_COLLECTION_NAME = String.format("category_%s", SUFFIX);
    public static final String BOOK_COLLECTION_NAME = String.format("book_%s", SUFFIX);
    private static SDK clientReadWrite;
    private static SDK clientReadOnly;
    public static final Category PULP = Category.builder().id(UUID.randomUUID().toString()).name("Pulp").build();
    public static final Category THRILLER = Category.builder().id(UUID.randomUUID().toString()).name("Thriller").build();

    @BeforeAll
    public static void init() {
        long now = System.currentTimeMillis();
        List<ClientScope> scopes = ClientScope.READ_WRITE.stream()
                .map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType))
                .collect(Collectors.toList());
        clientReadWrite = Clients.getIntance().createClientApiKey(
                "client-id-categories-rw-" + now,
                "api-key-" + now,
                scopes);
        clientReadOnly = Clients.getIntance().createClientApiKey(
                "client-id-categories-readonly-" + now,
                "api-key-readonly-" + now,
                ClientScope.READ.stream()
                        .map(clientScopeType -> new ClientScope(CATEGORY_COLLECTION_NAME, clientScopeType))
                        .collect(Collectors.toList()));
        DynamoPlusService.getInstance().setup(SUFFIX);
    }

    @AfterAll
    public static void clean() {
        DynamoPlusService.getInstance().cleanup(SUFFIX);
    }

    @DisplayName("Test create documents")
    @Test
    @Order(1)
    void createDocuments() {
        testCreateCategory(PULP);
        testCreateCategory(THRILLER);
    }

    @DisplayName("Test query all documents")
    @Test
    @Order(2)
    void queryAllCategories() {
        Either<PaginatedResult<Category>, SdkException> result = clientReadWrite.queryAll(
                CATEGORY_COLLECTION_NAME,
                null,
                null,
                Category.class);
        result.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(result.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(r -> r.getData().size() == 2, "expected size 1"))
                .hasValueSatisfying(new Condition<>(r -> r.getLastKey() == null, "expected no other results"))
                .hasValueSatisfying(new Condition<>(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName()) || c.getName().equals(THRILLER.getName())), "expected category found"));
    }

    @DisplayName("Test query category by name")
    @Test
    @Order(2)
    void queryCategoriesByName() {
        Either<PaginatedResult<Category>, SdkException> result = clientReadWrite.queryByIndex(
                CATEGORY_COLLECTION_NAME,
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
                .hasValueSatisfying(new Condition<>(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName())), "expected category found"))
        ;
    }

    @DisplayName("Test create forbidden")
    @Test
    @Order(3)
    void createForbidden() {
        Either<Category, SdkException> documentResult1 = clientReadOnly.createDocument(CATEGORY_COLLECTION_NAME,
                PULP,
                Category.class);
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

        Either<PaginatedResult<Book>, SdkException> result = clientReadWrite.queryAll(BOOK_COLLECTION_NAME, null, null, Book.class);
        assertThat(result.error())
                .get()
                .matches(e -> e instanceof SdkHttpException)
                .extracting(e -> (SdkHttpException) e)
                .matches(e -> e.getHttpCode() == 403);
    }

    private void testCreateCategory(Category category) {
        Either<Category, SdkException> documentResult1 = clientReadWrite.createDocument(CATEGORY_COLLECTION_NAME,
                category,
                Category.class);
        documentResult1.error().ifPresent(e -> fail(e.getMessage(), e));
        assertThat(documentResult1.ok())
                .isPresent()
                .hasValueSatisfying(new Condition<>(c -> c.getName().equals(category.getName()), "name must match"));
    }

}
