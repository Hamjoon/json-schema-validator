```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * <p>The method under test is {@code protected}.  A concrete subclass {@link TestValidator}
 * exposes it as {@code public} so that the tests can invoke it directly.</p>
 *
 * <p>All tests follow the given framework – the {@code @BeforeEach} and {@code @AfterEach}
 * methods are left untouched.  Each test case corresponds to one of the scenarios
 * described in the supplied JSON test matrix.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** Concrete validator used in the tests – it simply forwards the protected method. */
    private TestValidator validator;

    /** Mocked validation context – supplied to the validator. */
    @Mock
    private ValidationContext validationContext;

    /** Mocked parent schema – not used by the method under test. */
    @Mock
    private JsonSchema parentSchema;

    /** Dummy objects required by the {@code BaseJsonValidator} constructor. */
    @Mock
    private SchemaLocation schemaLocation;
    @Mock
    private JsonNodePath evaluationPath;
    @Mock
    private ValidatorTypeCode validatorType;

    /** A mock sub‑schema that will be returned by the stubbed {@code ValidationContext}. */
    @Mock
    private JsonSchema subSchema;

    /** Initialise Mockito annotations before each test. */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    /** Clean up after each test – kept for symmetry with the original framework. */
    @AfterEach
    void teardownAfterEach() {
        // No resources to release
    }

    /* --------------------------------------------------------------------- */
    /*  Existing basic scenarios (already present in the original stub)    */
    /* --------------------------------------------------------------------- */

    /** When {@code suppressSubSchemaRetrieval} is {@code true} the method must short‑circuit and return {@code null}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressed() {
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, validationContext, true);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "When sub‑schema retrieval is suppressed the result must be null");
    }

    /** When {@code suppressSubSchemaRetrieval} is {@code false} the method should delegate to {@code obtainSubSchemaNode}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnSubSchema_WhenNotSuppressed() {
        // Stub the ValidationContext to return a known JsonSchema instance.
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(subSchema);

        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, validationContext, false);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNotNull(result, "Result must not be null when retrieval is not suppressed");
        assertSame(subSchema, result, "The returned schema should be the one created by ValidationContext");
    }

    /* --------------------------------------------------------------------- */
    /*  Additional scenarios derived from the JSON test matrix               */
    /* --------------------------------------------------------------------- */

    /** Suppressed flag overrides any non‑null schema node – still returns {@code null}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSuppressedEvenIfSchemaNodeIsObject() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("$ref", "#/definitions/foo");

        validator = new TestValidator(schemaLocation, evaluationPath, objectNode,
                parentSchema, validatorType, validationContext, true);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "Even with a valid schema node, suppression must force a null result");
    }

    /** When {@code suppressSubSchemaRetrieval} is {@code false} but the schema node is {@code null},
     *  the private {@code obtainSubSchemaNode} implementation returns {@code null}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, null,
                parentSchema, validatorType, validationContext, false);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "A null schema node should lead to a null sub‑schema");
    }

    /** A non‑object node (e.g., a textual node) is treated as “not a schema” and yields {@code null}. */
    @Test
    void fetchSubSchemaNode_ShouldReturnNull_WhenSchemaNodeIsNonObject() {
        TextNode textNode = new TextNode("just a string");

        validator = new TestValidator(schemaLocation, evaluationPath, textNode,
                parentSchema, validatorType, validationContext, false);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNull(result, "A non‑object schema node must result in null");
    }

    /** An array node is illegal as a schema definition – the method must propagate {@link IllegalArgumentException}. */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenSchemaNodeIsArray() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        arrayNode.add(1).add(2);

        validator = new TestValidator(schemaLocation, evaluationPath, arrayNode,
                parentSchema, validatorType, validationContext, false);

        assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNodePublic(validationContext),
                "ArrayNode as a schema definition should cause IllegalArgumentException");
    }

    /** An object node that contains a $ref together with another keyword violates the $ref rule
     *  and must cause {@link IllegalArgumentException}. */
    @Test
    void fetchSubSchemaNode_ShouldThrowIllegalArgumentException_WhenRefWithOtherKeywords() {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        obj.put("$ref", "#/definitions/foo");
        obj.put("type", "string"); // illegal combination

        validator = new TestValidator(schemaLocation, evaluationPath, obj,
                parentSchema, validatorType, validationContext, false);

        assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNodePublic(validationContext),
                "ObjectNode with $ref plus other keywords must cause IllegalArgumentException");
    }

    /** When the supplied {@code ValidationContext} is {@code null} a {@link NullPointerException}
     *  must be thrown immediately. */
    @Test
    void fetchSubSchemaNode_ShouldThrowNullPointerException_WhenValidationContextIsNull() {
        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, null, false);

        assertThrows(NullPointerException.class,
                () -> validator.fetchSubSchemaNodePublic(null),
                "Passing a null ValidationContext must result in NPE");
    }

    /** If {@code ValidationContext.newSchema} throws a {@link RuntimeException},
     *  the exception must propagate unchanged. */
    @Test
    void fetchSubSchemaNode_ShouldPropagateRuntimeException_FromValidationContext() {
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, validationContext, false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.fetchSubSchemaNodePublic(validationContext));
        assertEquals("boom", ex.getMessage(), "RuntimeException should be propagated unchanged");
    }

    /** If {@code ValidationContext.newSchema} throws a {@link JsonSchemaException},
     *  the method must let it bubble up. */
    @Test
    void fetchSubSchemaNode_ShouldPropagateJsonSchemaException() {
        JsonSchemaException jse = new JsonSchemaException("invalid schema");
        when(validationContext.newSchema(any(), any(), any(), any()))
                .thenThrow(jse);

        validator = new TestValidator(schemaLocation, evaluationPath, mock(JsonNode.class),
                parentSchema, validatorType, validationContext, false);

        JsonSchemaException ex = assertThrows(JsonSchemaException.class,
                () -> validator.fetchSubSchemaNodePublic(validationContext));
        assertSame(jse, ex, "The same JsonSchemaException instance must be propagated");
    }

    /** When a valid object node (without $ref) is supplied, the method should return a non‑null
     *  {@link JsonSchema} whose {@code getSchemaNode()} equals the original node. */
    @Test
    void fetchSubSchemaNode_ShouldReturnValidSchema_WhenObjectNodeIsValid() {
        // Prepare a simple object schema node.
        ObjectNode schemaObj = JsonNodeFactory.instance.objectNode();
        schemaObj.put("type", "string");

        // Stub the ValidationContext to return a concrete JsonSchema implementation.
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         eq(schemaObj),
                                         any(JsonSchema.class)))
                .thenAnswer(invocation -> {
                    // Return a minimal anonymous JsonSchema that simply returns the node we passed.
                    JsonNode passedNode = invocation.getArgument(2);
                    return new JsonSchema() {
                        @Override public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) { return Collections.emptySet(); }
                        @Override public JsonNode getSchemaNode() { return passedNode; }
                        @Override public SchemaLocation getSchemaLocation() { return null; }
                        @Override public JsonSchema getParentSchema() { return null; }
                        @Override public JsonSchema getEvaluationParentSchema() { return null; }
                        @Override public JsonNodePath getEvaluationPath() { return null; }
                        @Override public String getKeyword() { return null; }
                        @Override public String toString() { return "AnonymousJsonSchema"; }
                    };
                });

        validator = new TestValidator(schemaLocation, evaluationPath, schemaObj,
                parentSchema, validatorType, validationContext, false);

        JsonSchema result = validator.fetchSubSchemaNodePublic(validationContext);
        assertNotNull(result, "A valid object node must produce a non‑null JsonSchema");
        assertSame(schemaObj, result.getSchemaNode(),
                "The returned schema's node should be the original schema node");
    }

    /* --------------------------------------------------------------------- */
    /*  Helper concrete subclass exposing the protected method                */
    /* --------------------------------------------------------------------- */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(suppressSubSchemaRetrieval,
                  schemaNode,
                  validationContext,
                  null,               // errorMessageType
                  null,               // errorMessageKeyword
                  null,               // messageSource
                  null,               // keyword
                  parentSchema,
                  schemaLocation,
                  evaluationPath,
                  null,               // evaluationParentSchema
                  null);              // errorMessage map
        }

        /** Public wrapper used by the test suite. */
        JsonSchema fetchSubSchemaNodePublic(ValidationContext vc) {
            return super.fetchSubSchemaNode(vc);
        }

        // Abstract methods from JsonValidator – not needed for these tests.
        @Override
        public java.util.Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return java.util.Collections.emptySet();
        }
    }
}
```