```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TypeValidator#validate(ExecutionContext, JsonNode, JsonNode, String)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Happy‑path validation (type matches)</li>
 *   <li>Type mismatch handling</li>
 *   <li>Side‑effects when {@code schemaPath} ends with {@code "/type"}</li>
 *   <li>Delegation to {@link UnionTypeValidator}</li>
 *   <li>Various null‑argument / mis‑configuration scenarios that should raise NPEs</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class TypeValidator_validate_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /*** shared test fixtures ***/
    private TypeValidator validatorString;      // validator for {"type":"string"}
    private TypeValidator validatorNumber;      // validator for {"type":"number"}
    private JsonNode stringSchemaNode;
    private JsonNode numberSchemaNode;

    @Mock
    private ValidationContext validationContext;   // mock – we do not need real behaviour

    @Mock
    private JsonSchema parentSchema;               // mock – not used by the logic we test

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // Build the schema nodes that describe the expected type.
        // -----------------------------------------------------------------
        stringSchemaNode = MAPPER.readTree("{\"type\":\"string\"}");
        numberSchemaNode = MAPPER.readTree("{\"type\":\"number\"}");

        // -----------------------------------------------------------------
        // Create the validators.  The constructor needs a ValidationContext;
        // we provide the mocked instance (its getConfig() will return null).
        // -----------------------------------------------------------------
        validatorString = new TypeValidator("testPath/type", stringSchemaNode, parentSchema, validationContext);
        validatorNumber = new TypeValidator("testPath/type", numberSchemaNode, parentSchema, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up between tests
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    /*** 1. Happy‑path – node type matches the schema type → empty set ***/
    @Test
    void whenNodeMatchesSchemaType_thenEmptySetReturned() throws Exception {
        JsonNode node = MAPPER.readTree("\"hello world\"");   // a JSON string
        JsonNode root = node;                                 // root is the same node
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validatorString.validate(ctx, node, root, "$");

        assertTrue(messages.isEmpty(), "Expected no validation messages when type matches");
    }

    /*** 2. Mismatch – node type differs from schema type → one ValidationMessage ***/
    @Test
    void whenNodeDoesNotMatchSchemaType_thenMessageReturned() throws Exception {
        JsonNode node = MAPPER.readTree("123");               // a JSON number
        JsonNode root = node;
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = validatorString.validate(ctx, node, root, "$.value");

        // Expect exactly one message describing the type error
        assertEquals(1, messages.size(), "One validation message expected for type mismatch");
        ValidationMessage msg = messages.iterator().next();
        assertTrue(msg.getMessage().contains("string"), "Message should contain expected type 'string'");
        assertTrue(msg.getMessage().contains("number"), "Message should contain actual type 'number'");
    }

    /*** 3. When the schemaPath ends with "/type" and the root is an array,
         the validator must add the location to evaluatedItems ***/
    @Test
    void whenSchemaPathEndsWithTypeAndRootIsArray_thenEvaluatedItemsUpdated() throws Exception {
        JsonNode node = MAPPER.readTree("\"foo\"");          // a valid string for the schema
        JsonNode rootArray = MAPPER.readTree("[\"foo\", \"bar\"]");
        ExecutionContext ctx = new ExecutionContext();

        // Path that ends with "/type" – this triggers the hack inside validate()
        String at = "$.0";

        Set<ValidationMessage> messages = validatorString.validate(ctx, node, rootArray, at);

        // No validation error expected
        assertTrue(messages.isEmpty());

        // The collector must contain the evaluated item path
        CollectorContext collector = ctx.getCollectorContext();
        assertTrue(collector.getEvaluatedItems().contains(at),
                "Evaluated items should contain the path when root is an array");
    }

    /*** 4. When the schemaPath ends with "/type" and the root is an object,
         the validator must add the location to evaluatedProperties ***/
    @Test
    void whenSchemaPathEndsWithTypeAndRootIsObject_thenEvaluatedPropertiesUpdated() throws Exception {
        JsonNode node = MAPPER.readTree("\"value\"");
        JsonNode rootObject = MAPPER.readTree("{\"prop\":\"value\"}");
        ExecutionContext ctx = new ExecutionContext();

        String at = "$.prop";

        Set<ValidationMessage> messages = validatorString.validate(ctx, node, rootObject, at);
        assertTrue(messages.isEmpty());

        CollectorContext collector = ctx.getCollectorContext();
        assertTrue(collector.getEvaluatedProperties().contains(at),
                "Evaluated properties should contain the path when root is an object");
    }

    /*** 5. UNION type – the validator delegates to UnionTypeValidator ***/
    @Test
    void whenSchemaIsUnion_thenDelegateToUnionValidator() throws Exception {
        // Union schema: ["string","null"]
        JsonNode unionSchemaNode = MAPPER.readTree("{\"type\":[\"string\",\"null\"]}");
        TypeValidator unionValidator = new TypeValidator("unionPath/type", unionSchemaNode, parentSchema, validationContext);

        // Replace the internal UnionTypeValidator with a Mockito spy
        UnionTypeValidator realUnion = new UnionTypeValidator("unionPath/type", unionSchemaNode, parentSchema, validationContext);
        UnionTypeValidator spyUnion = spy(realUnion);
        Field field = TypeValidator.class.getDeclaredField("unionTypeValidator");
        field.setAccessible(true);
        field.set(unionValidator, spyUnion);

        JsonNode node = MAPPER.readTree("\"any\"");
        JsonNode root = node;
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> result = unionValidator.validate(ctx, node, root, "$");

        // Verify delegation
        verify(spyUnion, times(1)).validate(any(ExecutionContext.class), eq(node), eq(root), eq("$"));
        assertNotNull(result);
    }

    /*** 6. UNION type handling – null UnionTypeValidator should throw NPE ***/
    @Test
    void whenUnionTypeValidatorIsNull_thenNpeOnValidate() throws Exception {
        JsonNode unionSchemaNode = MAPPER.readTree("{\"type\":[\"string\",\"null\"]}");
        TypeValidator unionValidator = new TypeValidator("unionPath/type", unionSchemaNode, parentSchema, validationContext);

        // Force the internal union validator to null via reflection
        Field field = TypeValidator.class.getDeclaredField("unionTypeValidator");
        field.setAccessible(true);
        field.set(unionValidator, null);

        JsonNode node = JsonNodeFactory.instance.textNode("any");
        JsonNode root = node;
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class,
                () -> unionValidator.validate(ctx, node, root, "/some/path"),
                "Calling validate on a UNION schema with a null UnionTypeValidator must throw NPE");
    }

    /*** 7. Schema path is null – endsWith call should throw NPE ***/
    @Test
    void whenSchemaPathIsNull_thenNpeDuringEndsWithCheck() throws Exception {
        // Construct a validator with a null schema path
        TypeValidator nullPathValidator = new TypeValidator(null, stringSchemaNode, parentSchema, validationContext);

        JsonNode node = JsonNodeFactory.instance.textNode("match");
        JsonNode root = JsonNodeFactory.instance.objectNode();
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class,
                () -> nullPathValidator.validate(ctx, node, root, "/prop"),
                "When schemaPath is null the endsWith call must raise NPE");
    }

    /*** 8. Root node null – isArray / isObject call should throw NPE ***/
    @Test
    void whenRootNodeIsNull_thenNpeOnIsArrayOrIsObject() throws Exception {
        // Use a validator whose schemaPath ends with "/type" so the hack block is entered
        JsonNode node = JsonNodeFactory.instance.textNode("match");
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class,
                () -> validatorString.validate(ctx, node, null, "/0"),
                "A null rootNode should cause NPE when validator attempts rootNode.isArray()/isObject()");
    }

    /*** 9. ExecutionContext null – getCollectorContext call should throw NPE ***/
    @Test
    void whenExecutionContextIsNull_thenNpeOnCollectorAccess() throws Exception {
        JsonNode node = JsonNodeFactory.instance.textNode("match");
        JsonNode root = JsonNodeFactory.instance.objectNode();

        // The path ends with "/type" to reach the side‑effect block
        assertThrows(NullPointerException.class,
                () -> validatorString.validate(null, node, root, "/prop"),
                "Passing a null ExecutionContext must cause NPE when getCollectorContext() is invoked");
    }

    /*** 10. CollectorContext null – evaluated set access should throw NPE ***/
    @Test
    void whenCollectorContextIsNull_thenNpeOnEvaluatedSetAccess(@Mock ExecutionContext execCtx) throws Exception {
        // Mock ExecutionContext to return a null CollectorContext
        when(execCtx.getCollectorContext()).thenReturn(null);

        JsonNode node = JsonNodeFactory.instance.textNode("match");
        JsonNode root = JsonNodeFactory.instance.arrayNode();

        assertThrows(NullPointerException.class,
                () -> validatorString.validate(execCtx, node, root, "/0"),
                "If CollectorContext is null, accessing evaluated sets must raise NPE");
    }

    /*** 11. Node null – equalsToSchemaType / TypeFactory should raise NPE ***/
    @Test
    void whenNodeIsNull_thenNpeDuringTypeCheck() throws Exception {
        JsonNode root = JsonNodeFactory.instance.objectNode();
        ExecutionContext ctx = new ExecutionContext();

        assertThrows(NullPointerException.class,
                () -> validatorString.validate(ctx, null, root, "/missing"),
                "Passing a null node must result in NPE in equalsToSchemaType or TypeFactory");
    }

    /*** 12. Non‑union – node matches, schemaPath does NOT end with "/type"
          → empty set and no side‑effects ***/
    @Test
    void whenNodeMatchesAndSchemaPathDoesNotEndWithType_thenNoSideEffect() throws Exception {
        // Build a validator whose schemaPath does NOT end with "/type"
        TypeValidator booleanValidator = new TypeValidator("testPath/boolean", 
                MAPPER.readTree("{\"type\":\"boolean\"}"), parentSchema, validationContext);

        JsonNode node = JsonNodeFactory.instance.booleanNode(true);
        JsonNode root = JsonNodeFactory.instance.objectNode().set("flag", node);
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> messages = booleanValidator.validate(ctx, node, root, "/flag");
        assertTrue(messages.isEmpty(), "No validation messages expected when type matches and schemaPath is unrelated");

        // Verify that no evaluated sets have been modified
        CollectorContext collector = ctx.getCollectorContext();
        assertTrue(collector.getEvaluatedItems().isEmpty(), "Evaluated items must stay empty");
        assertTrue(collector.getEvaluatedProperties().isEmpty(), "Evaluated properties must stay empty");
    }
}
```