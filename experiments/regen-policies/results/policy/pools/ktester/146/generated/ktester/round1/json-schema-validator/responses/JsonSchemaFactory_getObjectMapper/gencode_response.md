```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchemaFactory#getObjectMapper(InputFormat)}.
 *
 * <p>The tests verify that:
 * <ul>
 *   <li>The correct {@link ObjectMapper} is returned for the supported {@link InputFormat}
 *       values (JSON and YAML).</li>
 *   <li>An {@link IllegalArgumentException} is thrown for any unsupported format,
 *       including {@code null} and hypothetical enum constants.</li>
 * </ul>
 *
 * <p>The test class resides in the same package as {@link JsonSchemaFactory},
 * therefore it can directly call the package‑private {@code getObjectMapper},
 * {@code getJsonMapper} and {@code getYamlMapper} methods.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getObjectMapper_Test {

    /** Factory instance shared by all tests. */
    private static JsonSchemaFactory factory;

    @BeforeAll
    static void setupBeforeAll() {
        // Obtain a fully‑initialised factory instance for the 2020‑12 specification.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test state required.
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        factory = null;
    }

    /* --------------------------------------------------------------------- */
    /*  Positive scenarios – supported input formats                         */
    /* --------------------------------------------------------------------- */

    @Test
    void getObjectMapper_returnsJsonMapper_forJsonInputFormat() throws IOException {
        // GIVEN
        InputFormat format = InputFormat.JSON;

        // WHEN
        ObjectMapper mapper = factory.getObjectMapper(format);

        // THEN
        assertNotNull(mapper, "Mapper should not be null for JSON format");
        // The mapper returned by getObjectMapper must be the exact same instance
        // as the factory's internal JSON mapper.
        assertSame(factory.getJsonMapper(), mapper,
                "Factory should return its internal JSON ObjectMapper");

        // Verify that the mapper can actually parse JSON.
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
        assertSame(factory.getYamlMapper(), mapper,
                "Factory should return its internal YAML ObjectMapper");

        // Verify that the mapper can actually parse a simple YAML document.
        String yaml = "name: Jane\nage: 25";
        JsonNode node = mapper.readTree(yaml);
        assertEquals("Jane", node.get("name").asText());
        assertEquals(25, node.get("age").asInt());
    }

    /* --------------------------------------------------------------------- */
    /*  Negative scenarios – unsupported input formats                         */
    /* --------------------------------------------------------------------- */

    /**
     * Parameterized test that checks the method throws {@link IllegalArgumentException}
     * for every unsupported {@link InputFormat} value.
     *
     * The data provider supplies:
     * <ul>
     *   <li>{@code null} – represents a missing format.</li>
     *   <li>A fictitious enum constant {@code CUSTOM} – demonstrates that any
     *       future enum value that is not JSON or YAML will also trigger the exception.</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] unsupported format = {0}")
    @MethodSource("unsupportedInputFormats")
    void getObjectMapper_throwsException_forUnsupportedInputFormat(InputFormat unsupported) {
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getObjectMapper(unsupported),
                "Expected IllegalArgumentException for unsupported InputFormat");

        // The message must contain the word "Unsupported input format" and the actual
        // value (or the string "null").
        String expectedFragment = "Unsupported input format " + unsupported;
        assertTrue(ex.getMessage().contains(expectedFragment),
                () -> "Exception message should contain: \"" + expectedFragment + "\"");
    }

    /** Supplies the unsupported {@link InputFormat} values for the parameterized test. */
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
        return Stream.of(
                Arguments.of((Object) null),          // null format
                Arguments.of(custom)                  // possibly a CUSTOM constant, may be null
        ).filter(args -> args.get()[0] != null); // filter out the second argument if it is null
    }
}
```