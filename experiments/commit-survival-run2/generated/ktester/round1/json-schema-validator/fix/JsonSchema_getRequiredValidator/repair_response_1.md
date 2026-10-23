```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

/**
 * Tests for the {@code getRequiredValidator()} related behaviour of {@link JsonSchema}.
 */
class JsonSchema_getRequiredValidator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;

    /**
     * The schema under test – created freshly for each test method.
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a ValidationContext using real collaborators – no side‑effects needed for the tests.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(); // use default factory
        validationContext = new ValidationContext(
                null,               // URIFactory – not required for these tests
                null,               // URNFactory – not required for these tests
                null,
                metaSchema,
                schemaFactory,
                config);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        validationContext = null;
    }

    /**
     * Creates a {@link JsonSchema} from the supplied JSON schema node.
     *
     * @param schemaNode the JSON representation of the schema
     * @return a fully initialised {@link JsonSchema}
     */
    private JsonSchema createSchema(JsonNode schemaNode) {
        JsonSchema schema = JsonSchema.from(
                validationContext, // validation context
                "#",               // current URI fragment
                URI.create("http://example.com/base"), // base URI
                schemaNode,        // the schema node itself
                null,              // root node (not needed here)
                false);            // isRootSchema
        // Populate the validator objects (including the required validator, if any).
        schema.initializeValidators();
        return schema;
    }

    /**
     * Reflectively sets the private {@code requiredValidator} field on a
     * {@link JsonSchema} instance.
     *
     * @param schema    the target schema
     * @param validator the validator to inject
     */
    private void injectRequiredValidator(JsonSchema schema, JsonValidator validator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(schema, validator);
    }

    @Test
    void testGetRequiredValidatorWhenPresent() throws Exception {
        // GIVEN – a schema that contains a "required" keyword.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "name": { "type": "string" }
                  },
                  "required": ["name"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        // WHEN
        jsonSchema = createSchema(schemaNode);
        // THEN
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(),
                "Schema should report that it has a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator,
                "getRequiredValidator() must return a non‑null validator");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
                "Validator type should be RequiredValidator");
    }

    @Test
    void testGetRequiredValidatorWhenAbsent() throws Exception {
        // GIVEN – a schema without a "required" keyword.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": {
                    "age": { "type": "integer" }
                  }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        // WHEN
        jsonSchema = createSchema(schemaNode);
        // THEN
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(),
                "Schema should report that it does NOT have a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(),
                "getRequiredValidator() must return null when no required validator is present");
    }

    @Test
    @DisplayName("Default behavior without required validator")
    void testDefaultBehaviorWithoutRequiredValidator() throws Exception {
        // GIVEN – a minimal schema node; we do NOT call initializeValidators().
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        // WHEN – getRequiredValidator is queried immediately.
        JsonValidator validator = jsonSchema.getRequiredValidator();
        // THEN – the field is still null.
        Assertions.assertNull(validator,
                "A newly created JsonSchema should have requiredValidator == null");
    }

    @Test
    @DisplayName("Required validator set via reflection")
    void testRequiredValidatorSetViaReflection() throws Exception {
        // GIVEN – any schema instance.
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        // normal init (no required keyword => still null)
        jsonSchema.initializeValidators();

        // Create a simple stub JsonValidator to inject.
        JsonValidator stubValidator = new JsonValidator() {
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                                   JsonNode node,
                                                   JsonNode rootNode,
                                                   String at) {
                return Collections.emptySet();
            }

            // Some older versions also declare a single‑argument validate method.
            @Override
            public Set<ValidationMessage> validate(JsonNode node) {
                return Collections.emptySet();
            }

            @Override
            public void initialize(JsonNode node) {
                // no‑op
            }

            @Override
            public String getSchemaPath() {
                return "";
            }
        };
        injectRequiredValidator(jsonSchema, stubValidator);
        // WHEN – the getter is called.
        JsonValidator result = jsonSchema.getRequiredValidator();
        // THEN – the exact injected instance is returned.
        Assertions.assertSame(stubValidator, result,
                "Getter must return the validator that was injected via reflection");
    }

    @Test
    @DisplayName("Required validator set by initialise validators")
    void testRequiredValidatorSetByInitializeValidators() throws Exception {
        // GIVEN – a schema containing a non‑empty required array.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": { "name": { "type":"string"} },
                  "required": ["name"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        // WHEN – initialiseValidators populates the internal field.
        jsonSchema.initializeValidators();
        // THEN – a non‑null RequiredValidator is present.
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(),
                "Schema should report having a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator,
                "getRequiredValidator() must not return null");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
                "Validator should be an instance of RequiredValidator");
    }

    @Test
    @DisplayName("Required validator remains null for schema without required keyword")
    void testRequiredValidatorRemainsNullWhenKeywordAbsent() throws Exception {
        // GIVEN – a schema lacking the required keyword.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "properties": { "age": { "type":"integer"} }
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – the required validator stays null.
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(),
                "Schema should report NOT having a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(),
                "Getter must return null when no required keyword is present");
    }

    @Test
    @DisplayName("Required validator created for schema with empty required array")
    void testRequiredValidatorCreatedForEmptyRequiredArray() throws Exception {
        // GIVEN – a schema with an empty required array.
        String json = """
                {
                  "$schema": "http://json-schema.org/draft-07/schema#",
                  "type": "object",
                  "required": []
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        // THEN – a RequiredValidator instance is still created.
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(),
                "Even an empty required array should cause a RequiredValidator to be created");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator);
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
                "Validator class should be RequiredValidator");
    }

    @Test
    @DisplayName("Idempotent getRequiredValidator call")
    void testIdempotentGetRequiredValidatorCall() throws Exception {
        // GIVEN – a schema with a required keyword.
        String json = """
                {
                  "type": "object",
                  "required": ["id"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        // WHEN – getter is called twice.
        JsonValidator first = jsonSchema.getRequiredValidator();
        JsonValidator second = jsonSchema.getRequiredValidator();
        // THEN – both references point to the same object.
        Assertions.assertNotNull(first);
        Assertions.assertSame(first, second,
                "Subsequent calls to getRequiredValidator() must return the same instance");
    }

    @Test
    @DisplayName("Child schema without its own required keyword does not inherit parent's validator")
    void testChildSchemaDoesNotInheritParentValidator() throws Exception {
        // Parent schema with required.
        String parentJson = """
                {
                  "type": "object",
                  "required": ["parentProp"],
                  "properties": {
                    "child": { "type":"object", "properties": { "foo": {"type":"string"} } }
                  }
                }
                """;
        JsonNode parentNode = MAPPER.readTree(parentJson);
        JsonSchema parentSchema = createSchema(parentNode);
        parentSchema.initializeValidators();

        // Extract the child node that does NOT contain a required keyword.
        JsonNode childNode = parentNode.at("/properties/child");
        Assertions.assertFalse(childNode.isMissingNode(), "Child node must exist");

        // Create child schema via the protected helper.
        JsonSchema childSchema = parentSchema.createChildSchema("#/properties/child", childNode);
        childSchema.initializeValidators();

        // THEN – child must not have a required validator.
        Assertions.assertNull(childSchema.getRequiredValidator(),
                "Child schema without a required keyword must not inherit the parent's RequiredValidator");
    }

    @Test
    @DisplayName("Re‑initialising validators does not clear required validator")
    void testReinitialisingValidatorsDoesNotClear() throws Exception {
        // GIVEN – a schema with a required keyword.
        String json = """
                {
                  "type": "object",
                  "required": ["a"]
                }
                """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        // First initialisation.
        jsonSchema.initializeValidators();
        JsonValidator first = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(first, "First initialisation must produce a RequiredValidator");
        // Second initialisation.
        jsonSchema.initializeValidators();
        JsonValidator second = jsonSchema.getRequiredValidator();
        // THEN – validator must still be non‑null (may be same or new, but never null).
        Assertions.assertNotNull(second, "RequiredValidator must remain non‑null after re‑initialisation");
    }
}
```