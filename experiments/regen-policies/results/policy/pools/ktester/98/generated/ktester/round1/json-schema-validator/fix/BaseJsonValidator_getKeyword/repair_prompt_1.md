
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.math.BigInteger;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import org.mockito.*;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getKeyword_Test {

    // ----- Mocks for constructor dependencies -----
    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode // also used as Keyword
    validatorType;

    @Mock
    ValidationContext validationContext;

    // ----- System under test -----
    private TestValidator validator;

    // ----- Minimal stubs for constructor dependencies -----
    private static class DummySchemaLocation implements SchemaLocation {
        /* no behaviour needed */
    }

    private static class DummyJsonNodePath implements JsonNodePath {
        /* no behaviour needed */
    }

    private static class DummyJsonNode implements JsonNode {

        // Implement only the methods required by the compiler; all can return defaults.
        @Override
        public JsonNode get(String fieldName) {
            return null;
        }

        @Override
        public JsonNode path(String fieldName) {
            return null;
        }

        @Override
        public JsonNode get(int index) {
            return null;
        }

        @Override
        public JsonNode path(int index) {
            return null;
        }

        @Override
        public JsonNode at(JsonPointer ptr) {
            return null;
        }

        @Override
        public JsonNode at(String jsonPointerExpression) {
            return null;
        }

        @Override
        public JsonNodeType getNodeType() {
            return null;
        }

        @Override
        public boolean isValueNode() {
            return false;
        }

        @Override
        public boolean isContainerNode() {
            return false;
        }

        @Override
        public boolean isMissingNode() {
            return false;
        }

        @Override
        public boolean isArray() {
            return false;
        }

        @Override
        public boolean isObject() {
            return false;
        }

        @Override
        public boolean isNumber() {
            return false;
        }

        @Override
        public boolean isIntegralNumber() {
            return false;
        }

        @Override
        public boolean isFloatingPointNumber() {
            return false;
        }

        @Override
        public boolean isBinary() {
            return false;
        }

        @Override
        public boolean isTextual() {
            return false;
        }

        @Override
        public boolean isBoolean() {
            return false;
        }

        @Override
        public boolean isNull() {
            return false;
        }

        @Override
        public boolean asBoolean() {
            return false;
        }

        @Override
        public boolean asBoolean(boolean defaultValue) {
            return defaultValue;
        }

        @Override
        public int asInt() {
            return 0;
        }

        @Override
        public int asInt(int defaultValue) {
            return defaultValue;
        }

        @Override
        public long asLong() {
            return 0L;
        }

        @Override
        public long asLong(long defaultValue) {
            return defaultValue;
        }

        @Override
        public double asDouble() {
            return 0.0;
        }

        @Override
        public double asDouble(double defaultValue) {
            return defaultValue;
        }

        @Override
        public String asText() {
            return null;
        }

        @Override
        public String asText(String defaultValue) {
            return defaultValue;
        }

        @Override
        public byte[] binaryValue() {
            return null;
        }

        @Override
        public JsonParser.NumberType numberType() {
            return null;
        }

        @Override
        public Number numberValue() {
            return null;
        }

        @Override
        public short shortValue() {
            return 0;
        }

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0L;
        }

        @Override
        public float floatValue() {
            return 0f;
        }

        @Override
        public double doubleValue() {
            return 0.0;
        }

        @Override
        public BigDecimal decimalValue() {
            return null;
        }

        @Override
        public BigInteger bigIntegerValue() {
            return null;
        }

        @Override
        public JsonNode get(String fieldName, JsonNode defaultValue) {
            return defaultValue;
        }

        @Override
        public JsonNode get(int index, JsonNode defaultValue) {
            return defaultValue;
        }

        @Override
        public JsonNode findValue(String fieldName) {
            return null;
        }

        @Override
        public java.util.Iterator<JsonNode> elements() {
            return Collections.emptyIterator();
        }

        @Override
        public java.util.Iterator<String> fieldNames() {
            return Collections.emptyIterator();
        }

        @Override
        public JsonNode findParent(String fieldName) {
            return null;
        }

        @Override
        public java.util.Iterator<JsonNode> findParents(String fieldName) {
            return Collections.emptyIterator();
        }

        @Override
        public java.util.Iterator<JsonNode> findValues(String fieldName) {
            return Collections.emptyIterator();
        }

        @Override
        public java.util.Iterator<String> findValuesAsText(String fieldName) {
            return Collections.emptyIterator();
        }

        @Override
        public JsonNode deepCopy() {
            return null;
        }

        @Override
        public JsonNode with(String propertyName) {
            return null;
        }

        @Override
        public JsonNode withArray(String propertyName) {
            return null;
        }

        @Override
        public JsonNode without(String propertyName) {
            return null;
        }

        @Override
        public JsonNode set(String propertyName, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode replace(String propertyName, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode replace(int index, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode set(int index, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode remove(String propertyName) {
            return null;
        }

        @Override
        public JsonNode remove(int index) {
            return null;
        }

        @Override
        public JsonNode removeAll() {
            return null;
        }

        @Override
        public JsonNode retainAll(JsonNode elements) {
            return null;
        }

        @Override
        public JsonNode retainAll(java.util.Collection<JsonNode> elements) {
            return null;
        }

        @Override
        public JsonNode insert(int index, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode insertArray(int index, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode insertObject(int index, JsonNode value) {
            return null;
        }

        @Override
        public JsonNode setAll(java.util.Iterator<JsonNode> elements) {
            return null;
        }

        @Override
        public JsonNode setAll(java.util.Collection<JsonNode> elements) {
            return null;
        }

        @Override
        public JsonNode replaceAll(java.util.Iterator<JsonNode> elements) {
            return null;
        }

        @Override
        public JsonNode replaceAll(java.util.Collection<JsonNode> elements) {
            return null;
        }

        @Override
        public JsonNode path(String fieldName, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode path(int index, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode get(String fieldName, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode get(int index, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode findValue(String fieldName, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode findParent(String fieldName, JsonNode defaultNode) {
            return null;
        }

        @Override
        public JsonNode findPath(String fieldName) {
            return null;
        }

        @Override
        public JsonNode findValue(String fieldName, JsonNode defaultNode, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode get(String fieldName, JsonNode defaultNode, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode path(String fieldName, JsonNode defaultNode, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode get(int index, JsonNode defaultNode, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode path(int index, JsonNode defaultNode, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode findPath(String fieldName, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode findValue(String fieldName, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode findParent(String fieldName, boolean includeMissing) {
            return null;
        }

        @Override
        public JsonNode findParents(String fieldName, boolean includeMissing) {
            return null;
        }

        @Override
        public java.util.Iterator<JsonNode> findValues(String fieldName, boolean includeMissing) {
            return Collections.emptyIterator();
        }

        @Override
        public java.util.Iterator<String> findValuesAsText(String fieldName, boolean includeMissing) {
            return Collections.emptyIterator();
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }

        @Override
        public String toString() {
            return "";
        }
    }

    private static class DummyJsonSchema implements JsonSchema {
        /* no behaviour needed */
    }

    private static class DummyValidationContext implements ValidationContext {
        /* no behaviour needed */
    }

    // ----- Minimal Keyword implementation used for the tests -----
    private static class DummyKeyword implements Keyword {

        private final String value;

        DummyKeyword(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        // The remaining methods are not used by the tests.
        @Override
        public JsonNode getSchemaNode() {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return null;
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return null;
        }

        @Override
        public boolean isValid(JsonNode node) {
            return true;
        }

        @Override
        public String getMessage() {
            return null;
        }

        @Override
        public String getMessageKey() {
            return null;
        }

        @Override
        public String getMessageKey(String key) {
            return null;
        }

        @Override
        public String getMessageKey(String key, String defaultMessage) {
            return null;
        }

        @Override
        public String getMessageKey(String key, Object... args) {
            return null;
        }

        @Override
        public String getMessageKey(String key, String defaultMessage, Object... args) {
            return null;
        }

        @Override
        public String getMessageKey(String key, java.util.Locale locale) {
            return null;
        }

        @Override
        public String getMessageKey(String key, java.util.Locale locale, Object... args) {
            return null;
        }

        @Override
        public String getMessageKey(String key, java.util.Locale locale, String defaultMessage) {
            return null;
        }

        @Override
        public String getMessageKey(String key, java.util.Locale locale, String defaultMessage, Object... args) {
            return null;
        }
    }

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} that only
     * implements the abstract {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, Keyword validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The current version of {@code JsonValidator} defines a 4‑argument validate method.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the validator with a dummy keyword – the actual value will be injected via reflection.
        validator = new TestValidator(new DummySchemaLocation(), new DummyJsonNodePath(), new DummyJsonNode(), new DummyJsonSchema(), new DummyKeyword("initial"), new DummyValidationContext());
        // Inject the keyword that we want getKeyword() to return.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword("testKeyword"));
    }

    @Test
    void getKeyword_returnsConfiguredKeywordValue() {
        String result = validator.getKeyword();
        assertEquals("testKeyword", result, "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance");
    }

    /**
     * Parameterised test that verifies the method works for any keyword value.
     *
     * @param keyword the keyword string to be returned by the mock
     */
    @ParameterizedTest
    @ValueSource(strings = { "type", "properties" })
    void getKeyword_returnsVariousKeywordValues(String keyword) {
        // Replace the internal keyword with a new one for each iteration.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword(keyword));
        String result = validator.getKeyword();
        assertEquals(keyword, result, "BaseJsonValidator#getKeyword() should return the keyword value supplied at construction");
    }

    /**
     * When the internal {@code keyword} field (in {@link ValidationMessageHandler})
     * is {@code null}, {@code getKeyword()} is expected to throw a {@link NullPointerException}.
     */
    @Test
    void getKeyword_throwsNPEWhenKeywordIsNull() throws Exception {
        // Set the protected field to null.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, null);
        assertThrows(NullPointerException.class, validator::getKeyword, "BaseJsonValidator#getKeyword() should throw NPE when the internal keyword field is null");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:44: error: interface expected here
    private static class DummySchemaLocation implements SchemaLocation {
                                                        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:48: error: interface expected here
    private static class DummyJsonNodePath implements JsonNodePath {
                                                      ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:52: error: interface expected here
    private static class DummyJsonNode implements JsonNode {
                                                  ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:76: error: cannot find symbol
        public JsonNode at(JsonPointer ptr) {
                           ^
  symbol:   class JsonPointer
  location: class DummyJsonNode
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:206: error: package JsonParser does not exist
        public JsonParser.NumberType numberType() {
                         ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:406: error: method get(String,JsonNode) is already defined in class DummyJsonNode
        public JsonNode get(String fieldName, JsonNode defaultNode) {
                        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:411: error: method get(int,JsonNode) is already defined in class DummyJsonNode
        public JsonNode get(int index, JsonNode defaultNode) {
                        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:501: error: interface expected here
    private static class DummyJsonSchema implements JsonSchema {
                                                    ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:505: error: interface expected here
    private static class DummyValidationContext implements ValidationContext {
                                                           ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:17: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:37: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:612: error: cannot find symbol
    @BeforeEach
     ^
  symbol:   class BeforeEach
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:622: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:648: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_getKeyword_Test
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:55: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:60: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:65: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:70: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:75: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:80: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:85: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:90: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:95: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:100: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:105: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:110: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:115: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:120: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:125: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:130: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:135: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:140: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:145: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:150: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:155: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:160: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:165: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:170: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:175: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:180: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:185: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:190: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:195: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:200: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:205: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:210: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:215: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:220: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:225: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:230: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:235: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:240: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:245: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:250: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:255: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:260: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:265: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:270: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:275: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:280: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:285: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:290: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:295: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:300: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:305: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:310: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:315: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:320: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:325: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:330: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:335: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:340: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:345: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:350: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:355: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:360: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:365: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:370: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:375: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:380: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:385: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:390: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:395: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:400: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:405: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:410: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:415: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:420: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:425: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:430: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:435: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:440: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_getKeyword_Test.java:445: error: method does not override or implement a method from a supertype
        @Override
        ^
Note: Some messages have been simplified; recompile with -Xdiags:verbose to get full output
100 errors
only showing the first 100 errors, of 124 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'dependent classes': 'class `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;\n\trelated functions:\n\t\tmethod `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`;\nclass `com.networknt.schema.annotation.JsonNodeAnnotation.Builder`:\n\trelated functions:\n\t\tmethod `public Builder keyword(Keyword)` returns `com.networknt.schema.annotation.JsonNodeAnnotation.Builder`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`;\nclass `com.networknt.schema.JsonMetaSchema.Builder`:\n\trelated functions:\n\t\tmethod `public JsonMetaSchema build()` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`;\n\t\tmethod `private Map<String, Keyword> createKeywordsMap(Map<String, Keyword>, Map<String, Format>)` returns `java.util.Map<java.lang.String, com.networknt.schema.Keyword>`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`;\n\t\tmethod `public Builder keywords(Collection<? extends Keyword>)` returns `com.networknt.schema.JsonMetaSchema.Builder`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`, api document: \r\n         * Adds the keywords.\r\n         * \r\n         * @param keywords the keywords\r\n         * @return the builder\r\n         ;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public JsonSchema withConfig(SchemaValidatorsConfig)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.BaseJsonValidator.getKeyword()`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


