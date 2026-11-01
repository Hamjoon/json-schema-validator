```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#walk(JsonNode, OutputFormat, boolean)}.
 *
 * <p>The tests cover the most common behaviours of the {@code walk} method:
 *
 * <ul>
 *   <li>delegation to the supplied {@link OutputFormat}</li>
 *   <li>validation being performed (or skipped) according to the {@code validate}
 *       flag</li>
 *   <li>correct handling of error conditions such as {@code null} arguments,
 *       unresolved $ref, etc.</li>
 * </ul>
 *
 * <p>Custom {@link OutputFormat} implementations are defined inside the test
 * class to keep the test self‑contained.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_4_Test {

    /** Shared Jackson mapper used to build {@link JsonNode} instances. */
    private static ObjectMapper mapper;

    /** The schema under test – created per test case via {@link JsonSchemaFactory}. */
    private JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
    }

    @AfterAll
    static void teardownAfterAll() {
        // nothing to clean up
    }

    @AfterEach
    void teardownAfterEach() {
        // no per‑test resources
    }

    /**
     * Helper that builds a {@link JsonSchema} from a raw JSON schema string.
     */
    private JsonSchema buildSchema(String schemaJson) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        return factory.getSchema(schemaJson);
    }

    /* --------------------------------------------------------------------- */
    /*  Custom OutputFormat implementations used by several tests          */
    /* --------------------------------------------------------------------- */

    /**
     * An {@link OutputFormat} that extracts the validation messages from the
     * {@link ValidationResult} and returns them as a {@link List} of strings.
     */
    private static class CollectingOutputFormat implements OutputFormat<List<String>> {
        @Override
        public List<String> format(ValidationResult validationResult) {
            Set<ValidationMessage> msgs = validationResult.getMessages();
            List<String> list = new ArrayList<>(msgs.size());
            for (ValidationMessage vm : msgs) {
                list.add(vm.getMessage());
            }
            return list;
        }
    }

    /**
     * An {@link OutputFormat} that always throws a {@link RuntimeException}
     * when its {@code format} method is invoked.
     */
    private static class ExceptionThrowingOutputFormat implements OutputFormat<Object> {
        @Override
        public Object format(ValidationResult validationResult) {
            throw new RuntimeException("deliberate exception from OutputFormat");
        }
    }

    /**
     * An {@link OutputFormat} that returns {@code true} if validation succeeded,
     * {@code false} otherwise.
     */
    private static class BooleanResultOutputFormat implements OutputFormat<Boolean> {
        @Override
        public Boolean format(ValidationResult validationResult) {
            return validationResult.getMessages().isEmpty();
        }
    }

    /**
     * An {@link OutputFormat} that throws a checked {@link IOException}. The
     * {@code walk} method is expected to wrap it into an unchecked exception.
     */
    private static class IOExceptionThrowingOutputFormat implements OutputFormat<Object> {
        @Override
        public Object format(ValidationResult validationResult) throws IOException {
            throw new IOException("checked exception from OutputFormat");
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Successful validation with validate=true                           */
    /* --------------------------------------------------------------------- */

    @Test
    void successfulValidationValidateTrue() throws Exception {
        // schema expects an object with a string property "name"
        String schema = "{ \"type\": \"object\", \"properties\": { \"name\": { \"type\": \"string\" } }, \"required\": [\"name\"] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"name\":\"John\"}");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "No validation messages expected for a valid instance");
    }

    /* --------------------------------------------------------------------- */
    /*  2. Validation disabled (validate=false)                               */
    /* --------------------------------------------------------------------- */

    @Test
    void validationDisabledReturnsEmptyResult() throws Exception {
        // schema expects an integer, but we will feed a string – validation is disabled
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, false);
        assertTrue(result.isEmpty(), "When validation is disabled, no messages must be produced");
    }

    /* --------------------------------------------------------------------- */
    /*  3. Invalid instance node – validation error produced                 */
    /* --------------------------------------------------------------------- */

    @Test
    void invalidInstanceProducesValidationMessage() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "A type‑mismatch should yield at least one validation message");
        assertTrue(result.stream().anyMatch(msg -> msg.toLowerCase().contains("type")), "Message should mention a type error");
    }

    /* --------------------------------------------------------------------- */
    /*  4. Null node argument                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    void nullNodeThrowsException() {
        CollectingOutputFormat format = new CollectingOutputFormat();
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(null, format, true),
                "Passing a null JsonNode should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    /*  5. Null OutputFormat argument                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void nullOutputFormatThrowsException() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(node, null, true),
                "Passing a null OutputFormat should result in NPE");
    }

    /* --------------------------------------------------------------------- */
    /*  6. Missing required property – validation error                      */
    /* --------------------------------------------------------------------- */

    @Test
    void missingRequiredPropertyProducesMessage() throws Exception {
        String schema = "{ \"type\": \"object\", \"required\": [\"id\"], \"properties\": { \"id\": { \"type\": \"integer\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{}"); // empty object

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Missing required property must produce a validation message");
        assertTrue(result.stream().anyMatch(m -> m.contains("required")), "Message should mention 'required'");
    }

    /* --------------------------------------------------------------------- */
    /*  7. Enum constraint violation                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void enumConstraintViolationProducesMessage() throws Exception {
        String schema = "{ \"type\": \"string\", \"enum\": [\"red\", \"green\", \"blue\"] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"yellow\"");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Enum violation must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("enum")), "Message should reference enum");
    }

    /* --------------------------------------------------------------------- */
    /*  8. Pattern constraint violation                                       */
    /* --------------------------------------------------------------------- */

    @Test
    void patternConstraintViolationProducesMessage() throws Exception {
        String schema = "{ \"type\": \"string\", \"pattern\": \"^[A-Z]{3}[0-9]{4}$\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"abc1234\""); // lower‑case, does not match

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Pattern mismatch must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("pattern")), "Message should reference pattern");
    }

    /* --------------------------------------------------------------------- */
    /*  9. Array items type mismatch                                          */
    /* --------------------------------------------------------------------- */

    @Test
    void arrayItemsTypeMismatchProducesMessage() throws Exception {
        String schema = "{ \"type\": \"array\", \"items\": { \"type\": \"integer\" } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("[1, \"two\", 3]");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "Array element type mismatch must be reported");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("type")), "Message should mention type error");
    }

    /* --------------------------------------------------------------------- */
    /* 10. OneOf schema – matching subschema                                   */
    /* --------------------------------------------------------------------- */

    @Test
    void oneOfMatchingSubschemaProducesNoMessages() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"string\" }, { \"type\": \"integer\" } ] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"a string\"");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "When oneOf matches, no validation messages are expected");
    }

    /* --------------------------------------------------------------------- */
    /* 11. OneOf schema – no matching subschema                               */
    /* --------------------------------------------------------------------- */

    @Test
    void oneOfNoMatchingSubschemaProducesMessage() throws Exception {
        String schema = "{ \"oneOf\": [ { \"type\": \"boolean\" }, { \"type\": \"null\" } ] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("123"); // number

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertFalse(result.isEmpty(), "When none of the oneOf alternatives match, a message must be produced");
        assertTrue(result.stream().anyMatch(m -> m.toLowerCase().contains("oneof")), "Message should reference oneOf");
    }

    /* --------------------------------------------------------------------- */
    /* 12. OutputFormat throws RuntimeException                                */
    /* --------------------------------------------------------------------- */

    @Test
    void outputFormatRuntimeExceptionIsPropagated() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("42");

        OutputFormat<Object> format = new ExceptionThrowingOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(instance, format, true),
                "RuntimeException from OutputFormat must be propagated");
        assertEquals("deliberate exception from OutputFormat", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 13. OutputFormat throws checked IOException                              */
    /* --------------------------------------------------------------------- */

    @Test
    void outputFormatCheckedIOExceptionIsWrapped() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("42");

        OutputFormat<Object> format = new IOExceptionThrowingOutputFormat();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(instance, format, true),
                "Checked IOException should be wrapped in a RuntimeException");
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("checked exception from OutputFormat", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 14. Schema allowing null – node is JSON null                           */
    /* --------------------------------------------------------------------- */

    @Test
    void schemaAllowsNullNodeProducesNoMessages() throws Exception {
        String schema = "{ \"type\": [\"null\", \"string\"] }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("null"); // literal null

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Null value is allowed – no validation messages expected");
    }

    /* --------------------------------------------------------------------- */
    /* 15. Custom OutputFormat returning Boolean                               */
    /* --------------------------------------------------------------------- */

    @Test
    void booleanResultOutputFormatReturnsTrueOnSuccess() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("true");

        BooleanResultOutputFormat format = new BooleanResultOutputFormat();

        Boolean result = jsonSchema.walk(instance, format, true);
        assertTrue(result, "OutputFormat should return true when validation succeeds");
    }

    /* --------------------------------------------------------------------- */
    /* 16. Unresolved $ref leads to JsonSchemaException                       */
    /* --------------------------------------------------------------------- */

    @Test
    void unresolvedRefThrowsException() {
        String schema = "{ \"$ref\": \"#/nonexistent\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.createObjectNode(); // any node

        CollectingOutputFormat format = new CollectingOutputFormat();

        // The library throws a JsonSchemaException (runtime) when $ref cannot be resolved.
        assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(instance, format, true),
                "Unresolvable $ref must result in an exception");
    }

    /* --------------------------------------------------------------------- */
    /* 17. Recursive anchor – ensure no infinite recursion                    */
    /* --------------------------------------------------------------------- */

    @Test
    void recursiveAnchorDoesNotOverflowStack() throws Exception {
        // Draft‑2020‑12 supports "$recursiveAnchor": true
        String schema = "{ \"$id\": \"#\", \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"self\": { \"$ref\": \"#\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");

        CollectingOutputFormat format = new CollectingOutputFormat();

        // The walk should finish and return (no stack overflow). Validation passes.
        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Recursive anchor should be handled without error");
    }

    /* --------------------------------------------------------------------- */
    /* 18. MissingNode argument causes IllegalArgumentException               */
    /* --------------------------------------------------------------------- */

    @Test
    void missingNodeArgumentThrowsException() {
        JsonNode missing = MissingNode.getInstance();
        CollectingOutputFormat format = new CollectingOutputFormat();

        assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.walk(missing, format, true),
                "MissingNode should be rejected with IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    /* 19. ExecutionContext creation failure (simulated via reflection)      */
    /* --------------------------------------------------------------------- */

    @Test
    void executionContextCreationFailurePropagatesException() throws Exception {
        // Create a schema instance but force createExecutionContext() to throw.
        // We achieve this by creating a subclass that overrides the method.
        JsonSchema broken = new JsonSchema() {
            @Override
            public ExecutionContext createExecutionContext() {
                throw new IllegalStateException("forced failure");
            }
        };
        // Use a trivial schema that would otherwise be valid.
        JsonNode dummyNode = mapper.readTree("{}");
        CollectingOutputFormat format = new CollectingOutputFormat();

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> broken.walk(dummyNode, format, true),
                "Failure in createExecutionContext must be propagated");
        assertEquals("forced failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /* 20. Invalid $id URI causing UnsupportedEncodingException               */
    /* --------------------------------------------------------------------- */

    @Test
    void invalidIdUriThrowsUnsupportedEncodingException() {
        // The schema contains an invalid percent‑encoding sequence.
        String schema = "{ \"$id\": \"http://example.com/schema%ZZ\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.createObjectNode();

        CollectingOutputFormat format = new CollectingOutputFormat();

        // The library wraps the UnsupportedEncodingException into a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> jsonSchema.walk(instance, format, true),
                "Invalid $id should result in an exception");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException);
    }

    /* --------------------------------------------------------------------- */
    /*  21. Validate flag false with invalid instance – no messages          */
    /* --------------------------------------------------------------------- */

    @Test
    void validateFalseWithInvalidInstanceProducesNoMessages() throws Exception {
        String schema = "{ \"type\": \"integer\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("\"not-an-integer\"");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, false);
        assertTrue(result.isEmpty(), "When validation is disabled, even an invalid instance yields no messages");
    }

    /* --------------------------------------------------------------------- */
    /*  22. Schema with recursive anchor – ensure walk handles recursion flag */
    /* --------------------------------------------------------------------- */

    @Test
    void schemaWithRecursiveAnchorHandledGracefully() throws Exception {
        // Same as test 17 but focusing on the isRecursiveAnchor() flag.
        String schema = "{ \"$recursiveAnchor\": true, \"type\": \"object\", \"properties\": { \"self\": { \"$ref\": \"#\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("{\"self\":{}}");

        CollectingOutputFormat format = new CollectingOutputFormat();

        List<String> result = jsonSchema.walk(instance, format, true);
        assertTrue(result.isEmpty(), "Recursive anchor should not cause infinite recursion");
    }

    /* --------------------------------------------------------------------- */
    /*  23. OutputFormat that returns a non‑collection result (Boolean)    */
    /* --------------------------------------------------------------------- */

    @Test
    void customBooleanOutputFormatWorks() throws Exception {
        String schema = "{ \"type\": \"boolean\" }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.readTree("false");

        BooleanResultOutputFormat format = new BooleanResultOutputFormat();

        Boolean result = jsonSchema.walk(instance, format, true);
        assertFalse(result, "Boolean result should reflect validation outcome (false when messages exist)");
    }

    /* --------------------------------------------------------------------- */
    /*  24. Circular $ref without recursive anchor – expect StackOverflowError */
    /* --------------------------------------------------------------------- */

    @Test
    @Disabled("Running this test would cause a StackOverflowError and halt the test suite. " +
            "It is kept as documentation of the expected behaviour.")
    void circularRefWithoutRecursiveAnchorCausesStackOverflow() {
        String schema = "{ \"$ref\": \"#/definitions/self\", \"definitions\": { \"self\": { \"$ref\": \"#/definitions/self\" } } }";
        jsonSchema = buildSchema(schema);
        JsonNode instance = mapper.createObjectNode();

        CollectingOutputFormat format = new CollectingOutputFormat();

        assertThrows(StackOverflowError.class,
                () -> jsonSchema.walk(instance, format, true),
                "Circular $ref without recursive anchor should overflow the stack");
    }
}
```