```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    /** A minimal concrete implementation of the abstract BaseJsonValidator just for testing. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the getter test.
            return Collections.emptySet();
        }
    }

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    private ObjectMapper mapper;
    private JsonNode sampleSchemaNode;
    private TestValidator validatorWithNode;
    private TestValidator validatorWithNullNode;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        mapper = new ObjectMapper();
        // Create a simple JSON node to act as a schema fragment.
        sampleSchemaNode = mapper.readTree("{\"type\":\"string\"}");

        // Construct the validator using a non‑null schema node.
        validatorWithNode = new TestValidator(
                "testSchemaPath",
                sampleSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Construct another validator where the schema node is null.
        validatorWithNullNode = new TestValidator(
                "testSchemaPathNull",
                null,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for these simple unit tests.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    @Test
    void getSchemaNode_returnsNodeProvidedInConstructor() {
        JsonNode returned = validatorWithNode.getSchemaNode();
        assertNotNull(returned, "The schema node should not be null");
        assertSame(sampleSchemaNode, returned, "The returned node must be the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_returnsNullWhenConstructedWithNull() {
        assertNull(validatorWithNullNode.getSchemaNode(),
                "When the validator is constructed with a null schema node, getSchemaNode() should return null");
    }
}
```