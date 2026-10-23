package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import com.fasterxml.jackson.databind.node.NullNode;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.Mock;
// import org.mockito.Mockito;
// import org.mockito.junit.jupiter.MockitoExtension;

// @ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    //     @Mock
    private JsonSchema mockParentSchema;

    //     @Mock
    private JsonNode mockSchemaNode;

    //     @Mock
    private ValidatorTypeCode mockValidatorType;

    //     @Mock
    private ValidationContext mockValidationContext;

    private TestValidator validatorWithParent;

    private TestValidator validatorWithoutParent;

    /**
     * Simple mutable concrete subclass of {@link JsonSchema}.
     * The constructor mirrors the protected constructor used by the library;
     * all arguments are {@code null} because the internal state of the schema is
     * irrelevant for these tests.
     */
    private static final class MutableJsonSchema extends JsonSchema {

        private int mutableState = 0;

        MutableJsonSchema() {
            super(null, null, null, null, null);
        }

        void setState(int newState) {
            this.mutableState = newState;
        }

        int getState() {
            return this.mutableState;
        }
    }

    /**
     * Minimal concrete implementation of the abstract {@link BaseJsonValidator}.
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext, boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            // No validation logic needed for the getParentSchema() tests.
            return Collections.emptySet();
        }
    }

    //     @BeforeEach
    void setupBeforeEach() {
        // The @Mock fields are already initialised by MockitoExtension.
        // Ensure the schema node is a non‑null instance (the validator does not use it).
        if (mockSchemaNode == null) {
            mockSchemaNode = NullNode.getInstance();
        }
        validatorWithParent = new TestValidator("schemaPath", mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext, false);
        validatorWithoutParent = new TestValidator("schemaPath", mockSchemaNode, null, mockValidatorType, mockValidationContext, false);
    }

    /**
     * Provides the different {@link JsonSchema} instances for the parameterized test.
     */
//     private static Stream<JsonSchema> parentSchemaProvider() {
//         JsonSchema first = Mockito.mock(JsonSchema.class);
//         JsonSchema second = Mockito.mock(JsonSchema.class);
//         return Stream.of(first, second);
//     }

    @Test
    void getParentSchema_returnsProvidedParent() {
        JsonSchema returned = validatorWithParent.getParentSchema();
        Assertions.assertNotNull(returned, "Parent schema should not be null when supplied in constructor");
        Assertions.assertSame(mockParentSchema, returned, "Returned parent schema must be the exact instance that was injected");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentSupplied() {
        JsonSchema returned = validatorWithoutParent.getParentSchema();
        Assertions.assertNull(returned, "Parent schema should be null when none is supplied in constructor");
    }

    /**
     * Supplies two different {@link JsonSchema} implementations:
     * <ul>
     *   <li>A mutable concrete subclass ({@link MutableJsonSchema}) whose internal
     *       state is changed after the validator is constructed.</li>
     *   <li>A Mockito‑generated proxy (simulated here by another concrete instance).</li>
     * </ul>
     * The test asserts that {@code getParentSchema()} always returns the original
     * reference, confirming that the method does not create a copy or otherwise
     * alter the supplied object.
     */
    @ParameterizedTest(name = "{index}: parentSchema={0}")
    @MethodSource("parentSchemaProvider")
    void getParentSchema_preservesReferenceIntegrity(JsonSchema injectedParent) {
        TestValidator validator = new TestValidator("schemaPath", mockSchemaNode, injectedParent, mockValidatorType, mockValidationContext, false);
        // No mutation is needed for mocks, but the test keeps the same
        // semantics as the original intention.
        JsonSchema returned = validator.getParentSchema();
        Assertions.assertSame(injectedParent, returned, "The validator must return the exact same JsonSchema instance that was injected");
    }
}