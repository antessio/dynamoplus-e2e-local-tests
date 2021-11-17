package antessio.dynamoplus;

import antessio.dynamoplus.domain.*;
import antessio.dynamoplus.sdk.PaginatedResult;
import antessio.dynamoplus.sdk.SDKV2;
import antessio.dynamoplus.sdk.domain.system.aggregation.*;
import antessio.dynamoplus.sdk.domain.system.aggregation.payload.count.AggregationCountPayload;
import antessio.dynamoplus.sdk.domain.system.clientauthorization.ClientScope;
import antessio.dynamoplus.sdk.domain.system.collection.*;
import antessio.dynamoplus.sdk.domain.system.collection.Collection;
import org.assertj.core.groups.Tuple;
import org.assertj.core.internal.FieldByFieldComparator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

public abstract class RestaurantTest {
    private final static String NUMBERS = "0123456789";

    protected static final String RESTAURANT_BASE_COLLECTION_NAME = "restaurant";
    protected static final String REVIEW_BASE_COLLECTION_NAME = "review";
    protected static final String BOOKING_BASE_COLLECTION_NAME = "booking";

    protected static final Restaurant PIZZERIA = Restaurant.builder()
            .id(UUID.randomUUID())
            .name("Vurria")
            .type("PIZZERIA")
            .build();
    protected static final Restaurant SUSHI = Restaurant.builder()
            .id(UUID.randomUUID())
            .name("Arigato")
            .type("SUSHI")
            .build();

    protected static final Restaurant VEGAN = Restaurant.builder()
            .id(UUID.randomUUID())
            .name("Flower Burger")
            .type("VEGAN")
            .build();
    protected static final List<Restaurant> RESTAURANTS = Arrays.asList(PIZZERIA, SUSHI, VEGAN);
    private static final List<String> USERS = Arrays.asList(
            "antessio", "frizzy", "cioppi"
    );


    public static int getRandomInRange(int min, int max) {
        return new Random().nextInt(max - min + 1) + min;
    }

    public static long getRandomInRange(long min, long max) {
        return min + (long) (Math.random() * (max - min));

    }

    protected static final List<Review> REVIEWS = USERS.stream()
            .flatMap(u -> Stream.of(PIZZERIA, SUSHI, VEGAN)
                    .map(r -> Review.builder()
                            .username(u)
                            .restaurantId(r.getId())
                            .rate(getRandomInRange(4, 10))
                            .build()
                    ))
            .collect(Collectors.toList());

    protected static final List<Booking> BOOKINGS = USERS.stream()
            .flatMap(u -> Stream.of(PIZZERIA, SUSHI, VEGAN)
                    .map(r -> Booking.builder()
                            .username(u)
                            .restaurantId(r.getId())
                            .amount(getRandomInRange(100L, 19200L))
                            .at(new Date())
                            .build()
                    ))
            .collect(Collectors.toList());


    protected static Map<String, SDKV2> registeredClients = new HashMap<>();

    protected static final String CLIENT_ID_RESTAURANT_READ_ONLY = "client-id-restaurant-readonly";
    protected static final String CLIENT_ID_RESTAURANT_READ_WRITE = "client-id-restaurant-rw";
    protected static final String CLIENT_ID_REVIEW_READ_ONLY = "client-id-review-readonly";
    protected static final String CLIENT_ID_REVIEW_READ_WRITE = "client-id-review-rw";
    protected static final String CLIENT_ID_BOOKING_READ_WRITE = "client-id-booking-rw";

    protected abstract SDKV2 getClient(String clientId, List<ClientScope> scopes);


