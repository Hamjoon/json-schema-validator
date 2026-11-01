```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.i18n.DefaultMessageSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    /** Minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
    private static final class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode,
                  parentSchema, validatorType, validationContext);
        }

        /** The {@link JsonValidator} contract requires a {@code validate} method. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the getSchemaNode test.
            return Collections.emptySet();
        }
    }

    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private JsonSchema mockParentSchema;
    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private ValidatorTypeCode mockValidatorType;

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create the simplest possible supporting objects.
        SchemaLocation schemaLocation = new SchemaLocation(null); // fragment defaults to JSON_POINTER
        JsonNodePath evaluationPath = new JsonNodePath(PathType.JSON); // PathType is an enum inside the library

        // Instantiate the concrete validator under test.
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the current test scenario.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getSchemaNode_returnsTheNodeProvidedInConstructor() {
        JsonNode returned = validator.getSchemaNode();
        assertSame(mockSchemaNode, returned,
                "BaseJsonValidator#getSchemaNode should return the exact JsonNode instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_isNotNullWhenConstructorArgumentIsNonNull() {
        assertNotNull(validator.getSchemaNode(),
                "BaseJsonValidator#getSchemaNode must not return null when a non‑null schema node was given");
    }
}
```