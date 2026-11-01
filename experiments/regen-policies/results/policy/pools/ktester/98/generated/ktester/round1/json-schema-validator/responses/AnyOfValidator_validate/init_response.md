```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Tests for {@link AnyOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 */
@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private AnyOfValidator anyOfValidator;

    /** Build the objects that are shared by all tests. */
    @BeforeAll
    static void setupBeforeAll() {
        // No static resources needed for these tests.
    }

    /** Create a fresh validator instance before each test. */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ---- ValidationContext -------------------------------------------------
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonMetaSchema metaSchema = factory.getMetaSchema();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);

        // ---- SchemaLocation ----------------------------------------------------
        // AbsoluteIri has a public constructor that accepts a String.
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        JsonNodePath fragment = new JsonNodePath(PathType.JSON_POINTER);
        SchemaLocation schemaLocation = new SchemaLocation(absoluteIri, fragment);

        // ---- Evaluation path ----------------------------------------------------
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON_POINTER);

        // ---- anyOf schema (array of two simple subschemas) ----------------------
        String anyOfSchemaJson = "[ {\"type\":\"string\"}, {\"type\":\"integer\"} ]";
        JsonNode schemaNode = MAPPER.readTree(anyOfSchemaJson);

        // parent schema is not needed for these unit tests – pass null.
        JsonSchema parentSchema = null;

        // ---- Create the validator under test ------------------------------------
        anyOfValidator = new AnyOfValidator(schemaLocation, evaluationPath,
                schemaNode, parentSchema, validationContext);
    }

    /** Clean‑up after each test – nothing to release. */
    @AfterEach
    void teardownAfterEach() {
        // No resources to clean up.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No static resources to clean up.
    }

    /** A value that matches the first subschema (string) should produce no errors. */
    @Test
    void validateStringMatchesAnyOf_ShouldReturnEmptySet() throws Exception {
        JsonNode instance = MAPPER.readTree("\"hello\"");
        ExecutionContext execContext = new ExecutionContext();

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        Set<ValidationMessage> messages = anyOfValidator.validate(execContext, instance, instance, instanceLocation);

        assertTrue(messages.isEmpty(), "Expected no validation messages for a matching string");
    }

    /** A value that matches the second subschema (integer) should also produce no errors. */
    @Test
    void validateIntegerMatchesAnyOf_ShouldReturnEmptySet() throws Exception {
        JsonNode instance = MAPPER.readTree("42");
        ExecutionContext execContext = new ExecutionContext();

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        Set<ValidationMessage> messages = anyOfValidator.validate(execContext, instance, instance, instanceLocation);

        assertTrue(messages.isEmpty(), "Expected no validation messages for a matching integer");
    }

    /** A value that matches none of the subschemas should produce at least one error. */
    @Test
    void validateNonMatchingValue_ShouldReturnValidationMessage() throws Exception {
        JsonNode instance = MAPPER.readTree("true"); // boolean – does not match string nor integer
        ExecutionContext execContext = new ExecutionContext();

        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        Set<ValidationMessage> messages = anyOfValidator.validate(execContext, instance, instance, instanceLocation);

        assertFalse(messages.isEmpty(), "Expected validation messages for a non‑matching value");
    }
}
```