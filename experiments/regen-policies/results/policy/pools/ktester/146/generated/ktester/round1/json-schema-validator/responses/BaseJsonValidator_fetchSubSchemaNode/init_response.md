```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * The tests cover the two possible execution paths:
 * <ul>
 *   <li>When {@code suppressSubSchemaRetrieval} is {@code true} the method must return {@code null}.</li>
 *   <li>When {@code suppressSubSchemaRetrieval} is {@code false} the method must return a {@link JsonSchema}
 *       instance (the result of {@code obtainSubSchemaNode}).</li>
 * </ul>
 *
 * A minimal concrete subclass of {@link BaseJsonValidator} is defined inside the test
 * to allow instantiation (the abstract class cannot be instantiated directly).
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** Common objects used by several tests. */
    private static SchemaLocation schemaLocation;
    private static JsonNodePath evaluationPath;
    private static JsonNode emptySchemaNode;

    @Mock
    private ValidationContext mockValidationContext;   // used for the “suppressed” case
    @Mock
    private ValidatorTypeCode mockValidatorType;       // can be any mock – its behaviour is not relevant

    @BeforeAll
    static void setupBeforeAll() {
        // a very small, valid schema location – the absolute IRI can be any string
        AbsoluteIri absoluteIri = new AbsoluteIri("http://example.com/schema");
        schemaLocation = new SchemaLocation(absoluteIri);

        // an empty evaluation path (the root of the schema)
        evaluationPath = new JsonNodePath(JsonNodePath.PathType.JSON_POINTER);

        // an empty JSON object used as the schema node
        emptySchemaNode = new ObjectNode(JsonNodeFactory.instance);
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialisation required at the moment
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required at the moment
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /**
     * When the validator is constructed with {@code suppressSubSchemaRetrieval = true},
     * {@code fetchSubSchemaNode} must return {@code null} regardless of the validation context.
     */
    @Test
    void fetchSubSchemaNode_shouldReturnNullWhenSuppressed() {
        // ----------------------------------------------------------------------
        // GIVEN – a concrete validator with suppression enabled
        // ----------------------------------------------------------------------
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,                     // parent schema – not needed for this test
                mockValidatorType,
                mockValidationContext,
                true                      // suppressSubSchemaRetrieval = true
        );

        // ----------------------------------------------------------------------
        // WHEN
        // ----------------------------------------------------------------------
        JsonSchema result = validator.fetchSubSchemaNode(mockValidationContext);

        // ----------------------------------------------------------------------
        // THEN
        // ----------------------------------------------------------------------
        assertNull(result, "fetchSubSchemaNode should return null when suppression is enabled");
    }

    /**
     * When suppression is disabled the method should delegate to {@code obtainSubSchemaNode}
     * and return a non‑null {@link JsonSchema}.  A real {@link ValidationContext} is used
     * so that the private static helper can create a genuine {@link JsonSchema} instance.
     */
    @Test
    void fetchSubSchemaNode_shouldReturnSchemaWhenNotSuppressed() {
        // ----------------------------------------------------------------------
        // GIVEN – a real ValidationContext (required for the private helper)
        // ----------------------------------------------------------------------
        JsonMetaSchema metaSchema = JsonMetaSchema.getInstance(VersionFlag.V201909);
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V201909);
        ValidationContext realContext = new ValidationContext(
                metaSchema,
                factory,
                new SchemaValidatorsConfig()
        );

        // ----------------------------------------------------------------------
        // AND – a concrete validator with suppression disabled
        // ----------------------------------------------------------------------
        TestValidator validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                emptySchemaNode,
                null,                 // parent schema – not needed for this test
                mockValidatorType,   // the validator type is irrelevant for this path
                realContext,
                false                 // suppressSubSchemaRetrieval = false
        );

        // ----------------------------------------------------------------------
        // WHEN
        // ----------------------------------------------------------------------
        JsonSchema result = validator.fetchSubSchemaNode(realContext);

        // ----------------------------------------------------------------------
        // THEN
        // ----------------------------------------------------------------------
        assertNotNull(result, "fetchSubSchemaNode should return a JsonSchema when suppression is disabled");
    }

    // -------------------------------------------------------------------------
    // Helper concrete subclass – provides the minimal implementation required
    // to instantiate BaseJsonValidator.
    // -------------------------------------------------------------------------
    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     * Only the abstract {@code validate} and {@code getNodeFieldType} methods are
     * implemented with trivial behaviour because they are not exercised by the
     * tests for {@code fetchSubSchemaNode}.
     */
    static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaLocation,
                  evaluationPath,
                  schemaNode,
                  parentSchema,
                  validatorType,
                  validatorType,
                  validationContext,
                  suppressSubSchemaRetrieval);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests
            return Collections.emptySet();
        }

        @Override
        protected String getNodeFieldType() {
            // Arbitrary non‑null value – the method is required by the abstract class
            return "test";
        }
    }
}
```