```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for {@link JsonSchema#getRequiredValidator()} covering
 * the behaviour described in the supplied JSON test cases.
 *
 * <p>
 * The tests use the public static factory {@link JsonSchema#from(...)} to obtain
 * instances and rely on {@link JsonSchema#initializeValidators()} to populate
 * the internal {@code requiredValidator} field where appropriate.
 * </p>
 *
 * <p>
 * Heavy‑weight collaborators required to build a {@link ValidationContext} are
 * mocked, ensuring the tests run fast and deterministically.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_getRequiredValidator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /* ---------------------------------------------------------------------- */
    /* Mocked collaborators required for ValidationContext construction        */
    /* ---------------------------------------------------------------------- */
    @Mock private URIFactory mockUriFactory;
    @Mock private URNFactory mockUrnFactory;
    @Mock private JsonMetaSchema mockMetaSchema;
    @Mock private JsonSchemaFactory mockSchemaFactory;

    private ValidationContext validationContext;

    /** The {@link JsonSchema} instance under test – recreated for each test. */
    private JsonSchema jsonSchema;

    /* ---------------------------------------------------------------------- */
    /* JUnit lifecycle methods                                                */
    /* ---------------------------------------------------------------------- */
    @BeforeEach
    void setUp() {
        // Build a ValidationContext with the mocked collaborators.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig(); // real config – no side‑effects.
        validationContext = new ValidationContext(
                mockUriFactory,
                mockUrnFactory,
                mockMetaSchema,
                mockSchemaFactory,
                config);

        // The meta‑schema is queried during schema construction.
        when(mockMetaSchema.getId()).thenReturn("http://example.com/meta");
    }

    @AfterEach
    void tearDown() {
        jsonSchema = null;
        validationContext = null;
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                         */
    /* ---------------------------------------------------------------------- */

    /**
     * Creates a {@link JsonSchema} from a JSON node using the static {@code from}
     * factory method. The returned schema is *not* initialised – callers may invoke
     * {@link JsonSchema#initializeValidators()} explicitly.
     *
     * @param schemaNode the JSON representation of the schema
     * @return a freshly created {@link JsonSchema}
     */
    private JsonSchema createSchema(JsonNode schemaNode) {
        JsonSchema schema = JsonSchema.from(
                validationContext,
                "#",                                 // schemaPath
                URI.create("http://example.com/base"), // any base URI
                schemaNode,
                null,                                 // parent – top‑level
                false);                               // suppressSubSchemaRetrieval
        return schema;
    }

    /**
     * Reflectively sets the private {@code requiredValidator} field on a
     * {@link JsonSchema} instance.
     *
     * @param schema   the target schema
     * @param validator the validator to inject
     */
    private void injectRequiredValidator(JsonSchema schema, JsonValidator validator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(schema, validator);
    }

    /* ---------------------------------------------------------------------- */
    /* Test cases                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("Default behavior without required validator")
    void testDefaultBehaviorWithoutRequiredValidator() throws Exception {
        // GIVEN – a minimal schema node; we do NOT call initializeValidators().
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);

        // WHEN – getRequiredValidator is queried immediately.
        JsonValidator validator = jsonSchema.getRequiredValidator();

        // THEN – the field is still null.
        assertNull(validator, "A newly created JsonSchema should have requiredValidator == null");
    }

    @Test
    @DisplayName("Required validator set via reflection")
    void testRequiredValidatorSetViaReflection() throws Exception {
        // GIVEN – any schema instance.
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        jsonSchema.initializeValidators(); // normal init (no required keyword => still null)

        // Create a mock JsonValidator to inject.
        JsonValidator mockValidator = mock(JsonValidator.class);
        injectRequiredValidator(jsonSchema, mockValidator);

        // WHEN – the getter is called.
        JsonValidator result = jsonSchema.getRequiredValidator();

        // THEN – the exact injected instance is returned.
        assertSame(mockValidator, result, "Getter must return the validator that was injected via reflection");
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
        assertTrue(jsonSchema.hasRequiredValidator(),
                "Schema should report having a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        assertNotNull(validator, "getRequiredValidator() must not return null");
        assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
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
        assertFalse(jsonSchema.hasRequiredValidator(),
                "Schema should report NOT having a required validator");
        assertNull(jsonSchema.getRequiredValidator(),
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
        assertTrue(jsonSchema.hasRequiredValidator(),
                "Even an empty required array should cause a RequiredValidator to be created");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        assertNotNull(validator);
        assertEquals("RequiredValidator", validator.getClass().getSimpleName(),
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
        assertNotNull(first);
        assertSame(first, second, "Subsequent calls to getRequiredValidator() must return the same instance");
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
        assertFalse(childNode.isMissingNode(), "Child node must exist");

        // Create child schema via the protected helper.
        JsonSchema childSchema = parentSchema.createChildSchema("#/properties/child", childNode);
        childSchema.initializeValidators();

        // THEN – child must not have a required validator.
        assertNull(childSchema.getRequiredValidator(),
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
        assertNotNull(first, "First initialisation must produce a RequiredValidator");

        // Second initialisation.
        jsonSchema.initializeValidators();
        JsonValidator second = jsonSchema.getRequiredValidator();

        // THEN – validator must still be non‑null (may be same or new, but never null).
        assertNotNull(second, "RequiredValidator must remain non‑null after re‑initialisation");
        // We accept either the same instance or a newly created one, but ensure no loss.
        // (Optional strict check: same instance – uncomment if desired)
        // assertSame(first, second, "Re‑initialising should not replace the validator");
    }

    /* ---------------------------------------------------------------------- */
    /* Existing simple behaviour tests – kept for completeness                */
    /* ---------------------------------------------------------------------- */

    @Test
    @DisplayName("getRequiredValidator returns non‑null when required keyword is present (integration style)")
    void testGetRequiredValidatorWhenPresent() throws Exception {
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
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();

        assertTrue(jsonSchema.hasRequiredValidator(),
                "Schema should report that it has a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        assertNotNull(validator);
        assertEquals("RequiredValidator", validator.getClass().getSimpleName());
    }

    @Test
    @DisplayName("getRequiredValidator returns null when required keyword is absent (integration style)")
    void testGetRequiredValidatorWhenAbsent() throws Exception {
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
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();

        assertFalse(jsonSchema.hasRequiredValidator(),
                "Schema should report that it does NOT have a required validator");
        assertNull(jsonSchema.getRequiredValidator(),
                "Getter must return null when no required validator is present");
    }
}
```