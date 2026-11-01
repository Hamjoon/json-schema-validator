```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * The method is {@code protected}, therefore a concrete subclass is created
 * that simply exposes the method for the test suite.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** The validator under test – a concrete subclass that makes the method public. */
    private TestValidator validator;

    /** Mocked validation context – used to stub the creation of a sub‑schema. */
    @Mock
    private ValidationContext validationContext;

    /** A dummy schema node – the actual content is irrelevant for the current tests. */
    @Mock
    private JsonNode schemaNode;

    /** A dummy parent schema – not used by the method under test. */
    @Mock
    private JsonSchema parentSchema;

    /** Dummy objects required by the BaseJsonValidator constructor. */
    @Mock
    private SchemaLocation schemaLocation;
    @Mock
    private JsonNodePath evaluationPath;
    @Mock
    private ValidatorTypeCode validatorType;

    /** A mock sub‑schema that will be returned by the stubbed ValidationContext. */
    @Mock
    private JsonSchema subSchema;

    /** Initialise Mockito annotations before each test. */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /** Clean up after each test – not strictly required but kept for symmetry. */
    @AfterEach
    void teardownAfterEach() {
        // No resources to release
    }

    /** Test that when {@code suppressSubSchemaRetrieval} is {@code true} the method returns {@code null}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressed() {
        // given – a validator with suppress flag set to true
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext,
                /* suppressSubSchemaRetrieval = */ true);

        // when
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);

        // then
        assertNull(result, "When sub‑schema retrieval is suppressed the result must be null");
    }

    /** Test that when {@code suppressSubSchemaRetrieval} is {@code false} the method delegates to the
     *  ValidationContext and returns the created {@link JsonSchema}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnSubSchema_WhenNotSuppressed() {
        // given – stub the ValidationContext to return a known JsonSchema instance
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(subSchema);

        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                schemaNode,
                parentSchema,
                validatorType,
                validationContext,
                /* suppressSubSchemaRetrieval = */ false);

        // when
        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);

        // then
        assertNotNull(result, "Result must not be null when retrieval is not suppressed");
        assertSame(subSchema, result, "The returned schema should be the one created by ValidationContext");
    }

    /**
     * Concrete subclass of {@link BaseJsonValidator} that simply forwards the
     * protected {@code fetchSubSchemaNode} method as public so that it can be
     * exercised from the test suite.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            // The super‑constructor that accepts the full list of arguments is used
            // because it allows us to set the {@code suppressSubSchemaRetrieval}
            // flag directly.
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  /* from ValidationMessageHandler */ null, // errorMessageType
                  null,                                      // errorMessageKeyword
                  null,                                      // messageSource
                  null,                                      // keyword
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  null,                                      // evaluationParentSchema
                  null);                                     // errorMessage map
        }

        /** Expose the protected method for testing. */
        JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
            return super.fetchSubSchemaNode(vc);
        }

        // The abstract methods from JsonValidator are not needed for these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }
}
```