package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_canShortCircuit_2_Test {

    // ----- Mocks ------------------------------------------------------------
    @Mock
    JsonSchema parentSchemaMock;

    @Mock
    ValidationContext validationContextMock;

    @Mock
    SchemaLocation schemaLocationMock;

    @Mock
    JsonNodePath evaluationPathMock;

    // ----- System under test ------------------------------------------------
    AnyOfValidator anyOfValidator;

    @BeforeEach
    void setupBeforeEach() {
        // The validator is created fresh for each test so that the cached flag
        // does not leak between tests.
        anyOfValidator = createValidator();
    }

    // constructor expects an array node).
    private AnyOfValidator createValidator() {
        ObjectMapper mapper = new ObjectMapper();
        // [] – valid for AnyOf
        ArrayNode emptyArray = mapper.createArrayNode();
        return new AnyOfValidator(schemaLocationMock, evaluationPathMock, emptyArray, parentSchemaMock, validationContextMock);
    }

    // Helper to force a cached value via reflection (used for explicit cache tests)
    private void setCachedCanShortCircuit(boolean value) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("canShortCircuit");
        f.setAccessible(true);
        f.set(anyOfValidator, value);
    }

    @Test
    void canShortCircuit_returnsTrueWhenNoUnevaluatedKeywords() {
        // ----- Arrange -------------------------------------------------------
        JsonValidator validatorMock = mock(JsonValidator.class);
        // any non‑unevaluated keyword
        when(validatorMock.getKeyword()).thenReturn("type");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(validatorMock));
        // ----- Act -----------------------------------------------------------
        boolean firstCall = anyOfValidator.canShortCircuit();
        // ----- Assert --------------------------------------------------------
        assertTrue(firstCall, "Expected canShortCircuit() to be true when no unevaluated validators are present");
        // Verify that the parent schema was queried exactly once (caching)
        verify(parentSchemaMock, times(1)).getValidators();
        // Change the mock to return a different keyword and call again – the
        // cached value must be returned and the parent schema must NOT be queried again.
        when(parentSchemaMock.getValidators()).thenReturn(List.of(mock(JsonValidator.class, withSettings().defaultAnswer(invocation -> {
            // any call to getKeyword will now return an unevaluated keyword
            if ("getKeyword".equals(invocation.getMethod().getName())) {
                return "unevaluatedProperties";
            }
            return null;
        }))));
        boolean secondCall = anyOfValidator.canShortCircuit();
        assertTrue(secondCall, "Cached result should still be true");
        // No additional interaction with parentSchemaMock
        verifyNoMoreInteractions(parentSchemaMock);
    }

    @Test
    void canShortCircuit_returnsFalseWhenUnevaluatedKeywordPresent() {
        // ----- Arrange -------------------------------------------------------
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("unevaluatedProperties");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(validatorMock));
        // ----- Act -----------------------------------------------------------
        boolean firstCall = anyOfValidator.canShortCircuit();
        // ----- Assert --------------------------------------------------------
        assertFalse(firstCall, "Expected canShortCircuit() to be false when an unevaluated validator is present");
        // Verify single evaluation
        verify(parentSchemaMock, times(1)).getValidators();
        // Change the mock to return a safe keyword and call again – cached false must be returned.
        when(parentSchemaMock.getValidators()).thenReturn(List.of(mock(JsonValidator.class, withSettings().defaultAnswer(invocation -> {
            if ("getKeyword".equals(invocation.getMethod().getName())) {
                return "type";
            }
            return null;
        }))));
        boolean secondCall = anyOfValidator.canShortCircuit();
        assertFalse(secondCall, "Cached result should still be false");
        verifyNoMoreInteractions(parentSchemaMock);
    }

    // 2. Initial evaluation – contains "unevaluatedProperties"
    @Test
    void canShortCircuit_returnsFalseWhenUnevaluatedPropertiesKeywordPresent() {
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("unevaluatedProperties");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(validatorMock));
        boolean result = anyOfValidator.canShortCircuit();
        assertFalse(result, "Expected false when a validator has keyword unevaluatedProperties");
        verify(parentSchemaMock, times(1)).getValidators();
    }

    // 3. Initial evaluation – contains "unevaluatedItems"
    @Test
    void canShortCircuit_returnsFalseWhenUnevaluatedItemsKeywordPresent() {
        JsonValidator validatorMock = mock(JsonValidator.class);
        when(validatorMock.getKeyword()).thenReturn("unevaluatedItems");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(validatorMock));
        boolean result = anyOfValidator.canShortCircuit();
        assertFalse(result, "Expected false when a validator has keyword unevaluatedItems");
        verify(parentSchemaMock, times(1)).getValidators();
    }

    // 4. Initial evaluation – mixed validators (some blocking)
    @Test
    void canShortCircuit_detectsBlockingKeywordAmongMixedValidators() {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("type");
        // blocking
        when(v2.getKeyword()).thenReturn("unevaluatedItems");
        when(v3.getKeyword()).thenReturn("maxLength");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(v1, v2, v3));
        boolean result = anyOfValidator.canShortCircuit();
        assertFalse(result, "Mixed list containing a blocking keyword must yield false");
        verify(parentSchemaMock, times(1)).getValidators();
    }

    // 5. Initial evaluation – multiple non‑blocking validators
    @Test
    void canShortCircuit_returnsTrueWithMultipleNonBlockingValidators() {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        JsonValidator v3 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("type");
        when(v2.getKeyword()).thenReturn("minimum");
        when(v3.getKeyword()).thenReturn("format");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(v1, v2, v3));
        boolean result = anyOfValidator.canShortCircuit();
        assertTrue(result, "All non‑blocking validators should result in true");
        verify(parentSchemaMock, times(1)).getValidators();
    }

    // 6. Initial evaluation – validator with null keyword
    @Test
    void canShortCircuit_ignoresValidatorWithNullKeyword() {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        // should be ignored
        when(v1.getKeyword()).thenReturn(null);
        // non‑blocking
        when(v2.getKeyword()).thenReturn("type");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(v1, v2));
        boolean result = anyOfValidator.canShortCircuit();
        assertTrue(result, "Null keyword must be treated as non‑blocking");
        verify(parentSchemaMock, times(1)).getValidators();
    }

    // 7. Cached true persists after parent schema changes
    @Test
    void canShortCircuit_cachedTruePersistsEvenIfSchemaChanges() {
        // first call – no blocking keywords → true cached
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("type");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(v1));
        assertTrue(anyOfValidator.canShortCircuit());
        // change the mock to now return a blocking validator
        JsonValidator blocking = mock(JsonValidator.class);
        when(blocking.getKeyword()).thenReturn("unevaluatedProperties");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(blocking));
        // second call must still return true (cached) and must not query again
        assertTrue(anyOfValidator.canShortCircuit());
        // only the first call
        verify(parentSchemaMock, times(1)).getValidators();
        verifyNoMoreInteractions(parentSchemaMock);
    }

    // 8. Cached false persists after parent schema changes
    @Test
    void canShortCircuit_cachedFalsePersistsEvenIfSchemaChanges() {
        // first call – blocking keyword → false cached
        JsonValidator v1 = mock(JsonValidator.class);
        when(v1.getKeyword()).thenReturn("unevaluatedItems");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(v1));
        assertFalse(anyOfValidator.canShortCircuit());
        // modify the mock to return only safe validators
        JsonValidator safe = mock(JsonValidator.class);
        when(safe.getKeyword()).thenReturn("type");
        when(parentSchemaMock.getValidators()).thenReturn(List.of(safe));
        // second call must still return false (cached)
        assertFalse(anyOfValidator.canShortCircuit());
        // only first evaluation
        verify(parentSchemaMock, times(1)).getValidators();
        verifyNoMoreInteractions(parentSchemaMock);
    }

    // 9. Parent schema is null – should throw NPE
    @Test
    void canShortCircuit_throwsNPEWhenParentSchemaIsNull() {
        AnyOfValidator validatorWithNullParent = createValidator(null);
        assertThrows(NullPointerException.class, validatorWithNullParent::canShortCircuit, "Expected NPE when getEvaluationParentSchema() returns null");
    }

    // 10. Validators list is null – should throw NPE
    @Test
    void canShortCircuit_throwsNPEWhenValidatorsListIsNull() {
        when(parentSchemaMock.getValidators()).thenReturn(null);
        assertThrows(NullPointerException.class, anyOfValidator::canShortCircuit, "Expected NPE when parentSchema.getValidators() returns null");
    }

    // 11. Validator element in list is null – should throw NPE
    @Test
    void canShortCircuit_throwsNPEWhenValidatorElementIsNull() {
        when(parentSchemaMock.getValidators()).thenReturn(List.of((JsonValidator) null));
        assertThrows(NullPointerException.class, anyOfValidator::canShortCircuit, "Expected NPE when a validator element in the list is null");
    }

    // 12. validator.getKeyword() throws RuntimeException – should propagate
    @Test
    void canShortCircuit_propagatesRuntimeExceptionFromValidator() {
        JsonValidator badValidator = mock(JsonValidator.class);
        when(badValidator.getKeyword()).thenThrow(new RuntimeException("boom"));
        when(parentSchemaMock.getValidators()).thenReturn(List.of(badValidator));
        RuntimeException ex = assertThrows(RuntimeException.class, anyOfValidator::canShortCircuit, "RuntimeException from getKeyword() must propagate");
        assertEquals("boom", ex.getMessage());
    }

    // 13. Explicitly set cached true via reflection and verify no evaluation
    @Test
    void canShortCircuit_returnsCachedTrueWhenFieldPreSet() throws Exception {
        setCachedCanShortCircuit(true);
        // Even if parent schema would throw, the method must return cached true
        when(parentSchemaMock.getValidators()).thenThrow(new RuntimeException("should not be called"));
        assertTrue(anyOfValidator.canShortCircuit());
        // verify that getValidators() was never called
        verifyNoInteractions(parentSchemaMock);
    }

    // 14. Explicitly set cached false via reflection and verify no evaluation
    @Test
    void canShortCircuit_returnsCachedFalseWhenFieldPreSet() throws Exception {
        setCachedCanShortCircuit(false);
        when(parentSchemaMock.getValidators()).thenThrow(new RuntimeException("should not be called"));
        assertFalse(anyOfValidator.canShortCircuit());
        verifyNoInteractions(parentSchemaMock);
    }
}
