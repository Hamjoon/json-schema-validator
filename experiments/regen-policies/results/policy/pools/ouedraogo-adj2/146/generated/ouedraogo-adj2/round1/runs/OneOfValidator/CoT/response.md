**1. Public methods of `OneOfValidator`**

| # | Visibility | Return type | Method signature |
|---|------------|-------------|------------------|
| 1 | `public` | `OneOfValidator` | `OneOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` |
| 2 | `public` | `Set<ValidationMessage>` | `validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation)` |
| 3 | `public` | `Set<ValidationMessage>` | `walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema)` |
| 4 | `public` | `void` | `preloadJsonSchema()` |

---

**2. Basic test ideas for each public method**

| Method | Basic test description |
|--------|------------------------|
| **constructor** | Create a `OneOfValidator` with a correct *array* schema node – should succeed. |
|  | Pass a non‑array node (e.g. an object) – constructor must throw `JsonSchemaException`. |
| **validate(...)** | Node matches **exactly one** subschema → result set is empty (no validation errors). |
|  | Node matches **none** of the subschemas → result contains a single `ValidationMessage` with message key `oneOf`. |
|  | Node matches **more than one** subschema → result contains a message with key `oneOf.indexes` and the list of matching schema indexes. |
|  | When `canShortCircuit()` is *true* and three subschemas all validate, the validator must stop after the second valid schema – the reported indexes must be `"0, 1"` (no `"2"`). |
| **walk(...)** | `shouldValidateSchema == false` – the method only walks child schemas, no validation → returned set is empty. |
|  | `shouldValidateSchema == true` – identical to `validate(...)` – re‑use the same cases as for `validate`. |
| **preloadJsonSchema()** | After calling `preloadJsonSchema()` the internal *canShortCircuit* flag is cached (no exception is thrown). The test simply verifies that the method can be called without error. |

---

**3. Edge‑case / exception scenarios**

| Scenario | Why it matters | Expected outcome |
|----------|----------------|------------------|
| **Constructor receives a non‑array schema node** | The validator is defined only for an array of subschemas. | `JsonSchemaException` is thrown. |
| **`ExecutionContext` with `failFast = true`** | When fail‑fast is on, child errors must **not** be aggregated. | The result set contains **only** the top‑level `oneOf` message (no child messages). |
| **Discriminator feature disabled / enabled** | The logic for collecting child errors differs. In the default library configuration the discriminator is *disabled*, so the normal path (`reportChildErrors`) is exercised. | Tests that do not enable the discriminator still work – they verify the normal error‑aggregation behaviour. |
| **Empty `oneOf` array** | An empty array means *no* subschemas – the validator should always fail because `numberOfValidSchema` will be `0`. | Validation result contains a `oneOf` message (0 valid schemas). |
| **`null` instance node** | The `walk` method guards against a `null` node when `shouldValidateSchema` is `true`. | When `shouldValidateSchema` is `true` and `node == null` the method simply returns an empty set (no NPE). |

---

**4. JUnit 4 test cases**

