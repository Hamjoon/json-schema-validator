```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    /** A minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static final class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the getParentSchema() tests.
            return java.util.Collections.emptySet();
        }
    }

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    private TestValidator validatorWithParent;
    private TestValidator validatorWithoutParent;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create an instance where a parent schema is supplied.
        validatorWithParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Create an instance where the parent schema is null.
        validatorWithoutParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getParentSchema_returnsProvidedParent() {
        JsonSchema returned = validatorWithParent.getParentSchema();
        assertNotNull(returned, "Parent schema should not be null when supplied in constructor");
        assertSame(mockParentSchema, returned, "Returned parent schema must be the same instance supplied");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema returned = validatorWithoutParent.getParentSchema();
        assertNull(returned, "Parent schema should be null when none is supplied in constructor");
    }
}
```