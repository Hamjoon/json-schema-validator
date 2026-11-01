package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import com.networknt.schema.PathType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        mocks = MockitoAnnotations.openMocks(this);
        // Wire the mock ValidationContext to return the mock config
        when(validationContext.getConfig()).thenReturn(config);
        // Initialise Mockito annotations and wire the mock ValidationContext to return the mock config
        mocks = MockitoAnnotations.openMocks(this);
        when(validationContext.getConfig()).thenReturn(config);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session
        mocks.close();
        mocks.close();
    }

    /**
     * Helper method that creates a minimal concrete {@link BaseJsonValidator}
     * instance. All abstract methods are stubbed with harmless defaults because
     * the {@code atRoot()} logic only depends on the {@link ValidationContext}.
     */
    private BaseJsonValidator createValidator() {
        return new BaseJsonValidator(/* schemaLocation */
        null, /* evaluationPath */
        null, /* schemaNode */
        null, /* parentSchema */
        null, /* validatorType */
        null, validationContext) {

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
        return Stream.of(Arguments.of(PathType.JSON_POINTER, JsonNodePathJsonPointer.getInstance(), "JSON‑Pointer singleton"), Arguments.of(PathType.LEGACY, JsonNodePathLegacy.getInstance(), "Legacy singleton"), Arguments.of(PathType.JSON_PATH, JsonNodePathJsonPath.getInstance(), "JSON‑Path singleton"));
    }

    @Test
    void atRoot_returnsJsonPointerPath_whenConfigIsJsonPointer() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        BaseJsonValidator validator = createValidator();
        // WHEN
        JsonNodePath result = validator.atRoot();
        // THEN
        assertSame(JsonNodePathJsonPointer.getInstance(), result, "Expected the JSON‑Pointer singleton instance");
    }

    @Test
    void atRoot_returnsLegacyPath_whenConfigIsLegacy() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.LEGACY);
        BaseJsonValidator validator = createValidator();
        // WHEN
        JsonNodePath result = validator.atRoot();
        // THEN
        assertSame(JsonNodePathLegacy.getInstance(), result, "Expected the LEGACY singleton instance");
    }

    @Test
    void atRoot_returnsJsonPath_whenConfigIsJsonPath() {
        // GIVEN
        when(config.getPathType()).thenReturn(PathType.JSON_PATH);
        BaseJsonValidator validator = createValidator();
        // WHEN
        JsonNodePath result = validator.atRoot();
        // THEN
        assertSame(JsonNodePathJsonPath.getInstance(), result, "Expected the JSON‑PATH singleton instance");
    }

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
        assertEquals(custom, result.getType(), "The JsonNodePath should be created with the custom PathType");
    }

    @ParameterizedTest(name = "atRoot() returns {2} when config returns {0}")
    @MethodSource("standardPathTypes")
    void atRoot_returnsSingleton_forStandardPathTypes(PathType type, JsonNodePath expectedInstance, String description) {
        // GIVEN
        when(config.getPathType()).thenReturn(type);
        BaseJsonValidator validator = createValidator(validationContext);
        // WHEN
        JsonNodePath result = validator.atRoot();
        // THEN
        assertSame(expectedInstance, result, () -> "Expected the " + description + " instance");
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
        assertThrows(NullPointerException.class, validator::atRoot, "Expected NullPointerException when config is null");
    }

    @Test
    void atRoot_throwsNPE_whenPathTypeIsNull() {
        // GIVEN: Config returns null PathType
        when(config.getPathType()).thenReturn(null);
        BaseJsonValidator validator = createValidator(validationContext);
        // WHEN / THEN
        assertThrows(NullPointerException.class, validator::atRoot, "Expected NullPointerException when PathType is null");
    }

    @Test
    void atRoot_throwsNPE_whenValidationContextIsNull() {
        // GIVEN: validator constructed with a null ValidationContext
        BaseJsonValidator validator = createValidator(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, validator::atRoot, "Expected NullPointerException when ValidationContext is null");
    }
}