    protected static void initialize(String suffix) {
        Collection restaurant = DynamoPlusService.getInstance().getOrCreateCollection("restaurant",
                new CollectionBuilder()
                        .idKey("id")
                        .name(getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, suffix))
                        .autoGenerateId(true)
                        .fields(Arrays.asList(
                                new AttributeBuilder()
                                        .attributeName("name")
                                        .attributeType(CollectionAttributeType.STRING)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build(),
                                new AttributeBuilder()
                                        .attributeName("type")
                                        .attributeType(CollectionAttributeType.STRING)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build()
                        ))
                        .createCollection()
        );
        Collection review = DynamoPlusService.getInstance().getOrCreateCollection("review",
                new CollectionBuilder()
                        .idKey("id")
                        .name(getCollectionName(REVIEW_BASE_COLLECTION_NAME, suffix))
                        .autoGenerateId(true)
                        .fields(Arrays.asList(
                                new AttributeBuilder()
                                        .attributeName("rate")
                                        .attributeType(CollectionAttributeType.NUMBER)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build(),
                                new AttributeBuilder()
                                        .attributeName("username")
                                        .attributeType(CollectionAttributeType.STRING)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build(),
                                new AttributeBuilder()
                                        .attributeName("restaurant_id")
                                        .attributeType(CollectionAttributeType.STRING)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build()
                        ))
                        .createCollection()
        );
        Collection booking = DynamoPlusService.getInstance().getOrCreateCollection("booking",
                new CollectionBuilder()
                        .idKey("id")
                        .name(getCollectionName(BOOKING_BASE_COLLECTION_NAME, suffix))
                        .autoGenerateId(true)
                        .fields(Arrays.asList(
                                new AttributeBuilder()
                                        .attributeName("amount")
                                        .attributeType(CollectionAttributeType.NUMBER)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build(),
                                new AttributeBuilder()
                                        .attributeName("at")
                                        .attributeType(CollectionAttributeType.DATE)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build(),
                                new AttributeBuilder()
                                        .attributeName("restaurant_id")
                                        .attributeType(CollectionAttributeType.STRING)
                                        .constraints(Collections.singletonList(CollectionAttributeConstraint.NOT_NULL))
                                        .build()
                        ))
                        .createCollection()
        );
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("type"), restaurant, "order_unique");
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("restaurant_id"), review, "order_unique");
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("username"), review, "order_unique");
        DynamoPlusService.getInstance().createIndex(Collections.singletonList("restaurant_id"), booking, "order_unique");

        DynamoPlusService.getInstance().createAggregationConfigurationCount(restaurant);
        DynamoPlusService.getInstance().createAggregation(new AggregationConfigurationBuilder()
                .withCollection(review)
                .withType(AggregationConfigurationType.AVG)
                .withConfiguration(new AggregationConfigurationPayloadBuilder()
                        .withTargetField("rate")
                        .withOn(DynamoPlusService.ALL_TRIGGERS)
                        .build())
                .build());
        DynamoPlusService.getInstance().createAggregation(new AggregationConfigurationBuilder()
                .withCollection(booking)
                .withType(AggregationConfigurationType.SUM)
                .withConfiguration(new AggregationConfigurationPayloadBuilder()
                        .withTargetField("amount")
                        .withOn(new HashSet<>(Collections.singletonList(
                                AggregationConfigurationTrigger.INSERT
                        )))
                        .build())
                .build());

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
        DynamoPlusService.getInstance().cleanupCollection(getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, suffix), Restaurant.class, () -> "id");
        DynamoPlusService.getInstance().cleanupCollection(getCollectionName(BOOKING_BASE_COLLECTION_NAME, suffix), Booking.class, () -> "id");
        DynamoPlusService.getInstance().cleanupCollection(getCollectionName(REVIEW_BASE_COLLECTION_NAME, suffix), Review.class, () -> "id");

    }

    protected static String getCollectionName(String baseCollection, String suffix) {
        return String.format("%s_%s", baseCollection, suffix);
    }

    protected void testCreateRestaurant(Restaurant restaurant, String suffix) {
        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, suffix);
        Restaurant documentResult1 = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName))
                .createDocument(
                        collectionName,
                        restaurant,
                        Restaurant.class);
        assertThat(documentResult1)
                .usingComparator(new FieldByFieldComparator())
                .isEqualToIgnoringGivenFields(restaurant, "id");
    }

    protected void testCreateReview(Review review, String suffix) {
        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, suffix);
        Review documentResult1 = getClient(CLIENT_ID_REVIEW_READ_WRITE, getClientScopeRW(collectionName))
                .createDocument(
                        collectionName,
                        review,
                        Review.class);
        assertThat(documentResult1)
                .usingComparator(new FieldByFieldComparator())
                .isEqualToIgnoringGivenFields(review, "id");
    }

    protected void testCreateBooking(Booking booking, String suffix) {
        String collectionName = getCollectionName(BOOKING_BASE_COLLECTION_NAME, suffix);
        Booking documentResult1 = getClient(CLIENT_ID_BOOKING_READ_WRITE, getClientScopeRW(collectionName))
                .createDocument(
                        collectionName,
                        booking,
                        Booking.class);
        assertThat(documentResult1)
                .usingComparator(new FieldByFieldComparator())
                .isEqualToIgnoringGivenFields(booking, "id", "at")
                .matches(b -> b.getAt().toInstant().truncatedTo(ChronoUnit.SECONDS).equals(booking.getAt().toInstant().truncatedTo(ChronoUnit.SECONDS)));
    }


    private String getRandomIsbn() {
        return IntStream.range(0, 13)
                .mapToObj(i -> new Random().nextInt(NUMBERS.length()))
                .map(NUMBERS::charAt)
                .map(c -> c + "")
                .collect(Collectors.joining());
    }


    protected abstract String getSuffix();

    @DisplayName("Test create restaurants")
    @Test
    @Order(100)
    void createRestaurants() {
        RESTAURANTS.forEach(r -> testCreateRestaurant(r, getSuffix()));
    }

    @DisplayName("Test create booking")
    @Test
    @Order(101)
    void createBooking() {
        BOOKINGS
                .forEach(b -> testCreateBooking(b,
                        getSuffix()));
    }

    @DisplayName("Test create reviews")
    @Test
    @Order(102)
    void createReviews() {
        REVIEWS
                .forEach(b -> testCreateReview(b,
                        getSuffix()));
    }

    @DisplayName("Test query all restaurants")
    @Test
    @Order(200)
    void queryAllRestaurants() {
        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Restaurant> result = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName))
                .getAll(
                        collectionName,
                        null,
                        null,
                        Restaurant.class);
        assertThat(result)
                .matches(r -> r.getData().size() == 3, "expected size 3")
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results")
                .extracting(r -> r.getData().stream().map(r1 -> tuple(r1.getName(), r1.getType())).collect(Collectors.toList()))
                .asList()
                .containsExactlyInAnyOrder(Stream.of(PIZZERIA, SUSHI, VEGAN)
                        .map(r1 -> tuple(r1.getName(), r1.getType()))
                        .toArray(Tuple[]::new));
    }

    @DisplayName("Test query all bookings")
    @Test
    @Order(201)
    void queryAllBookings() {
        String collectionName = getCollectionName(BOOKING_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Booking> result = getClient(CLIENT_ID_BOOKING_READ_WRITE, getClientScopeRW(collectionName))
                .getAll(
                        collectionName,
                        null,
                        null,
                        Booking.class);
        assertThat(result)
                .matches(b -> b.getData().size() == USERS.size() * 3, "expected size " + USERS.size() * 3)
                .matches(b -> b.getHasMore() != null && b.getHasMore().equals(Boolean.FALSE), "expected no other results")
                .extracting(b -> b.getData().stream().map(b1 -> tuple(b1.getRestaurantId(), b1.getAt().toInstant().truncatedTo(ChronoUnit.SECONDS), b1.getAmount(), b1.getUsername())).collect(Collectors.toList()))
                .asList()
                .containsExactlyInAnyOrder(BOOKINGS.stream()
                        .sorted(Comparator.comparing(Booking::getAt).reversed())
                        .map(b1 -> tuple(b1.getRestaurantId(), b1.getAt().toInstant().truncatedTo(ChronoUnit.SECONDS), b1.getAmount(), b1.getUsername()))
                        .toArray(Tuple[]::new));
    }

    @DisplayName("Test query all reviews")
    @Test
    @Order(202)
    void queryAllReviews() {
        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<Review> result = getClient(CLIENT_ID_REVIEW_READ_WRITE, getClientScopeRW(collectionName))
                .getAll(
                        collectionName,
                        null,
                        null,
                        Review.class);
        assertThat(result)
                .matches(r -> r.getData().size() == USERS.size() * 3, "expected size " + USERS.size() * 3)
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results")
                .extracting(r -> r.getData().stream().map(b1 -> tuple(b1.getRestaurantId(), b1.getRate(), b1.getUsername())).collect(Collectors.toList()))
                .asList()
                .containsExactlyInAnyOrder(REVIEWS.stream()
                        .map(b1 -> tuple(b1.getRestaurantId(), b1.getRate(), b1.getUsername()))
                        .toArray(Tuple[]::new));
    }


    @DisplayName("Test aggregations ")
    @Test
    @Order(203)
    void getAggregations() {
        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, getSuffix());
        PaginatedResult<AggregationConfiguration> result = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName))
                .getAggregationConfigurations(
                        collectionName,
                        20, null);
        assertThat(result)
                .matches(r -> r.getData().size() == 1, "expected size " + 3)
                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");

        assertThat(result.getData().stream().filter(d -> d.getType() == AggregationConfigurationType.COLLECTION_COUNT).findFirst())
                .get()
                .matches(aggregationConfiguration -> aggregationConfiguration.getAggregation() != null)
                .matches(aggregationConfiguration -> aggregationConfiguration.getAggregation().getPayload() instanceof AggregationCountPayload)
                .matches(aggregationConfiguration -> ((AggregationCountPayload) aggregationConfiguration.getAggregation().getPayload()).getCount() == RESTAURANTS.size());
