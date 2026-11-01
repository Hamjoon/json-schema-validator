```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link JsonSchema#validate(String, InputFormat, OutputFormat, ExecutionContextCustomizer)}.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_13_Test {

    /** The schema under test. */
    private JsonSchema jsonSchema;

    /** Real validation context used to build a functional {@link JsonSchema} instance. */
    private ValidationContext validationContext;

    /** Mocked collaborators for the {@code validate} call. */
    @Mock
    private InputFormat inputFormat;

    @Mock
    private ExecutionContextCustomizer executionCustomizer;

    @Mock
    private OutputFormat<Integer> outputFormat;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // Build a minimal, valid JsonSchema instance.
        // -----------------------------------------------------------------
        // 1. Create a real ValidationContext (uses real meta‑schema & factory).
        JsonMetaSchema metaSchema = JsonMetaSchema.getV201909();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(metaSchema, factory, config);

        // 2. Provide a trivial (empty) schema node – this accepts any JSON.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode emptySchemaNode = mapper.createObjectNode(); // {}

        // 3. Mock the location and evaluation path – they are only used for
        //    bookkeeping inside JsonSchema and are not relevant for validation.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        when(schemaLocation.toString()).thenReturn("mocked-location");
        when(schemaLocation.getAbsoluteIri()).thenReturn(null);
        when(schemaLocation.resolve(anyString())).thenReturn(schemaLocation);

        JsonNodePath evaluationPath = mock(JsonNodePath.class);
        when(evaluationPath.toString()).thenReturn("mocked-path");

        // 4. Build the JsonSchema instance via the package‑private factory method.
        jsonSchema = JsonSchema.from(validationContext,
                                    schemaLocation,
                                    evaluationPath,
                                    emptySchemaNode,
                                    null,               // no parent schema
                                    false);             // do not suppress sub‑schema retrieval

        // -----------------------------------------------------------------
        // Stub the OutputFormat – we simply return a constant value so that
        // we can assert the result of the validate(...) call.
        // -----------------------------------------------------------------
        when(outputFormat.apply(any(JsonNode.class), anySet()))
                .thenReturn(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for this simple test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    @Test
    void validate_shouldReturnFormattedResult_usingExecutionContextCustomizer() {
        // GIVEN
        String json = "{\"name\":\"John\",\"age\":30}";

        // WHEN
        Integer result = jsonSchema.validate(json, inputFormat, outputFormat, executionCustomizer);

        // THEN
        assertEquals(42, result, "The formatted result should be the value supplied by the OutputFormat mock");
        // Verify that the OutputFormat was indeed invoked with a JsonNode and a (possibly empty) set of messages.
        verify(outputFormat, times(1)).apply(any(JsonNode.class), anySet());
    }
}
```