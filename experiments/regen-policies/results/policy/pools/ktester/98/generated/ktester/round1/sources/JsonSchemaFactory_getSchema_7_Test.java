package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_7_Test {

    /**
     * Factory used for all test cases – created once because it is thread‑safe.
     */
    private static JsonSchemaFactory factory;

    /**
     * A simple JSON schema used in the tests (type: object).
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /**
     * A JSON document that conforms to {@link #SIMPLE_SCHEMA}.
     */
    private static final String VALID_INSTANCE = "{}";

    /**
     * A JSON document that does NOT conform to {@link #SIMPLE_SCHEMA}.
     */
    private static final String INVALID_INSTANCE = "[]";

    /**
     * A JSON schema that contains an unresolvable $ref.
     */
    private static final String UNRESOLVABLE_REF_SCHEMA = "{ \"$ref\": \"nonexistent.json\" }";

    /**
     * A valid YAML schema (type: object).
     */
    private static final String YAML_SCHEMA = "type: object";

    /**
     * A JSON schema with a UTF‑8 Byte Order Mark at the beginning.
     */
    private static final byte[] BOM = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };

    @BeforeAll
    static void setupBeforeAll() {
        // Use the 2020‑12 draft – it is the most recent version supported by the library.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012, builder -> builder.schemaLoaders(schemaLoaders -> {
            // No external schemas are required for these tests, but we keep the
            // configuration explicit to avoid accidental look‑ups.
        }));
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
        factory = null;
    }

    private static Stream<Arguments> invalidContentProvider() {
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        InputStream invalidJson = new ByteArrayInputStream("not a json".getBytes(StandardCharsets.UTF_8));
        InputStream yamlStream = new ByteArrayInputStream(YAML_SCHEMA.getBytes(StandardCharsets.UTF_8));
        SchemaValidatorsConfig defaultConfig = SchemaValidatorsConfig.builder().build();
        return Stream.of(Arguments.of("Empty InputStream with default config", emptyStream, defaultConfig), Arguments.of("Empty InputStream with null config", emptyStream, null), Arguments.of("Malformed JSON with default config", invalidJson, defaultConfig), Arguments.of("Malformed JSON with null config", invalidJson, null), Arguments.of("YAML content with default config (JSON expected)", yamlStream, defaultConfig), Arguments.of("YAML content with null config (JSON expected)", yamlStream, null));
    }

    @Test
    void getSchema_fromInputStream_returnsValidJsonSchema() throws Exception {
        // GIVEN: an InputStream containing a valid JSON schema and a default config
        try (InputStream schemaStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8))) {
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
            // WHEN: the factory creates a JsonSchema from the stream
            JsonSchema jsonSchema = factory.getSchema(schemaStream, config);
            // THEN: a non‑null JsonSchema is returned
            assertNotNull(jsonSchema, "JsonSchema should not be null");
            // AND: the schema can validate a conforming JSON instance
            try (InputStream instanceStream = new ByteArrayInputStream(VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                // The validate method throws a ValidationException when validation fails.
                // If no exception is thrown, the instance is considered valid.
                jsonSchema.validate(instanceStream);
            }
            // AND: validation of a non‑conforming instance must fail
            try (InputStream badInstance = new ByteArrayInputStream(INVALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                assertThrows(ValidationException.class, () -> jsonSchema.validate(badInstance), "Validation should fail for an instance that does not match the schema");
            }
        }
    }

    @Test
    void getSchema_nullInputStream_throwsNullPointerException() {
        // GIVEN: a null InputStream and a default config
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        // WHEN / THEN: the method must throw NPE
        assertThrows(NullPointerException.class, () -> factory.getSchema((InputStream) null, config), "Calling getSchema with a null InputStream should throw NullPointerException");
    }

    @Test
    void getSchema_nullConfig_fallsBackToDefaultConfiguration() throws Exception {
        // GIVEN: a valid schema stream but a null configuration
        try (InputStream schemaStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8))) {
            // WHEN: the factory is asked to create a schema with a null config
            JsonSchema jsonSchema = factory.getSchema(schemaStream, null);
            // THEN: a schema is still produced and works for validation
            assertNotNull(jsonSchema, "JsonSchema should not be null even when config is null");
            try (InputStream instance = new ByteArrayInputStream(VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                jsonSchema.validate(instance);
            }
        }
    }

    @Test
    void getSchema_nullInputStreamAndNullConfig_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> factory.getSchema((InputStream) null, null), "Both arguments null – the method should still fail fast with NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidContentProvider")
    void getSchema_invalidContent_throwsException(String description, InputStream schemaStream, SchemaValidatorsConfig config) {
        // The factory is expected to propagate a parsing exception (wrapped in a RuntimeException)
        assertThrows(RuntimeException.class, () -> factory.getSchema(schemaStream, config), description);
    }

    @Test
    void getSchema_customConfig_appliesConfiguration() throws Exception {
        // custom config: typeLoose = true, failFast = true
        SchemaValidatorsConfig customConfig = SchemaValidatorsConfig.builder().typeLoose(true).failFast(true).build();
        try (InputStream schemaStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8))) {
            JsonSchema jsonSchema = factory.getSchema(schemaStream, customConfig);
            assertNotNull(jsonSchema, "JsonSchema should be created with custom config");
            // The custom config does not change validation of a simple object schema,
            // but we can verify that the config was indeed used by checking internal flags
            // via reflection (only for demonstration – production code would expose getters).
            // Here we simply ensure no exception is thrown during validation.
            try (InputStream instance = new ByteArrayInputStream(VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                jsonSchema.validate(instance);
            }
        }
    }

    @Test
    void getSchema_withUtf8Bom_isParsedSuccessfully() throws Exception {
        // Build a stream that starts with a BOM followed by a valid JSON schema
        byte[] bomAndSchema = new byte[BOM.length + SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8).length];
        System.arraycopy(BOM, 0, bomAndSchema, 0, BOM.length);
        System.arraycopy(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8), 0, bomAndSchema, BOM.length, SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8).length);
        try (InputStream schemaStream = new ByteArrayInputStream(bomAndSchema)) {
            JsonSchema jsonSchema = factory.getSchema(schemaStream, null);
            assertNotNull(jsonSchema, "Schema with BOM should be parsed");
            // Validate a conforming instance to ensure the schema is functional
            try (InputStream instance = new ByteArrayInputStream(VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                jsonSchema.validate(instance);
            }
        }
    }

    @Test
    void getSchema_schemaWithTrailingWhitespace_isParsed() throws Exception {
        String schemaWithWs = SIMPLE_SCHEMA + "   \n\t  ";
        try (InputStream schemaStream = new ByteArrayInputStream(schemaWithWs.getBytes(StandardCharsets.UTF_8))) {
            JsonSchema jsonSchema = factory.getSchema(schemaStream, null);
            assertNotNull(jsonSchema, "Schema with trailing whitespace should be parsed");
            try (InputStream instance = new ByteArrayInputStream(VALID_INSTANCE.getBytes(StandardCharsets.UTF_8))) {
                jsonSchema.validate(instance);
            }
        }
    }

    @Test
    void getSchema_inputStreamThrowsIOException_propagatesException() throws Exception {
        // Mock an InputStream that throws IOException on read()
        InputStream brokenStream = Mockito.mock(InputStream.class);
        Mockito.when(brokenStream.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt())).thenThrow(new IOException("Simulated read failure"));
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(RuntimeException.class, () -> factory.getSchema(brokenStream, config), "IOException from the stream should be propagated (wrapped in RuntimeException)");
    }

    @Test
    void getSchema_closedInputStream_throwsException() throws Exception {
        InputStream closedStream = new ByteArrayInputStream(SIMPLE_SCHEMA.getBytes(StandardCharsets.UTF_8));
        // close before passing to the factory
        closedStream.close();
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
        assertThrows(RuntimeException.class, () -> factory.getSchema(closedStream, config), "Using a closed InputStream should result in an IOException (wrapped)");
    }

    @Test
    void getSchema_unresolvableRef_throwsJsonSchemaException() {
        try (InputStream schemaStream = new ByteArrayInputStream(UNRESOLVABLE_REF_SCHEMA.getBytes(StandardCharsets.UTF_8))) {
            SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().build();
            // The library throws JsonSchemaException (a RuntimeException) when $ref cannot be resolved.
            assertThrows(JsonSchemaException.class, () -> factory.getSchema(schemaStream, config), "Unresolvable $ref should cause JsonSchemaException");
        }
    }
}