//
//                .extracting(r -> r.getData().stream().map(b1 -> tuple(b1.getAggregation().getPayload())).collect(Collectors.toList()))
//                .asList()
//                .containsExactlyInAnyOrder(REVIEWS.stream()
//                        .map(b1 -> tuple(b1.getRestaurantId(), b1.getRate(), b1.getUsername()))
//                        .toArray(Tuple[]::new));
    }

//    @DisplayName("Test query category by name")
//    @Test
//    @Order(300)
//    void queryCategoriesByName() {
//        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, getSuffix());
//        PaginatedResult<Category> result = getClient(CLIENT_ID_REVIEW_READ_WRITE, getClientScopeRW(collectionName)).query(
//                collectionName,
//                new Query(new PredicateBuilder()
//                        .withEq("name", "Pulp")),
//                Category.class,
//                null,
//                null);
//        assertThat(result)
//                .matches(r -> r.getData().size() == 1, "expected size 1")
//                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results")
//                .matches(r -> r.getData().stream().allMatch(c -> c.getName().equals(PIZZERIA.getName())), "expected category found")
//        ;
//    }
//
//    @DisplayName("Test query books by author")
//    @Test
//    @Order(301)
//    void queryBooksByAuthor() {
//        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
//        PaginatedResult<Book> result = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName)).query(
//                collectionName,
//                new Query(new PredicateBuilder().withEq("author", CHUCK_PALHANIUK)),
//                Book.class, null, null);
//        assertThat(result)
//                .matches(r -> r.getData().size() == 2, "expected size 2 ")
//                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
//        AssertionsForInterfaceTypes.assertThat(result.getData())
//                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
//                .contains(
//                        tuple("Fight Club", CHUCK_PALHANIUK),
//                        tuple("Choke", CHUCK_PALHANIUK)
//                );
//    }
//
//    @DisplayName("Test query books by category")
//    @Test
//    @Order(302)
//    void queryBooksByCategory() {
//        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
//        PaginatedResult<Book> result = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName)).query(
//                collectionName,
//                new Query(new PredicateBuilder().withEq("category.name", THRILLER.getName())),
//                Book.class,
//                null,
//                null);
//        assertThat(result)
//                .matches(r -> r.getData().size() == 1, "expected size 1")
//                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
//        AssertionsForInterfaceTypes.assertThat(result.getData())
//                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
//                .contains(
//                        tuple("Män som hatar kvinnor", "Stieg Larsson")
//                );
//    }
//
//    @DisplayName("Test query books by category")
//    @Test
//    @Order(303)
//    void queryBooksByCategoryAndRating() {
//        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
//        PaginatedResult<Book> result = getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName)).query(
//                collectionName,
//                new Query(new PredicateBuilder()
//                        .withAnd(Arrays.asList(
//                                new Eq("category.name", PIZZERIA.getName()),
//                                new Range("rating", "07", "09")
//                        ))),
//                Book.class,
//                null,
//                null);
//        assertThat(result)
//                .matches(r -> r.getHasMore() != null && r.getHasMore().equals(Boolean.FALSE), "expected no other results");
//        AssertionsForInterfaceTypes.assertThat(result.getData())
//                .hasSize(2)
//                .extracting(b -> tuple(b.getTitle(), b.getAuthor()))
//                .contains(
//                        tuple("Fight Club", CHUCK_PALHANIUK),
//                        tuple("Choke", CHUCK_PALHANIUK)
//                );
//    }
//
//    @DisplayName("Test create forbidden")
//    @Test
//    @Order(400)
//    void createCategoryForbidden() {
//
//        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, getSuffix());
//        assertThatExceptionOfType(SdkException.class)
//                .isThrownBy(() -> getClient(CLIENT_ID_REVIEW_READ_ONLY, getClientScopeR(collectionName)).createDocument(collectionName,
//                        PIZZERIA,
//                        Category.class)
//                )
//                .isInstanceOf(SdkHttpException.class)
//                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
//
//    }
//
//    @DisplayName("Test query book forbidden")
//    @Test
//    @Order(500)
//    void queryBookForbidden() {
//        String collectionName = getCollectionName(RESTAURANT_BASE_COLLECTION_NAME, getSuffix());
//        assertThatExceptionOfType(SdkException.class)
//                .isThrownBy(() -> getClient(CLIENT_ID_RESTAURANT_READ_WRITE, getClientScopeRW(collectionName)).getAll(collectionName, null, null, Book.class))
//                .isInstanceOf(SdkHttpException.class)
//                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
//    }
//
//
//    @DisplayName("Test create book forbidden")
//    @Test
//    @Order(501)
//    void createBookForbidden() {
//        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
//
//        assertThatExceptionOfType(SdkException.class)
//                .isThrownBy(() -> getClient(CLIENT_ID_RESTAURANT_READ_ONLY, getClientScopeR(collectionName)).createDocument(collectionName,
//                        Book.builder()
//                                .isbn(getRandomIsbn())
//                                .title("Survivor")
//                                .author(CHUCK_PALHANIUK)
//                                .category(PIZZERIA)
//                                .build(),
//                        Book.class)
//                )
//                .isInstanceOf(SdkHttpException.class)
//                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
//
//    }
//
//    @DisplayName("Test query category forbidden")
//    @Test
//    @Order(5002)
//    void queryCategoryForbidden() {
//        String collectionName = getCollectionName(REVIEW_BASE_COLLECTION_NAME, getSuffix());
//        assertThatExceptionOfType(SdkException.class)
//                .isThrownBy(() -> getClient(CLIENT_ID_REVIEW_READ_WRITE, getClientScopeRW(collectionName)).getAll(
//                        collectionName, null, null, Category.class)
//                )
//                .isInstanceOf(SdkHttpException.class)
//                .matches(e -> ((SdkHttpException) e).getHttpCode() == 403);
//    }


}
