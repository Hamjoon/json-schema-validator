```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Tests for {@link PropertiesValidator#walk(ExecutionContext, JsonNode, JsonNode, String, boolean)}.
 *
 * <p>
 * The first two tests exercise the public {@link JsonSchema#walk(JsonNode, boolean)} API
 * (which internally delegates to {@code PropertiesValidator#walk}). The third test
 * targets a very specific edge‑case: when the {@code applyDefaultsStrategy} field of
 * {@code BaseJsonValidator} (the superclass of {@code PropertiesValidator}) is {@code null}.
 * The method attempts to call {@code applyDefaultsStrategy.shouldApplyPropertyDefaults()},
 * which must result in a {@link NullPointerException}.  Because the field is private we
 * set it to {@code null} via reflection.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class PropertiesValidator_walk_Test {

    private static JsonSchemaFactory factory;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version; the exact version is not important for the
        // behaviour we are testing.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /**
     * When {@code shouldValidateSchema} is {@code true} the validator must perform
     * the actual validation and return any {@link ValidationMessage}s produced by
     * the underlying schema.
     */
    @Test
    void walkShouldValidateWhenFlagTrue() throws Exception {
        // schema: property "age" must be an integer
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // node violates the schema – "age" is a string, not an integer
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");

        // invoke the walk method with validation enabled
        ValidationResult result = schema.walk(node, true);

        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertFalse(messages.isEmpty(),
                "Expected validation messages because the instance does not satisfy the schema");
    }

    /**
     * When {@code shouldValidateSchema} is {@code false} the validator must only
     * walk the schema (triggering any walk listeners) but must **not** perform
     * validation. Consequently the returned message set should be empty.
     */
    @Test
    void walkShouldSkipValidationWhenFlagFalse() throws Exception {
        // same schema as above – property "age" must be an integer
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // node still violates the schema, but we ask the walk method *not* to validate
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");

        // invoke the walk method with validation disabled
        ValidationResult result = schema.walk(node, false);

        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertTrue(messages.isEmpty(),
                "Expected no validation messages because validation was disabled");
    }

    /**
     * {@code PropertiesValidator.walk} first checks the {@code applyDefaultsStrategy}
     * field (declared in {@code BaseJsonValidator}).  If that field is {@code null}
     * a {@link NullPointerException} is thrown when the method tries to call
     * {@code applyDefaultsStrategy.shouldApplyPropertyDefaults()}.
     *
     * <p>This test forces the field to {@code null} via reflection and verifies
     * that the expected exception is thrown.</p>
     */
    @Test
    void testWalkApplyDefaultsStrategyNull(@Mock ExecutionContext mockExecutionContext) throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣ Build a schema that contains a "properties" keyword – this guarantees
        //    that a {@code PropertiesValidator} instance exists inside the validator
        //    tree.
        // --------------------------------------------------------------------
        String schemaJson = """
                {
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  }
                }
                """;

        JsonSchema schema = factory.getSchema(schemaJson);

        // --------------------------------------------------------------------
        // 2️⃣ Locate the concrete {@code PropertiesValidator} inside the schema.
        //    The internal structure of {@code JsonSchema} is not part of the public API,
        //    therefore we use reflection.  In the reference implementation the root
        //    validator holds a list called "validators".  We scan that list for an
        //    instance of {@code PropertiesValidator}.
        // --------------------------------------------------------------------
        JsonValidator rootValidator = getRootValidator(schema);
        PropertiesValidator propertiesValidator = findPropertiesValidator(rootValidator);
        Assertions.assertNotNull(propertiesValidator,
                "Unable to locate PropertiesValidator inside the compiled schema – test cannot proceed.");

        // --------------------------------------------------------------------
        // 3️⃣ Set the private field {@code applyDefaultsStrategy} (declared in
        //    {@code BaseJsonValidator}) to {@code null}.
        // --------------------------------------------------------------------
        Field defaultsStrategyField = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        defaultsStrategyField.setAccessible(true);
        defaultsStrategyField.set(propertiesValidator, null);

        // --------------------------------------------------------------------
        // 4️⃣ Prepare a non‑null {@code ObjectNode} as required by the test case.
        // --------------------------------------------------------------------
        ObjectNode node = (ObjectNode) MAPPER.readTree("{\"name\":\"John\"}");

        // --------------------------------------------------------------------
        // 5️⃣ Invoke the method under test and assert that a NPE is thrown.
        // --------------------------------------------------------------------
        Assertions.assertThrows(NullPointerException.class,
                () -> propertiesValidator.walk(mockExecutionContext, node, null, "/", true),
                "Expected NullPointerException because applyDefaultsStrategy was set to null");
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    /**
     * Parses a JSON string into a {@link JsonNode}.
     */
    private static JsonNode getJsonNodeFromStringContent(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    /**
     * Retrieves the root {@link JsonValidator} from a {@link JsonSchema} instance
     * via reflection.  The exact field name depends on the library version; we
     * try the known possibilities.
     */
    private static JsonValidator getRootValidator(JsonSchema schema) throws Exception {
        // The field is called "validator" in many versions.
        Field validatorField;
        try {
            validatorField = JsonSchema.class.getDeclaredField("validator");
        } catch (NoSuchFieldException e) {
            // Fallback to "validators" (a list) – pick the first element.
            validatorField = JsonSchema.class.getDeclaredField("validators");
            validatorField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonValidator> list = (List<JsonValidator>) validatorField.get(schema);
            return list.isEmpty() ? null : list.get(0);
        }
        validatorField.setAccessible(true);
        return (JsonValidator) validatorField.get(schema);
    }

    /**
     * Recursively searches the validator tree for an instance of
     * {@link PropertiesValidator}.  The tree can contain composite validators
     * (e.g. {@code ObjectValidator}) that hold child validators in a collection.
     */
    private static PropertiesValidator findPropertiesValidator(JsonValidator validator) throws Exception {
        if (validator instanceof PropertiesValidator) {
            return (PropertiesValidator) validator;
        }

        // Many validators expose a field called "validators" (a List<JsonValidator>)
        // or "schemaValidators" (a Map<String, JsonValidator>).  We inspect both.
        Field listField = null;
        try {
            listField = validator.getClass().getDeclaredField("validators");
        } catch (NoSuchFieldException ignored) {
        }

        if (listField != null) {
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonValidator> children = (List<JsonValidator>) listField.get(validator);
            for (JsonValidator child : children) {
                PropertiesValidator found = findPropertiesValidator(child);
                if (found != null) {
                    return found;
                }
            }
        }

        // Try a map named "schemaValidators"
        Field mapField = null;
        try {
            mapField = validator.getClass().getDeclaredField("schemaValidators");
        } catch (NoSuchFieldException ignored) {
        }

        if (mapField != null) {
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, JsonValidator> map = (java.util.Map<String, JsonValidator>) mapField.get(validator);
            for (JsonValidator child : map.values()) {
                PropertiesValidator found = findPropertiesValidator(child);
                if (found != null) {
                    return found;
                }
            }
        }

        // No matching validator found in this branch.
        return null;
    }
}
```