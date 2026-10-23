```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tests for {@link BaseJsonValidator#getParentSchema()}.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    @Mock
    private JsonSchema mockParentSchema;          // injected parent schema

    @Mock
    private JsonNode mockSchemaNode;              // schema node (not used in these tests)

    @Mock
    private ValidationContext mockValidationContext; // validation context – can be a mock

    // any enum constant works for the validator type
    private final ValidatorTypeCode mockValidatorType = ValidatorTypeCode.TYPE;

    private TestValidator validatorWithParent;
    private TestValidator validatorWithoutParent;

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     */
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
        public Set<ValidationMessage> validate(ExecutionContext executionContext,
                                               JsonNode node,
                                               JsonNode rootNode,
                                               String at) {
            // No validation logic needed for the getParentSchema() tests.
            return Collections.emptySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The @Mock fields are already initialised by MockitoExtension.
        // Ensure the schema node is a non‑null instance (the validator does not use it).
        if (mockSchemaNode == null) {
            mockSchemaNode = NullNode.getInstance();
        }

        validatorWithParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        validatorWithoutParent = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                null,
                mockValidatorType,
                mockValidationContext,
                false);
    }

    /**
     * Provides two distinct {@link JsonSchema} mock instances for the
     * {@link #getParentSchema_preservesReferenceIntegrity(JsonSchema)} test.
     */
    private static Stream<JsonSchema> parentSchemaProvider() {
        JsonSchema first = Mockito.mock(JsonSchema.class);
        JsonSchema second = Mockito.mock(JsonSchema.class);
        return Stream.of(first, second);
    }

    @Test
    void getParentSchema_returnsProvidedParent() {
        JsonSchema returned = validatorWithParent.getParentSchema();
        Assertions.assertNotNull(returned, "Parent schema should not be null when supplied in constructor");
        Assertions.assertSame(mockParentSchema, returned,
                "Returned parent schema must be the exact instance that was injected");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema returned = validatorWithoutParent.getParentSchema();
        Assertions.assertNull(returned,
                "Parent schema should be null when none is supplied in constructor");
    }

    /**
     * Verifies that the validator returns the exact same {@link JsonSchema}
     * instance that was injected, regardless of any later mutations.
     */
    @ParameterizedTest(name = "{index}: parentSchema={0}")
    @MethodSource("parentSchemaProvider")
    void getParentSchema_preservesReferenceIntegrity(JsonSchema injectedParent) {
        TestValidator validator = new TestValidator(
                "schemaPath",
                mockSchemaNode,
                injectedParent,
                mockValidatorType,
                mockValidationContext,
                false);

        // No mutation is needed for mocks, but the test keeps the same
        // semantics as the original intention.
        JsonSchema returned = validator.getParentSchema();
        Assertions.assertSame(injectedParent, returned,
                "The validator must return the exact same JsonSchema instance that was injected");
    }
}
```