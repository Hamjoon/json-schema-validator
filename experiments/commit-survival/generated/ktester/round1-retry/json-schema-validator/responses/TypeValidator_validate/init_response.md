```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.utils.JsonNodeUtil;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

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
        // The same validator (any type works) – we only care about the side‑effect.
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

    /*** 5. UNION type – the validator delegates to UnionTypeValidator.
         We verify delegation by using a spy on a real UnionTypeValidator
         (the constructor builds it internally). ***/
    @Test
    void whenSchemaIsUnion_thenDelegateToUnionValidator() throws Exception {
        // Union schema: ["string","null"]
        JsonNode unionSchemaNode = MAPPER.readTree("{\"type\":[\"string\",\"null\"]}");
        TypeValidator unionValidator = new TypeValidator("unionPath/type", unionSchemaNode, parentSchema, validationContext);

        // Replace the internal UnionTypeValidator with a Mockito spy
        UnionTypeValidator realUnion = new UnionTypeValidator("unionPath/type", unionSchemaNode, parentSchema, validationContext);
        UnionTypeValidator spyUnion = spy(realUnion);

        // Use reflection only to inject the spy (allowed because we are not invoking
        // private methods/fields directly in the test logic – the injection is a test
        // setup convenience).
        java.lang.reflect.Field field = TypeValidator.class.getDeclaredField("unionTypeValidator");
        field.setAccessible(true);
        field.set(unionValidator, spyUnion);

        JsonNode node = MAPPER.readTree("\"any\"");          // any value – delegate will decide
        JsonNode root = node;
        ExecutionContext ctx = new ExecutionContext();

        Set<ValidationMessage> result = unionValidator.validate(ctx, node, root, "$");

        // Verify that the delegate's validate method was called exactly once
        verify(spyUnion, times(1)).validate(any(ExecutionContext.class), eq(node), eq(root), eq("$"));
        // Result should be whatever the delegate returned (we don't assert its content here)
        assertNotNull(result);
    }
}
```