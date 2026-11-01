```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockitoAnnotations;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the protected {@code atRoot()} method of {@link BaseJsonValidator}.
 *
 * <p>The method selects a {@link JsonNodePath} implementation based on the
 * {@link PathType} configured in the {@link ValidationContext}. The tests cover:
 *
 * <ul>
 *   <li>Standard {@link PathType} values (JSON_POINTER, LEGACY, JSON_PATH) – expecting the
 *       corresponding singleton {@link JsonNodePath} instances.</li>
 *   <li>A non‑standard/custom {@link PathType} – expecting a newly created {@link JsonNodePath}
 *       that is not one of the known singletons.</li>
 *   <li>Null handling – when the {@link ValidationContext}, its {@link SchemaValidatorsConfig},
 *       or the {@link PathType} itself is {@code null}.</li>
 * </ul>
 *
 * The concrete {@link BaseJsonValidator} is created via an anonymous subclass that
 * provides harmless implementations for the abstract methods – they are irrelevant
 * for the {@code atRoot()} logic.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    private AutoCloseable mocks;

    @BeforeEach
    void setup() {
        // Initialise Mockito annotations and wire the mock ValidationContext to return the mock config
        mocks = MockitoAnnotations.openMocks(this);
        when(validationContext.getConfig()).thenReturn(config);
    }

    @AfterEach
    void teardown() throws Exception {
        mocks.close();
    }

    /**
     * Creates a minimal concrete {@link BaseJsonValidator} instance.
     *
     * @param ctx the {@link ValidationContext} to be used by the validator
     * @return a concrete validator ready for testing
     */
    private BaseJsonValidator createValidator(ValidationContext ctx) {
        return new BaseJsonValidator(
                /* schemaLocation */ null,
                /* evaluationPath */ null,
                /* schemaNode */ null,
                /* parentSchema */ null,
                /* validatorType */ null,
                ctx) {

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

    /* -------------------------------------------------------------
     *  Parameterized tests for the three standard PathType values
     * ------------------------------------------------------------- */
    private static Stream<Arguments> standardPathTypes() {
        return Stream.of(
                Arguments.of(PathType.JSON_POINTER, JsonNodePathJsonPointer.getInstance(),
                        "JSON‑Pointer singleton"),
                Arguments.of(PathType.LEGACY, JsonNodePathLegacy.getInstance(),
                        "Legacy singleton"),
                Arguments.of(PathType.JSON_PATH, JsonNodePathJsonPath.getInstance(),
                        "JSON‑Path singleton")
        );
    }

    @ParameterizedTest(name = "atRoot() returns {2} when config returns {0}")
    @MethodSource("standardPathTypes")
    void atRoot_returnsSingleton_forStandardPathTypes(PathType type,
                                                      JsonNodePath expectedInstance,
                                                      String description) {
        // GIVEN
        when(config.getPathType()).thenReturn(type);
        BaseJsonValidator validator = createValidator(validationContext);

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertSame(expectedInstance, result,
                () -> "Expected the " + description + " instance");
    }

    /* -------------------------------------------------------------
     *  Custom / non‑standard PathType handling
     * ------------------------------------------------------------- */
    @Test
    void atRoot_returnsNewPath_whenConfigIsCustom() {
        // GIVEN: a mock PathType that does NOT equal any of the known constants
        PathType custom = mock(PathType.class);
        when(config.getPathType()).thenReturn(custom);
        BaseJsonValidator validator = createValidator(validationContext);

        // WHEN
        JsonNodePath result = validator.atRoot();

        // THEN
        assertNotNull(result, "Result should not be null for a custom PathType");
        // It must not be one of the predefined singletons
        assertNotSame(JsonNodePathJsonPointer.getInstance(), result);
        assertNotSame(JsonNodePathLegacy.getInstance(), result);
        assertNotSame(JsonNodePathJsonPath.getInstance(), result);
        // The internal type stored in the new JsonNodePath should be the mock we supplied
        assertEquals(custom, result.getType(),
                "The JsonNodePath should be created with the custom PathType");
    }

    /* -------------------------------------------------------------
     *  Null handling tests
     * ------------------------------------------------------------- */
    @Test
    void atRoot_throwsNPE_whenConfigIsNull() {
        // GIVEN: ValidationContext returns a null config
        when(validationContext.getConfig()).thenReturn(null);
        BaseJsonValidator validator = createValidator(validationContext);

        // WHEN / THEN
        assertThrows(NullPointerException.class, validator::atRoot,
                "Expected NullPointerException when config is null");
    }

    @Test
    void atRoot_throwsNPE_whenPathTypeIsNull() {
        // GIVEN: Config returns null PathType
        when(config.getPathType()).thenReturn(null);
        BaseJsonValidator validator = createValidator(validationContext);

        // WHEN / THEN
        assertThrows(NullPointerException.class, validator::atRoot,
                "Expected NullPointerException when PathType is null");
    }

    @Test
    void atRoot_throwsNPE_whenValidationContextIsNull() {
        // GIVEN: validator constructed with a null ValidationContext
        BaseJsonValidator validator = createValidator(null);

        // WHEN / THEN
        assertThrows(NullPointerException.class, validator::atRoot,
                "Expected NullPointerException when ValidationContext is null");
    }
}
```