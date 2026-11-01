**Step 1 – Public API of `OneOfValidator`**

| Visibility | Return type | Method name & signature |
|------------|-------------|--------------------------|
| `public` | `Set<ValidationMessage>` | `validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation)` |
| `public` | `Set<ValidationMessage>` | `walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema)` |
| `public` | `void` | `preloadJsonSchema()` |
| `public` | *constructor* | `OneOfValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` |

---

**Step 2 – Basic JUnit‑4 test ideas (one per method)**  

| Method | Basic test description |
|--------|------------------------|
| **constructor** | Build a `OneOfValidator` with a schema node that contains two subschemas (`type:string` and `type:number`). Verify that the internal `schemas` list has size 2. |
| **validate** | Provide a JSON node that matches only the *string* subschema. Expect the returned error set to be empty (valid). |
| **walk (shouldValidateSchema = true)** | Call `walk` with the same matching node and `true`. The result must be identical to `validate`. |
| **walk (shouldValidateSchema = false)** | Call `walk` with `false`. No validation is performed, therefore the returned set must be empty regardless of the input node. |
| **preloadJsonSchema** | Invoke `preloadJsonSchema()` and verify that no exception is thrown (the child schemas are initialised). |

---

**Step 3 – Edge‑case & exception scenarios that need dedicated tests**

| Scenario | Why it matters | Expected outcome |
|----------|----------------|-------------------|
| **Zero subschemas** (empty array) | `OneOfValidator` must still produce a *one‑of* error because `numberOfValidSchema` will be 0. | `validate` returns a non‑empty set containing a “oneOf” `ValidationMessage`. |
| **Exactly two matching subschemas** | The validator is required to fail when more than one subschema validates. | `validate` returns a non‑empty set (error) and `numberOfValidSchema` > 1 (short‑circuit). |
| **`failFast` flag set to `true`** | When the validator is configured to fail fast it must throw `JsonSchemaException` instead of returning a set. | Calling `validate` throws `JsonSchemaException`. |
| **`null` instance node** | The library treats `null` as a valid JSON node, but the validator must still evaluate it against each subschema. | If none of the subschemas accept `null`, the error set contains a one‑of message; otherwise it is empty. |
| **`state.isWalkEnabled()` true** | The validator must delegate to `schema.walk` instead of `schema.validate`. The behaviour must be identical to the normal path. | Validation result is the same as when walk is disabled. |
| **Dynamic scope merging** – ensure that when a subschema validates successfully the collector‑scope is merged into the parent scope. | After a successful validation the collector’s *evaluated items/properties* must contain the paths produced by the child schema. (We only assert that the merge does **not** throw.) |

---

**Step 4 – JUnit‑4 test cases for the identified edge cases**

Below are concrete test methods that implement the above scenarios.  
All helper methods (`createOneOfValidator`, `createExecutionContext`, …) are written once and reused throughout the file.

