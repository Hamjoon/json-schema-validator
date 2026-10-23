```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * The only deterministic behaviour that can be verified without relying on the
 * private static {@code obtainSubSchemaNode} implementation is the handling of
 * the {@code suppressSubSchemaRetrieval} flag. When this flag is {@code true}
 * the method must return {@code null}; otherwise it delegates to the private
 * helper.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** A minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema,
                  validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests.
            return Collections.emptySet();
        }
    }

    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private JsonSchema mockParentSchema;
    @Mock
    private ValidatorTypeCode mockValidatorType;
    @Mock
    private ValidationContext mockValidationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are initialised by the extension.
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown needed.
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed() {
        // Arrange: create a validator with suppressSubSchemaRetrieval = true
        TestValidator validator = new TestValidator(
                "dummyPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                true   // suppressSubSchemaRetrieval
        );

        // Act
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);

        // Assert
        assertNull(result, "When sub‑schema retrieval is suppressed the method must return null");
    }

    @Test
    void fetchSubSchemaNode_delegatesWhenNotSuppressed() {
        // Arrange: validator with suppressSubSchemaRetrieval = false.
        // The private static method will be exercised; we only verify that
        // the result is *not* null for a well‑formed (mocked) schema node.
        TestValidator validator = new TestValidator(
                "dummyPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false   // do NOT suppress
        );

        // The private method creates a new JsonSchema instance; to keep the test
        // lightweight we let it run with the mocked arguments. The constructor
        // of JsonSchema expects a non‑null ValidationContext, so we stub the
        // minimal behaviour needed.
        when(mockValidationContext.getConfig()).thenReturn(null);
        when(mockValidationContext.getMetaSchema()).thenReturn(null);
        when(mockValidationContext.getJsonSchemaFactory()).thenReturn(null);
        when(mockValidationContext.getUriFactory()).thenReturn(null);
        when(mockValidationContext.getUrnFactory()).thenReturn(null);

        // Act
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);

        // Assert
        assertNotNull(result, "When sub‑schema retrieval is not suppressed a JsonSchema should be returned");
    }
}
```