Below are the concrete JUnit 4 test methods that cover the above situations.  
All tests use only the public API that is present in the supplied source code (or in the listed dependency signatures).  
Real objects are created – **no mocking** is used.

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link OneOfValidator}.
 *
 * The tests build real {@link ValidationContext}, {@link JsonSchema},
 * {@link ExecutionContext} and {@link JsonNode} objects – no mocks are used.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonNodePath rootPath;
    private SchemaLocation rootLocation;

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // 1. Create the objects that are needed for every test case
        // -----------------------------------------------------------------
        JsonMetaSchema meta = JsonMetaSchema.getDraftV7();               // default draft‑07
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();    // standard factory
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();   // default config
        validationContext = new ValidationContext(meta, factory, config);

        rootLocation = SchemaLocation.DOCUMENT;                         // the document root
        rootPath = new JsonNodePath(JsonNodePath.PathType.ROOT);
    }

    /* --------------------------------------------------------------
     * Helper – builds a OneOfValidator from a JSON array that contains
     * the supplied subschema strings.
     * -------------------------------------------------------------- */
    private OneOfValidator buildValidator(String... subschemaJson) {
        ArrayNode array = MAPPER.createArrayNode();
        for (String s : subschemaJson) {
            try {
                JsonNode node = MAPPER.readTree(s);
                array.add(node);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return new OneOfValidator(rootLocation, rootPath, array, null, validationContext);
    }

    /* --------------------------------------------------------------
     * 1. Constructor – happy path (array node)
     * -------------------------------------------------------------- */
    @Test
    public void constructorShouldAcceptArrayNode() {
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"integer\"}");
        assertNotNull(validator);
    }

    /* --------------------------------------------------------------
     * 2. Constructor – error when schemaNode is not an array
     * -------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void constructorShouldThrowWhenSchemaNodeIsNotArray() {
        ObjectNode notArray = MAPPER.createObjectNode();
        notArray.put("type", "string");
        new OneOfValidator(rootLocation, rootPath, notArray, null, validationContext);
    }

    /* --------------------------------------------------------------
     * 3. validate – exactly one matching subschema (no errors)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldPassWhenExactlyOneSchemaMatches() {
        // oneOf: [ {type:"string"}, {type:"integer"} ]
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"integer\"}");

        ExecutionContext exec = new ExecutionContext(); // default – failFast = false
        TextNode instance = new TextNode("hello");

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        assertTrue("Expected no validation errors", messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 4. validate – no subschema matches
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldFailWhenNoSchemaMatches() {
        // oneOf: [ {type:"string"}, {type":"boolean"} ]
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"boolean\"}");

        ExecutionContext exec = new ExecutionContext();
        IntNode instance = new IntNode(5); // integer does not match string nor boolean

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        assertEquals(1, messages.size());

        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getMessageKey());   // message key for 0 matches
        assertArrayEquals(new Object[] { "0", "" }, vm.getArguments()); // 0 valid schemas, empty index list
    }

    /* --------------------------------------------------------------
     * 5. validate – more than one matching subschema (indexes reported)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldReportIndexesWhenMultipleSchemasMatch() {
        // oneOf: [ {type:"number"}, {type:"integer"} ]
        // integer 10 matches both “number” and “integer”.
        OneOfValidator validator = buildValidator("{\"type\":\"number\"}", "{\"type\":\"integer\"}");

        ExecutionContext exec = new ExecutionContext();
        IntNode instance = new IntNode(10);

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        assertEquals(1, messages.size());

        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf.indexes", vm.getMessageKey()); // more than one match
        // arguments[0] = number of valid schemas, arguments[1] = comma‑separated indexes
        assertEquals("2", vm.getArguments()[0]);
        assertEquals("0, 1", vm.getArguments()[1]);
    }

    /* --------------------------------------------------------------
     * 6. validate – short‑circuit after two valid schemas
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldShortCircuitAfterTwoValidSchemas() {
        // three empty schemas (they accept any value)
        OneOfValidator validator = buildValidator("{}", "{}", "{}");

        ExecutionContext exec = new ExecutionContext();
        TextNode instance = new TextNode("anything");

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        assertEquals(1, messages.size());

        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf.indexes", vm.getMessageKey());
        // Because of short‑circuit only the first two indexes are reported
        assertEquals("2", vm.getArguments()[0]);           // two valid schemas
        assertEquals("0, 1", vm.getArguments()[1]);       // not “0, 1, 2”
    }

    /* --------------------------------------------------------------
     * 7. walk – shouldValidateSchema = false (no validation performed)
     * -------------------------------------------------------------- */
    @Test
    public void walkShouldReturnEmptySetWhenShouldValidateSchemaIsFalse() {
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"integer\"}");

        ExecutionContext exec = new ExecutionContext();
        TextNode instance = new TextNode("irrelevant");

        Set<ValidationMessage> messages = validator.walk(exec, instance, instance, rootPath, false);
        // walk with shouldValidateSchema == false only traverses children; they do not add messages
        assertTrue(messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 8. walk – shouldValidateSchema = true (delegates to validate)
     * -------------------------------------------------------------- */
    @Test
    public void walkShouldDelegateToValidateWhenShouldValidateSchemaIsTrue() {
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"integer\"}");

        ExecutionContext exec = new ExecutionContext();
        TextNode instance = new TextNode("hello");

        Set<ValidationMessage> messages = validator.walk(exec, instance, instance, rootPath, true);
        // identical to the “exactly one match” case → no errors
        assertTrue(messages.isEmpty());
    }

    /* --------------------------------------------------------------
     * 9. validate – failFast = true (child errors must not be aggregated)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldSuppressChildErrorsWhenFailFastIsTrue() {
        // oneOf: [ {type:"string"}, {type:"integer"} ]
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}", "{\"type\":\"integer\"}");

        ExecutionContext exec = new ExecutionContext();
        exec.setFailFast(true);               // enable fail‑fast
        IntNode instance = new IntNode(10);   // does NOT match the first subschema (string)

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        // Because failFast is true, only the top‑level oneOf message should be present.
        assertEquals(1, messages.size());

        ValidationMessage vm = messages.iterator().next();
        // No child messages are attached → the message key must be “oneOf”
        assertEquals("oneOf", vm.getMessageKey());
    }

    /* --------------------------------------------------------------
     * 10. validate – empty oneOf array (always fails)
     * -------------------------------------------------------------- */
    @Test
    public void validateShouldFailWhenOneOfArrayIsEmpty() {
        OneOfValidator validator = buildValidator(); // no subschemas

        ExecutionContext exec = new ExecutionContext();
        TextNode instance = new TextNode("any");

        Set<ValidationMessage> messages = validator.validate(exec, instance, instance, rootPath);
        assertEquals(1, messages.size());

        ValidationMessage vm = messages.iterator().next();
        assertEquals("oneOf", vm.getMessageKey());   // 0 valid schemas
    }

    /* --------------------------------------------------------------
     * 11. preloadJsonSchema – should not throw and should cache canShortCircuit
     * -------------------------------------------------------------- */
    @Test
    public void preloadJsonSchemaShouldExecuteWithoutException() {
        OneOfValidator validator = buildValidator("{\"type\":\"string\"}");
        // The method does not return anything – we only verify that it runs.
        validator.preloadJsonSchema();

        // Calling again must still be safe (cached flag used internally)
        validator.preloadJsonSchema();
    }
}
###Test END##
```