```java
package com.networknt.schema;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import java.util.*;

public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private ExecutionContext executionContext;
    private JsonSchema parentSchema;          // dummy parent – never used by OneOfValidator itself
    private SchemaLocation baseLocation;
    private JsonNodePath basePath;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // Minimal, real objects required by the library – the concrete implementations are part of the
        // networknt‑json‑schema library, so we rely on their public constructors.
        validationContext = new ValidationContext(
                new URIFactory(),
                new URNFactory(),
                JsonMetaSchema.getV7(),               // the default meta‑schema
                new JsonSchemaFactory(),
                new SchemaValidatorsConfig()
        );

        // a dummy parent schema – we just need a concrete instance; it will never be inspected
        parentSchema = new JsonSchema(
                SchemaLocation.DOCUMENT,
                JsonNodePath.ROOT,
                MAPPER.createObjectNode(),
                null,
                ValidatorTypeCode.OBJECT,
                validationContext
        );

        executionContext = new ExecutionContext(new CollectorContext());

        baseLocation = SchemaLocation.DOCUMENT;
        basePath = JsonNodePath.ROOT;
    }

    /** --------------------------------------------------------------
     *  Helper – builds a OneOfValidator from a JSON array of subschemas.
     *  ---------------------------------------------------------------- */
    private OneOfValidator createOneOfValidator(String schemaArrayJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaArrayJson);
        return new OneOfValidator(baseLocation, basePath, schemaNode, parentSchema, validationContext);
    }

    /** --------------------------------------------------------------
     *  Helper – creates an ExecutionContext with a fresh CollectorContext.
     *  ---------------------------------------------------------------- */
    private ExecutionContext freshExecutionContext() {
        return new ExecutionContext(new CollectorContext());
    }

    /** --------------------------------------------------------------
     *  Helper – enables the internal “failFast” flag via reflection.
     *  ---------------------------------------------------------------- */
    private void enableFailFast(OneOfValidator validator) throws Exception {
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.setBoolean(validator, true);
    }

    /** --------------------------------------------------------------
     *  1. Constructor – the internal list must contain the same number of
     *     subschemas as the source array.
     *  ---------------------------------------------------------------- */
    @Test
    public void testConstructorCreatesCorrectNumberOfSchemas() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);

        // Use reflection to read the private “schemas” field
        java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
        assertEquals(2, list.size());
    }

    /** --------------------------------------------------------------
     *  2. Simple valid case – only the string schema matches.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateExactlyOneMatch() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> errors = validator.validate(freshExecutionContext(),
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT);
        assertTrue(errors.isEmpty());
    }

    /** --------------------------------------------------------------
     *  3. No subschema matches – should return a oneOf error.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateNoMatch() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("true");          // boolean – matches none

        Set<ValidationMessage> errors = validator.validate(freshExecutionContext(),
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT);
        assertFalse(errors.isEmpty());

        // the first message must be the “oneOf” error (its code contains “oneOf”)
        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /** --------------------------------------------------------------
     *  4. More than one subschema matches – also a oneOf error.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateMultipleMatches() throws Exception {
        // both string and enum (which also matches a string) – two matches
        String json = "[ {\"type\":\"string\"}, {\"enum\":[\"hello\"]} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> errors = validator.validate(freshExecutionContext(),
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT);
        assertFalse(errors.isEmpty());
        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /** --------------------------------------------------------------
     *  5. Empty schema array – must produce a oneOf error.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateEmptySchemaArray() throws Exception {
        String json = "[]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"anything\"");

        Set<ValidationMessage> errors = validator.validate(freshExecutionContext(),
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT);
        assertFalse(errors.isEmpty());
        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /** --------------------------------------------------------------
     *  6. failFast = true – the validator must throw JsonSchemaException.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateFailFastThrowsException() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        enableFailFast(validator);          // turn on the protected flag

        JsonNode instance = MAPPER.readTree("true");   // matches none → error

        thrown.expect(JsonSchemaException.class);
        validator.validate(freshExecutionContext(),
                           instance,
                           instance,
                           JsonNodePath.ROOT);
    }

    /** --------------------------------------------------------------
     *  7. walk(…, true) must behave exactly like validate().
     *  ---------------------------------------------------------------- */
    @Test
    public void testWalkValidatePath() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> viaValidate = validator.validate(freshExecutionContext(),
                                                               instance,
                                                               instance,
                                                               JsonNodePath.ROOT);
        Set<ValidationMessage> viaWalk = validator.walk(freshExecutionContext(),
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT,
                                                          true);
        assertEquals(viaValidate, viaWalk);
    }

    /** --------------------------------------------------------------
     *  8. walk(…, false) – no validation is performed, result must be empty.
     *  ---------------------------------------------------------------- */
    @Test
    public void testWalkSkipValidation() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("true");

        Set<ValidationMessage> result = validator.walk(freshExecutionContext(),
                                                        instance,
                                                        instance,
                                                        JsonNodePath.ROOT,
                                                        false);
        assertTrue(result.isEmpty());
    }

    /** --------------------------------------------------------------
     *  9. preloadJsonSchema() must not throw – it simply forwards to each child.
     *  ---------------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaDoesNotFail() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        // The method has no return value – we only verify that no exception propagates.
        validator.preloadJsonSchema();
    }

    /** --------------------------------------------------------------
     *  10. State walk‑enabled path – ensure the same result as the normal path.
     *  ---------------------------------------------------------------- */
    @Test
    public void testValidateWhenWalkEnabled() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        ExecutionContext ctx = freshExecutionContext();

        // Force walk‑enabled flag inside the validator state
        ctx.getValidatorState().setWalkEnabled(true);

        JsonNode instance = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> errors = validator.validate(ctx,
                                                          instance,
                                                          instance,
                                                          JsonNodePath.ROOT);
        assertTrue(errors.isEmpty());
    }
}
```

---

### Complete JUnit‑4 test file  

