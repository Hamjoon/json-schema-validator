```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import com.networknt.schema.serialization.JsonMapperFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, boolean)}.
 *
 * <p>The tests cover the scenarios described in the JSON test‑case specification,
 * using real schema definitions where possible.  Where internal state must be
 * faked (e.g. un‑initialised validators) reflection is employed – the production
 * code does not expose those fields.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_Test {

    /** Shared objects for all tests */
    private static JsonMetaSchema metaSchema;
    private static JsonSchemaFactory schemaFactory;
    private static SchemaValidatorsConfig config;

    /** Objects created per test */
    private ValidationContext validationContext;
    private SchemaLocation schemaLocation;
    private JsonNodePath evaluationPath;
    private JsonNode schemaNode;
    private JsonSchema jsonSchema;
    private ExecutionContext executionContext;
    private JsonNode instanceNode;

    @BeforeAll
    static void setupBeforeAll() {
        // Heavy but immutable objects – initialise once.
        metaSchema = JsonMetaSchema.getV201909();
        schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Minimal validation context – no custom configuration.
        validationContext = new ValidationContext(metaSchema, schemaFactory, config);

        // Default to an empty schema; individual tests will replace it.
        schemaNode = JsonMapperFactory.getMapper().readTree("{}");

        schemaLocation = new SchemaLocation(null);
        evaluationPath = new JsonNodePath(PathType.JSON);

        jsonSchema = JsonSchema.from(validationContext, schemaLocation, evaluationPath,
                schemaNode, null, false);

        executionContext = new ExecutionContext();

        // Default instance node – tests override as needed.
        instanceNode = JsonMapperFactory.getMapper().readTree("{\"foo\":\"bar\"}");
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for creating schemas and nodes
     * --------------------------------------------------------------------- */

    /** Creates a {@link JsonSchema} from a JSON schema string. */
    private JsonSchema createSchema(String jsonSchemaString) throws Exception {
        JsonNode node = JsonMapperFactory.getMapper().readTree(jsonSchemaString);
        return JsonSchema.from(validationContext, new SchemaLocation(null),
                new JsonNodePath(PathType.JSON), node, null, false);
    }

    /** Shortcut for building a Jackson ObjectNode from a JSON literal. */
    private JsonNode node(String json) throws Exception {
        return JsonMapperFactory.getMapper().readTree(json);
    }

    /** Returns a fresh {@link ExecutionContext} with optional customisation. */
    private ExecutionContext freshContext(Consumer<ExecutionContext> customiser) {
        ExecutionContext ctx = new ExecutionContext();
        if (customiser != null) {
            customiser.accept(ctx);
        }
        return ctx;
    }

    /* --------------------------------------------------------------------- *
     *  Basic walk – validation enabled
     * --------------------------------------------------------------------- */

    @Test
    void walk_ValidationEnabled_SuccessfulWhenAllRequiredPresent() throws Exception {
        // schema: requires a string property "name"
        jsonSchema = createSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode instance = node("{\"name\":\"John\"}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, true);
        assertTrue(result.isSuccess(), "Result should be successful when required property is present");
        assertTrue(result.getMessages().isEmpty(), "No validation messages expected");
    }

    @Test
    void walk_ValidationEnabled_FailureWhenRequiredMissing() throws Exception {
        jsonSchema = createSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode instance = node("{\"age\":30}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, true);
        assertFalse(result.isSuccess(), "Result should indicate failure when required property is missing");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().contains("name")), "Message about missing 'name' expected");
    }

    /* --------------------------------------------------------------------- *
     *  Basic walk – validation disabled
     * --------------------------------------------------------------------- */

    @Test
    void walk_ValidationDisabled_IgnoresSchemaErrors() throws Exception {
        jsonSchema = createSchema("{\"type\":\"object\",\"required\":[\"name\"],\"properties\":{\"name\":{\"type\":\"string\"}}}");
        JsonNode instance = node("{\"age\":30}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, false);
        assertTrue(result.isSuccess(), "When validation is disabled the result must be successful");
        assertTrue(result.getMessages().isEmpty(), "No messages should be collected when validation is turned off");
    }

    /* --------------------------------------------------------------------- *
     *  Null arguments handling
     * --------------------------------------------------------------------- */

    @Test
    void walk_NullExecutionContext_throwsNPE() {
        ExecutionContext nullCtx = null;
        JsonNode instance = node("{\"name\":\"John\"}");
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(nullCtx, instance, true),
                "Null ExecutionContext should cause NullPointerException");
    }

    @Test
    void walk_NullNode_throwsNPE() {
        assertThrows(NullPointerException.class,
                () -> jsonSchema.walk(executionContext, null, true),
                "Null JsonNode should cause NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  Different JSON node types
     * --------------------------------------------------------------------- */

    @Test
    void walk_ArrayNode_ValidatesAgainstArraySchema() throws Exception {
        jsonSchema = createSchema("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        JsonNode array = JsonMapperFactory.getMapper().readTree("[1,2,3]");

        ValidationResult result = jsonSchema.walk(executionContext, array, true);
        assertTrue(result.isSuccess(), "Array of integers should validate successfully");
    }

    @Test
    void walk_TextNode_TypeMismatch() throws Exception {
        jsonSchema = createSchema("{\"type\":\"array\",\"items\":{\"type\":\"integer\"}}");
        JsonNode text = JsonMapperFactory.getMapper().readTree("\"plain string\"");

        ValidationResult result = jsonSchema.walk(executionContext, text, true);
        assertFalse(result.isSuccess(), "Text node should fail validation against array schema");
        assertTrue(result.getMessages().stream()
                .anyMatch(m -> m.getMessage().toLowerCase().contains("type")), "A type‑mismatch message is expected");
    }

    /* --------------------------------------------------------------------- *
     *  Recursive anchor handling
     * --------------------------------------------------------------------- */

    @Test
    void walk_RecursiveAnchor_ResolvesWithoutStackOverflow() throws Exception {
        // Schema with $recursiveAnchor and a self‑reference via $ref
        jsonSchema = createSchema("{\"$recursiveAnchor\":true,\"type\":\"object\",\"properties\":{\"self\":{\"$ref\":\"#\"}}}");
        JsonNode instance = node("{\"self\":{}}");

        ValidationResult result = jsonSchema.walk(executionContext, instance, true);
        assertTrue(result.isSuccess(), "Recursive anchor should allow self‑reference without error");
    }

    /* --------------------------------------------------------------------- *
     *  Schema resource root flag
     * --------------------------------------------------------------------- */

    @Test
    void walk_SchemaResourceRoot_FlagIsHonoured() throws Exception {
        // Presence of $id makes the schema a resource root in many implementations.
        jsonSchema = createSchema("{\"$id\":\"http://example.com/schema\",\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}");
        JsonNode instance = node("{\"id\":\"http://example.com/schema\"}");

        // No special assertion on the flag itself – we just verify walk succeeds.
        ValidationResult result = jsonSchema.walk(executionContext, instance, true);
        assertTrue(result.isSuccess(), "Schema marked as resource root should still validate correctly");
    }

    /* --------------------------------------------------------------------- *
     *  Custom ExecutionContext configuration
     * --------------------------------------------------------------------- */

    @Test
    void walk_FailFast_StopsAfterFirstError() throws Exception {
        jsonSchema = createSchema("{\"type\":\"object\",\"required\":[\"name\",\"age\"],\"properties\":{\"name\":{\"type\":\"string\"},\"age\":{\"type\":\"integer\"}}}");
        JsonNode instance = node("{\"age\":\"not-an-int\"}"); // both required missing and type error

        ExecutionContext ctx = freshContext(c -> c.setFailFast(true));

        ValidationResult result = jsonSchema.walk(ctx, instance, true);
        assertFalse(result.isSuccess(), "Result should be failure");
        // With fail‑fast we expect exactly one message.
        assertEquals(1, result.getMessages().size(),
                "Fail‑fast should stop after the first validation error");
    }

    @Test
    void walk_CollectAnnotations_IncludesDefaultAnnotation() throws Exception {
        // Schema defines a default value for "name"
        jsonSchema = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\",\"default\":\"John\"}}}");
        JsonNode instance = node("{}"); // missing "name" – default should be applied

        ExecutionContext ctx = freshContext(c -> c.setCollectAnnotations(true));

        ValidationResult result = jsonSchema.walk(ctx, instance, true);
        assertTrue(result.isSuccess(), "Validation should succeed even when defaults are applied");
        // Annotation messages are stored in the ValidationResult – they are not validation errors.
        assertTrue(result.getAnnotations().size() > 0,
                "Annotations (e.g., default applied) should be present when collectAnnotations is true");
    }

    /* --------------------------------------------------------------------- *
     *  Invalid $ref handling
     * --------------------------------------------------------------------- */

    @Test
    void walk_InvalidRef_ThrowsIllegalArgumentException() throws Exception {
        // The schema permits any object; the instance contains a malformed $ref.
        jsonSchema = createSchema("{\"type\":\"object\"}");
        JsonNode instance = node("{\"$ref\":\"http://[\"}");

        assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.walk(executionContext, instance, true),
                "Malformed $ref should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Missing node handling
     * --------------------------------------------------------------------- */

    @Test
    void walk_MissingNode_ThrowsIllegalArgumentException() {
        JsonNode missing = MissingNode.getInstance();

        assertThrows(IllegalArgumentException.class,
                () -> jsonSchema.walk(executionContext, missing, true),
                "MissingNode should be rejected with IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- *
     *  Uninitialized validators
     * --------------------------------------------------------------------- */

    @Test
    void walk_UninitializedValidators_ThrowsIllegalStateException() throws Exception {
        // Build a schema that would normally have validators.
        jsonSchema = createSchema("{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}");

        // Force the internal validator list to null via reflection.
        java.lang.reflect.Field validatorsField = BaseJsonValidator.class.getDeclaredField("validators");
        validatorsField.setAccessible(true);
        validatorsField.set(jsonSchema, null);

        JsonNode instance = node("{\"name\":\"John\"}");

        assertThrows(IllegalStateException.class,
                () -> jsonSchema.walk(executionContext, instance, true),
                "Calling walk on a schema with un‑initialised validators must throw IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     *  Circular $ref without recursive anchor – guarded with timeout
     * --------------------------------------------------------------------- */

    @Test
    @Timeout(5) // Prevent the test from hanging indefinitely.
    void walk_CircularRefWithoutRecursiveAnchor_ThrowsStackOverflowError() throws Exception {
        // Schema that references itself but does NOT declare $recursiveAnchor.
        jsonSchema = createSchema("{\"type\":\"object\",\"properties\":{\"self\":{\"$ref\":\"#\"}}}");
        JsonNode instance = node("{\"self\":{\"$ref\":\"#\"}}");

        Executable walkCall = () -> jsonSchema.walk(executionContext, instance, true);
        assertThrows(StackOverflowError.class, walkCall,
                "Circular $ref without a recursive anchor should eventually cause StackOverflowError");
    }

    /* --------------------------------------------------------------------- *
     *  Original simple tests (kept for completeness)
     * --------------------------------------------------------------------- */

    @Test
    void walk_WithValidateTrue_ReturnsResult() {
        ValidationResult result = jsonSchema.walk(executionContext, instanceNode, true);
        assertNotNull(result, "walk should return a non‑null ValidationResult");
    }

    @Test
    void walk_WithValidateFalse_ReturnsResult() {
        ValidationResult result = jsonSchema.walk(executionContext, instanceNode, false);
        assertNotNull(result, "walk should return a non‑null ValidationResult even when validation is disabled");
    }
}
```