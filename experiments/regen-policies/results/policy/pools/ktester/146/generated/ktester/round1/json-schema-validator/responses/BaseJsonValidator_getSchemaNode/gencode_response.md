```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaNode_Test {

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonSchema parentSchema;

    @Mock
    ValidatorTypeCode validatorType;

    @Mock
    ValidationContext validationContext;

    /** Concrete subclass used only for testing the protected constructor and the
     *  {@code getSchemaNode()} method. */
    private static class TestValidator extends BaseJsonValidator {
        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for these tests
            return Collections.emptySet();
        }
    }

    private TestValidator validator;   // default validator created in @BeforeEach

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // ValidationContext is used inside the super‑constructor; it must return a non‑null config
        when(validationContext.getConfig()).thenReturn(new SchemaValidatorsConfig());

        // By default we use a mocked JsonNode – individual tests will replace it with real nodes when needed
        validator = new TestValidator(
                schemaLocation,
                evaluationPath,
                mock(JsonNode.class),
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test contamination
        Mockito.reset(schemaLocation, evaluationPath, parentSchema,
                validatorType, validationContext);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up
    }

    /* --------------------------------------------------------------------- *
     *  1. Simple identity test – the node supplied to the constructor must be
     *     returned unchanged.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("getSchemaNode() returns the exact ObjectNode supplied to the constructor")
    void testGetSchemaNodeReturnsSuppliedObjectNode() {
        ObjectNode supplied = JsonNodeFactory.instance.objectNode().put("type", "string");
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, supplied,
                parentSchema, validatorType, validationContext);
        JsonNode result = v.getSchemaNode();
        assertSame(supplied, result, "Returned node must be the same instance that was supplied");
    }

    /* --------------------------------------------------------------------- *
     *  2. Null handling – when the constructor receives a null schemaNode the
     *     getter must simply return null.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("getSchemaNode() returns null when validator is constructed with a null schemaNode")
    void testGetSchemaNodeReturnsNullWhenSuppliedNull() {
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, null,
                parentSchema, validatorType, validationContext);
        assertNull(v.getSchemaNode(), "Getter should return null if the constructor argument was null");
    }

    /* --------------------------------------------------------------------- *
     *  3. Parameterised test covering several concrete JsonNode implementations.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => node type=''{0}''")
    @MethodSource("jsonNodeProvider")
    @DisplayName("getSchemaNode() returns the exact instance for various JsonNode types")
    void testGetSchemaNodeVariousNodeTypes(JsonNode suppliedNode) {
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, suppliedNode,
                parentSchema, validatorType, validationContext);
        JsonNode result = v.getSchemaNode();
        assertSame(suppliedNode, result,
                () -> "Returned node should be the same instance for type " + suppliedNode.getClass().getSimpleName());
    }

    private static Stream<Arguments> jsonNodeProvider() {
        JsonNodeFactory f = JsonNodeFactory.instance;
        return Stream.of(
                Arguments.of(f.arrayNode().add(1).add(2).add(3)),               // ArrayNode
                Arguments.of(BooleanNode.TRUE),                                 // BooleanNode
                Arguments.of(new IntNode(42)),                                 // NumericNode (int)
                Arguments.of(NullNode.getInstance())                           // NullNode
        );
    }

    /* --------------------------------------------------------------------- *
     *  4. Mutability check – the node returned by getSchemaNode() must be the
     *     same mutable instance; modifications after retrieval must be visible
     *     on subsequent calls.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("getSchemaNode() returns a mutable instance – mutations are reflected")
    void testGetSchemaNodeMutableReflection() {
        ObjectNode mutableNode = JsonNodeFactory.instance.objectNode()
                .put("type", "string");
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, mutableNode,
                parentSchema, validatorType, validationContext);

        // First retrieval – we keep the reference and mutate it
        JsonNode first = v.getSchemaNode();
        assertSame(mutableNode, first, "First call should return the original instance");
        ((ObjectNode) first).put("maxLength", 5);

        // Second retrieval – must see the mutation
        JsonNode second = v.getSchemaNode();
        assertSame(mutableNode, second, "Second call should still return the original instance");
        assertTrue(second.has("maxLength") && second.get("maxLength").asInt() == 5,
                "Mutation performed on the returned node must be visible on subsequent calls");
    }

    /* --------------------------------------------------------------------- *
     *  5. Sub‑class behaviour when the internal flag `suppressSubSchemaRetrieval`
     *     is forced to true via reflection. The flag does not influence the
     *     behaviour of getSchemaNode(), but we verify that the method still
     *     returns the exact supplied node.
     * --------------------------------------------------------------------- */
    @Test
    @DisplayName("getSchemaNode() works unchanged even when suppressSubSchemaRetrieval is true")
    void testGetSchemaNodeWithSuppressFlagTrue() throws Exception {
        ObjectNode supplied = JsonNodeFactory.instance.objectNode().put("type", "integer");
        TestValidator v = new TestValidator(schemaLocation, evaluationPath, supplied,
                parentSchema, validatorType, validationContext);

        // Force the protected final field to true using reflection
        Field flagField = BaseJsonValidator.class.getDeclaredField("suppressSubSchemaRetrieval");
        flagField.setAccessible(true);
        flagField.setBoolean(v, true);

        // Verify that the getter still returns the exact node instance
        JsonNode result = v.getSchemaNode();
        assertSame(supplied, result,
                "Even with suppressSubSchemaRetrieval set to true, getSchemaNode() must return the original node");
    }
}
```