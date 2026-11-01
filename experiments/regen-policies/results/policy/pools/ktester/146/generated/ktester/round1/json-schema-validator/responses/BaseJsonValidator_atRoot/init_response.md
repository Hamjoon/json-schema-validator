```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;

import com.networknt.schema.PathType;

/**
 * Unit tests for the protected {@code atRoot()} method of {@link BaseJsonValidator}.
 * The method selects the appropriate {@link JsonNodePath} implementation based on the
 * {@link PathType} configured in the {@link ValidationContext}.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    private AutoCloseable mocks;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        mocks = MockitoAnnotations.openMocks(this);
        // Wire the mock ValidationContext to return the mock config
        when(validationContext.getConfig()).thenReturn(config);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session
        mocks.close();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * Helper method that creates a minimal concrete {@link BaseJsonValidator}
     * instance. All abstract methods are stubbed with harmless defaults because
     * the {@code atRoot()} logic only depends on the {@link ValidationContext}.
     */
    private BaseJsonValidator createValidator() {
        return new BaseJsonValidator(
                /* schemaLocation */ null,
                /* evaluationPath */ null,
                /* schemaNode */ null,
                /* parentSchema */ null,
                /* validatorType */ null,
                validationContext) {

            @Override
            public String getKeyword() {
                return null;
            }

            @Override
            public JsonNodePath getEvaluationPath() {
                return null;
            }

            @Override
            public SchemaLocation getSchemaLocation() {
                return null;
            }

            @Override
            public JsonSchema getParentSchema() {
                return null;
            }
        };
    }

    @Test
    void atRoot_returnsJsonPointerPath_whenConfigIsJsonPointer() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        BaseJsonValidator validator = createValidator();

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertSame(JsonNodePathJsonPointer.getInstance(), result,
                "Expected the JSON‑Pointer singleton instance");
    }

    @Test
    void atRoot_returnsLegacyPath_whenConfigIsLegacy() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.LEGACY);
        BaseJsonValidator validator = createValidator();

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertSame(JsonNodePathLegacy.getInstance(), result,
                "Expected the LEGACY singleton instance");
    }

    @Test
    void atRoot_returnsJsonPath_whenConfigIsJsonPath() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.JSON_PATH);
        BaseJsonValidator validator = createValidator();

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertSame(JsonNodePathJsonPath.getInstance(), result,
                "Expected the JSON‑PATH singleton instance");
    }

    @Test
    void atRoot_returnsNewPath_whenConfigIsCustom() {
        // GIVEN
        PathType custom = PathType.CUSTOM; // assume a non‑standard enum value exists
        when(config.getPathType()).thenReturn(custom);
        BaseJsonValidator validator = createValidator();

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertNotNull(result, "Result should not be null for a custom PathType");
        // The returned instance should not be one of the known singletons
        assertNotSame(JsonNodePathJsonPointer.getInstance(), result);
        assertNotSame(JsonNodePathLegacy.getInstance(), result);
        assertNotSame(JsonNodePathJsonPath.getInstance(), result);
        // Verify that the internal type matches the custom value
        assertEquals(custom, result.getType(),
                "The JsonNodePath should be created with the custom PathType");
    }
}
```