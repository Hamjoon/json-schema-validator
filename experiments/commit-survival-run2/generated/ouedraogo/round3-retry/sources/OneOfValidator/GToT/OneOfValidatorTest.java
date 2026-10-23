/**
 * JUnit‑4 test suite for {@link com.networknt.schema.OneOfValidator}.
 *
 * The tests are written from the perspective of three “experts”:
 * 1. **ConstructorExpert** – verifies that the constructor builds the internal list of schemas.
 * 2. **ValidateExpert** – covers the normal flow of {@code validate(...)} together with edge‑cases
 *    (no match, exactly one match, multiple matches, fail‑fast mode, walk‑enabled flag, etc.).
 * 3. **WalkAndPreloadExpert** – checks the behaviour of {@code walk(...)} and {@code preloadJsonSchema()}.
 *
 * All tests use Mockito to stub the complex collaborators (`JsonNode`, `JsonSchema`,
 * `ExecutionContext`, `CollectorContext`, `ValidatorState`, …) so that the logic inside
 * {@code OneOfValidator} can be exercised in isolation.
 */
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OneOfValidatorTest {

    /*** -----------------------------------------------------------------------
     *  Common test fixtures
     * ----------------------------------------------------------------------- */
    @Mock private ValidationContext validationContext;
    @Mock private ExecutionContext executionContext;
    @Mock private CollectorContext collectorContext;
    @Mock private Scope dynamicScope;               // used for both enter/exit scopes
    @Mock private ValidatorState validatorState;    // stored inside CollectorContext

    // The node that is being validated – its content is irrelevant for the tests.
    @Mock private JsonNode instanceNode;
    @Mock private JsonNode rootNode;
    @Mock private JsonNode schemaNode;               // the ONE‑OF node supplied to the ctor

    private static final String SCHEMA_PATH = "#/properties/foo";

    @Before
    public void setUp() {
        // CollectorContext returns a fresh Scope for every enter/exit call.
        when(collectorContext.enterDynamicScope()).thenReturn(dynamicScope);
        when(collectorContext.exitDynamicScope()).thenReturn(dynamicScope);
        // The ValidatorState is stored under a well‑known key.
        when(collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)).thenReturn(validatorState);
        // By default the validator is *not* in walk‑mode and validation is enabled.
        when(validatorState.isWalkEnabled()).thenReturn(false);
        when(validatorState.isValidationEnabled()).thenReturn(true);
        when(validatorState.hasMatchedNode()).thenReturn(true);
        when(validatorState.isMatchedNode()).thenReturn(true);

        when(executionContext.getCollectorContext()).thenReturn(collectorContext);
    }

    /*** -----------------------------------------------------------------------
     *  Helper to create a OneOfValidator with a configurable number of child schemas.
     * ----------------------------------------------------------------------- */
    private OneOfValidator createValidator(int childCount) {
        // Build a mock schemaNode that pretends to have {@code childCount} elements.
        when(schemaNode.size()).thenReturn(childCount);
        List<JsonNode> childNodes = new ArrayList<>();
        for (int i = 0; i < childCount; i++) {
            JsonNode child = mock(JsonNode.class);
            when(schemaNode.get(i)).thenReturn(child);
            childNodes.add(child);
        }

        // For each child we need validationContext.newSchema(...) to return a mock JsonSchema.
        for (int i = 0; i < childCount; i++) {
            JsonSchema mockSchema = mock(JsonSchema.class);
            when(validationContext.newSchema(eq(SCHEMA_PATH + "/" + i), eq(childNodes.get(i)), any()))
                    .thenReturn(mockSchema);
        }

        return new OneOfValidator(SCHEMA_PATH, schemaNode, mock(JsonSchema.class), validationContext);
    }

    /*** -----------------------------------------------------------------------
     *  1️⃣ ConstructorExpert – basic construction test
     * ----------------------------------------------------------------------- */
    @Test
    public void constructor_shouldCreateCorrectNumberOfChildSchemas() {
        final int CHILD_COUNT = 3;
        OneOfValidator validator = createValidator(CHILD_COUNT);
        // Use reflection to read the private «schemas» field.
        try {
            Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            List<?> schemas = (List<?>) f.get(validator);
            assertEquals("Number of child schemas should match the size of the schema node",
                    CHILD_COUNT, schemas.size());
        } catch (Exception e) {
            fail("Reflection error: " + e);
        }
    }

    /*** -----------------------------------------------------------------------
     *  2️⃣ ValidateExpert – happy path: exactly one child validates successfully
     * ----------------------------------------------------------------------- */
    @Test
    public void validate_whenExactlyOneSchemaMatches_shouldReturnNoErrors() {
        OneOfValidator validator = createValidator(2);

        // Grab the two child schemas that the constructor created.
        JsonSchema schema0 = getChildSchema(validator, 0);
        JsonSchema schema1 = getChildSchema(validator, 1);

        // First schema returns errors, second schema returns an empty set (i.e. matches).
        when(schema0.validate(any(), any(), any(), any())).thenReturn(
                Collections.singleton(new ValidationMessage("error0")));
        when(schema1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        assertTrue("When exactly one schema matches we expect an empty error set", result.isEmpty());
        // The validator must have set the matched‑node flag to true.
        verify(validatorState).setMatchedNode(true);
    }

    /*** -----------------------------------------------------------------------
     *  2️⃣ ValidateExpert – no schema matches
     * ----------------------------------------------------------------------- */
    @Test
    public void validate_whenNoSchemaMatches_shouldReturnOneOfErrorAndChildErrors() {
        OneOfValidator validator = createValidator(2);

        JsonSchema schema0 = getChildSchema(validator, 0);
        JsonSchema schema1 = getChildSchema(validator, 1);

        ValidationMessage childMsg0 = new ValidationMessage("error0");
        ValidationMessage childMsg1 = new ValidationMessage("error1");

        when(schema0.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(childMsg0));
        when(schema1.validate(any(), any(), any(), any()))
                .thenReturn(Collections.singleton(childMsg1));

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Expect ONE‑OF error (numberOfValidSchema == 0) plus the two child messages.
        assertEquals("Should contain 3 messages (one ONE‑OF + two child errors)", 3, result.size());

        // Verify that the ONE‑OF message contains the string "0".
        boolean oneOfFound = result.stream()
                .anyMatch(m -> m.getMessage().contains("0"));
        assertTrue("OneOf error message should contain the number of valid schemas (0)", oneOfFound);
    }

    /*** -----------------------------------------------------------------------
     *  2️⃣ ValidateExpert – more than one schema matches (short‑circuit)
     * ----------------------------------------------------------------------- */
    @Test
    public void validate_whenMultipleSchemasMatch_shouldReturnOneOfError() {
        OneOfValidator validator = createValidator(3);

        JsonSchema s0 = getChildSchema(validator, 0);
        JsonSchema s1 = getChildSchema(validator, 1);
        JsonSchema s2 = getChildSchema(validator, 2);

        // s0 and s1 both succeed, s2 is not even consulted (short‑circuit).
        when(s0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(s1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        // Two schemas matched → ONE‑OF error with number "2".
        assertEquals("Exactly one ONE‑OF error expected", 1, result.size());
        ValidationMessage msg = result.iterator().next();
        assertTrue("Message should indicate that 2 schemas matched", msg.getMessage().contains("2"));
    }

    /*** -----------------------------------------------------------------------
     *  2️⃣ ValidateExpert – fail‑fast mode (exception is thrown)
     * ----------------------------------------------------------------------- */
    @Test(expected = JsonSchemaException.class)
    public void validate_whenFailFastAndInvalidMatchCount_shouldThrowException() throws Exception {
        OneOfValidator validator = createValidator(2);
        // Use reflection to enable failFast (protected field in BaseJsonValidator)
        Field ff = BaseJsonValidator.class.getDeclaredField("failFast");
        ff.setAccessible(true);
        ff.setBoolean(validator, true);

        JsonSchema s0 = getChildSchema(validator, 0);
        JsonSchema s1 = getChildSchema(validator, 1);

        // Both schemas succeed → invalid count (2) → exception.
        when(s0.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());
        when(s1.validate(any(), any(), any(), any())).thenReturn(Collections.emptySet());

        validator.validate(executionContext, instanceNode, rootNode, "$");
    }

    /*** -----------------------------------------------------------------------
     *  2️⃣ ValidateExpert – walk‑enabled flag causes {@code walk} to be used
     * ----------------------------------------------------------------------- */
    @Test
    public void validate_whenWalkEnabled_shouldCallSchemaWalk() {
        OneOfValidator validator = createValidator(1);
        JsonSchema child = getChildSchema(validator, 0);

        // Enable walk mode on the validator state.
        when(validatorState.isWalkEnabled()).thenReturn(true);
        // Simulate that the walk call returns an empty set (i.e. matches).
        when(child.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.validate(executionContext, instanceNode, rootNode, "$");

        assertTrue("When the child schema walks without errors we expect no ONE‑OF error", result.isEmpty());
        verify(child).walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq("$"), eq(true));
    }

    /*** -----------------------------------------------------------------------
     *  3️⃣ WalkAndPreloadExpert – walk with shouldValidateSchema = true delegates to validate()
     * ----------------------------------------------------------------------- */
    @Test
    public void walk_whenShouldValidateSchemaTrue_shouldDelegateToValidate() {
        OneOfValidator validator = spy(createValidator(1));
        // Stub the internal validate() to return a known set.
        Set<ValidationMessage> fakeResult = Collections.singleton(new ValidationMessage("fake"));
        doReturn(fakeResult).when(validator).validate(any(), any(), any(), anyString());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "$", true);
        assertEquals("walk(true) should return exactly what validate() returns", fakeResult, result);
        verify(validator).validate(executionContext, instanceNode, rootNode, "$");
    }

    /*** -----------------------------------------------------------------------
     *  3️⃣ WalkAndPreloadExpert – walk with shouldValidateSchema = false iterates over children
     * ----------------------------------------------------------------------- */
    @Test
    public void walk_whenShouldValidateSchemaFalse_shouldInvokeChildWalks() {
        OneOfValidator validator = createValidator(2);
        JsonSchema s0 = getChildSchema(validator, 0);
        JsonSchema s1 = getChildSchema(validator, 1);

        // Child walks return empty sets – we only care that they are called.
        when(s0.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());
        when(s1.walk(any(), any(), any(), any(), anyBoolean())).thenReturn(Collections.emptySet());

        Set<ValidationMessage> result = validator.walk(executionContext, instanceNode, rootNode, "$", false);
        assertTrue("walk(false) should return an empty set when children return no messages", result.isEmpty());

        verify(s0).walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq("$"), eq(false));
        verify(s1).walk(eq(executionContext), eq(instanceNode), eq(rootNode), eq("$"), eq(false));
    }

    /*** -----------------------------------------------------------------------
     *  3️⃣ WalkAndPreloadExpert – preloadJsonSchema should initialise each child
     * ----------------------------------------------------------------------- */
    @Test
    public void preloadJsonSchema_shouldInitialiseAllChildSchemas() {
        OneOfValidator validator = createValidator(3);
        JsonSchema s0 = getChildSchema(validator, 0);
        JsonSchema s1 = getChildSchema(validator, 1);
        JsonSchema s2 = getChildSchema(validator, 2);

        validator.preloadJsonSchema();

        verify(s0).initializeValidators();
        verify(s1).initializeValidators();
        verify(s2).initializeValidators();
    }

    /*** -----------------------------------------------------------------------
     *  Utility method – retrieve a child schema from the private «schemas» list
     * ----------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private JsonSchema getChildSchema(OneOfValidator validator, int index) {
        try {
            Field f = OneOfValidator.class.getDeclaredField("schemas");
            f.setAccessible(true);
            List<JsonSchema> list = (List<JsonSchema>) f.get(validator);
            return list.get(index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
