```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

import com.networknt.schema.i18n.DefaultMessageSource;

/**
 * Unit tests for the protected {@code atRoot()} method of {@link BaseJsonValidator}.
 *
 * <p>The method returns a {@link JsonNodePath} implementation that depends on the
 * {@link PathType} configured in the {@link ValidationContext}.  Because {@code atRoot}
 * is protected we expose it through a concrete subclass that provides a public
 * delegating method.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    /** Mocked validation context used by the concrete validator. */
    @Mock
    ValidationContext validationContext;

    /** Mocked configuration returned from the validation context. */
    @Mock
    SchemaValidatorsConfig config;

    /** Concrete implementation that delegates to the protected {@code atRoot()}. */
    private static class ConcreteValidator extends BaseJsonValidator {
        ConcreteValidator(ValidationContext vc) {
            // use the most detailed protected constructor – most arguments are not needed for this test
            super(
                false,                     // suppressSubSchemaRetrieval
                null,                      // schemaNode
                vc,                        // validationContext
                null,                      // errorMessageType
                null,                      // errorMessageKeyword
                DefaultMessageSource.getInstance(), // messageSource
                null,                      // keyword
                null,                      // parentSchema
                null,                      // schemaLocation
                null,                      // evaluationPath
                null,                      // evaluationParentSchema
                null                       // errorMessage map
            );
        }

        /** Public wrapper so the test can invoke the protected method. */
        JsonNodePath invokeAtRoot() {
            return atRoot();
        }
    }

    private ConcreteValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
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

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
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
        assertSame(JsonNodePathJsonPointer.getInstance(), result,
                "Expected the singleton JsonNodePathJsonPointer instance");
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
        assertSame(JsonNodePathLegacy.getInstance(), result,
                "Expected the singleton JsonNodePathLegacy instance");
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
        assertSame(JsonNodePathJsonPath.getInstance(), result,
                "Expected the singleton JsonNodePathJsonPath instance");
    }
}
```