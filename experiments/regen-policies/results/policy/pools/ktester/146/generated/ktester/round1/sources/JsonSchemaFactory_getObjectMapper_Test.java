package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getObjectMapper_Test {

    /**
     * Factory instance used for all tests.
     */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a standard factory instance – the builder creates a fully‑initialised object.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        factory = null;
    }

    /**
     * Supplies the unsupported {@link InputFormat} values for the parameterized test.
     */
    private static Stream<Arguments> unsupportedInputFormats() {
        // The enum may not have a CUSTOM constant at compile time, so we create a
        // synthetic enum instance via reflection only for demonstration purposes.
        // If the enum already defines CUSTOM, the cast will succeed; otherwise we pass null.
        InputFormat custom = null;
        try {
            // Attempt to obtain a constant named "CUSTOM" via reflection.
            java.lang.reflect.Field field = InputFormat.class.getDeclaredField("CUSTOM");
            field.setAccessible(true);
            custom = (InputFormat) field.get(null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // No CUSTOM constant – keep custom as null; the test will still run for null.
        }
        return Stream.of(// null format
        Arguments.of((Object) null), // possibly a CUSTOM constant, may be null
        Arguments.of(custom)).filter(// filter out the second argument if it is null
        args -> args.get()[0] != null);
    }

    @Test
    void getObjectMapper_returnsJsonMapper_forJsonInputFormat() throws IOException {
        // GIVEN
        InputFormat format = InputFormat.JSON;
        // WHEN
        ObjectMapper mapper = factory.getObjectMapper(format);
        // THEN
        assertNotNull(mapper, "Mapper should not be null for JSON format");
        // The mapper returned by getObjectMapper should be the same instance as the
        // factory's internal JSON mapper.
        assertSame(factory.getJsonMapper(), mapper, "Factory should return its internal JSON ObjectMapper");
        // Additionally verify that the mapper can parse a JSON string.
        String json = "{\"name\":\"John\",\"age\":30}";
        JsonNode node = mapper.readTree(json);
        assertEquals("John", node.get("name").asText());
        assertEquals(30, node.get("age").asInt());
    }

    @Test
    void getObjectMapper_returnsYamlMapper_forYamlInputFormat() throws IOException {
        // GIVEN
        InputFormat format = InputFormat.YAML;
        // WHEN
        ObjectMapper mapper = factory.getObjectMapper(format);
        // THEN
        assertNotNull(mapper, "Mapper should not be null for YAML format");
        // The mapper returned by getObjectMapper must be the exact same instance
        // as the factory's internal YAML mapper.
        assertSame(factory.getYamlMapper(), mapper, "Factory should return its internal YAML ObjectMapper");
        // Verify that the mapper can actually parse a simple YAML document.
        String yaml = "name: Jane\nage: 25";
        JsonNode node = mapper.readTree(yaml);
        assertEquals("Jane", node.get("name").asText());
        assertEquals(25, node.get("age").asInt());
    }

    @Test
    void getObjectMapper_throwsException_forUnsupportedInputFormat() {
        // GIVEN
        // An input format that is not JSON nor YAML – we use null to simulate an
        // unsupported value (the method checks equality against the two constants).
        InputFormat unsupported = null;
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> factory.getObjectMapper(unsupported), "Expected IllegalArgumentException for null/unsupported InputFormat");
        assertTrue(ex.getMessage().contains("Unsupported input format"), "Exception message should mention unsupported format");
    }
}
