**1. Public methods of `AnyOfValidator`**

| # | Method signature |
|---|------------------|
| 1 | `public AnyOfValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext)` |
| 2 | `public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at)` |
| 3 | `public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema)` |
| 4 | `public void preloadJsonSchema()` |

---

**2. Basic JUnit‑4 test cases (one per method)**  

| Method | Test purpose | Test name |
|--------|--------------|-----------|
| Constructor | Object can be instantiated with an empty *anyOf* array. | `testConstructorCreatesValidatorWithNoSchemas()` |
| `validate` | Returns an empty error set when there are no subschemas. | `testValidateReturnsEmptyWhenNoSchemas()` |
| `walk` ( `shouldValidateSchema = true` ) | Delegates to `validate`. | `testWalkDelegatesToValidateWhenShouldValidateTrue()` |
| `walk` ( `shouldValidateSchema = false` ) | Calls `walk` on each subschema and returns an empty set. | `testWalkCallsSubSchemasWhenShouldValidateFalse()` |
| `preloadJsonSchema` | Calls the protected `preloadJsonSchemas` helper. | `testPreloadJsonSchemaInvokesHelper()` |

---

**3. Edge‑case & exception scenarios that deserve dedicated tests**

| Scenario | Why it is an edge case |
|----------|------------------------|
| **a) Type‑validator mismatch** – a subschema has a `TypeValidator` whose schema type does **not** match the instance node. The validator must add a *type‑mismatch* `ValidationMessage` and continue with the next subschema. |
| **b) First subschema validates successfully** – the validator must clear any previously collected errors and return an empty set (unless a discriminator is active). |
| **c) OpenAPI‑3 discriminator enabled – a match is found but the subschema still produces errors** – the returned error set must contain the original errors **plus** the special `DISCRIMINATOR_REMARK` message. |
| **d) OpenAPI‑3 discriminator enabled – **no** match is found** – after the loop the validator must return a single error explaining that no discriminator alternative could be chosen. |
| **e) `walk` with `shouldValidateSchema = false` must **not** return the errors produced by the subschemas; it must always return an empty set. |
| **f) `validate` when `state.isWalkEnabled()` is `true` – the validator must call `schema.walk` instead of `schema.validate`. |
| **g) `validate` when the collector‑scope handling throws an exception** – the method must still release the dynamic scopes in the `finally` block (no exception is expected to escape). This is verified by ensuring `exitDynamicScope` is called even if a mocked subschema throws. |

---

**4. JUnit‑4 test cases for the identified edge cases**

*All tests use Mockito for mocking the complex collaborators (`ValidationContext`, `ExecutionContext`, `CollectorContext`, `Scope`, `ValidatorState`, `JsonSchema`, `TypeValidator`, `ValidationMessage`, etc.).*  

