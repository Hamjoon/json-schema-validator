package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.i18n.DefaultMessageSource;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    /**
     * Mocked validation context used by the concrete validator.
     */
    @Mock
    ValidationContext validationContext;

    /**
     * Mocked configuration returned from the validation context.
     */
    @Mock
    SchemaValidatorsConfig config;

    private ConcreteValidator validator;

    /**
     * Concrete implementation that delegates to the protected {@code atRoot()}.
     */
    private static class ConcreteValidator extends BaseJsonValidator {

        ConcreteValidator(ValidationContext vc) {
            // The super‑constructor has many parameters; for these tests only the three
            // arguments below are relevant.
            super(// suppressSubSchemaRetrieval
            false, // schemaNode
            null, // validationContext
            vc, // errorMessageType
            null, // errorMessageKeyword
            null, // messageSource
            DefaultMessageSource.getInstance(), // keyword
            null, // parentSchema
            null, // schemaLocation
            null, // evaluationPath
            null, // evaluationParentSchema
            null, // errorMessage map
            null);
        }

        /**
         * Public wrapper so the test can invoke the protected method.
         */
        JsonNodePath invokeAtRoot() {
            return atRoot();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Wire the mocks: ValidationContext#getConfig() returns the mocked config
        when(validationContext.getConfig()).thenReturn(config);
        // Create a fresh validator instance for every test
        validator = new ConcreteValidator(validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the interactions to avoid cross‑test contamination
        reset(validationContext, config);
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPointer when PathType is JSON_POINTER")
    void atRootReturnsJsonPointer() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPointer.getInstance(), result, "Expected the singleton JsonNodePathJsonPointer instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathLegacy when PathType is LEGACY")
    void atRootReturnsLegacy() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.LEGACY);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathLegacy.getInstance(), result, "Expected the singleton JsonNodePathLegacy instance");
    }

    @Test
    @DisplayName("atRoot returns JsonNodePathJsonPath when PathType is JSON_PATH")
    void atRootReturnsJsonPath() {
        // arrange
        when(config.getPathType()).thenReturn(PathType.JSON_PATH);
        // act
        JsonNodePath result = validator.invokeAtRoot();
        // assert
        assertNotNull(result, "Result must not be null");
        assertSame(JsonNodePathJsonPath.getInstance(), result, "Expected the singleton JsonNodePathJsonPath instance");
    }

    @Test
    @DisplayName("atRoot creates a new JsonNodePath for a custom PathType")
    void atRootCreatesNewInstanceForCustomPathType() {
        // arrange – assume the library defines a CUSTOM constant
        when(config.getPathType()).thenReturn(PathType.CUSTOM);
        // act – first call
        JsonNodePath first = validator.invokeAtRoot();
        // act – second call
        JsonNodePath second = validator.invokeAtRoot();
        // assert – two distinct objects (no singleton reuse)
        assertNotNull(first, "First result must not be null");
        assertNotNull(second, "Second result must not be null");
        assertNotSame(first, second, "Two successive calls with a custom PathType should return different instances");
        // assert – the PathType stored inside the returned JsonNodePath matches the config
        assertEquals(PathType.CUSTOM, first.getPathType(), "The JsonNodePath should carry the custom PathType supplied by the config");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when validator's validationContext is null")
    void atRootThrowsWhenValidationContextIsNull() {
        // Build a validator with a null ValidationContext
        ConcreteValidator nullContextValidator = new ConcreteValidator(null);
        // act & assert
        assertThrows(NullPointerException.class, nullContextValidator::invokeAtRoot, "Calling atRoot() with a null validationContext should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when ValidationContext#getConfig() returns null")
    void atRootThrowsWhenConfigIsNull() {
        // arrange – make getConfig() return null
        when(validationContext.getConfig()).thenReturn(null);
        // act & assert
        assertThrows(NullPointerException.class, validator::invokeAtRoot, "Calling atRoot() when config is null should throw NPE");
    }

    @Test
    @DisplayName("atRoot throws NullPointerException when Config#getPathType() returns null")
    void atRootThrowsWhenPathTypeIsNull() {
        // arrange – config returns null for getPathType()
        when(config.getPathType()).thenReturn(null);
        // act & assert
        assertThrows(NullPointerException.class, validator::invokeAtRoot, "Calling atRoot() when PathType is null should throw NPE");
    }
}
