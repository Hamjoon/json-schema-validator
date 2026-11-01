package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /**
     * Factory instance used by all tests.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the public factory method – it creates a fully‑initialised instance
        // with the default meta‑schema (the exact version is irrelevant for these tests).
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        assertNotNull(factory, "Factory must be created");
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        factory = null;
    }

    private static Stream<Arguments> validSchemaProvider() {
        return Stream.of(Arguments.of("{\"type\":\"object\"}", false), Arguments.of("{\"type\":\"string\"}", true));
    }

    /**
     * Verify that a well‑formed JSON schema supplied via an {@link InputStream}
     * results in a non‑null {@link JsonSchema} instance.
     */
    @Test
    void testGetSchemaWithValidInput() throws IOException {
        // A minimal, valid JSON schema (object with no properties)
        String jsonSchema = "{\"type\":\"object\"}";
        try (InputStream schemaStream = new ByteArrayInputStream(jsonSchema.getBytes())) {
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
            JsonSchema schema = factory.getSchema(schemaStream, config);
            assertNotNull(schema, "Returned JsonSchema must not be null");
            // The concrete class is package‑private, but we can assert the type via the interface.
            assertTrue(schema instanceof JsonSchema, "Result should be an instance of JsonSchema");
        }
    }

    /**
     * Verify that malformed JSON causes a {@link JsonSchemaException}
     * to be thrown. The underlying {@link ObjectMapper} throws an {@link IOException},
     * which the method should wrap.
     */
    @Test
    void testGetSchemaWithInvalidInputThrowsException() {
        // Deliberately malformed JSON
        String malformed = "this is not json";
        InputStream schemaStream = new ByteArrayInputStream(malformed.getBytes());
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStream, config), "Expected JsonSchemaException for malformed JSON");
        // The cause should be an IOException (often a JsonParseException)
        assertTrue(thrown.getCause() instanceof IOException, "Cause of JsonSchemaException should be an IOException");
    }

    /**
     * Group: normal execution – different valid schemas and a {@code null} config.
     */
    @ParameterizedTest(name = "[{index}] schema={0}, configIsNull={1}")
    @MethodSource("validSchemaProvider")
    void testGetSchemaValidScenarios(String json, boolean configIsNull) {
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            SchemaValidatorsConfig cfg = configIsNull ? null : new SchemaValidatorsConfig();
            JsonSchema schema = factory.getSchema(stream, cfg);
            assertNotNull(schema, "Schema must not be null for valid input");
            // Verify that the underlying JsonNode has the expected "type" value.
            // We cannot access the private node directly, but we can re‑parse the schema
            // string and compare the type field.
            JsonNode node = factory.getJsonMapper().readTree(json);
            assertEquals(node.get("type").asText(), schema.getSchemaNode().get("type").asText(), "The created JsonSchema should reflect the supplied type");
        } catch (IOException e) {
            fail("Unexpected IOException while testing valid schema: " + e.getMessage());
        }
    }

    /**
     * Group: null InputStream.
     * Expect a {@link JsonSchemaException} whose cause is a {@link NullPointerException}.
     */
    @Test
    void testGetSchemaWithNullInputStream() {
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(null, config), "Null InputStream should result in JsonSchemaException");
        assertTrue(thrown.getCause() instanceof NullPointerException, "Cause should be NullPointerException");
    }

    /**
     * Group: empty InputStream.
     * The ObjectMapper will throw a parsing exception (subclass of {@link IOException}),
     * which must be wrapped in {@link JsonSchemaException}.
     */
    @Test
    void testGetSchemaWithEmptyInputStream() {
        InputStream empty = new ByteArrayInputStream(new byte[0]);
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(empty, config), "Empty stream should cause JsonSchemaException");
        assertTrue(thrown.getCause() instanceof IOException, "Cause must be an IOException (e.g., JsonParseException)");
    }

    /**
     * Group: IOException from stream.
     * Simulate a failing stream by overriding {@link InputStream#read()} to throw.
     */
    @Test
    void testGetSchemaWhenStreamThrowsIOException() {
        InputStream badStream = new InputStream() {

            @Override
            public int read() throws IOException {
                throw new IOException("Simulated read failure");
            }
        };
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> factory.getSchema(badStream, config), "Stream IOException should be wrapped");
        assertEquals("Simulated read failure", thrown.getCause().getMessage(), "Original IOException message must be preserved");
    }

    /**
     * Group: custom configuration flags (typeLoose & failFast).
     * The method should still return a non‑null schema.
     */
    @Test
    void testGetSchemaWithCustomConfigFlags() {
        String json = "{\"type\":\"string\",\"format\":\"date-time\"}";
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            SchemaValidatorsConfig config = new SchemaValidatorsConfig();
            // Assuming the setters exist – they are part of the public API.
            config.setTypeLoose(true);
            config.setFailFast(true);
            JsonSchema schema = factory.getSchema(stream, config);
            assertNotNull(schema, "Schema must be created even with custom flags");
            // No direct way to assert that the flags are honoured; we just ensure creation succeeds.
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Group: large schema payload (10 000 properties).
     * Verify that the factory can handle a big schema without OOM or unexpected errors.
     */
    @Test
    void testGetSchemaWithLargePayload() {
        // Build a JSON object with 10 000 simple properties.
        StringBuilder sb = new StringBuilder("{\"type\":\"object\",\"properties\":{");
        for (int i = 0; i < 10_000; i++) {
            sb.append("\"p").append(i).append("\":{\"type\":\"string\"}");
            if (i < 9_999)
                sb.append(',');
        }
        sb.append("}}");
        String largeSchema = sb.toString();
        try (InputStream stream = new ByteArrayInputStream(largeSchema.getBytes())) {
            JsonSchema schema = factory.getSchema(stream, new SchemaValidatorsConfig());
            assertNotNull(schema, "Schema must be created for large payload");
            // Quick sanity check: the underlying node should contain the expected number of properties.
            assertEquals(10_000, schema.getSchemaNode().get("properties").size(), "Properties count should match the generated payload");
        } catch (IOException e) {
            fail("Unexpected IOException while handling large schema: " + e.getMessage());
        }
    }

    /**
     * Group: schema containing a $ref.
     * Ensure that the $ref node is preserved in the parsed schema.
     */
    @Test
    void testGetSchemaWithRefNode() {
        String json = "{\"$ref\":\"#/definitions/foo\",\"definitions\":{\"foo\":{\"type\":\"integer\"}}}";
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            JsonSchema schema = factory.getSchema(stream, new SchemaValidatorsConfig());
            assertNotNull(schema, "Schema must not be null when $ref is present");
            JsonNode root = schema.getSchemaNode();
            assertTrue(root.has("$ref"), "Root node should contain $ref");
            assertEquals("#/definitions/foo", root.get("$ref").asText(), "$ref value must be preserved");
        } catch (IOException e) {
            fail("Unexpected IOException: " + e.getMessage());
        }
    }

    /**
     * Group: null ObjectMapper.
     * By subclassing the factory we can force {@code getJsonMapper()} to return {@code null}
     * and verify that a {@link NullPointerException} propagates (not wrapped).
     */
    @Test
    void testGetSchemaWhenObjectMapperIsNull() {
        JsonSchemaFactory brokenFactory = new JsonSchemaFactory() {

            @Override
            protected ObjectMapper getJsonMapper() {
                // force NPE inside getSchema
                return null;
            }
        };
        String json = "{\"type\":\"object\"}";
        InputStream stream = new ByteArrayInputStream(json.getBytes());
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> brokenFactory.getSchema(stream, config), "When ObjectMapper is null a NullPointerException should be thrown");
        // No further assertions – the exception type itself confirms the behaviour.
    }
}