```java
/**
 * AnyOfValidatorTest.java
 *
 * Comprehensive unit tests for com.networknt.schema.AnyOfValidator.
 *
 * ###Test START##
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    private static final String SCHEMA_PATH = "#/anyOf";
    private static final String AT = "$";

    @Mock
    private ValidationContext validationContext;

    @Mock
    private ValidationContext.ValidationConfig validationConfig;

    @Mock
    private ExecutionContext executionContext;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private Scope dynamicScope;

    @Mock
    private ValidatorState validatorState;

    @Mock
    private JsonSchema parentSchema;

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() {
        // default behaviour for the validation config (discriminator disabled)
        when(validationContext.getConfig()).thenReturn(validationConfig);
        when(validationConfig.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // collector context handling
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
    }

    /*** 1. BASIC CONSTRUCTOR TEST ***/
    @Test
    public void testConstructorCreatesValidatorWithNoSchemas() throws Exception {
        JsonNode emptyArray = mapper.readTree("[]");
        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, emptyArray, parentSchema, validationContext);
        // the internal list should be empty – we use reflection to verify
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        List<?> schemas = (List<?>) f.get(validator);
        assertTrue("schemas list must be empty", schemas.isEmpty());
    }

    /*** 2. BASIC VALIDATE – NO SUBSCHEMAS ***/
    @Test
    public void testValidateReturnsEmptyWhenNoSchemas() throws Exception {
        JsonNode emptyArray = mapper.readTree("[]");
        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, emptyArray, parentSchema, validationContext);

        JsonNode instance = mapper.readTree("\"any string\"");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);
        assertTrue("Result should be empty when there are no subschemas", result.isEmpty());
    }

    /*** 3. BASIC WALK – delegate to validate ***/
    @Test
    public void testWalkDelegatesToValidateWhenShouldValidateTrue() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        AnyOfValidator validator = spy(new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext));

        JsonNode instance = mapper.readTree("1");
        Set<ValidationMessage> expected = Collections.singleton(mock(ValidationMessage.class));
        doReturn(expected).when(validator).validate(executionContext, instance, instance, AT);

        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, AT, true);
        assertSame("walk should return the same set that validate returns", expected, result);
    }

    /*** 4. BASIC WALK – shouldValidateSchema = false ***/
    @Test
    public void testWalkCallsSubSchemasWhenShouldValidateFalse() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{},{}]");
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(subSchema1)
                .thenReturn(subSchema2);
        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);

        JsonNode instance = mapper.readTree("\"foo\"");
        Set<ValidationMessage> result = validator.walk(executionContext, instance, instance, AT, false);
        // walk must have been called on both subschemas with shouldValidateSchema = false
        verify(subSchema1).walk(executionContext, instance, instance, AT, false);
        verify(subSchema2).walk(executionContext, instance, instance, AT, false);
        assertTrue("walk should return an empty set when shouldValidateSchema is false", result.isEmpty());
    }

    /*** 5. BASIC PRELOAD ***/
    @Test
    public void testPreloadJsonSchemaInvokesHelper() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{},{}]");
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(mock(JsonSchema.class));
        AnyOfValidator validator = spy(new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext));

        validator.preloadJsonSchema();
        // BaseJsonValidator defines protected preloadJsonSchemas(List<JsonSchema>) – we verify that it was called
        verify(validator, times(1)).preloadJsonSchemas(anyList());
    }

    /*** 6. EDGE CASE – Type‑validator mismatch ***/
    @Test
    public void testValidateAddsTypeMismatchMessageWhenSchemaTypeDoesNotMatch() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);

        // mock a TypeValidator that reports INTEGER while the instance node is a string
        TypeValidator typeValidator = mock(TypeValidator.class);
        when(subSchema.hasTypeValidator()).thenReturn(true);
        when(subSchema.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        when(typeValidator.equalsToSchemaType(any(JsonNode.class))).thenReturn(false);

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        JsonNode instance = mapper.readTree("\"string\"");

        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);
        assertEquals("Exactly one validation message expected", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertEquals("Message type must be the mismatched schema type",
                JsonType.INTEGER.toString(), msg.getMessage());
    }

    /*** 7. EDGE CASE – First subschema validates successfully (no discriminator) ***/
    @Test
    public void testValidateSuccessfulSubschemaClearsErrors() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.emptySet());

        // make validatorState report that a node has already been matched (so the "matchedNode" check passes)
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        JsonNode instance = mapper.readTree("42");

        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);
        assertTrue("Result must be empty when a subschema validates successfully", result.isEmpty());
    }

    /*** 8. EDGE CASE – Discriminator enabled, match found but errors present ***/
    @Test
    public void testValidateDiscriminatorMatchFoundWithErrorsAddsRemark() throws Exception {
        // enable discriminator handling
        when(validationConfig.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        when(subSchema.hasTypeValidator()).thenReturn(false);

        ValidationMessage subError = mock(ValidationMessage.class);
        when(subSchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(subError));

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);

        // replace the internally created DiscriminatorContext with a mock we can control
        ValidationContext.DiscriminatorContext discCtx = mock(ValidationContext.DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        f.setAccessible(true);
        f.set(validator, discCtx);

        JsonNode instance = mapper.readTree("\"foo\"");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);

        // expect original error + the discriminator remark
        assertEquals("Two messages expected (original + discriminator remark)", 2, result.size());
        boolean remarkFound = result.stream()
                .anyMatch(m -> m.getMessage().contains(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue("Result must contain the discriminator remark", remarkFound);
    }

    /*** 9. EDGE CASE – Discriminator enabled, no match found ***/
    @Test
    public void testValidateDiscriminatorNoMatchProducesFinalError() throws Exception {
        when(validationConfig.isOpenAPI3StyleDiscriminators()).thenReturn(true);

        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString())).thenReturn(Collections.singleton(mock(ValidationMessage.class)));

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);

        // mock a discriminator context that is *active* but never finds a match
        ValidationContext.DiscriminatorContext discCtx = mock(ValidationContext.DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(false);
        when(discCtx.isActive()).thenReturn(true);
        java.lang.reflect.Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        f.setAccessible(true);
        f.set(validator, discCtx);

        JsonNode instance = mapper.readTree("null");
        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);

        assertEquals("Exactly one error message should be returned when no discriminator match is found", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message must refer to the discriminator", msg.getMessage().contains("discriminator"));
    }

    /*** 10. EDGE CASE – walkEnabled = true forces schema.walk() ***/
    @Test
    public void testValidateCallsSchemaWalkWhenStateWalkEnabled() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.walk(any(), any(), any(), anyString(), eq(true))).thenReturn(Collections.emptySet());

        // make validatorState report walkEnabled = true
        when(validatorState.isWalkEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true); // to satisfy matched‑node check

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        JsonNode instance = mapper.readTree("[]");

        Set<ValidationMessage> result = validator.validate(executionContext, instance, instance, AT);
        verify(subSchema, times(1)).walk(any(), any(), any(), anyString(), eq(true));
        assertTrue("Result must be empty when schema.walk returns empty", result.isEmpty());
    }

    /*** 11. EDGE CASE – Exception inside subschema.validate does not prevent scope cleanup ***/
    @Test
    public void testValidateEnsuresScopeExitEvenWhenSubSchemaThrows() throws Exception {
        JsonNode schemaNode = mapper.readTree("[{}]");
        JsonSchema subSchema = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema))).thenReturn(subSchema);
        when(subSchema.hasTypeValidator()).thenReturn(false);
        when(subSchema.validate(any(), any(), any(), anyString()))
                .thenThrow(new RuntimeException("boom"));

        AnyOfValidator validator = new AnyOfValidator(SCHEMA_PATH, schemaNode, parentSchema, validationContext);
        JsonNode instance = mapper.readTree("\"test\"");

        try {
            validator.validate(executionContext, instance, instance, AT);
            fail("Exception from subschema should propagate");
        } catch (RuntimeException e) {
            assertEquals("boom", e.getMessage());
        }

        // verify that exitDynamicScope was still called
        verify(collectorContext, atLeastOnce()).exitDynamicScope();
    }
}
/**
 * ###Test END##
 */
```

**Explanation of the test file**

* The file begins with the required markers `###Test START##` and `###Test END##`.
* All public methods are exercised:
  * Constructor – verified via reflection that the internal schema list is built correctly.
  * `validate` – exercised for normal flow, type‑mismatch, successful subschema, discriminator‑enabled paths, walk‑enabled flag, and exception safety.
  * `walk` – two tests cover both `shouldValidateSchema` values.
  * `preloadJsonSchema` – verified that the protected helper is invoked.
* Edge‑case tests make extensive use of Mockito to stub the complex collaborators (`ValidationContext`, `ExecutionContext`, `CollectorContext`, `ValidatorState`, `JsonSchema`, `TypeValidator`, `ValidationMessage`, and the internal `DiscriminatorContext`).
* Reflection is used only to inject a mock `DiscriminatorContext` when the discriminator‑related edge cases are tested.
* The tests are self‑contained and can be run with a standard JUnit‑4 runner.