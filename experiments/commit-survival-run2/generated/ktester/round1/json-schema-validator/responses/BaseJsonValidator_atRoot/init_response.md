```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Set;

/**
 * Tests for the protected {@code atRoot()} method of {@link BaseJsonValidator}.
 *
 * <p>The method simply returns {@code pathType.getRoot()}. The {@code PathType}
 * instance is derived from the {@link ValidationContext} (or defaults to
 * {@link PathType#DEFAULT} when the context is {@code null}).  The tests verify
 * both the default behaviour and the case where a custom {@code PathType}
 * instance is supplied via a mocked {@link ValidationContext}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atRoot_Test {

    /** Minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static class DummyValidator extends BaseJsonValidator {
        DummyValidator(String schemaPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ValidatorTypeCode validatorType,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the atRoot() tests.
            return Collections.emptySet();
        }
    }

    @Mock
    private JsonSchema mockParentSchema;
    @Mock
    private ValidatorTypeCode mockValidatorType;
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private SchemaValidatorsConfig mockConfig;
    @Mock
    private PathType mockPathType;
    @Mock
    private JsonNode mockSchemaNode;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default stubbing for the custom‑path test.
        when(mockValidationContext.getConfig()).thenReturn(mockConfig);
        when(mockConfig.getPathType()).thenReturn(mockPathType);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between tests to avoid cross‑test contamination.
        Mockito.reset(mockParentSchema, mockValidatorType, mockValidationContext,
                     mockConfig, mockPathType, mockSchemaNode);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void atRoot_returnsDefaultRoot_whenValidationContextIsNull() {
        // Arrange – create the validator with a null ValidationContext so that
        // the constructor falls back to PathType.DEFAULT.
        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                null,                     // <-- null ValidationContext
                false);

        // Act
        String root = validator.atRoot();

        // Assert – PathType.DEFAULT's root is "$" in the library.
        assertEquals("$", root,
                "When ValidationContext is null the validator should use PathType.DEFAULT and return \"$\" as the root path.");
    }

    @Test
    void atRoot_returnsCustomRoot_whenPathTypeIsProvided() {
        // Arrange – stub the custom PathType to return a specific root string.
        when(mockPathType.getRoot()).thenReturn("customRoot");

        DummyValidator validator = new DummyValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,    // <-- non‑null context
                false);

        // Act
        String root = validator.atRoot();

        // Assert
        assertEquals("customRoot", root,
                "The atRoot() method should delegate to the PathType supplied via ValidationContext.");
    }
}
```