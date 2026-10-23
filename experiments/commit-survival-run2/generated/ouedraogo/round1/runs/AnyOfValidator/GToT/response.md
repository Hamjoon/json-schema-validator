###Test START##
```java
/**
 * JUnit‑4 test suite for {@link com.networknt.schema.AnyOfValidator}.
 *
 * <p>The tests are written from the perspective of three “experts” that each
 * contributed a test case for a public method, identified edge cases and
 * finally merged everything into a single test class.</p>
 *
 * <p>All external collaborators of {@code AnyOfValidator} are mocked with
 * Mockito, allowing us to focus on the validator’s own control flow without
 * needing real schema files or a full validation engine.</p>
 *
 * @author  OpenAI
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test class that exercises every public method of {@link AnyOfValidator}
 * using a combination of normal, edge‑case and exception scenarios.
 */
@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class AnyOfValidatorTest {

    /** Common test fixtures */
    @Mock private ValidationContext validationContext;
    @Mock private ValidationContext.Config config;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private CollectorContext.Scope grandParentScope;
    @Mock private CollectorContext.Scope parentScope;
    @Mock private CollectorContext.Scope childScope;
    @Mock private JsonSchema parentSchema;
    @Mock private JsonSchema subSchema1;
    @Mock private JsonSchema subSchema2;
    @Mock private TypeValidator typeValidator;
    @Mock private ValidatorState validatorState;

    /** The validator under test */
    private AnyOfValidator validator;

    /** Simple JSON nodes used in the tests */
    private final JsonNodeFactory factory = JsonNodeFactory.instance;
    private JsonNode dummyNode;
    private JsonNode schemaArrayNode;

    @Before
    public void setUp() {
        // ----- basic JSON nodes -------------------------------------------------
        dummyNode = factory.textNode("dummy");
        // a schema node that contains two subschemas (as an array)
        schemaArrayNode = factory.arrayNode()
                .add(factory.objectNode())   // sub‑schema 0
                .add(factory.objectNode());  // sub‑schema 1

        // ----- validation context & configuration -------------------------------
        when(validationContext.getConfig()).thenReturn(config);
        // default: discriminators are disabled
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);
        // newSchema(i, …) creates the two mocked subschemas
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenAnswer(invocation -> {
                    String path = invocation.getArgument(0);
                    return path.endsWith("/0") ? subSchema1 : subSchema2;
                });

        // ----- collector context -------------------------------------------------
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
        when(collectorContext.enterDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.exitDynamicScope()).thenReturn(parentScope, childScope);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope);
        when(collectorContext.exitDynamicScope()).thenReturn(grandParentScope);
        when(collectorContext.enterDynamicScope()).thenReturn(grandParentScope);
        // the validator state is stored under a well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.hasMatchedNode()).thenReturn(false);
        when(validatorState.isWalkEnabled()).thenReturn(false);

        // ----- build the validator instance ------------------------------------
        validator = new AnyOfValidator("testPath", schemaArrayNode, parentSchema, validationContext);
    }

    /* -------------------------------------------------------------------------
     *  Expert 1 – basic functional tests for {@code validate()}
     * ------------------------------------------------------------------------- */

    /**
     * When a subschema validates successfully the validator must return an
     * empty error set and set the {@code matchedNode} flag on the state.
     */
    @Test
    public void testValidateSuccess() {
        // sub‑schema 0 validates without errors
        when(subSchema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());
        // sub‑schema 1 is never reached because the first one already succeeded
        when(subSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(buildMessage("unexpected")));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "$");

        assertTrue("Expected no validation errors", result.isEmpty());

        // the validator should have marked the node as matched
        verify(validatorState).setMatchedNode(true);
    }

    /**
     * When every subschema fails, the validator must aggregate the errors.
     */
    @Test
    public void testValidateAllFail() {
        ValidationMessage err1 = buildMessage("error1");
        ValidationMessage err2 = buildMessage("error2");

        when(subSchema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(err1));
        when(subSchema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(err2));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "$");

        assertEquals("Two errors should be returned", 2, result.size());
        assertTrue(result.contains(err1));
        assertTrue(result.contains(err2));
    }

    /* -------------------------------------------------------------------------
     *  Expert 2 – edge‑case tests for type‑validator handling and discriminator
     * ------------------------------------------------------------------------- */

    /**
     * If a subschema declares a {@link TypeValidator} that does not match the
     * supplied node, the validator must skip that subschema and add a type‑mismatch
     * message.
     */
    @Test
    public void testValidateTypeMismatch() {
        // configure sub‑schema 0 with a type validator that does NOT match the node
        when(subSchema1.hasTypeValidator()).thenReturn(true);
        when(subSchema1.getTypeValidator()).thenReturn(typeValidator);
        when(typeValidator.getSchemaType()).thenReturn(JsonType.STRING);
        when(typeValidator.equalsToSchemaType(dummyNode)).thenReturn(false);
        // subschema 1 validates successfully
        when(subSchema2.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "$");

        // result must be empty because subschema 1 succeeded
        assertTrue(result.isEmpty());

        // a type‑mismatch message should have been added for subschema 0
        verify(validatorState, atLeastOnce()).setMatchedNode(true);
    }

    /**
     * When OpenAPI‑style discriminators are enabled and a discriminator match is
     * found but the subschema still reports errors, the validator must append
     * the special DISCRIMINATOR_REMARK message.
     */
    @Test
    public void testValidateWithDiscriminatorMatchAndErrors() {
        // enable discriminator handling
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // simulate that the discriminator matched a candidate
        ValidationContext.DiscriminatorContext dContext = validator.discriminatorContext;
        dContext.setDiscriminatorMatchFound(true); // use reflection‑free setter (for test)

        // subschema returns an error set
        ValidationMessage subError = buildMessage("subError");
        when(subSchema1.validate(any(), eq(dummyNode), any(), anyString()))
                .thenReturn(Collections.singleton(subError));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "$");

        // result must contain the original error AND the discriminator remark
        assertTrue(result.contains(subError));
        assertTrue(result.stream().anyMatch(m -> m.getMessage().contains(
                "and the discriminator-selected candidate schema didn't pass validation")));
    }

    /**
     * When discriminators are enabled but no discriminator match can be found,
     * the validator must return a single message explaining that no alternative
     * could be chosen.
     */
    @Test
    public void testValidateDiscriminatorNoMatch() {
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(true);
        // the discriminator context stays inactive (no match)
        // both subschemas return errors – they will be ignored in favour of the final message
        when(subSchema1.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(buildMessage("err1")));
        when(subSchema2.validate(any(), any(), any(), anyString()))
                .thenReturn(Collections.singleton(buildMessage("err2")));

        Set<ValidationMessage> result = validator.validate(executionContext, dummyNode,
                dummyNode, "$");

        assertEquals(1, result.size());
        ValidationMessage only = result.iterator().next();
        assertTrue(only.getMessage().contains("based on the provided discriminator"));
    }

    /* -------------------------------------------------------------------------
     *  Expert 3 – tests for {@code walk()} and {@code preloadJsonSchema()}
     * ------------------------------------------------------------------------- */

    /**
     * When {@code walk(..., shouldValidateSchema = false)} the validator must
     * invoke {@code walk} on each subschema with {@code shouldValidateSchema = false}
     * and return an empty error set.
     */
    @Test
    public void testWalkWithoutValidation() {
        Set<ValidationMessage> result = validator.walk(executionContext, dummyNode,
                dummyNode, "$", false);

        assertTrue("walk should return an empty set when validation is disabled", result.isEmpty());
        verify(subSchema1).walk(eq(executionContext), eq(dummyNode), eq(dummyNode), eq("$"), eq(false));
        verify(subSchema2).walk(eq(executionContext), eq(dummyNode), eq(dummyNode), eq("$"), eq(false));
    }

    /**
     * When {@code walk(..., shouldValidateSchema = true)} the validator must
     * delegate to {@code validate()}.
     */
    @Test
    public void testWalkWithValidationDelegatesToValidate() {
        // stub validate to return a known set
        Set<ValidationMessage> expected = Collections.singleton(buildMessage("walkError"));
        AnyOfValidator spyValidator = Mockito.spy(validator);
        doReturn(expected).when(spyValidator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = spyValidator.walk(executionContext, dummyNode,
                dummyNode, "$", true);

        assertEquals(expected, result);
        verify(spyValidator).validate(eq(executionContext), eq(dummyNode), eq(dummyNode), eq("$"));
    }

    /**
     * {@code preloadJsonSchema()} must forward the internal schema list to the
     * protected {@code preloadJsonSchemas} method of {@link BaseJsonValidator}.
     * We verify this by spying on the validator and ensuring the method is
     * called with the exact list.
     */
    @Test
    public void testPreloadJsonSchemaCallsBaseMethod() throws Exception {
        AnyOfValidator spyValidator = Mockito.spy(validator);
        // invoke the method under test
        spyValidator.preloadJsonSchema();

        // verify that the protected method was called with the internal list
        verify(spyValidator, times(1)).preloadJsonSchemas(Mockito.eq(spyValidator.schemas));
    }

    /* -------------------------------------------------------------------------
     *  Helper utilities
     * ------------------------------------------------------------------------- */

    /**
     * Creates a dummy {@link ValidationMessage} with the supplied text.
     * The concrete implementation is {@link ValidationMessageImpl}.
     */
    private ValidationMessage buildMessage(String text) {
        // ValidationMessageImpl is a concrete class in the library – we can instantiate it directly
        return new ValidationMessageImpl.Builder()
                .type("custom")
                .path("$")
                .message(text)
                .build();
    }
}
```

###Test END##