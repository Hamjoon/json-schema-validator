package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_4_Test {

    /**
     * Mocks required to build a {@link JsonSchema} instance
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    /**
     * The object under test
     */
    JsonSchema jsonSchema;

    /* --------------------------------------------------------------------- *
     *  Helper class used by the parameterised tests
     * --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String description;

        final String input;

        final InputFormat format;

        final boolean validate;

        ArgumentsHolder(String description, String input, InputFormat format, boolean validate) {
            this.description = description;
            this.input = input;
            this.format = format;
            this.validate = validate;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /* -------------------------------------------------------------
         *  Configure the behaviour of the mocked ValidationContext & friends
         * ------------------------------------------------------------- */
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The schema does not contain $id, $anchor or $dynamicAnchor
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // The maps used inside the constructor must be mutable
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        /* -------------------------------------------------------------
         *  The remaining collaborators are simple stubs – their methods are
         *  never called directly by the code path we exercise.
         * ------------------------------------------------------------- */
        when(schemaLocation.toString()).thenReturn("http://example.com/schema");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        /* -------------------------------------------------------------
         *  Instantiate JsonSchema via reflection because its constructor is
         *  package‑private.  All constructor arguments are either real
         *  objects or mocks configured above.
         * ------------------------------------------------------------- */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
        /* -------------------------------------------------------------
         *  Configure the behaviour of the mocked ValidationContext & friends
         * ------------------------------------------------------------- */
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // The schema does not contain $id, $anchor or $dynamicAnchor
        when(validationContext.resolveSchemaId(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readAnchor(any(JsonNode.class))).thenReturn(null);
        when(metaSchema.readDynamicAnchor(any(JsonNode.class))).thenReturn(null);
        // The maps used inside the constructor must be mutable
        when(validationContext.getSchemaResources()).thenReturn(new ConcurrentHashMap<>());
        when(validationContext.getDynamicAnchors()).thenReturn(new ConcurrentHashMap<>());
        /* -------------------------------------------------------------
         *  Stub out the remaining collaborators – they are never called
         *  by the code paths exercised in these tests.
         * ------------------------------------------------------------- */
        when(schemaLocation.toString()).thenReturn("http://example.com/schema");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        /* -------------------------------------------------------------
         *  Instantiate JsonSchema via reflection because its constructor is
         *  package‑private.
         * ------------------------------------------------------------- */
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, SchemaLocation.class, JsonNodePath.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        jsonSchema = ctor.newInstance(validationContext, schemaLocation, evaluationPath, schemaNode, null, false);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to create a fresh ExecutionContext for each test
     * --------------------------------------------------------------------- */
    private ExecutionContext freshContext() {
        return jsonSchema.createExecutionContext();
    }

    /* --------------------------------------------------------------------- *
     *  Parameter source for the “valid walk” groups (JSON & YAML, with/without validation)
     * --------------------------------------------------------------------- */
    static Stream<ArgumentsHolder> validWalkProvider() {
        return Stream.of(// JSON – validate = true
        new ArgumentsHolder("Valid walk with validation (JSON input, validate = true)", "{\"name\":\"John\",\"age\":30}", InputFormat.JSON, true), // JSON – validate = false
        new ArgumentsHolder("Valid walk without validation (JSON input, validate = false)", "{\"id\":1,\"active\":true}", InputFormat.JSON, false), // YAML – validate = true
        new ArgumentsHolder("YAML input format – successful deserialization and validation", "name: Jane\nage: 25", InputFormat.YAML, true), // YAML – validate = false
        new ArgumentsHolder("Valid YAML input without validation (validate = false)", "name: Alice\nage: 30", InputFormat.YAML, false), // JSON literal null
        new ArgumentsHolder("Valid JSON literal null input – walk succeeds", "null", InputFormat.JSON, true), // JSON array
        new ArgumentsHolder("Valid JSON array input – walk succeeds", "[\"a\", \"b\", \"c\"]", InputFormat.JSON, true), // JSON number
        new ArgumentsHolder("Valid JSON number input – walk succeeds", "12345", InputFormat.JSON, true), // JSON empty object
        new ArgumentsHolder("Valid JSON empty object – walk succeeds", "{}", InputFormat.JSON, true));
    }

    /**
     * Simple happy‑path test for {@link JsonSchema#walk(ExecutionContext,String,InputFormat,boolean)}
     */
    @Test
    void walk_shouldReturnNonNullResult_forValidJsonInput() {
        // GIVEN
        ExecutionContext executionContext = new ExecutionContext();
        // minimal valid JSON document
        String json = "{ \"type\": \"object\" }";
        // the JSON format enum value
        InputFormat format = InputFormat.JSON;
        // we want the schema to be validated while walking
        boolean validate = true;
        // WHEN
        ValidationResult result = jsonSchema.walk(executionContext, json, format, validate);
        // THEN
        assertNotNull(result, "walk() must return a ValidationResult instance");
        // The actual content of ValidationResult depends on the schema that was supplied.
        // For this generic test we only assert that the result object exists.
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validWalkProvider")
    void walk_validInputs_returnsSuccessfulResult(ArgumentsHolder holder) {
        // GIVEN
        ExecutionContext ctx = freshContext();
        // WHEN
        ValidationResult result = jsonSchema.walk(ctx, holder.input, holder.format, holder.validate);
        // THEN
        assertNotNull(result, "Result must never be null");
        assertTrue(result.isSuccess(), () -> "Result should be successful for case: " + holder.description);
        assertTrue(result.getMessages().isEmpty(), () -> "No validation messages expected for case: " + holder.description);
    }

    /* --------------------------------------------------------------------- *
     *  Tests that exercise deserialization failures (invalid JSON/YAML,
     *  empty string, whitespace‑only, etc.)
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidJson_throwsJsonProcessingException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        // missing closing ]
        String malformed = "{\"unclosed\": [1,2,3}";
        // WHEN / THEN
        assertThrows(JsonProcessingException.class, () -> jsonSchema.walk(ctx, malformed, InputFormat.JSON, true), "Malformed JSON should cause JsonProcessingException");
    }

    @Test
    void walk_invalidJson_withoutValidation_throwsJsonProcessingException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        // malformed array
        String malformed = "{\"bad\": [1,2,}";
        // WHEN / THEN
        assertThrows(JsonProcessingException.class, () -> jsonSchema.walk(ctx, malformed, InputFormat.JSON, false), "Even without validation, malformed JSON must propagate the parsing exception");
    }

    @Test
    void walk_invalidYaml_throwsJsonProcessingException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        // missing closing ]
        String malformedYaml = "name: John\nage: [1, 2";
        // WHEN / THEN
        assertThrows(JsonProcessingException.class, () -> jsonSchema.walk(ctx, malformedYaml, InputFormat.YAML, true), "Malformed YAML should cause JsonProcessingException");
    }

    @Test
    void walk_emptyString_throwsJsonProcessingException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        String empty = "";
        // WHEN / THEN
        assertThrows(JsonProcessingException.class, () -> jsonSchema.walk(ctx, empty, InputFormat.JSON, true), "Empty string is not valid JSON and must trigger a parsing exception");
    }

    @Test
    void walk_whitespaceOnly_throwsJsonProcessingException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        String whitespace = "   \n\t  ";
        // WHEN / THEN
        assertThrows(JsonProcessingException.class, () -> jsonSchema.walk(ctx, whitespace, InputFormat.JSON, true), "Whitespace‑only input should be considered invalid JSON");
    }

    /* --------------------------------------------------------------------- *
     *  Tests for illegal arguments (null inputFormat, null executionContext)
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullInputFormat_throwsIllegalArgumentException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        String payload = "some text";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.walk(ctx, payload, null, true), "A null InputFormat must be rejected with IllegalArgumentException");
    }

    @Test
    void walk_unsupportedInputFormat_throwsIllegalArgumentException() {
        // GIVEN
        ExecutionContext ctx = freshContext();
        String payload = "<root><name>John</name></root>";
        // The library does not support XML – we use the enum constant to simulate it.
        // assume such constant exists
        InputFormat unsupported = InputFormat.XML;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> jsonSchema.walk(ctx, payload, unsupported, true), "Unsupported InputFormat should cause IllegalArgumentException");
    }

    @Test
    void walk_nullExecutionContext_throwsNullPointerException() {
        // GIVEN
        String json = "{\"key\":\"value\"}";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> jsonSchema.walk(null, json, InputFormat.JSON, true), "Null ExecutionContext must cause NullPointerException");
    }
}
