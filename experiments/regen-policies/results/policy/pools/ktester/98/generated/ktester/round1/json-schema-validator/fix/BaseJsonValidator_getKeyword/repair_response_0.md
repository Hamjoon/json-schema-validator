```java
package com.networknt.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getKeyword()}.
 *
 * <p>This test does not rely on Mockito; instead it creates minimal stub
 * implementations of the required interfaces and uses reflection to
 * manipulate the protected {@code keyword} field defined in
 * {@link ValidationMessageHandler}.</p>
 */
class BaseJsonValidator_getKeyword_Test {

    // ----- Minimal stubs for constructor dependencies -----
    private static class DummySchemaLocation implements SchemaLocation { /* no behaviour needed */ }

    private static class DummyJsonNodePath implements JsonNodePath { /* no behaviour needed */ }

    private static class DummyJsonNode implements JsonNode {
        // Implement only the methods required by the compiler; all can return defaults.
        @Override public JsonNode get(String fieldName) { return null; }
        @Override public JsonNode path(String fieldName) { return null; }
        @Override public JsonNode get(int index) { return null; }
        @Override public JsonNode path(int index) { return null; }
        @Override public JsonNode at(JsonPointer ptr) { return null; }
        @Override public JsonNode at(String jsonPointerExpression) { return null; }
        @Override public JsonNodeType getNodeType() { return null; }
        @Override public boolean isValueNode() { return false; }
        @Override public boolean isContainerNode() { return false; }
        @Override public boolean isMissingNode() { return false; }
        @Override public boolean isArray() { return false; }
        @Override public boolean isObject() { return false; }
        @Override public boolean isNumber() { return false; }
        @Override public boolean isIntegralNumber() { return false; }
        @Override public boolean isFloatingPointNumber() { return false; }
        @Override public boolean isBinary() { return false; }
        @Override public boolean isTextual() { return false; }
        @Override public boolean isBoolean() { return false; }
        @Override public boolean isNull() { return false; }
        @Override public boolean asBoolean() { return false; }
        @Override public boolean asBoolean(boolean defaultValue) { return defaultValue; }
        @Override public int asInt() { return 0; }
        @Override public int asInt(int defaultValue) { return defaultValue; }
        @Override public long asLong() { return 0L; }
        @Override public long asLong(long defaultValue) { return defaultValue; }
        @Override public double asDouble() { return 0.0; }
        @Override public double asDouble(double defaultValue) { return defaultValue; }
        @Override public String asText() { return null; }
        @Override public String asText(String defaultValue) { return defaultValue; }
        @Override public byte[] binaryValue() { return null; }
        @Override public JsonParser.NumberType numberType() { return null; }
        @Override public Number numberValue() { return null; }
        @Override public short shortValue() { return 0; }
        @Override public int intValue() { return 0; }
        @Override public long longValue() { return 0L; }
        @Override public float floatValue() { return 0f; }
        @Override public double doubleValue() { return 0.0; }
        @Override public BigDecimal decimalValue() { return null; }
        @Override public BigInteger bigIntegerValue() { return null; }
        @Override public JsonNode get(String fieldName, JsonNode defaultValue) { return defaultValue; }
        @Override public JsonNode get(int index, JsonNode defaultValue) { return defaultValue; }
        @Override public JsonNode findValue(String fieldName) { return null; }
        @Override public java.util.Iterator<JsonNode> elements() { return Collections.emptyIterator(); }
        @Override public java.util.Iterator<String> fieldNames() { return Collections.emptyIterator(); }
        @Override public JsonNode findParent(String fieldName) { return null; }
        @Override public java.util.Iterator<JsonNode> findParents(String fieldName) { return Collections.emptyIterator(); }
        @Override public java.util.Iterator<JsonNode> findValues(String fieldName) { return Collections.emptyIterator(); }
        @Override public java.util.Iterator<String> findValuesAsText(String fieldName) { return Collections.emptyIterator(); }
        @Override public JsonNode deepCopy() { return null; }
        @Override public JsonNode with(String propertyName) { return null; }
        @Override public JsonNode withArray(String propertyName) { return null; }
        @Override public JsonNode without(String propertyName) { return null; }
        @Override public JsonNode set(String propertyName, JsonNode value) { return null; }
        @Override public JsonNode replace(String propertyName, JsonNode value) { return null; }
        @Override public JsonNode replace(int index, JsonNode value) { return null; }
        @Override public JsonNode set(int index, JsonNode value) { return null; }
        @Override public JsonNode remove(String propertyName) { return null; }
        @Override public JsonNode remove(int index) { return null; }
        @Override public JsonNode removeAll() { return null; }
        @Override public JsonNode retainAll(JsonNode elements) { return null; }
        @Override public JsonNode retainAll(java.util.Collection<JsonNode> elements) { return null; }
        @Override public JsonNode insert(int index, JsonNode value) { return null; }
        @Override public JsonNode insertArray(int index, JsonNode value) { return null; }
        @Override public JsonNode insertObject(int index, JsonNode value) { return null; }
        @Override public JsonNode setAll(java.util.Iterator<JsonNode> elements) { return null; }
        @Override public JsonNode setAll(java.util.Collection<JsonNode> elements) { return null; }
        @Override public JsonNode replaceAll(java.util.Iterator<JsonNode> elements) { return null; }
        @Override public JsonNode replaceAll(java.util.Collection<JsonNode> elements) { return null; }
        @Override public JsonNode path(String fieldName, JsonNode defaultNode) { return null; }
        @Override public JsonNode path(int index, JsonNode defaultNode) { return null; }
        @Override public JsonNode get(String fieldName, JsonNode defaultNode) { return null; }
        @Override public JsonNode get(int index, JsonNode defaultNode) { return null; }
        @Override public JsonNode findValue(String fieldName, JsonNode defaultNode) { return null; }
        @Override public JsonNode findParent(String fieldName, JsonNode defaultNode) { return null; }
        @Override public JsonNode findPath(String fieldName) { return null; }
        @Override public JsonNode findValue(String fieldName, JsonNode defaultNode, boolean includeMissing) { return null; }
        @Override public JsonNode get(String fieldName, JsonNode defaultNode, boolean includeMissing) { return null; }
        @Override public JsonNode path(String fieldName, JsonNode defaultNode, boolean includeMissing) { return null; }
        @Override public JsonNode get(int index, JsonNode defaultNode, boolean includeMissing) { return null; }
        @Override public JsonNode path(int index, JsonNode defaultNode, boolean includeMissing) { return null; }
        @Override public JsonNode findPath(String fieldName, boolean includeMissing) { return null; }
        @Override public JsonNode findValue(String fieldName, boolean includeMissing) { return null; }
        @Override public JsonNode findParent(String fieldName, boolean includeMissing) { return null; }
        @Override public JsonNode findParents(String fieldName, boolean includeMissing) { return null; }
        @Override public java.util.Iterator<JsonNode> findValues(String fieldName, boolean includeMissing) { return Collections.emptyIterator(); }
        @Override public java.util.Iterator<String> findValuesAsText(String fieldName, boolean includeMissing) { return Collections.emptyIterator(); }
        @Override public boolean equals(Object o) { return this == o; }
        @Override public int hashCode() { return System.identityHashCode(this); }
        @Override public String toString() { return ""; }
    }

    private static class DummyJsonSchema implements JsonSchema { /* no behaviour needed */ }

    private static class DummyValidationContext implements ValidationContext { /* no behaviour needed */ }

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
        @Override public JsonNode getSchemaNode() { return null; }
        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) { return null; }
        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath at) { return null; }
        @Override public boolean isValid(JsonNode node) { return true; }
        @Override public String getMessage() { return null; }
        @Override public String getMessageKey() { return null; }
        @Override public String getMessageKey(String key) { return null; }
        @Override public String getMessageKey(String key, String defaultMessage) { return null; }
        @Override public String getMessageKey(String key, Object... args) { return null; }
        @Override public String getMessageKey(String key, String defaultMessage, Object... args) { return null; }
        @Override public String getMessageKey(String key, java.util.Locale locale) { return null; }
        @Override public String getMessageKey(String key, java.util.Locale locale, Object... args) { return null; }
        @Override public String getMessageKey(String key, java.util.Locale locale, String defaultMessage) { return null; }
        @Override public String getMessageKey(String key, java.util.Locale locale, String defaultMessage, Object... args) { return null; }
    }

    // ----- System under test -----
    private TestValidator validator;

    /**
     * Minimal concrete subclass of {@link BaseJsonValidator} that only
     * implements the abstract {@code validate} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      Keyword validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        // The current version of {@code JsonValidator} defines a 4‑argument validate method.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               JsonNodePath at) {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Create the validator with a dummy keyword – the actual value will be injected via reflection.
        validator = new TestValidator(
                new DummySchemaLocation(),
                new DummyJsonNodePath(),
                new DummyJsonNode(),
                new DummyJsonSchema(),
                new DummyKeyword("initial"),
                new DummyValidationContext()
        );

        // Inject the keyword that we want getKeyword() to return.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword("testKeyword"));
    }

    @Test
    void getKeyword_returnsConfiguredKeywordValue() {
        String result = validator.getKeyword();
        assertEquals("testKeyword",
                result,
                "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance");
    }

    @ParameterizedTest
    @ValueSource(strings = { "type", "properties" })
    void getKeyword_returnsVariousKeywordValues(String keyword) throws Exception {
        // Replace the internal keyword with a new one for each iteration.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword(keyword));

        String result = validator.getKeyword();
        assertEquals(keyword,
                result,
                "BaseJsonValidator#getKeyword() should return the keyword value supplied at construction");
    }

    @Test
    void getKeyword_throwsNPEWhenKeywordIsNull() throws Exception {
        // Set the protected field to null.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, null);

        assertThrows(NullPointerException.class,
                validator::getKeyword,
                "BaseJsonValidator#getKeyword() should throw NPE when the internal keyword field is null");
    }
}
```