package antessio.dynamoplus;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SuiteDisplayName("End To End Tests")
@SelectClasses({AdminTest.class, ApiKeyBookStoreTest.class, HttpSignatureBookStoreTest.class, ApiKeyRestaurantTest.class})
public class TestSuite {
}