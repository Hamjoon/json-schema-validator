```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link OneOfValidator#validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)}.
 *
 * The tests create a real {@link OneOfValidator} instance using a minimal
 * {@link ValidationContext}.  The validator is exercised directly rather than
 * through {@link JsonSchema#validate(String, InputFormat)} so that we can
 * focus on the behaviour of the {@code validate} method itself.
 */
@ExtendWith(MockitoExtension.class)
class OneOfValidator_validate_Test {

    private static ObjectMapper mapper;
    private static ValidationContext validationContext;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // Build a ValidationContext that uses the 2020‑12 meta‑schema.
        JsonMetaSchema meta = JsonMetaSchema.getV202012();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        validationContext = new ValidationContext(meta, factory, new SchemaValidatorsConfig());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Nothing needed before each test.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing needed after each test.
    }

    /**
     * When the instance matches exactly one subschema the validator must return
     * an empty {@link Set}.
     */
    @Test
    void validateMatchesExactlyOneSchema() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        JsonNode schemaNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        // Dummy objects for the constructor – they are not consulted by the
        // validator for this test.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;

        OneOfValidator validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);

        // Node that is a JSON string – matches the first subschema only.
        JsonNode instance = mapper.readTree("\"hello\"");

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertTrue(messages.isEmpty(), "Expected no validation messages when exactly one schema matches");
    }

    /**
     * When the instance matches none of the subschemas the validator must report
     * a {@code oneOf} error.
     */
    @Test
    void validateMatchesNoSchema() throws Exception {
        // oneOf: [ {"type":"string"}, {"type":"integer"} ]
        JsonNode schemaNode = mapper.readTree("[{\"type\":\"string\"},{\"type\":\"integer\"}]");

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;

        OneOfValidator validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);

        // Node that is a boolean – matches neither subschema.
        JsonNode instance = mapper.readTree("true");

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(messages.isEmpty(), "Expected validation messages when no schema matches");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getType(), "The validation message type should be 'oneOf'");
        assertTrue(msg.getMessage().contains("0 are valid"),
                "Message should indicate that 0 subschemas are valid");
    }

    /**
     * When the instance matches more than one subschema the validator must report
     * a {@code oneOf} error that lists the indexes of the matching subschemas.
     */
    @Test
    void validateMatchesMultipleSchemas() throws Exception {
        // oneOf: [ {}, {} ] – both empty schemas accept any JSON value.
        JsonNode schemaNode = mapper.readTree("[{},{}]");

        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        JsonSchema parentSchema = null;

        OneOfValidator validator = new OneOfValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validationContext);

        // Node that is a number – matches both empty subschemas.
        JsonNode instance = mapper.readTree("42");

        ExecutionContext ctx = new ExecutionContext();
        Set<ValidationMessage> messages = validator.validate(ctx, instance, instance, JsonNodePath.ROOT);
        assertFalse(messages.isEmpty(), "Expected validation messages when multiple schemas match");
        ValidationMessage msg = messages.iterator().next();
        assertEquals("oneOf", msg.getType(), "The validation message type should be 'oneOf'");
        assertTrue(msg.getMessage().contains("2 are valid"),
                "Message should indicate that 2 subschemas are valid");
        assertTrue(msg.getMessage().contains("indexes '0, 1'"),
                "Message should list the indexes of the matching subschemas");
    }

    // Helper method to create simple Mockito mocks without pulling in the whole
    // static import set.
    @SuppressWarnings("unchecked")
    private static <T> T mock(Class<T> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
```