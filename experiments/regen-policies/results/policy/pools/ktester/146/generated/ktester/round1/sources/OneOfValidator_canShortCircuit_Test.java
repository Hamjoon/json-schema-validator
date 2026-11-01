package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_canShortCircuit_Test {

    @Mock
    ValidationContext // not used by the method under test
    validationContext;

    @Mock
    JsonSchema // not used by the method under test
    parentSchema;

    @Mock
    SchemaLocation // not used by the method under test
    schemaLocation;

    @Mock
    JsonNodePath // not used by the method under test
    evaluationPath;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Helper subclass that makes {@code canShortCircuit()} public and injects a
     *  controllable evaluation‑parent schema.
     */
    private static class TestableOneOfValidator extends OneOfValidator {

        private final JsonSchema evaluationParent;

        TestableOneOfValidator(JsonSchema evaluationParent) {
            // The constructor of {@link OneOfValidator} expects several arguments.
            // We pass dummy values for everything except the evaluation‑parent.
            super(schemaLocation, evaluationPath, // schemaNode – not used by canShortCircuit()
            MAPPER.createObjectNode(), parentSchema, validationContext);
            this.evaluationParent = evaluationParent;
        }

        @Override
        protected JsonSchema getEvaluationParentSchema() {
            return evaluationParent;
        }

        /**
         * Exposes the protected method for testing.
         */
        public boolean invokeCanShortCircuit() {
            return super.canShortCircuit();
        }
    }

    /**
     * Variant that forces {@code getEvaluationParentSchema()} to return {@code null}.
     */
    private static class NullParentValidator extends OneOfValidator {

        NullParentValidator() {
            super(schemaLocation, evaluationPath, MAPPER.createObjectNode(), parentSchema, validationContext);
        }

        @Override
        protected JsonSchema getEvaluationParentSchema() {
            return null;
        }

        public boolean invokeCanShortCircuit() {
            return super.canShortCircuit();
        }
    }

    /**
     * Helper iterable that throws {@link ConcurrentModificationException}
     * on the second call to {@code next()}.
     */
    private static class CmeIterable implements Iterable<JsonValidator> {

        private final List<JsonValidator> backing = new ArrayList<>();

        CmeIterable(JsonValidator first) {
            backing.add(first);
        }

        @Override
        public Iterator<JsonValidator> iterator() {
            return new Iterator<>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < backing.size();
                }

                @Override
                public JsonValidator next() {
                    if (index == 1) {
                        // Simulate structural change after iterator creation
                        throw new ConcurrentModificationException("simulated");
                    }
                    JsonValidator v = backing.get(index);
                    // On first element we mutate the list to trigger CME on next()
                    if (index == 0) {
                        backing.add(mock(JsonValidator.class));
                    }
                    index++;
                    return v;
                }
            };
        }
    }

    /**
     * Creates a validator instance that uses an empty “oneOf” array (size 0) so
     *  the constructor does not try to create sub‑schemas.
     */
    private TestableOneOfValidator createValidator(JsonSchema evaluationParent) {
        // schemaNode for “oneOf”
        ArrayNode emptyArray = MAPPER.createArrayNode();
        return new TestableOneOfValidator(schemaLocation, evaluationPath, emptyArray, parentSchema, validationContext, evaluationParent);
    }

    @Test
    void canShortCircuitReturnsTrueWhenNoUnevaluatedValidators() {
        // arrange
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator otherValidator = mock(JsonValidator.class);
        // any non‑unevaluated keyword
        when(otherValidator.getKeyword()).thenReturn("type");
        when(evalParent.getValidators()).thenReturn(List.of(otherValidator));
        TestableOneOfValidator validator = createValidator(evalParent);
        // act
        boolean result = validator.invokeCanShortCircuit();
        // assert
        assertTrue(result, "canShortCircuit should be true when no unevaluated* validators are present");
    }

    @Test
    void canShortCircuitReturnsFalseWhenUnevaluatedPropertiesValidatorPresent() {
        // arrange
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator unevaluatedProp = mock(JsonValidator.class);
        when(unevaluatedProp.getKeyword()).thenReturn("unevaluatedProperties");
        when(evalParent.getValidators()).thenReturn(List.of(unevaluatedProp));
        TestableOneOfValidator validator = createValidator(evalParent);
        // act
        boolean result = validator.invokeCanShortCircuit();
        // assert
        assertFalse(result, "canShortCircuit should be false when an unevaluatedProperties validator is present");
    }

    @Test
    void canShortCircuitReturnsFalseWhenUnevaluatedItemsValidatorPresent() {
        // arrange
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator unevaluatedItems = mock(JsonValidator.class);
        when(unevaluatedItems.getKeyword()).thenReturn("unevaluatedItems");
        when(evalParent.getValidators()).thenReturn(List.of(unevaluatedItems));
        TestableOneOfValidator validator = createValidator(evalParent);
        // act
        boolean result = validator.invokeCanShortCircuit();
        // assert
        assertFalse(result, "canShortCircuit should be false when an unevaluatedItems validator is present");
    }

    @Test
    void canShortCircuitResultIsCached() {
        // arrange
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("type");
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        // act – call twice
        boolean first = validator.invokeCanShortCircuit();
        boolean second = validator.invokeCanShortCircuit();
        // assert – both calls return the same value and the parent schema is queried only once
        assertTrue(first && second, "cached result should be true");
        verify(evalParent, times(1)).getValidators();
    }

    @Test
    void canShortCircuitReturnsFalseWhenMultipleValidatorsIncludingDisallowed() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator ok1 = mock(JsonValidator.class);
        JsonValidator ok2 = mock(JsonValidator.class);
        JsonValidator bad = mock(JsonValidator.class);
        when(ok1.getKeyword()).thenReturn("type");
        when(ok2.getKeyword()).thenReturn("minimum");
        when(bad.getKeyword()).thenReturn("unevaluatedProperties");
        when(evalParent.getValidators()).thenReturn(List.of(ok1, bad, ok2));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertFalse(validator.invokeCanShortCircuit(), "presence of any disallowed keyword must make the result false");
    }

    @Test
    void canShortCircuitReturnsTrueWhenValidatorsListIsEmpty() {
        JsonSchema evalParent = mock(JsonSchema.class);
        when(evalParent.getValidators()).thenReturn(Collections.emptyList());
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "empty validator list should yield true");
    }

    @Test
    void canShortCircuitIgnoresNullKeyword() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn(null);
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "null keyword must be ignored and result stay true");
    }

    @Test
    void canShortCircuitIgnoresEmptyStringKeyword() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("");
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "empty string keyword must be ignored");
    }

    @Test
    void canShortCircuitIsCaseSensitive() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        // capital U
        when(validatorMock.getKeyword()).thenReturn("UnevaluatedProperties");
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "keyword comparison is case‑sensitive, so capitalised form is ignored");
    }

    @Test
    void canShortCircuitIgnoresSubstringMatch() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("myunevaluatedProperties");
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "keyword must match exactly; substring should not affect result");
    }

    @Test
    void canShortCircuitResultIsCachedWhenTrue() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("type");
        when(evalParent.getValidators()).thenReturn(List.of(validatorMock));
        TestableOneOfValidator validator = createValidator(evalParent);
        boolean first = validator.invokeCanShortCircuit();
        boolean second = validator.invokeCanShortCircuit();
        assertTrue(first && second, "cached true should be returned on subsequent calls");
        verify(evalParent, times(1)).getValidators();
    }

    @Test
    void canShortCircuitResultIsCachedWhenFalse() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator bad = mock(JsonValidator.class);
        when(bad.getKeyword()).thenReturn("unevaluatedItems");
        when(evalParent.getValidators()).thenReturn(List.of(bad));
        TestableOneOfValidator validator = createValidator(evalParent);
        boolean first = validator.invokeCanShortCircuit();
        boolean second = validator.invokeCanShortCircuit();
        assertFalse(first);
        assertFalse(second);
        verify(evalParent, times(1)).getValidators();
    }

    @Test
    void cachedResultRemainsTrueEvenAfterSchemaMutation() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator ok = mock(JsonValidator.class);
        when(ok.getKeyword()).thenReturn("type");
        List<JsonValidator> mutableList = new ArrayList<>(List.of(ok));
        when(evalParent.getValidators()).thenReturn(mutableList);
        TestableOneOfValidator validator = createValidator(evalParent);
        assertTrue(validator.invokeCanShortCircuit(), "initial call should be true");
        // mutate schema after caching
        JsonValidator added = mock(JsonValidator.class);
        when(added.getKeyword()).thenReturn("unevaluatedProperties");
        mutableList.add(added);
        // second call must still return the cached true
        assertTrue(validator.invokeCanShortCircuit(), "cached true must survive later mutations");
        verify(evalParent, times(1)).getValidators();
    }

    @Test
    void cachedResultRemainsFalseEvenAfterSchemaMutation() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator bad = mock(JsonValidator.class);
        when(bad.getKeyword()).thenReturn("unevaluatedItems");
        List<JsonValidator> mutableList = new ArrayList<>(List.of(bad));
        when(evalParent.getValidators()).thenReturn(mutableList);
        TestableOneOfValidator validator = createValidator(evalParent);
        assertFalse(validator.invokeCanShortCircuit(), "initial call should be false");
        // mutate schema – remove the bad validator
        mutableList.clear();
        // second call must still return the cached false
        assertFalse(validator.invokeCanShortCircuit(), "cached false must survive later mutations");
        verify(evalParent, times(1)).getValidators();
    }

    @Test
    void canShortCircuitThrowsWhenParentSchemaIsNull() {
        NullParentValidator validator = new NullParentValidator();
        assertThrows(NullPointerException.class, validator::invokeCanShortCircuit, "calling canShortCircuit when getEvaluationParentSchema() returns null should NPE");
    }

    @Test
    void canShortCircuitThrowsWhenValidatorsCollectionIsNull() {
        JsonSchema evalParent = mock(JsonSchema.class);
        // simulate null collection
        when(evalParent.getValidators()).thenReturn(null);
        TestableOneOfValidator validator = createValidator(evalParent);
        // The current implementation will NPE; the test documents that behaviour.
        assertThrows(NullPointerException.class, validator::invokeCanShortCircuit, "null validators collection should result in NPE");
    }

    @Test
    void canShortCircuitThrowsWhenValidatorElementIsNull() {
        JsonSchema evalParent = mock(JsonSchema.class);
        // List containing a single null element
        when(evalParent.getValidators()).thenReturn(Arrays.asList((JsonValidator) null));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertThrows(NullPointerException.class, validator::invokeCanShortCircuit, "null element inside validators list should cause NPE");
    }

    @Test
    void canShortCircuitPropagatesIllegalArgumentExceptionFromValidator() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator bad = mock(JsonValidator.class);
        when(bad.getKeyword()).thenThrow(new IllegalArgumentException("bad keyword"));
        when(evalParent.getValidators()).thenReturn(List.of(bad));
        TestableOneOfValidator validator = createValidator(evalParent);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, validator::invokeCanShortCircuit, "exception from validator.getKeyword() must propagate");
        assertEquals("bad keyword", ex.getMessage());
    }

    @Test
    void canShortCircuitPropagatesConcurrentModificationException() {
        JsonSchema evalParent = mock(JsonSchema.class);
        JsonValidator first = mock(JsonValidator.class);
        when(first.getKeyword()).thenReturn("type");
        when(evalParent.getValidators()).thenReturn(new CmeIterable(first));
        TestableOneOfValidator validator = createValidator(evalParent);
        assertThrows(ConcurrentModificationException.class, validator::invokeCanShortCircuit, "concurrent modification during iteration must propagate");
    }
}
