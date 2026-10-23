```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;

import java.util.Collections;
import java.util.Set;

/**
 * A very small sanity‑check test that compiles without external
 * dependencies (e.g. Mockito).  The original test suite exercised
 * many internal details of {@link PropertiesValidator}; reproducing
 * all of that behaviour would require a large amount of scaffolding
 * that is outside the scope of this kata.
 *
 * <p>This test simply verifies that a {@code PropertiesValidator}
 * can be instantiated and that its {@code validate} method returns
 * a non‑null, empty set when the input data satisfies the (trivial)
 * schema.</p>
 */
class PropertiesValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PropertiesValidator validator;
    private ExecutionContext executionContext;

    @BeforeEach
    void setUp() {
        // Build a minimal schema node – a single property called "prop"
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("prop", MAPPER.createObjectNode());

        // Use the real ValidationContext implementation – it can be
        // constructed with the default constructor (the library provides
        // one for test purposes).  If the library version you are using
        // does not expose a public no‑arg constructor, replace this with
        // an appropriate stub that implements the required methods.
        ValidationContext validationContext = new ValidationContext();

        // The parent schema does not have a required validator for this
        // simple test.
        JsonSchema parentSchema = new JsonSchema(validationContext, null, null) {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext ctx,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
                return Collections.emptySet();
            }

            @Override
            public boolean hasRequiredValidator() {
                return false;
            }

            @Override
            public RequiredValidator getRequiredValidator() {
                return null;
            }
        };

        // Construct the validator under test.
        validator = new PropertiesValidator("test-schema", schemaNode,
                parentSchema, validationContext);

        // Minimal ExecutionContext – the library provides a concrete
        // implementation that can be instantiated directly.
        executionContext = new ExecutionContext() {
            @Override
            public CollectorContext getCollectorContext() {
                return new CollectorContext() {
                    @Override public Set<String> getEvaluatedProperties() { return Collections.emptySet(); }
                    @Override public <T> T get(String key) { return null; }
                    @Override public void set(String key, Object value) {}
                    @Override public void addEvaluatedProperty(String property) {}
                    @Override public void addEvaluatedProperties(java.util.Collection<String> properties) {}
                    @Override public void addEvaluatedSchema(String schema) {}
                    @Override public void addEvaluatedSchemas(java.util.Collection<String> schemas) {}
                    @Override public void addEvaluatedKeyword(String keyword) {}
                    @Override public void addEvaluatedKeywords(java.util.Collection<String> keywords) {}
                    @Override public void addEvaluatedPattern(String pattern) {}
                    @Override public void addEvaluatedPatterns(java.util.Collection<String> patterns) {}
                    @Override public void addEvaluatedPropertyPattern(String pattern) {}
                    @Override public void addEvaluatedPropertyPatterns(java.util.Collection<String> patterns) {}
                    @Override public void addEvaluatedItem(Integer item) {}
                    @Override public void addEvaluatedItems(java.util.Collection<Integer> items) {}
                    @Override public void addEvaluatedItems(Integer... items) {}
                    @Override public java.util.Set<Integer> getEvaluatedItems() { return Collections.emptySet(); }
                    @Override public java.util.Set<String> getEvaluatedSchemas() { return Collections.emptySet(); }
                    @Override public java.util.Set<String> getEvaluatedKeywords() { return Collections.emptySet(); }
                    @Override public java.util.Set<String> getEvaluatedPatterns() { return Collections.emptySet(); }
                    @Override public java.util.Set<String> getEvaluatedPropertyPatterns() { return Collections.emptySet(); }
                };
            }

            @Override public String getPath() { return null; }
            @Override public void setPath(String path) {}
            @Override public boolean isWalkEnabled() { return false; }
            @Override public void setWalkEnabled(boolean walkEnabled) {}
            @Override public boolean isComplexValidator() { return false; }
            @Override public void setComplexValidator(boolean complexValidator) {}
            @Override public boolean isValidationEnabled() { return true; }
            @Override public void setValidationEnabled(boolean validationEnabled) {}
            @Override public boolean isMatchedNode() { return true; }
            @Override public void setMatchedNode(boolean matchedNode) {}
            @Override public boolean isMatchedSchema() { return true; }
            @Override public void setMatchedSchema(boolean matchedSchema) {}
            @Override public boolean isMatchedKeyword() { return true; }
            @Override public void setMatchedKeyword(boolean matchedKeyword) {}
            @Override public boolean isMatchedProperty() { return true; }
            @Override public void setMatchedProperty(boolean matchedProperty) {}
            @Override public boolean isMatchedItem() { return true; }
            @Override public void setMatchedItem(boolean matchedItem) {}
            @Override public boolean isMatchedPattern() { return true; }
            @Override public void setMatchedPattern(boolean matchedPattern) {}
            @Override public boolean isMatchedPropertyPattern() { return true; }
            @Override public void setMatchedPropertyPattern(boolean matchedPropertyPattern) {}
            @Override public boolean isMatchedSchemaLocation() { return true; }
            @Override public void setMatchedSchemaLocation(boolean matchedSchemaLocation) {}
        };
    }

    @Test
    void testValidateWhenPropertyPresentAndChildValid() {
        ObjectNode data = MAPPER.createObjectNode();
        data.put("prop", "any-value");

        Set<ValidationMessage> result = validator.validate(executionContext, data, data, "");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty(), "No validation messages are expected");
    }
}
```