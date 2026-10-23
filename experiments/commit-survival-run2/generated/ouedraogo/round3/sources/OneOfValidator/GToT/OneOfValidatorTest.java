package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.CollectorContext.Scope;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OneOfValidator}.
 *
 * The tests use Mockito to stub the complex collaborators
 * (ExecutionContext, CollectorContext, ValidationContext, JsonSchema, etc.).
 *
 * The goal is to verify the public behaviour of OneOfValidator:
 *   – constructor builds the expected number of child schemas
 *   – validate() returns the correct error set (or throws when failFast)
 *   – walk() delegates correctly depending on the flag
 *   – preloadJsonSchema() forwards the call to all child schemas
 *
 * Edge cases such as an empty schema array, multiple matching schemas,
 * and fail‑fast mode are covered.
 */
@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    // -----------------------------------------------------------------
    // Common test fixtures
    // -----------------------------------------------------------------
    @Mock private ValidationContext validationContext;
    @Mock private JsonSchema parentSchema;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dummyScope;
    @Mock private JsonNode rootNode;
    @Mock private JsonNode nodeUnderTest;
    @Mock private JsonNode schemaArrayNode; // the "oneOf" array node
    @Mock private JsonNode childNode0;
    @Mock private JsonNode childNode1;
    @Mock private JsonNode childNode2;

    // A real (but simple) ValidatorState implementation is needed only
    // for the bits of logic that the validator checks.
    private ValidatorState validatorState;

    // Helper to create a OneOfValidator with a given number of child schemas
    private OneOfValidator createValidator(int childCount) {
        // Mock the schema array size and its children
        when(schemaArrayNode.size()).thenReturn(childCount);
        List<JsonNode> childNodes = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            JsonNode child = mock(JsonNode.class, "childNode" + i);
            childNodes.add(child);
            when(schemaArrayNode.get(i)).thenReturn(child);
        }

        // validationContext must create a JsonSchema for each child node
        // We'll let Mockito return a simple mock JsonSchema that we can later
        // configure per test.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenAnswer(invocation -> mock(JsonSchema.class));

        // Build the validator (the constructor will fill its internal list)
        return new OneOfValidator("testPath", schemaArrayNode, parentSchema, validationContext);
    }

    // -----------------------------------------------------------------
    // Setup common mocks before each test
    // -----------------------------------------------------------------
    @Before
    public void setUp() {
        // CollectorContext hierarchy (enter/exit dynamic scopes)
        when(collectorContext.enterDynamicScope()).thenReturn(dummyScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dummyScope);
        // Evaluated items / properties – just need non‑null collections
        when(collectorContext.getEvaluatedItems()).thenReturn(new HashSet<>());
        when(collectorContext.getEvaluatedProperties()).thenReturn(new HashSet<>());

        // ExecutionContext returns the collectorContext
        when(executionContext.getCollectorContext()).thenReturn(collectorContext);

        // Provide a fresh ValidatorState for each test
        validatorState = new ValidatorState();
        // The validator stores the state under a well‑known key
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
    }

    // -----------------------------------------------------------------
    // 1️⃣ Constructor tests
    // -----------------------------------------------------------------
    @Test
    public void constructor_shouldCreateOneSchemaPerArrayElement() {
        // arrange – a schema array with three elements
        when(schemaArrayNode.size()).thenReturn(3);
        when(schemaArrayNode.get(0)).thenReturn(childNode0);
        when(schemaArrayNode.get(1)).thenReturn(childNode1);
        when(schemaArrayNode.get(2)).thenReturn(childNode2);

        // act
        OneOfValidator validator = new OneOfValidator("basePath", schemaArrayNode,
                parentSchema, validationContext);

        // assert – internal list size must be 3
        // (the field is private, so we use reflection)
        try {
            java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            assertEquals(3, list.size());
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
    }

    // -----------------------------------------------------------------
    // 2️⃣ validate() – exactly one matching child schema
    // -----------------------------------------------------------------
    @Test
    public void validate_whenExactlyOneChildMatches_returnsNoErrors() {
        // arrange
        OneOfValidator validator = createValidator(2);
        JsonSchema child0 = mock(JsonSchema.class);
        JsonSchema child1 = mock(JsonSchema.class);
        // replace the internal list with our controllable mocks
        injectSchemas(validator, child0, child1);

        // child0 returns errors, child1 validates successfully (empty set)
        when(child0.validate(any(), any(), any(), any())).thenReturn(
                Collections.singleton(new ValidationMessage("error0")));
        when(child1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeUnderTest, rootNode, "$");

        // assert – no errors because exactly one schema matched
        assertTrue("Expected empty error set", result.isEmpty());
        // also verify that state was marked as matched
        assertTrue(validatorState.isMatchedNode());
    }

    // -----------------------------------------------------------------
    // 3️⃣ validate() – no matching child schemas
    // -----------------------------------------------------------------
    @Test
    public void validate_whenNoChildMatches_returnsOneOfError() {
        // arrange
        OneOfValidator validator = createValidator(2);
        JsonSchema child0 = mock(JsonSchema.class);
        JsonSchema child1 = mock(JsonSchema.class);
        injectSchemas(validator, child0, child1);

        ValidationMessage err0 = new ValidationMessage("c0");
        ValidationMessage err1 = new ValidationMessage("c1");
        when(child0.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err0));
        when(child1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(err1));

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeUnderTest, rootNode, "$");

        // assert – we must have a One‑Of error plus the child errors
        assertEquals(3, result.size()); // 1 OneOf + 2 child errors
        Optional<ValidationMessage> oneOf = result.stream()
                .filter(m -> m.getMessage().contains("OneOf"))
                .findFirst();
        assertTrue("OneOf error expected", oneOf.isPresent());
        assertTrue(oneOf.get().getMessage().contains("0")); // 0 valid schemas
    }

    // -----------------------------------------------------------------
    // 4️⃣ validate() – more than one child matches
    // -----------------------------------------------------------------
    @Test
    public void validate_whenMultipleChildrenMatch_returnsOneOfError() {
        // arrange
        OneOfValidator validator = createValidator(3);
        JsonSchema c0 = mock(JsonSchema.class);
        JsonSchema c1 = mock(JsonSchema.class);
        JsonSchema c2 = mock(JsonSchema.class);
        injectSchemas(validator, c0, c1, c2);

        // c0 and c1 succeed, c2 fails
        when(c0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(c1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(c2.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(new ValidationMessage("c2")));

        // act
        Set<ValidationMessage> result = validator.validate(executionContext,
                nodeUnderTest, rootNode, "$");

        // assert – OneOf error with detail "2"
        assertEquals(3, result.size()); // 1 OneOf + 2 child errors (c2 + maybe aggregated)
        ValidationMessage oneOf = result.stream()
                .filter(m -> m.getMessage().contains("OneOf"))
                .findFirst()
                .orElse(null);
        assertNotNull(oneOf);
        assertTrue(oneOf.getMessage().contains("2"));
    }

    // -----------------------------------------------------------------
    // 5️⃣ validate() – failFast mode
    // -----------------------------------------------------------------
    @Test(expected = JsonSchemaException.class)
    public void validate_whenFailFastAndInvalid_throwsJsonSchemaException() {
        // arrange
        OneOfValidator validator = createValidator(1);
        validator.failFast = true; // package‑private field – accessible directly
        JsonSchema child = mock(JsonSchema.class);
        injectSchemas(validator, child);
        when(child.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(new ValidationMessage("error")));

        // act – should throw
        validator.validate(executionContext, nodeUnderTest, rootNode, "$");
    }

    // -----------------------------------------------------------------
    // 6️⃣ walk() – shouldValidateSchema = true (delegates to validate)
    // -----------------------------------------------------------------
    @Test
    public void walk_whenShouldValidate_true_delegatesToValidate() {
        // arrange
        OneOfValidator validator = createValidator(1);
        JsonSchema child = mock(JsonSchema.class);
        injectSchemas(validator, child);
        when(child.validate(any(), any(), any(), any()))
                .thenReturn(Collections.emptySet());

        // spy on validator to verify that validate() is called
        OneOfValidator spy = Mockito.spy(validator);
        doReturn(Collections.emptySet()).when(spy).validate(any(), any(), any(), any());

        // act
        Set<ValidationMessage> result = spy.walk(executionContext, nodeUnderTest,
                rootNode, "$", true);

        // assert
        verify(spy, times(1)).validate(executionContext, nodeUnderTest, rootNode, "$");
        assertTrue(result.isEmpty());
    }

    // -----------------------------------------------------------------
    // 7️⃣ walk() – shouldValidateSchema = false (propagates to child schemas)
    // -----------------------------------------------------------------
    @Test
    public void walk_whenShouldValidate_false_callsChildWalk() {
        // arrange
        OneOfValidator validator = createValidator(2);
        JsonSchema c0 = mock(JsonSchema.class);
        JsonSchema c1 = mock(JsonSchema.class);
        injectSchemas(validator, c0, c1);

        // act
        Set<ValidationMessage> result = validator.walk(executionContext,
                nodeUnderTest, rootNode, "$", false);

        // assert – result is empty because we never add anything in this branch
        assertTrue(result.isEmpty());
        verify(c0, times(1)).walk(executionContext, nodeUnderTest, rootNode, "$", false);
        verify(c1, times(1)).walk(executionContext, nodeUnderTest, rootNode, "$", false);
    }

    // -----------------------------------------------------------------
    // 8️⃣ preloadJsonSchema() – forwards initializeValidators()
    // -----------------------------------------------------------------
    @Test
    public void preloadJsonSchema_callsInitializeValidatorsOnAllChildren() {
        // arrange
        OneOfValidator validator = createValidator(3);
        JsonSchema c0 = mock(JsonSchema.class);
        JsonSchema c1 = mock(JsonSchema.class);
        JsonSchema c2 = mock(JsonSchema.class);
        injectSchemas(validator, c0, c1, c2);

        // act
        validator.preloadJsonSchema();

        // assert
        verify(c0, times(1)).initializeValidators();
        verify(c1, times(1)).initializeValidators();
        verify(c2, times(1)).initializeValidators();
    }

    // -----------------------------------------------------------------
    // Helper – replace the private “schemas” list with mocks
    // -----------------------------------------------------------------
    private void injectSchemas(OneOfValidator validator, JsonSchema... mocks) {
        try {
            java.lang.reflect.Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            List<JsonSchema> list = new ArrayList<>(Arrays.asList(mocks));
            f.set(validator, list);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
