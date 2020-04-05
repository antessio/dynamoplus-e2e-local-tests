package antessio.dynamoplus;


import antessio.dynamoplus.domain.Book;
import antessio.dynamoplus.domain.Category;
import antessio.dynamoplus.sdk.*;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Unit test for simple App.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ApiKeyClientTest {

    public static final String SUFFIX = "__api_key_test";
    public static final String CATEGORY_COLLECTION_NAME = String.format("category_%s", SUFFIX);
    public static final String BOOK_COLLECTION_NAME = String.format("book_%s", SUFFIX);
    private static SDKV2 clientReadWrite;
    private static SDKV2 clientReadOnly;
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
        PaginatedResult<Category> result = clientReadWrite.queryAll(
                CATEGORY_COLLECTION_NAME,
                null,
                null,
                Category.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 2, "expected size 2")
                .matches(r -> r.getLastKey() == null, "expected no other results")
                .matches(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName()) || c.getName().equals(THRILLER.getName())), "expected category found");
    }

    @DisplayName("Test query category by name")
    @Test
    @Order(2)
    void queryCategoriesByName() {
        PaginatedResult<Category> result = clientReadWrite.queryByIndex(
                CATEGORY_COLLECTION_NAME,
                "category__name",
                new QueryBuilder<Category>()
                        .matches(Category.builder().name("Pulp").build())
                        .build(),
                Category.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 1, "expected size 1")
                .matches(r -> r.getLastKey() == null, "expected no other results")
                .matches(r -> r.getData().stream().allMatch(c -> c.getName().equals(PULP.getName())), "expected category found")
        ;
    }

    @DisplayName("Test create forbidden")
    @Test
    @Order(3)
    void createForbidden() {

        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> clientReadOnly.createDocument(CATEGORY_COLLECTION_NAME,
                        PULP,
                        Category.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);

    }

    @DisplayName("Test query forbidden")
    @Test
    @Order(4)
    void queryForbidden() {
        assertThatExceptionOfType(SdkException.class)
                .isThrownBy(() -> clientReadWrite.queryAll(BOOK_COLLECTION_NAME, null, null, Book.class)
                )
                .isInstanceOf(SdkHttpException.class)
                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
    }

    private void testCreateCategory(Category category) {
        Category documentResult1 = clientReadWrite.createDocument(CATEGORY_COLLECTION_NAME,
                category,
                Category.class);
        assertThat(documentResult1)
                .matches(c -> c.getName().equals(category.getName()), "name must match");
    }

}