```java
###Test START##
package com.networknt.schema;

import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests cover:
 *   • normal construction,
 *   • successful validation (exactly one subschema matches),
 *   • all error‑paths (no match, multiple matches, empty schema array),
 *   • fail‑fast behaviour,
 *   • both {@code validate} and {@code walk} entry points,
 *   • the {@code preloadJsonSchema} hook,
 *   • and the walk‑enabled state flag.
 *
 * All objects are created through the public constructors / factory methods
 * supplied by the library – no mocking or subclassing is performed.
 */
public class OneOfValidatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ValidationContext validationContext;
    private JsonSchema parentSchema;
    private SchemaLocation baseLocation;
    private JsonNodePath basePath;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // Minimal, real objects required by the library.
        validationContext = new ValidationContext(
                new URIFactory(),
                new URNFactory(),
                JsonMetaSchema.getV7(),
                new JsonSchemaFactory(),
                new SchemaValidatorsConfig()
        );

        parentSchema = new JsonSchema(
                SchemaLocation.DOCUMENT,
                JsonNodePath.ROOT,
                MAPPER.createObjectNode(),
                null,
                ValidatorTypeCode.OBJECT,
                validationContext
        );

        baseLocation = SchemaLocation.DOCUMENT;
        basePath = JsonNodePath.ROOT;
    }

    /** Helper – builds a {@link OneOfValidator} from a JSON array string. */
    private OneOfValidator createOneOfValidator(String schemaArrayJson) throws Exception {
        JsonNode schemaNode = MAPPER.readTree(schemaArrayJson);
        return new OneOfValidator(baseLocation, basePath, schemaNode, parentSchema, validationContext);
    }

    /** Helper – creates a fresh {@link ExecutionContext} with an empty {@link CollectorContext}. */
    private ExecutionContext freshExecutionContext() {
        return new ExecutionContext(new CollectorContext());
    }

    /** Helper – toggles the protected {@code failFast} flag via reflection. */
    private void enableFailFast(OneOfValidator validator) throws Exception {
        java.lang.reflect.Field f = BaseJsonValidator.class.getDeclaredField("failFast");
        f.setAccessible(true);
        f.setBoolean(validator, true);
    }

    /* -------------------------------------------------------------
     *  1. Constructor – verify the internal schema list size.
     * ----------------------------------------------------------- */
    @Test
    public void testConstructorCreatesCorrectNumberOfSchemas() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);

        java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
        assertEquals(2, list.size());
    }

    /* -------------------------------------------------------------
     *  2. Exactly one subschema matches – validation succeeds.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateExactlyOneMatch() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> errors = validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );
        assertTrue(errors.isEmpty());
    }

    /* -------------------------------------------------------------
     *  3. No subschema matches – a oneOf error is returned.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateNoMatch() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("true");           // boolean – matches none

        Set<ValidationMessage> errors = validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );
        assertFalse(errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /* -------------------------------------------------------------
     *  4. More than one subschema matches – also a oneOf error.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateMultipleMatches() throws Exception {
        // string type and an enum that also accepts the same string → two matches
        String json = "[ {\"type\":\"string\"}, {\"enum\":[\"hello\"]} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> errors = validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );
        assertFalse(errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /* -------------------------------------------------------------
     *  5. Empty schema array – must still emit a oneOf error.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateEmptySchemaArray() throws Exception {
        String json = "[]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"anything\"");

        Set<ValidationMessage> errors = validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );
        assertFalse(errors.isEmpty());

        ValidationMessage first = errors.iterator().next();
        assertTrue(first.getMessage().toLowerCase().contains("oneof"));
    }

    /* -------------------------------------------------------------
     *  6. failFast = true – validator throws JsonSchemaException.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateFailFastThrowsException() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        enableFailFast(validator);

        JsonNode instance = MAPPER.readTree("true");   // matches none → error

        thrown.expect(JsonSchemaException.class);
        validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );
    }

    /* -------------------------------------------------------------
     *  7. walk(…, true) must return exactly the same set as validate().
     * ----------------------------------------------------------- */
    @Test
    public void testWalkValidatePath() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("\"hello\"");

        Set<ValidationMessage> viaValidate = validator.validate(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT
        );

        Set<ValidationMessage> viaWalk = validator.walk(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT,
                true
        );

        assertEquals(viaValidate, viaWalk);
    }

    /* -------------------------------------------------------------
     *  8. walk(…, false) – validation is deliberately skipped.
     * ----------------------------------------------------------- */
    @Test
    public void testWalkSkipValidation() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        JsonNode instance = MAPPER.readTree("true");

        Set<ValidationMessage> result = validator.walk(
                freshExecutionContext(),
                instance,
                instance,
                JsonNodePath.ROOT,
                false
        );
        assertTrue(result.isEmpty());
    }

    /* -------------------------------------------------------------
     *  9. preloadJsonSchema() must complete without throwing.
     * ----------------------------------------------------------- */
    @Test
    public void testPreloadJsonSchemaDoesNotFail() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        validator.preloadJsonSchema();   // no exception → success
    }

    /* -------------------------------------------------------------
     * 10. Validation when the internal state is “walk‑enabled”.
     * ----------------------------------------------------------- */
    @Test
    public void testValidateWhenWalkEnabled() throws Exception {
        String json = "[ {\"type\":\"string\"}, {\"type\":\"number\"} ]";
        OneOfValidator validator = createOneOfValidator(json);
        ExecutionContext ctx = freshExecutionContext();

        // Force the “walk enabled” flag inside the validator state.
        ctx.getValidatorState().setWalkEnabled(true);

        JsonNode instance = MAPPER.readTree("\"hello\"");
        Set<ValidationMessage> errors = validator.validate(
                ctx,
                instance,
                instance,
                JsonNodePath.ROOT
        );
        assertTrue(errors.isEmpty());
    }
}
###Test END##
```