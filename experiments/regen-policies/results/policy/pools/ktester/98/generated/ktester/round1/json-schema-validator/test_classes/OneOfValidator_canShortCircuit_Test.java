package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_canShortCircuit_Test {

    /**
     * Mocks used in every test
     */
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    // the schema that contains the OneOfValidator
    @Mock
    JsonSchema parentSchema;

    /**
     * The OneOfValidator instance created for each test
     */
    OneOfValidator validator;

    /**
     * Helper to create a fresh validator with a given list of child validators
     */
    private OneOfValidator createValidatorWithChildValidators(List<JsonValidator> childValidators) {
        // The schema node passed to the constructor must be an array node (the "oneOf" array)
        JsonNode oneOfArrayNode = JsonNodeFactory.instance.arrayNode();
        // Build the real OneOfValidator (its constructor does not touch the parent schema's validators)
        OneOfValidator realValidator = new OneOfValidator(schemaLocation, evaluationPath, oneOfArrayNode, parentSchema, validationContext);
        // Spy the instance so we can stub the protected method getEvaluationParentSchema()
        OneOfValidator spyValidator = spy(realValidator);
        // When the validator asks for its evaluation parent schema, return the mock that we control
        doReturn(parentSchema).when(spyValidator).getEvaluationParentSchema();
        // Stub the parent schema to return the supplied child validators
        when(parentSchema.getValidators()).thenReturn(childValidators);
        return spyValidator;
    }

    /**
     * Utility to build a list of mock validators from a list of keyword strings.
     */
    private static List<JsonValidator> buildValidatorsFromKeywords(List<String> keywords) {
        List<JsonValidator> list = new ArrayList<>();
        for (String kw : keywords) {
            JsonValidator v = mock(JsonValidator.class);
            when(v.getKeyword()).thenReturn(kw);
            list.add(v);
        }
        return list;
    }

    private static Stream<Arguments> initialEvaluationProvider() {
        return Stream.of(// no disallowed keywords → true
        Arguments.of(List.of("type", "properties"), true), // contains unevaluatedProperties → false
        Arguments.of(List.of("unevaluatedProperties"), false), // contains unevaluatedItems → false
        Arguments.of(List.of("unevaluatedItems"), false), // both disallowed keywords → false (first match already forces false)
        Arguments.of(List.of("unevaluatedProperties", "unevaluatedItems"), false));
    }

    private static Stream<Arguments> nonTargetProvider() {
        return Stream.of(// only unrelated keywords → true
        Arguments.of(List.of("type", "minimum", "maxLength"), true), // mix with unevaluatedProperties → false
        Arguments.of(List.of("type", "unevaluatedProperties", "maximum"), false), // mix with unevaluatedItems → false
        Arguments.of(List.of("unevaluatedItems", "format", "pattern"), false));
    }

    @Test
    void canShortCircuit_whenNoUnevaluatedValidators_returnsTrue() {
        // Arrange: two dummy validators that do NOT have the keywords that disable short‑circuiting
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("type");
        JsonValidator v2 = mock(JsonValidator.class);
        when(v2.getKeyword()).thenReturn("properties");
        List<JsonValidator> childValidators = new ArrayList<>();
        Collections.addAll(childValidators, v1, v2);
        validator = createValidatorWithChildValidators(childValidators);
        // Act
        boolean result = validator.canShortCircuit();
        // Assert
        assertTrue(result, "canShortCircuit should be true when no unevaluated* validators are present");
    }

    @Test
    void canShortCircuit_whenUnevaluatedPropertiesValidatorPresent_returnsFalse() {
        // Arrange: include a validator with the keyword "unevaluatedProperties"
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("type");
        JsonValidator v2 = mock(JsonValidator.class);
        // disables short‑circuit
        when(v2.getKeyword()).thenReturn("unevaluatedProperties");
        List<JsonValidator> childValidators = new ArrayList<>();
        Collections.addAll(childValidators, v1, v2);
        validator = createValidatorWithChildValidators(childValidators);
        // Act
        boolean result = validator.canShortCircuit();
        // Assert
        assertFalse(result, "canShortCircuit should be false when an unevaluatedProperties validator is present");
    }

    @Test
    void canShortCircuit_whenUnevaluatedItemsValidatorPresent_returnsFalse() {
        // Arrange: include a validator with the keyword "unevaluatedItems"
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("properties");
        JsonValidator v2 = mock(JsonValidator.class);
        // disables short‑circuit
        when(v2.getKeyword()).thenReturn("unevaluatedItems");
        List<JsonValidator> childValidators = new ArrayList<>();
        Collections.addAll(childValidators, v1, v2);
        validator = createValidatorWithChildValidators(childValidators);
        // Act
        boolean result = validator.canShortCircuit();
        // Assert
        assertFalse(result, "canShortCircuit should be false when an unevaluatedItems validator is present");
    }

    @Test
    void canShortCircuit_cachesResult_onSubsequentCalls() {
        // Arrange: first call should compute the value, second call should return cached value
        JsonValidator v1 = mock(JsonValidator.class);
        // forces false
        when(v1.getKeyword()).thenReturn("unevaluatedProperties");
        List<JsonValidator> childValidators = Collections.singletonList(v1);
        validator = createValidatorWithChildValidators(childValidators);
        // Act: first call
        boolean first = validator.canShortCircuit();
        // Change the parent schema's validators after the first call to see if caching works
        JsonValidator v2 = mock(JsonValidator.class);
        when(v2.getKeyword()).thenReturn("type");
        when(parentSchema.getValidators()).thenReturn(Collections.singletonList(v2));
        // Act: second call – should return the cached result (false) despite the change above
        boolean second = validator.canShortCircuit();
        // Assert
        assertFalse(first, "First evaluation should be false due to unevaluatedProperties");
        assertFalse(second, "Second evaluation should return the cached false value");
    }

    @ParameterizedTest(name = "{index}: keywords={0} => expected={1}")
    @MethodSource("initialEvaluationProvider")
    void canShortCircuit_initialEvaluation(List<String> keywords, boolean expected) {
        List<JsonValidator> childValidators = buildValidatorsFromKeywords(keywords);
        validator = createValidatorWithChildValidators(childValidators);
        boolean result = validator.canShortCircuit();
        assertEquals(expected, result, "Unexpected result for keywords " + keywords);
    }

    @Test
    void canShortCircuit_cachedValue_true() throws Exception {
        // create a validator with any child list – it will never be consulted
        validator = createValidatorWithChildValidators(Collections.emptyList());
        // force the private cache to true via reflection
        Field f = OneOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        f.set(validator, Boolean.TRUE);
        // the parent schema should NOT be queried – verify no interaction
        verify(parentSchema, never()).getValidators();
        assertTrue(validator.canShortCircuit(), "When cached as true, canShortCircuit should return true without recomputation");
    }

    @Test
    void canShortCircuit_cachedValue_false() throws Exception {
        validator = createValidatorWithChildValidators(Collections.emptyList());
        Field f = OneOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        f.set(validator, Boolean.FALSE);
        verify(parentSchema, never()).getValidators();
        assertFalse(validator.canShortCircuit(), "When cached as false, canShortCircuit should return false without recomputation");
    }

    @Test
    void canShortCircuit_emptyValidatorList_returnsTrue() {
        validator = createValidatorWithChildValidators(Collections.emptyList());
        assertTrue(validator.canShortCircuit(), "Empty validator list should result in true (no disallowed keywords)");
    }

    @ParameterizedTest(name = "{index}: keywords={0} => expected={1}")
    @MethodSource("nonTargetProvider")
    void canShortCircuit_nonTargetValidators(List<String> keywords, boolean expected) {
        validator = createValidatorWithChildValidators(buildValidatorsFromKeywords(keywords));
        assertEquals(expected, validator.canShortCircuit(), "Result mismatch for keyword list: " + keywords);
    }

    @Test
    void canShortCircuit_duplicateDisallowedKeywords_returnsFalse() {
        List<String> keywords = List.of("unevaluatedProperties", "unevaluatedProperties", "unevaluatedItems");
        validator = createValidatorWithChildValidators(buildValidatorsFromKeywords(keywords));
        assertFalse(validator.canShortCircuit(), "Presence of any disallowed keyword (even duplicated) must yield false");
    }

    @Test
    void canShortCircuit_cachesResult_onSubsequentCalls_true() {
        // first call computes true
        validator = createValidatorWithChildValidators(buildValidatorsFromKeywords(List.of("type", "minimum")));
        boolean first = validator.canShortCircuit();
        // mutate parent schema after first evaluation
        when(parentSchema.getValidators()).thenReturn(buildValidatorsFromKeywords(List.of("unevaluatedItems")));
        // second call should return cached true
        boolean second = validator.canShortCircuit();
        assertTrue(first, "First evaluation should be true");
        assertTrue(second, "Second evaluation must return cached true");
    }

    @Test
    void canShortCircuit_cachesResult_onSubsequentCalls_false() {
        // first call computes false
        validator = createValidatorWithChildValidators(buildValidatorsFromKeywords(List.of("unevaluatedItems", "type")));
        boolean first = validator.canShortCircuit();
        // change parent schema after first evaluation
        when(parentSchema.getValidators()).thenReturn(buildValidatorsFromKeywords(List.of("type", "minimum")));
        boolean second = validator.canShortCircuit();
        assertFalse(first, "First evaluation should be false");
        assertFalse(second, "Second evaluation must return cached false");
    }

    @Test
    void canShortCircuit_whenAlreadyCached_true_doesNotInvokeValidators() throws Exception {
        // validator that would throw if getKeyword() is called
        JsonValidator dangerous = mock(JsonValidator.class);
        when(dangerous.getKeyword()).thenThrow(new AssertionError("Should not be called"));
        validator = createValidatorWithChildValidators(List.of(dangerous));
        // pre‑set cache to true
        Field f = OneOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        f.set(validator, Boolean.TRUE);
        assertTrue(validator.canShortCircuit(), "When cached true, method must return true without touching validators");
    }

    @Test
    void canShortCircuit_whenAlreadyCached_false_doesNotInvokeValidators() throws Exception {
        JsonValidator dangerous = mock(JsonValidator.class);
        when(dangerous.getKeyword()).thenThrow(new AssertionError("Should not be called"));
        validator = createValidatorWithChildValidators(List.of(dangerous));
        // pre‑set cache to false
        Field f = OneOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        f.set(validator, Boolean.FALSE);
        assertFalse(validator.canShortCircuit(), "When cached false, method must return false without touching validators");
    }

    @Test
    void canShortCircuit_nullParentSchema_throwsNPE() {
        // Build a real validator but stub getEvaluationParentSchema() to return null
        JsonNode oneOfArrayNode = JsonNodeFactory.instance.arrayNode();
        OneOfValidator real = new OneOfValidator(schemaLocation, evaluationPath, oneOfArrayNode, parentSchema, validationContext);
        OneOfValidator spy = spy(real);
        doReturn(null).when(spy).getEvaluationParentSchema();
        assertThrows(NullPointerException.class, spy::canShortCircuit, "Calling canShortCircuit with a null parent schema should throw NPE");
    }

    @Test
    void canShortCircuit_nullValidatorsList_throwsNPE() {
        // parentSchema.getValidators() returns null
        when(parentSchema.getValidators()).thenReturn(null);
        // list ignored because stub will return null
        validator = createValidatorWithChildValidators(Collections.emptyList());
        assertThrows(NullPointerException.class, validator::canShortCircuit, "Null validators list should cause NPE during iteration");
    }

    @Test
    void canShortCircuit_nullValidatorInList_throwsNPE() {
        // List containing a null element
        List<JsonValidator> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        validator = createValidatorWithChildValidators(listWithNull);
        assertThrows(NullPointerException.class, validator::canShortCircuit, "Null element in validators list should cause NPE when dereferenced");
    }

    @Test
    void canShortCircuit_validatorKeywordThrows_propagatesException() {
        JsonValidator exploding = mock(JsonValidator.class);
        when(exploding.getKeyword()).thenThrow(new RuntimeException("boom"));
        validator = createValidatorWithChildValidators(List.of(exploding));
        RuntimeException ex = assertThrows(RuntimeException.class, validator::canShortCircuit, "Exception from getKeyword should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
