```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BaseJsonValidator#fetchSubSchemaNode(ValidationContext)}.
 *
 * The method has two distinct behaviours:
 * <ul>
 *   <li>When {@code suppressSubSchemaRetrieval} is {@code true} it must return {@code null}
 *       without touching the supplied {@link ValidationContext}.</li>
 *   <li>When the flag is {@code false} the method delegates to the private static
 *       {@code obtainSubSchemaNode} method which creates a {@link JsonSchema}
 *       instance.  The tests therefore verify that the delegation works correctly
 *       for a variety of {@code schemaNode} values and {@link ValidationContext}
 *       configurations, as well as that exceptions are propagated unchanged.</li>
 * </ul>
 *
 * A tiny concrete subclass {@code TestValidator} is used to expose the protected
 * method.  The subclass forwards the required constructor arguments to the
 * {@link BaseJsonValidator} super‑class; all arguments that are irrelevant for
 * the behaviour under test are passed as {@code null}.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_fetchSubSchemaNode_Test {

    /** Minimal concrete validator used only for exercising the protected method. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(JsonNode schemaNode, boolean suppressSubSchemaRetrieval) {
            // The real BaseJsonValidator constructor has many parameters; for the
            // purpose of these tests only {@code schemaNode} and the suppression flag
            // matter – the rest can be {@code null}.
            super(
                    /* schemaLocation */ null,
                    /* evaluationPath */ null,
                    schemaNode,
                    /* parentSchema */ null,
                    /* errorMessageType */ null,
                    /* keyword */ null,
                    /* validationContext */ null,
                    suppressSubSchemaRetrieval
            );
        }

        // ---------------------------------------------------------------------
        // Abstract methods – they are not used in these tests, so they are
        // implemented as no‑ops.
        // ---------------------------------------------------------------------
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Set.of();
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

        @Override
        public String getKeyword() {
            return "test";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }
    }

    /* --------------------------------------------------------------------- *
     * Helper methods to build the various ValidationContext instances used   *
     * in the test matrix.                                                   *
     * --------------------------------------------------------------------- */

    private static ValidationContext ctxWithFactory(JsonSchemaFactory factory) {
        return new ValidationContext.Builder()
                .schemaFactory(factory)
                .build();
    }

    private static ValidationContext ctxWithFactoryAndUriFactory(JsonSchemaFactory factory) {
        return new ValidationContext.Builder()
                .schemaFactory(factory)
                .uriFactory(new UriFactory())
                .build();
    }

    /* --------------------------------------------------------------------- *
     * Test cases – each group from the provided JSON is represented by a   *
     * dedicated test method (or a parameterised method when appropriate).  *
     * --------------------------------------------------------------------- */

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_evenWithNullContext() {
        TestValidator validator = new TestValidator(null, true);
        JsonSchema result = validator.fetchSubSchemaNode(null);
        assertNull(result, "Expected null when sub‑schema retrieval is suppressed");
    }

    @Test
    void fetchSubSchemaNode_returnsNullWhenSuppressed_withNonNullContext() {
        TestValidator validator = new TestValidator(null, true);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());
        JsonSchema result = validator.fetchSubSchemaNode(ctx);
        assertNull(result, "Expected null when sub‑schema retrieval is suppressed");
    }

    @Test
    void fetchSubSchemaNode_returnsNonNullWhenNotSuppressed_minimalStringSchema() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());
        JsonSchema result = validator.fetchSubSchemaNode(ctx);
        assertNotNull(result, "Expected a non‑null JsonSchema for a valid schema");
    }

    @Test
    void fetchSubSchemaNode_throwsNPE_whenContextIsNull_andNotSuppressed() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator validator = new TestValidator(schemaNode, false);
        assertThrows(NullPointerException.class,
                () -> validator.fetchSubSchemaNode(null),
                "Expected NullPointerException when ValidationContext is null");
    }

    @Test
    void fetchSubSchemaNode_booleanTrueSchema_returnsNonNull() {
        JsonNode schemaNode = JsonNodeFactory.instance.booleanNode(true);
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());
        JsonSchema result = validator.fetchSubSchemaNode(ctx);
        assertNotNull(result, "A boolean true schema should produce a non‑null JsonSchema");
    }

    @Test
    void fetchSubSchemaNode_emptyObjectSchema_returnsObjectSchema() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode(); // {}
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());
        JsonSchema result = validator.fetchSubSchemaNode(ctx);
        assertNotNull(result, "Empty object schema should return a JsonSchema instance");
        // The concrete type is an implementation of ObjectSchema – we only assert that the schema node is empty.
        assertTrue(result.getSchemaNode().isObject() && result.getSchemaNode().size() == 0,
                "Returned schema node should be an empty object");
    }

    @Test
    void fetchSubSchemaNode_nullSchemaNode_throwsNPE() {
        TestValidator validator = new TestValidator(null, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());
        assertThrows(NullPointerException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "When schemaNode is null, obtainSubSchemaNode should throw NPE");
    }

    @Test
    void fetchSubSchemaNode_resolvesInternalRef() {
        // Build a schema node that contains a $ref to an internal definition.
        JsonNode definitions = JsonNodeFactory.instance.objectNode()
                .set("name", JsonNodeFactory.instance.objectNode().put("type", "string"));
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode()
                .put("$ref", "#/definitions/name")
                .set("definitions", definitions);

        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactoryAndUriFactory(JsonSchemaFactory.getInstance(VersionFlag.V7));

        JsonSchema result = validator.fetchSubSchemaNode(ctx);
        assertNotNull(result, "Resolved $ref should produce a non‑null JsonSchema");
        JsonNode resolvedNode = result.getSchemaNode();
        assertEquals("string", resolvedNode.get("type").asText(),
                "The resolved schema should have type \"string\"");
    }

    @Test
    void fetchSubSchemaNode_factoryThrowsRuntimeException_isPropagated() {
        JsonSchemaFactory explodingFactory = new JsonSchemaFactory() {
            @Override
            public JsonSchema getSchema(JsonNode schemaNode, JsonSchemaFactory factory) {
                throw new RuntimeException("Factory failure");
            }
        };
        ValidationContext ctx = ctxWithFactory(explodingFactory);
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator validator = new TestValidator(schemaNode, false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Factory‑thrown RuntimeException must propagate");
        assertEquals("Factory failure", ex.getMessage());
    }

    @Test
    void fetchSubSchemaNode_numericSchemaNode_throwsIllegalArgumentException() {
        JsonNode schemaNode = JsonNodeFactory.instance.numberNode(42);
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());

        assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Numeric literal schemaNode should cause IllegalArgumentException");
    }

    @Test
    void fetchSubSchemaNode_arraySchemaNode_throwsIllegalArgumentException() {
        JsonNode arrayItem = JsonNodeFactory.instance.objectNode().put("type", "string");
        JsonNode schemaNode = JsonNodeFactory.instance.arrayNode().add(arrayItem);
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactory(JsonSchemaFactory.getInstance());

        assertThrows(IllegalArgumentException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Array schemaNode should cause IllegalArgumentException");
    }

    @Test
    void fetchSubSchemaNode_contextWithNullFactory_throwsNPE() {
        ValidationContext ctx = ctxWithFactory(null); // factory == null
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator validator = new TestValidator(schemaNode, false);

        assertThrows(NullPointerException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Missing JsonSchemaFactory in ValidationContext should cause NPE");
    }

    @Test
    void fetchSubSchemaNode_refToMissingDefinition_throwsJsonSchemaException() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode()
                .put("$ref", "#/definitions/unknown"); // missing definition
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactoryAndUriFactory(JsonSchemaFactory.getInstance(VersionFlag.V7));

        // The library throws JsonSchemaException (a subclass of RuntimeException) for unresolved $ref.
        assertThrows(RuntimeException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Unresolved internal $ref should result in an exception");
    }

    @Test
    void fetchSubSchemaNode_externalUnresolvedRef_throwsRuntimeException() {
        JsonNode schemaNode = JsonNodeFactory.instance.objectNode()
                .put("$ref", "http://nonexistent.example.com/schema.json");
        TestValidator validator = new TestValidator(schemaNode, false);
        ValidationContext ctx = ctxWithFactoryAndUriFactory(JsonSchemaFactory.getInstance(VersionFlag.V7));

        // The external reference cannot be fetched – the underlying factory throws.
        assertThrows(RuntimeException.class,
                () -> validator.fetchSubSchemaNode(ctx),
                "Unresolved external $ref should propagate a RuntimeException");
    }
}
```