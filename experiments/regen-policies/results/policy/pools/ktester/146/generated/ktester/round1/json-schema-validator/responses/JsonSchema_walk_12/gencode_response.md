```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.collectors.CollectingOutputFormat;

/**
 * Additional tests for {@link JsonSchema#walk(ExecutionContext, JsonNode, OutputFormat, boolean, ExecutionContextCustomizer)}.
 *
 * <p>The existing {@code @BeforeEach} creates a permissive schema that always validates.
 * For some scenarios (e.g., validation failures or $ref handling) a dedicated schema is
 * built inside the test method.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_walk_12_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema; // permissive schema created in @BeforeEach

    /**
     * Build a very small schema (an empty object schema) that will always validate
     * successfully.  The schema is created via {@link JsonSchemaFactory} because the
     * {@code JsonSchema} constructor is private.
     */
    @BeforeEach
    void setupBeforeEach() {
        // a schema that accepts any JSON value – the simplest possible valid schema
        String schemaJson = "{ \"$schema\" : \"https://json-schema.org/draft/2019-09/schema\" }";

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(s -> s.schemas(Collections.emptyMap())));

        // use the default configuration – nothing special is required for the tests
        schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Schema should have been created");
    }

    @AfterEach
    void teardownAfterEach() {
        // nothing to clean up for now
    }

    /* --------------------------------------------------------------------- */
    /* Existing tests (kept unchanged)                                      */
    /* --------------------------------------------------------------------- */

    /**
     * Verify that {@code walk(...)} returns the value produced by the supplied
     * {@link OutputFormat}.  The test uses a Mockito mock for {@code OutputFormat}
     * that simply returns a constant string.  The underlying {@link ValidationResult}
     * should contain no validation messages because the schema is permissive.
     */
    @Test
    void walkReturnsOutputFromOutputFormat() {
        // given
        JsonNode node = MAPPER.createObjectNode(); // an empty JSON object
        @SuppressWarnings("unchecked")
        OutputFormat<String> format = mock(OutputFormat.class);
        when(format.apply(any(ValidationResult.class))).thenReturn("OK");

        // when
        String result = schema.walk(schema.createExecutionContext(),
                                    node,
                                    format,
                                    true,                     // ask the walk to validate
                                    (ExecutionContextCustomizer) null);

        // then
        assertEquals("OK", result, "walk should return the value produced by OutputFormat");
        // verify that the format was invoked exactly once with a ValidationResult that has no errors
        ArgumentCaptor<ValidationResult> captor = ArgumentCaptor.forClass(ValidationResult.class);
        verify(format, times(1)).apply(captor.capture());
        ValidationResult vr = captor.getValue();
        assertTrue(vr.getMessages().isEmpty(), "ValidationResult should contain no messages for a permissive schema");
    }

    /**
     * Verify that a non‑null {@link ExecutionContextCustomizer} is invoked before the walk
     * proceeds.  The customizer is mocked and we assert that its {@code customize} method is
     * called exactly once.
     */
    @Test
    void walkInvokesExecutionContextCustomizer() {
        // given
        JsonNode node = MAPPER.createObjectNode();
        @SuppressWarnings("unchecked")
        OutputFormat<Set<ValidationMessage>> format = mock(OutputFormat.class);
        when(format.apply(any(ValidationResult.class))).thenReturn(Collections.emptySet());

        ExecutionContextCustomizer customizer = mock(ExecutionContextCustomizer.class);
        // the customizer does not need to do anything; we just want to verify the call

        // when
        Set<ValidationMessage> result = schema.walk(schema.createExecutionContext(),
                                                    node,
                                                    format,
                                                    false,               // do not validate – we only care about the customizer call
                                                    customizer);

        // then
        assertNotNull(result, "Result must not be null");
        verify(customizer, times(1)).customize(any(ExecutionContext.class));
        // also verify that the output format was used
        verify(format, times(1)).apply(any(ValidationResult.class));
    }

    /* --------------------------------------------------------------------- */
    /* New tests derived from the JSON test‑case description                 */
    /* --------------------------------------------------------------------- */

    /**
     * Validate true with the default {@link CollectingOutputFormat}.  The node conforms
     * to the schema, therefore the resulting set of {@link ValidationMessage}s must be empty.
     */
    @Test
    void validateTrueCollectingOutputFormatSuccess() {
        JsonNode node = MAPPER.createObjectNode(); // any node is valid for the permissive schema
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        Set<ValidationMessage> messages = schema.walk(schema.createExecutionContext(),
                                                     node,
                                                     format,
                                                     true,
                                                     (ExecutionContextCustomizer) null);

        assertNotNull(messages, "Resulting set must not be null");
        assertTrue(messages.isEmpty(), "No validation errors expected for a valid node");
    }

    /**
     * Validate true with the default {@link CollectingOutputFormat} but the node violates
     * the schema.  A schema that requires a string is used; an object node is supplied,
     * so at least one {@link ValidationMessage} must be present.
     */
    @Test
    void validateTrueCollectingOutputFormatFailure() {
        // a schema that only accepts strings
        String schemaJson = "{ \"$schema\" : \"https://json-schema.org/draft/2019-09/schema\", \"type\" : \"string\" }";
        JsonSchema stringSchema = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                .getSchema(schemaJson);

        JsonNode invalidNode = MAPPER.createObjectNode(); // object, not a string
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        Set<ValidationMessage> messages = stringSchema.walk(stringSchema.createExecutionContext(),
                                                             invalidNode,
                                                             format,
                                                             true,
                                                             (ExecutionContextCustomizer) null);

        assertNotNull(messages, "Resulting set must not be null");
        assertFalse(messages.isEmpty(), "Validation errors expected because the node does not match the schema");
    }

    /**
     * When {@code validate} is {@code false} the walk must skip validation entirely.
     * The collecting output format therefore receives an empty {@link ValidationResult}.
     */
    @Test
    void validateFalseSkipsValidation() {
        JsonNode node = MAPPER.readTree("{\"any\":\"value\"}");
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        Set<ValidationMessage> messages = schema.walk(schema.createExecutionContext(),
                                                     node,
                                                     format,
                                                     false,
                                                     (ExecutionContextCustomizer) null);

        assertNotNull(messages, "Result must not be null");
        assertTrue(messages.isEmpty(), "No validation should be performed when validate=false");
    }

    /**
     * Custom {@link ExecutionContextCustomizer} must be invoked before validation.
     * The test uses a simple flag that the lambda flips, proving that the customizer ran.
     */
    @Test
    void customExecutionContextCustomizerIsInvoked() {
        JsonNode node = MAPPER.createObjectNode();
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        final boolean[] called = { false };
        ExecutionContextCustomizer customizer = ctx -> called[0] = true;

        Set<ValidationMessage> messages = schema.walk(schema.createExecutionContext(),
                                                     node,
                                                     format,
                                                     true,
                                                     customizer);

        assertTrue(called[0], "ExecutionContextCustomizer should have been executed");
        assertNotNull(messages);
        assertTrue(messages.isEmpty(), "Schema is permissive, therefore no messages expected");
    }

    /**
     * Passing a {@code null} {@link ExecutionContext} must result in a {@link NullPointerException}.
     */
    @Test
    void nullExecutionContextThrowsNPE() {
        JsonNode node = MAPPER.createObjectNode();
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        assertThrows(NullPointerException.class,
                () -> schema.walk(null, node, format, true, (ExecutionContextCustomizer) null),
                "walk should NPE when executionContext is null");
    }

    /**
     * Passing a {@code null} {@link JsonNode} must result in a {@link NullPointerException}.
     */
    @Test
    void nullJsonNodeThrowsNPE() {
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        assertThrows(NullPointerException.class,
                () -> schema.walk(schema.createExecutionContext(), null, format, true, (ExecutionContextCustomizer) null),
                "walk should NPE when JsonNode is null");
    }

    /**
     * An {@link OutputFormat} that throws a {@link RuntimeException} must have that exception
     * propagated by {@code walk}.
     */
    @Test
    void outputFormatThrowsRuntimeException() {
        JsonNode node = MAPPER.createObjectNode();

        OutputFormat<Object> explodingFormat = new OutputFormat<Object>() {
            @Override
            public Object apply(ValidationResult validationResult) {
                throw new IllegalStateException("format failure");
            }
        };

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> schema.walk(schema.createExecutionContext(),
                                 node,
                                 explodingFormat,
                                 true,
                                 (ExecutionContextCustomizer) null));

        assertEquals("format failure", ex.getMessage(),
                "Exception message should be propagated unchanged");
    }

    /**
     * If the {@link ExecutionContextCustomizer} itself throws an exception, that exception
     * must be propagated to the caller.
     */
    @Test
    void executionContextCustomizerThrowsException() {
        JsonNode node = MAPPER.createObjectNode();
        CollectingOutputFormat<Set<ValidationMessage>> format = new CollectingOutputFormat<>();

        ExecutionContextCustomizer badCustomizer = ctx -> {
            throw new RuntimeException("customizer failure");
        };

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> schema.walk(schema.createExecutionContext(),
                                 node,
                                 format,
                                 true,
                                 badCustomizer));

        assertEquals("customizer failure", ex.getMessage(),
                "Exception from customizer should be propagated");
    }

    /**
     * Passing a {@code null} {@link OutputFormat} must result in a {@link NullPointerException}
     * before any validation is performed.
     */
    @Test
    void nullOutputFormatThrowsNPE() {
        JsonNode node = MAPPER.createObjectNode();

        assertThrows(NullPointerException.class,
                () -> schema.walk(schema.createExecutionContext(),
                                 node,
                                 null,
                                 true,
                                 (ExecutionContextCustomizer) null),
                "walk should NPE when OutputFormat is null");
    }

    /**
     * A schema containing a malformed $ref (invalid percent‑encoding) should cause
     * {@code walk} to throw an {@link IllegalArgumentException}.
     */
    @Test
    void malformedRefThrowsIllegalArgumentException() {
        // schema with a malformed $ref
        String malformedRefSchema = "{ \"$schema\" : \"https://json-schema.org/draft/2019-09/schema\", \"$ref\" : \"%ZZ\" }";
        JsonSchema schemaWithBadRef = JsonSchemaFactory.getInstance(VersionFlag.V202012)
                .getSchema(malformedRefSchema);

        JsonNode anyNode = MAPPER.createObjectNode();

        assertThrows(IllegalArgumentException.class,
                () -> schemaWithBadRef.walk(schemaWithBadRef.createExecutionContext(),
                                           anyNode,
                                           new CollectingOutputFormat<>(),
                                           true,
                                           (ExecutionContextCustomizer) null),
                "Malformed $ref should result in IllegalArgumentException");
    }

    /**
     * An {@link UnsupportedEncodingException} that occurs while decoding a $ref is wrapped
     * in an {@link IllegalArgumentException}.  This test forces the situation by using a
     * custom {@link JsonSchemaFactory} that supplies a {@link SchemaLoader} which throws
     * the checked exception.  The walk method must surface it as an {@code IllegalArgumentException}.
     */
    @Test
    void unsupportedEncodingExceptionFromRefIsWrappedAsIllegalArgumentException() {
        // Build a schema that triggers UnsupportedEncodingException during $ref handling.
        // The library decodes the $ref using URLDecoder.decode(ref, "UTF-8").
        // To simulate the failure we create a schema with a $ref that contains an
        // illegal charset name via a custom loader (the easiest way is to mock the
        // loader to throw the exception).  Here we mock the loader directly.

        SchemaLoader mockLoader = mock(SchemaLoader.class);
        when(mockLoader.load(anyString())).thenAnswer(invocation -> {
            // Simulate the library trying to decode a ref and hitting an unsupported charset
            throw new UnsupportedEncodingException("unsupported charset");
        });

        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012,
                builder -> builder.schemaLoaders(s -> s.loader(mockLoader)));

        // The actual schema content is irrelevant because the loader will throw.
        JsonSchema schemaWithBadLoader = factory.getSchema("{\"$ref\":\"any\"}");

        JsonNode anyNode = MAPPER.createObjectNode();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> schemaWithBadLoader.walk(schemaWithBadLoader.createExecutionContext(),
                                               anyNode,
                                               new CollectingOutputFormat<>(),
                                               true,
                                               (ExecutionContextCustomizer) null));

        assertTrue(ex.getMessage().contains("unsupported charset"),
                "Exception message should indicate the underlying UnsupportedEncodingException");
    }

    /* --------------------------------------------------------------------- */
    /* Boilerplate for global resources (kept unchanged)                    */
    /* --------------------------------------------------------------------- */

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these unit tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }
}
```