package antessio.dynamoplus;

import org.junit.platform.runner.JUnitPlatform;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.junit.runner.RunWith;

@RunWith(JUnitPlatform.class)
@SuiteDisplayName("End To End Tests")
@SelectClasses({AdminTest.class, ApiKeyClientTest.class, HttpSignatureClientTest.class})
public class TestSuite {
}