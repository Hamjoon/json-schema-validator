```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit‑tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, ExecutionContextCustomizer)}.
 *
 * <p>The original test class already covers the happy‑path scenarios.  The tests below
 * focus on the edge‑cases described in the supplied JSON test‑case matrix:
 *
 * <ul>
 *   <li>Null arguments → {@link NullPointerException}</li>
 *   <li>Deserialization problems → {@link JsonSchemaException} wrapping the original cause</li>
 *   <li>ExecutionContextCustomizer that throws → exception is propagated</li>
 *   <li>OutputFormat that throws → exception is propagated</li>
 *   <li>Custom InputFormat that throws {@link IOException} → wrapped in {@link JsonSchemaException}</li>
 *   <li>Invalid $ref in the instance → {@link JsonSchemaException}</li>
 * </ul>
 *
 * <p>All tests reuse the shared {@code schema} created in {@code @BeforeAll}.  The
 * schema expects an object with a required string property {@code name}.  This
 * makes it easy to produce both valid and invalid JSON documents.
 */
class JsonSchema_validate_15_Test {

    /** Shared schema used by all test cases */
    private static JsonSchema schema;

    /** Input format used for the tests (JSON string) */
    private static final InputFormat INPUT_FORMAT = InputFormat.JSON;

    /** Simple {@link OutputFormat} implementation that materialises the set of
     *  {@link ValidationMessage}s into a {@link List}. */
    private static final OutputFormat<List<ValidationMessage>> LIST_OUTPUT_FORMAT =
            messages -> new ArrayList<>(messages);

    /** No‑op customiser – required by the method signature but does not alter the
     *  execution context. */
    private static final ExecutionContextCustomizer NO_OP_CUSTOMIZER = ctx -> {
        // intentionally left blank
    };

    @BeforeAll
    static void setupBeforeAll() {
        // A tiny schema that expects an object with a string property “name”.
        String schemaData = "{\n"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\",\n"
                + "  \"type\": \"object\",\n"
                + "  \"properties\": {\n"
                + "    \"name\": { \"type\": \"string\" }\n"
                + "  },\n"
                + "  \"required\": [\"name\"]\n"
                + "}";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(
                SpecVersion.VersionFlag.V202012,
                builder -> builder.schemaLoaders(schemaLoaders -> schemaLoaders.schemas(Collections.emptyMap())));

        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();

        schema = factory.getSchema(schemaData, config);
    }

    @AfterAll
    static void teardownAfterAll() {
        schema = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Null / Invalid argument tests                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_nullInput_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate(null, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("input must not be null", ex.getMessage());
    }

    @Test
    void validate_nullInputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", null, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));
        assertEquals("inputFormat must not be null", ex.getMessage());
    }

    @Test
    void validate_nullOutputFormat_throwsNullPointerException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, null, NO_OP_CUSTOMIZER));
        assertEquals("output format must not be null", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Deserialization error handling                                        */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_urlEncodedInvalid_throwsWrappedUnsupportedEncodingException() {
        // UrlEncodedInputFormat will try to URL‑decode the string; the bogus value
        // triggers an UnsupportedEncodingException which must be wrapped.
        InputFormat urlEncoded = UrlEncodedInputFormat.INSTANCE;

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate("%ZZInvalid%", urlEncoded, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));

        assertTrue(ex.getCause() instanceof UnsupportedEncodingException,
                "Cause should be UnsupportedEncodingException");
    }

    @Test
    void validate_malformedJson_throwsWrappedJsonProcessingException() {
        // JsonInputFormat parses the string as JSON – malformed content causes a
        // JsonProcessingException that must be wrapped.
        InputFormat jsonFormat = JsonInputFormat.INSTANCE;

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate("not a json", jsonFormat, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));

        assertTrue(ex.getCause() instanceof com.fasterxml.jackson.core.JsonProcessingException,
                "Cause should be JsonProcessingException");
    }

    /* --------------------------------------------------------------------- */
    /*  ExecutionContextCustomizer exception propagation                     */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_customizerThrows_propagatesException() {
        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new IllegalStateException("customizer failure");
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, LIST_OUTPUT_FORMAT, badCustomizer));

        assertEquals("customizer failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  OutputFormat exception propagation                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_outputFormatThrows_propagatesRuntimeException() {
        // OutputFormat that deliberately throws.
        OutputFormat<List<ValidationMessage>> throwingFormat = messages -> {
            throw new RuntimeException("output format failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.validate("{\"name\":\"John\"}", INPUT_FORMAT, throwingFormat, NO_OP_CUSTOMIZER));

        assertEquals("output format failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  InputFormat that throws IOException                                    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_inputFormatThrowsIOException_wrappedInJsonSchemaException() throws IOException {
        // Create a mock InputFormat whose deserialize method throws IOException.
        InputFormat ioThrowingFormat = mock(InputFormat.class);
        // The real InputFormat interface defines a method `deserialize(String)`.
        // We stub any invocation to throw the checked exception.
        when(ioThrowingFormat.deserialize(anyString())).thenThrow(new IOException("boom"));

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate("any string", ioThrowingFormat, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));

        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*  Schema‑internal $ref resolution error                                 */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_invalidRef_throwsJsonSchemaException() {
        // The instance contains a $ref that cannot be resolved by the schema.
        String inputWithBadRef = "{\"$ref\":\"#/nonexistent\"}";

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> schema.validate(inputWithBadRef, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER));

        assertTrue(ex.getMessage().contains("Unable to resolve reference"),
                "Exception message should mention inability to resolve reference");
    }

    /* --------------------------------------------------------------------- */
    /*  Validation success / failure (reuse of earlier happy‑path tests)    */
    /* --------------------------------------------------------------------- */

    @Test
    void validate_validInput_returnsEmptyList() {
        String goodInput = "{\"name\":\"John\"}";
        List<ValidationMessage> messages = schema.validate(goodInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        assertTrue(messages.isEmpty(), "Expected no validation messages for valid input");
    }

    @Test
    void validate_invalidInput_returnsValidationMessages() {
        String badInput = "{\"name\":1}";
        List<ValidationMessage> messages = schema.validate(badInput, INPUT_FORMAT, LIST_OUTPUT_FORMAT, NO_OP_CUSTOMIZER);
        assertEquals(1, messages.size(), "Expected a single validation message");
        assertTrue(messages.get(0).getMessage().toLowerCase().contains("type"),
                "Message should mention a type problem");
    }
}
```