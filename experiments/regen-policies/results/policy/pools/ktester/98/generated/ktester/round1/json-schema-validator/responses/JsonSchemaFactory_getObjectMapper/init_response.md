```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getObjectMapper(InputFormat)}.
 * The test class resides in the same package as the class under test,
 * therefore the package‑private method can be accessed directly.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getObjectMapper_Test {

    /** Factory instance used in all tests. */
    private JsonSchemaFactory factory;

    /** Runs once before all tests – currently no global setup required. */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    /** Initialise a fresh factory before each test. */
    @BeforeEach
    void setupBeforeEach() {
        // Use the public factory method; it creates a fully‑initialised instance
        // with default JSON and YAML mappers.
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        assertNotNull(factory, "Factory must be created");
    }

    /** Clean up after each test – nothing to release at the moment. */
    @AfterEach
    void teardownAfterEach() {
        // No per‑test resources to clean up.
    }

    /** Runs once after all tests – currently no global teardown required. */
    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /** Verify that the JSON mapper is returned for the JSON input format. */
    @Test
    void getObjectMapper_returnsJsonMapper_forJsonFormat() {
        // given
        InputFormat jsonFormat = InputFormat.JSON;

        // when
        ObjectMapper mapper = factory.getObjectMapper(jsonFormat);

        // then
        assertNotNull(mapper, "Mapper must not be null");
        // The factory also exposes the concrete JSON mapper via a package‑private method.
        // Since we are in the same package we can compare references.
        assertSame(factory.getJsonMapper(), mapper,
                "Returned mapper should be the same instance as the factory's JSON mapper");
    }

    /** Verify that the YAML mapper is returned for the YAML input format. */
    @Test
    void getObjectMapper_returnsYamlMapper_forYamlFormat() {
        // given
        InputFormat yamlFormat = InputFormat.YAML;

        // when
        ObjectMapper mapper = factory.getObjectMapper(yamlFormat);

        // then
        assertNotNull(mapper, "Mapper must not be null");
        assertSame(factory.getYamlMapper(), mapper,
                "Returned mapper should be the same instance as the factory's YAML mapper");
    }

    /** Verify that an unsupported (null) format results in an IllegalArgumentException. */
    @Test
    void getObjectMapper_throwsException_forUnsupportedFormat() {
        // given
        InputFormat unsupported = null;

        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getObjectMapper(unsupported));
        assertTrue(ex.getMessage().contains("Unsupported input format"),
                "Exception message should mention unsupported format");
    }
}
```