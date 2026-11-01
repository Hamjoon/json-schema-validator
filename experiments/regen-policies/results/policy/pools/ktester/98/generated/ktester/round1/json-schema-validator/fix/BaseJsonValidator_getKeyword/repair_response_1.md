```java
package com.networknt.schema;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#getKeyword()}.
 *
 * The test does not rely on any external mocking framework; it supplies the
 * minimal required constructor arguments (mostly {@code null}) and injects a
 * {@link Keyword} implementation via reflection.
 */
class BaseJsonValidator_getKeyword_Test {

    /** System under test. */
    private TestValidator validator;

    /** Simple {@link Keyword} implementation that returns a configurable value. */
    private static class DummyKeyword implements Keyword {

        private final String value;

        DummyKeyword(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        // The remaining methods are not needed for these tests.
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
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // All constructor arguments except the keyword are irrelevant for the getKeyword() tests,
        // so we pass {@code null} for them.
        validator = new TestValidator(
                null,               // SchemaLocation
                null,               // JsonNodePath
                null,               // JsonNode (schemaNode)
                null,               // JsonSchema (parentSchema)
                new DummyKeyword("initial"),
                null                // ValidationContext
        );

        // Inject the keyword that we want getKeyword() to return.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword("testKeyword"));
    }

    @Test
    void getKeyword_returnsConfiguredKeywordValue() {
        String result = validator.getKeyword();
        assertEquals("testKeyword", result,
                "BaseJsonValidator#getKeyword() should return the value supplied by the Keyword instance");
    }

    /**
     * Parameterised test that verifies the method works for any keyword value.
     *
     * @param keyword the keyword string to be returned by the dummy keyword
     */
    @ParameterizedTest
    @ValueSource(strings = { "type", "properties" })
    void getKeyword_returnsVariousKeywordValues(String keyword) throws Exception {
        // Replace the internal keyword with a new one for each iteration.
        Field keywordField = ValidationMessageHandler.class.getDeclaredField("keyword");
        keywordField.setAccessible(true);
        keywordField.set(validator, new DummyKeyword(keyword));

        String result = validator.getKeyword();
        assertEquals(keyword, result,
                "BaseJsonValidator#getKeyword() should return the keyword value supplied at construction");
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

        assertThrows(NullPointerException.class, validator::getKeyword,
                "BaseJsonValidator#getKeyword() should throw NPE when the internal keyword field is null");
    }
}
```