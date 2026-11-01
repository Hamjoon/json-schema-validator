```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;

/**
 * Unit tests for {@link JsonSchemaFactory#getObjectMapper(InputFormat)}.
 * The method is package‑private, therefore the test class lives in the same
 * package and can invoke it directly.
 *
 * The test suite covers:
 * <ul>
 *   <li>Normal delegation to the JSON and YAML mappers.</li>
 *   <li>Exact exception messages for unsupported formats (null and a mock).</li>
 *   <li>Behaviour when a subclass overrides the mapper methods.</li>
 *   <li>Propagation of runtime exceptions thrown by overridden mapper methods.</li>
 *   <li>Verification that JSON and YAML mappers are distinct instances.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getObjectMapper_Test {

    /** Factory instance used in the default‑behaviour tests. */
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

    /* -------------------------------------------------------------
     *  Normal delegation tests (already present in the skeleton)
     * ------------------------------------------------------------- */

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

    /** Verify that a null format results in an IllegalArgumentException with the exact message. */
    @Test
    void getObjectMapper_throwsException_forNullFormat_exactMessage() {
        // given
        InputFormat unsupported = null;

        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getObjectMapper(unsupported));
        assertEquals("Unsupported input format null", ex.getMessage(),
                "Exception message must match exactly for null input");
    }

    /* -------------------------------------------------------------
     *  Unsupported format tests (using a Mockito mock)
     * ------------------------------------------------------------- */

    /** Verify that a non‑JSON/YAML InputFormat (mocked) triggers the default branch. */
    @Test
    void getObjectMapper_throwsException_forUnsupportedMockFormat_exactMessage() {
        // given: a mock that pretends to be an enum constant named "UNSUPPORTED"
        InputFormat unsupported = Mockito.mock(InputFormat.class);
        Mockito.when(unsupported.toString()).thenReturn("UNSUPPORTED");
        // equals with JSON/YAML will be false because the mock is not the same instance
        // (no need to stub equals – default mock behavior returns false for any argument)

        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> factory.getObjectMapper(unsupported));
        assertEquals("Unsupported input format UNSUPPORTED", ex.getMessage(),
                "Exception message must match exactly for unsupported format");
    }

    /* -------------------------------------------------------------
     *  Distinct mapper instances test
     * ------------------------------------------------------------- */

    /** Ensure that JSON and YAML mappers are different objects. */
    @Test
    void getObjectMapper_distinctInstances_betweenJsonAndYaml() {
        ObjectMapper jsonMapper = factory.getObjectMapper(InputFormat.JSON);
        ObjectMapper yamlMapper = factory.getObjectMapper(InputFormat.YAML);
        assertNotSame(jsonMapper, yamlMapper,
                "JSON and YAML ObjectMapper instances must be distinct");
    }

    /* -------------------------------------------------------------
     *  Overridden mapper delegation tests
     * ------------------------------------------------------------- */

    /** Subclass that overrides getJsonMapper() – verify delegation returns the overridden instance. */
    @Test
    void getObjectMapper_delegatesToOverriddenJsonMapper() {
        // given
        CustomJsonSchemaFactory customFactory = new CustomJsonSchemaFactory();

        // when
        ObjectMapper mapper = customFactory.getObjectMapper(InputFormat.JSON);

        // then
        assertSame(customFactory.customJsonMapper, mapper,
                "Should return the ObjectMapper supplied by the overridden getJsonMapper()");
    }

    /** Subclass that overrides getYamlMapper() – verify delegation returns the overridden instance. */
    @Test
    void getObjectMapper_delegatesToOverriddenYamlMapper() {
        // given
        CustomJsonSchemaFactory customFactory = new CustomJsonSchemaFactory();

        // when
        ObjectMapper mapper = customFactory.getObjectMapper(InputFormat.YAML);

        // then
        assertSame(customFactory.customYamlMapper, mapper,
                "Should return the ObjectMapper supplied by the overridden getYamlMapper()");
    }

    /** Subclass where getJsonMapper() throws – verify exception is propagated unchanged. */
    @Test
    void getObjectMapper_propagatesException_fromJsonMapper() {
        // given
        ExceptionThrowingFactory exceptionFactory = new ExceptionThrowingFactory();

        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> exceptionFactory.getObjectMapper(InputFormat.JSON));
        assertEquals("json mapper failure", ex.getMessage(),
                "Exception message must be propagated unchanged");
    }

    /** Subclass where getYamlMapper() throws – verify exception is propagated unchanged. */
    @Test
    void getObjectMapper_propagatesException_fromYamlMapper() {
        // given
        ExceptionThrowingFactory exceptionFactory = new ExceptionThrowingFactory();

        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> exceptionFactory.getObjectMapper(InputFormat.YAML));
        assertEquals("yaml mapper failure", ex.getMessage(),
                "Exception message must be propagated unchanged");
    }

    /* -------------------------------------------------------------
     *  Helper subclasses used in the overridden‑mapper tests
     * ------------------------------------------------------------- */

    /**
     * A concrete subclass that overrides the mapper creation methods to return
     * identifiable {@link ObjectMapper} instances.
     */
    private static class CustomJsonSchemaFactory extends JsonSchemaFactory {
        final ObjectMapper customJsonMapper = new ObjectMapper();
        final ObjectMapper customYamlMapper = new ObjectMapper();

        @Override
        ObjectMapper getJsonMapper() {
            return customJsonMapper;
        }

        @Override
        ObjectMapper getYamlMapper() {
            return customYamlMapper;
        }
    }

    /**
     * A subclass that deliberately throws a {@link RuntimeException} from the
     * mapper methods to test exception propagation.
     */
    private static class ExceptionThrowingFactory extends JsonSchemaFactory {
        @Override
        ObjectMapper getJsonMapper() {
            throw new RuntimeException("json mapper failure");
        }

        @Override
        ObjectMapper getYamlMapper() {
            throw new RuntimeException("yaml mapper failure");
        }
    }
}
```