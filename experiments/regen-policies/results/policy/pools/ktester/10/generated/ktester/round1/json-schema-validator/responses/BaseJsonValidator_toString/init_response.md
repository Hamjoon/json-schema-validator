```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_toString_Test {

    /** Mocks for the constructor parameters */
    @Mock SchemaLocation mockSchemaLocation;
    @Mock JsonNodePath mockEvaluationPath;
    @Mock JsonNode mockSchemaNode;
    @Mock JsonSchema mockParentSchema;
    @Mock ValidatorTypeCode mockValidatorType;
    @Mock ValidationContext mockValidationContext;

    /** The concrete instance of the abstract BaseJsonValidator we are testing */
    BaseJsonValidator validatorUnderTest;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this test class
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stub the evaluation path to return a known name for the last segment.
        // The toString() implementation uses getEvaluationPath().getName(-1)
        when(mockEvaluationPath.getName(-1)).thenReturn("expectedPath");

        // Create a minimal concrete subclass of BaseJsonValidator.
        // Only the abstract methods of JsonValidator need to be supplied.
        validatorUnderTest = new BaseJsonValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext) {

            // JsonValidator requires a validate method – we provide a dummy implementation.
            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
                return Set.of(); // no validation performed
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format) {
                return null;
            }

            @Override
            public <T> T validate(ExecutionContext executionContext, JsonNode node, OutputFormat<T> format,
                                 ExecutionContextCustomizer executionCustomizer) {
                return null;
            }

            // getKeyword is abstract in ValidationMessageHandler; return a dummy value.
            @Override
            public String getKeyword() {
                return "dummyKeyword";
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference.
        clearInvocations(mockEvaluationPath, mockSchemaLocation, mockSchemaNode,
                mockParentSchema, mockValidatorType, mockValidationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void toString_returnsLastSegmentOfEvaluationPath() {
        // GIVEN the evaluation path is stubbed to return "expectedPath" for getName(-1)
        // WHEN toString() is invoked
        String result = validatorUnderTest.toString();
        // THEN the result should be the stubbed name
        assertEquals("expectedPath", result);
        // Verify that getName(-1) was indeed called.
        verify(mockEvaluationPath, times(1)).getName(-1);
    }
}
```