```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaNode()}.
 *
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Returning {@code null} when the validator is constructed with a {@code null} schema node.</li>
 *   <li>Returning the exact instance supplied to the constructor for various {@link JsonNode} sub‑types.</li>
 *   <li>Reference stability – repeated calls must return the same object.</li>
 *   <li>Mutability – changes to the returned node are reflected in the validator’s internal state.</li>
 *   <li>That unrelated public calls (e.g., {@code getSchemaPath()}) do not replace the stored node.</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    /** Minimal concrete implementation of the abstract {@link BaseJsonValidator}. */
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
            // Validation logic not required for getter tests.
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
    private JsonNode sampleSchemaNode;                 // {"type":"string"}
    private TestValidator validatorWithNode;           // non‑null schemaNode
    private TestValidator validatorWithNullNode;       // constructed with null

    @BeforeEach
    void setUp() throws Exception {
        mapper = new ObjectMapper();
        sampleSchemaNode = mapper.readTree("{\"type\":\"string\"}");

        validatorWithNode = new TestValidator(
                "testSchemaPath",
                sampleSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        validatorWithNullNode = new TestValidator(
                "testSchemaPathNull",
                null,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);
    }

    /* --------------------------------------------------------------------- */
    /*                     Basic null / non‑null scenarios                  */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchemaNode_returnsNodeProvidedInConstructor() {
        JsonNode returned = validatorWithNode.getSchemaNode();
        assertNotNull(returned, "The schema node should not be null");
        assertSame(sampleSchemaNode, returned,
                "The returned node must be the exact instance supplied to the constructor");
    }

    @Test
    void getSchemaNode_returnsNullWhenConstructedWithNull() {
        assertNull(validatorWithNullNode.getSchemaNode(),
                "When the validator is constructed with a null schema node, getSchemaNode() should return null");
    }

    /* --------------------------------------------------------------------- */
    /*                Reference stability and mutability tests               */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchemaNode_multipleCallsReturnSameInstance() {
        JsonNode first = validatorWithNode.getSchemaNode();
        JsonNode second = validatorWithNode.getSchemaNode();
        assertSame(first, second, "Repeated calls must return the identical JsonNode instance");
    }

    @Test
    void getSchemaNode_mutatingReturnedNodeIsReflectedInsideValidator() {
        // schemaNode is an ObjectNode {"type":"string"}
        ObjectNode returned = (ObjectNode) validatorWithNode.getSchemaNode();
        returned.put("newField", "newValue");          // mutate

        // A second call should see the mutation – same instance.
        ObjectNode secondCall = (ObjectNode) validatorWithNode.getSchemaNode();
        assertEquals("newValue", secondCall.get("newField").asText(),
                "Mutation of the returned node must be visible on subsequent getSchemaNode() calls");
        assertSame(returned, secondCall, "The instance must still be the original one");
    }

    @Test
    void getSchemaNode_unrelatedPublicMethodDoesNotReplaceNode() {
        JsonNode before = validatorWithNode.getSchemaNode();
        // Call an unrelated public method – it should be a no‑op for the schema node.
        String path = validatorWithNode.getSchemaPath();
        assertNotNull(path, "getSchemaPath() should return a non‑null string (its exact value is irrelevant here)");

        JsonNode after = validatorWithNode.getSchemaNode();
        assertSame(before, after, "Calling other public methods must not replace the stored schemaNode");
    }

    /* --------------------------------------------------------------------- */
    /*                 Parameterised tests for diverse JsonNode types        */
    /* --------------------------------------------------------------------- */

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("jsonNodeProvider")
    void getSchemaNode_returnsExactNodeInstance(String description, JsonNode node) {
        TestValidator validator = new TestValidator(
                "path-" + description,
                node,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        JsonNode returned = validator.getSchemaNode();
        assertSame(node, returned,
                "For " + description + " the validator must return the exact same JsonNode instance");
        // Additionally verify that the content matches what we expect.
        assertEquals(node, returned, "The returned node should be equal to the supplied node");
    }

    /**
     * Supplies a variety of {@link JsonNode} implementations.
     */
    private static Stream<Arguments> jsonNodeProvider() throws Exception {
        ObjectMapper om = new ObjectMapper();

        // ArrayNode ["alpha","beta","gamma"]
        ArrayNode arrayNode = om.createArrayNode()
                .add("alpha")
                .add("beta")
                .add("gamma");

        // NullNode (Jackson's representation of JSON literal null)
        NullNode nullNode = NullNode.getInstance();

        // IntNode with value 12345
        IntNode intNode = new IntNode(12345);

        // BooleanNode true
        BooleanNode boolNode = BooleanNode.TRUE;

        // TextNode "sample text"
        TextNode textNode = new TextNode("sample text");

        // ObjectNode with nested child, used later for a child‑mutation test
        ObjectNode nestedObject = om.createObjectNode();
        ObjectNode inner = om.createObjectNode().put("value", 1);
        nestedObject.set("inner", inner);

        return Stream.of(
                arguments("ArrayNode", arrayNode),
                arguments("NullNode", nullNode),
                arguments("IntNode", intNode),
                arguments("BooleanNode", boolNode),
                arguments("TextNode", textNode),
                arguments("ObjectNode with nested child (for mutation later)", nestedObject)
        );
    }

    /* --------------------------------------------------------------------- */
    /*                     Child‑mutation specific test                      */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchemaNode_nestedChildMutationIsVisible() {
        // Build {"inner":{"value":1}}
        ObjectNode outer = mapper.createObjectNode();
        ObjectNode inner = mapper.createObjectNode().put("value", 1);
        outer.set("inner", inner);

        TestValidator validator = new TestValidator(
                "nestedMutationPath",
                outer,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext,
                false);

        // Retrieve and mutate the nested child.
        ObjectNode returned = (ObjectNode) validator.getSchemaNode();
        ((ObjectNode) returned.get("inner")).put("value", 2); // change 1 → 2

        // Subsequent call must reflect the change.
        ObjectNode after = (ObjectNode) validator.getSchemaNode();
        assertEquals(2, after.get("inner").get("value").asInt(),
                "Mutation of a nested child must be observable through later getSchemaNode() calls");
        assertSame(returned, after, "The instance must remain the same after child mutation");
    }
}
```