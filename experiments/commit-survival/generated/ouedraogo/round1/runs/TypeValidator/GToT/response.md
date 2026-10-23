**Step 1 – Public methods of `TypeValidator`**

| # | Modifier | Return type | Method name | Parameters |
|---|----------|-------------|-------------|------------|
| 1 | `public` | `JsonType` | `getSchemaType` | – |
| 2 | `public` | `boolean` | `equalsToSchemaType` | `JsonNode node` |
| 3 | `public` | `Set<ValidationMessage>` | `validate` | `ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at` |

---

**Step 2 – Basic JUnit 4 test cases (one per method)**  

| Method | Basic test idea |
|--------|-----------------|
| `getSchemaType()` | Build a `TypeValidator` for a schema `{"type":"string"}` and assert that `getSchemaType()` returns `JsonType.STRING`. |
| `equalsToSchemaType(JsonNode)` | • Pass a node of the same type (e.g., a text node) → expect `true`. <br>• Pass a node of a different type (e.g., a number node) → expect `false`. |
| `validate(...)` | • When the node matches the declared type → expect an empty `Set`. <br>• When the node does **not** match → expect a single `ValidationMessage` containing the expected/actual types. |

---

**Step 3 – Potential edge‑cases & exception scenarios**

| Method | Edge case / exception | Why it matters |
|--------|------------------------|----------------|
| `getSchemaType()` | Schema node with **union** type (`["string","null"]`). | Must still return `JsonType.UNION`. |
| `equalsToSchemaType(JsonNode)` | `node` is **null**. | The utility should safely return `false` (no NPE). |
| `equalsToSchemaType(JsonNode)` | Schema type is **UNION** – the utility must evaluate each possible type. |
| `validate(...)` | **Union** schema – should delegate to `UnionTypeValidator`. |
| `validate(...)` | `rootNode` is an **array** and the validator’s `schemaPath` ends with `/type`. | Should add the *at* location to `evaluatedItems`. |
| `validate(...)` | `rootNode` is an **object** and the validator’s `schemaPath` ends with `/type`. | Should add the *at* location to `evaluatedProperties`. |
| `validate(...)` | `node` is **null** (JSON `null` value). | Must be handled as a regular `null` node type (`JsonType.NULL`). |
| `validate(...)` | `node` is a **missing** field (i.e., `null` reference passed). | The method should not throw NPE – treat as non‑matching. |
| `validate(...)` | Unexpected **exception** inside `JsonNodeUtil.equalsToSchemaType`. | Propagation should be verified (the test can use a spy to force an exception). |

---

**Step 4 – JUnit 4 test cases for the identified edge‑cases**

*The implementation below uses **Jackson** (`ObjectMapper`) to build `JsonNode`s, **Mockito** for the heavy‑weight collaborators (`JsonSchema`, `ValidationContext`, `ExecutionContext`, `CollectorContext`), and the real `TypeValidator` class.*

```java
###Test START##
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TypeValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Before
    public void setUp() {
        // The ValidationContext must return a Config; we can let it be a mock as well.
        when(validationContext.getConfig()).thenReturn(new Config());
        // ExecutionContext returns a CollectorContext that holds the evaluated sets.
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.getEvaluatedItems()).thenReturn(new java.util.HashSet<String>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new java.util.HashSet<String>());
    }

    /*** 1. getSchemaType() **********************************************/
    @Test
    public void testGetSchemaType_string() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);
        assertEquals(JsonType.STRING, validator.getSchemaType());
    }

    @Test
    public void testGetSchemaType_union() {
        JsonNode schemaNode = MAPPER.createArrayNode().add("string").add("null");
        // Union schema is expressed as an array under the "type" keyword.
        JsonNode wrapper = MAPPER.createObjectNode().set("type", schemaNode);
        TypeValidator validator = new TypeValidator("#/type", wrapper, parentSchema, validationContext);
        assertEquals(JsonType.UNION, validator.getSchemaType());
    }

    /*** 2. equalsToSchemaType(JsonNode) *********************************/
    @Test
    public void testEqualsToSchemaType_matching() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "integer");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode intNode = MAPPER.createObjectNode().put("value", 5).get("value"); // a numeric node
        assertTrue(validator.equalsToSchemaType(intNode));
    }

    @Test
    public void testEqualsToSchemaType_notMatching() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "boolean");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode textNode = MAPPER.createObjectNode().put("value", "hello").get("value"); // a text node
        assertFalse(validator.equalsToSchemaType(textNode));
    }

    @Test
    public void testEqualsToSchemaType_nullNode() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "null");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode nullNode = MAPPER.nullNode();
        assertTrue(validator.equalsToSchemaType(nullNode));
    }

    @Test
    public void testEqualsToSchemaType_nullArgument() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        // Passing a Java null reference – should be handled gracefully (return false)
        assertFalse(validator.equalsToSchemaType(null));
    }

    /*** 3. validate(...) – happy path ************************************/
    @Test
    public void testValidate_matchingType_returnsEmptySet() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "number");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode numberNode = MAPPER.createObjectNode().put("value", 3.14).get("value");

        Set<ValidationMessage> result = validator.validate(executionContext, numberNode, numberNode, "#");
        assertTrue(result.isEmpty());

        // No evaluation side‑effects because schemaPath does not end with "/type"
        verify(collectorContext, never()).getEvaluatedItems();
        verify(collectorContext, never()).getEvaluatedProperties();
    }

    @Test
    public void testValidate_nonMatchingType_returnsMessage() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "array");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode textNode = MAPPER.createObjectNode().put("value", "not an array").get("value");

        Set<ValidationMessage> result = validator.validate(executionContext, textNode, textNode, "/field");
        assertEquals(1, result.size());

        ValidationMessage msg = result.iterator().next();
        assertTrue(msg.getMessage().contains("array"));      // expected type
        assertTrue(msg.getMessage().contains("string"));     // actual type
    }

    /*** 4. validate(...) – union handling *******************************/
    @Test
    public void testValidate_unionType_delegatesToUnionValidator() {
        // schema: {"type":["string","null"]}
        JsonNode unionArray = MAPPER.createArrayNode().add("string").add("null");
        JsonNode schemaNode = MAPPER.createObjectNode().set("type", unionArray);
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        // Spy on the internal UnionTypeValidator
        UnionTypeValidator spyUnion = spy(new UnionTypeValidator("#/type", schemaNode, parentSchema, validationContext));
        // Replace the real instance (reflection for test only)
        try {
            java.lang.reflect.Field f = TypeValidator.class.getDeclaredField("unionTypeValidator");
            f.setAccessible(true);
            f.set(validator, spyUnion);
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }

        JsonNode nullNode = MAPPER.nullNode();
        Set<ValidationMessage> result = validator.validate(executionContext, nullNode, nullNode, "#");

        // Verify delegation
        verify(spyUnion, times(1)).validate(executionContext, nullNode, nullNode, "#");
    }

    /*** 5. validate(...) – root array / object evaluation ***************/
    @Test
    public void testValidate_rootArrayAddsEvaluatedItem() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        // Force the schemaPath to end with "/type" so the hack is executed
        TypeValidator validator = new TypeValidator("#/properties/name/type", schemaNode, parentSchema, validationContext);

        JsonNode rootArray = MAPPER.createArrayNode().add("a").add("b");
        JsonNode element = MAPPER.textNode("a");

        Set<ValidationMessage> result = validator.validate(executionContext, element, rootArray, "/0");
        assertTrue(result.isEmpty());

        // The evaluatedItems set should now contain the location "/0"
        verify(collectorContext).getEvaluatedItems();
        assertTrue(collectorContext.getEvaluatedItems().contains("/0"));
    }

    @Test
    public void testValidate_rootObjectAddsEvaluatedProperty() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "integer");
        TypeValidator validator = new TypeValidator("#/properties/age/type", schemaNode, parentSchema, validationContext);

        JsonNode rootObject = MAPPER.createObjectNode().put("age", 30);
        JsonNode ageNode = rootObject.get("age");

        Set<ValidationMessage> result = validator.validate(executionContext, ageNode, rootObject, "/age");
        assertTrue(result.isEmpty());

        verify(collectorContext).getEvaluatedProperties();
        assertTrue(collectorContext.getEvaluatedProperties().contains("/age"));
    }

    /*** 6. validate(...) – defensive null handling **********************/
    @Test
    public void testValidate_nullJsonNodeHandledGracefully() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "null");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        JsonNode nullNode = MAPPER.nullNode();
        Set<ValidationMessage> result = validator.validate(executionContext, nullNode, nullNode, "#");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testValidate_missingNodeDoesNotThrow() {
        JsonNode schemaNode = MAPPER.createObjectNode().put("type", "string");
        TypeValidator validator = new TypeValidator("#/type", schemaNode, parentSchema, validationContext);

        // Pass a Java null reference as the node to validate
        Set<ValidationMessage> result = validator.validate(executionContext, null, MAPPER.nullNode(), "#");
        // The validator should treat it as non‑matching and return a message, not NPE
        assertEquals(1, result.size());
    }
}
###Test END##
```