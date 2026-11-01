package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.annotation.JsonNodeAnnotation;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.function.Consumer;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_putAnnotation_Test {

    @Mock
    private SchemaLocation mockSchemaLocation;

    @Mock
    private JsonNodePath mockEvaluationPath;

    @Mock
    private JsonNode mockSchemaNode;

    @Mock
    private JsonSchema mockParentSchema;

    @Mock
    private ValidatorTypeCode mockValidatorType;

    @Mock
    private ValidationContext mockValidationContext;

    private TestValidator validator;

    /**
     * Minimal concrete validator used only for exercising {@code putAnnotation}.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parentSchema, ValidatorTypeCode validatorType, ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the putAnnotation test.
            return Collections.emptySet();
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            // The protected field {@code evaluationPath} is already set by the super‑constructor.
            return super.getEvaluationPath();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Build a concrete validator instance with mocked collaborators.
        validator = new TestValidator(mockSchemaLocation, mockEvaluationPath, mockSchemaNode, mockParentSchema, mockValidatorType, mockValidationContext);
    }

    /**
     * Verifies that {@code putAnnotation} stores a {@link JsonNodeAnnotation}
     * built by the supplied customiser into the {@link ExecutionContext}'s
     * annotation map.
     */
    @Test
    void putAnnotationAddsAnnotationToExecutionContext() {
        // GIVEN – a fresh execution context
        ExecutionContext executionContext = new ExecutionContext();
        // The annotations map is initially empty
        Map<String, JsonNodeAnnotation> before = executionContext.getAnnotations();
        assertTrue(before.isEmpty(), "Annotations map should be empty before putAnnotation");
        // WHEN – we add a custom annotation
        validator.putAnnotation(executionContext, builder -> builder.annotation("myKey", "myValue"));
        // THEN – the annotation map contains exactly one entry
        Map<String, JsonNodeAnnotation> after = executionContext.getAnnotations();
        assertEquals(1, after.size(), "Exactly one annotation should have been added");
        JsonNodeAnnotation annotation = after.values().iterator().next();
        assertNotNull(annotation, "Stored annotation must not be null");
        // The builder copies the supplied key/value into the annotation.
        // The concrete JsonNodeAnnotation API provides {@code getKey} and {@code getValue}
        // – verify that they match the customiser's input.
        assertEquals("myKey", annotation.getKey(), "Annotation key should match the customiser");
        assertEquals("myValue", annotation.getValue(), "Annotation value should match the customiser");
        // Also verify that the annotation carries the evaluation path, schema location
        // and keyword supplied by the validator.
        assertEquals(mockEvaluationPath, annotation.getEvaluationPath(), "Evaluation path should be the one from the validator");
        assertEquals(mockSchemaLocation, annotation.getSchemaLocation(), "Schema location should be the one from the validator");
        assertEquals("testKeyword", annotation.getKeyword(), "Keyword should be the one returned by getKeyword()");
    }

    /* -----------------------------------------------------------
     *  1. Default behaviour – customiser does nothing
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_defaultCustomizerDoesNothing() {
        ExecutionContext ctx = new ExecutionContext();
        // customiser that simply returns without touching the builder
        Consumer<JsonNodeAnnotation.Builder> noop = builder -> {
        };
        validator.putAnnotation(ctx, noop);
        Map<String, JsonNodeAnnotation> map = ctx.getAnnotations();
        assertEquals(1, map.size(), "Exactly one annotation should be added");
        JsonNodeAnnotation ann = map.values().iterator().next();
        assertEquals(mockEvaluationPath, ann.getEvaluationPath());
        assertEquals(mockSchemaLocation, ann.getSchemaLocation());
        assertEquals("testKeyword", ann.getKeyword());
        // No additional attributes should be present
        assertTrue(ann.getAttributes().isEmpty(), "Annotation should not contain extra attributes");
    }

    /* -----------------------------------------------------------
     *  2. Custom attribute added by the customiser
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_customAttributeIsStored() {
        ExecutionContext ctx = new ExecutionContext();
        validator.putAnnotation(ctx, b -> b.annotation("customKey", "customValue"));
        JsonNodeAnnotation ann = ctx.getAnnotations().values().iterator().next();
        assertEquals("customValue", ann.getAttributes().get("customKey"));
    }

    /* -----------------------------------------------------------
     *  3. Customiser throws an exception – it must propagate
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_customiserThrowsExceptionPropagates() {
        ExecutionContext ctx = new ExecutionContext();
        RuntimeException boom = new RuntimeException("boom");
        Consumer<JsonNodeAnnotation.Builder> exploding = b -> {
            throw boom;
        };
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> validator.putAnnotation(ctx, exploding));
        assertSame(boom, thrown);
        assertTrue(ctx.getAnnotations().isEmpty(), "Annotation map must stay empty after exception");
    }

    /* -----------------------------------------------------------
     *  4. Null ExecutionContext – NPE expected
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_nullExecutionContextThrowsNPE() {
        Consumer<JsonNodeAnnotation.Builder> dummy = b -> {
        };
        assertThrows(NullPointerException.class, () -> validator.putAnnotation(null, dummy));
    }

    /* -----------------------------------------------------------
     *  5. Overriding core fields via the customiser
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_overridesCoreFields() {
        ExecutionContext ctx = new ExecutionContext();
        // new values that the customiser will set
        JsonNodePath overriddenPath = mock(JsonNodePath.class);
        SchemaLocation overriddenLocation = mock(SchemaLocation.class);
        String overriddenKeyword = "overrideKeyword";
        validator.putAnnotation(ctx, b -> b.evaluationPath(overriddenPath).schemaLocation(overriddenLocation).keyword(overriddenKeyword));
        JsonNodeAnnotation ann = ctx.getAnnotations().values().iterator().next();
        assertEquals(overriddenPath, ann.getEvaluationPath());
        assertEquals(overriddenLocation, ann.getSchemaLocation());
        assertEquals(overriddenKeyword, ann.getKeyword());
    }

    /* -----------------------------------------------------------
     *  6. Existing annotation with same key is replaced
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_replacesExistingAnnotationWithSameKey() {
        ExecutionContext ctx = new ExecutionContext();
        // create a default annotation to obtain the default key
        JsonNodeAnnotation defaultAnn = JsonNodeAnnotation.builder().evaluationPath(mockEvaluationPath).schemaLocation(mockSchemaLocation).keyword("oldKeyword").build();
        String defaultKey = defaultAnn.getKey();
        // pre‑populate the map with the old annotation
        ctx.getAnnotations().put(defaultKey, defaultAnn);
        assertEquals(1, ctx.getAnnotations().size());
        // now put a new annotation that adds an extra attribute
        validator.putAnnotation(ctx, b -> b.annotation("newKey", "newValue"));
        // map size must stay 1 and the stored annotation must be the new one
        assertEquals(1, ctx.getAnnotations().size());
        JsonNodeAnnotation stored = ctx.getAnnotations().get(defaultKey);
        assertNotNull(stored);
        assertEquals("newValue", stored.getAttributes().get("newKey"));
        // core fields must be the validator's defaults (not the old ones)
        assertEquals(mockEvaluationPath, stored.getEvaluationPath());
        assertEquals(mockSchemaLocation, stored.getSchemaLocation());
        assertEquals("testKeyword", stored.getKeyword());
    }

    /* -----------------------------------------------------------
     *  7. Customiser invoked exactly once
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_customiserInvokedExactlyOnce() {
        ExecutionContext ctx = new ExecutionContext();
        @SuppressWarnings("unchecked")
        Consumer<JsonNodeAnnotation.Builder> spyCustomizer = mock(Consumer.class);
        doAnswer(invocation -> {
            JsonNodeAnnotation.Builder b = invocation.getArgument(0);
            b.annotation("count", "1");
            return null;
        }).when(spyCustomizer).accept(any());
        validator.putAnnotation(ctx, spyCustomizer);
        verify(spyCustomizer, times(1)).accept(any());
        JsonNodeAnnotation ann = ctx.getAnnotations().values().iterator().next();
        assertEquals("1", ann.getAttributes().get("count"));
    }

    /* -----------------------------------------------------------
     *  8. Customiser adds a null attribute value
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_customiserAddsNullAttributeValue() {
        ExecutionContext ctx = new ExecutionContext();
        validator.putAnnotation(ctx, b -> b.annotation("nullKey", null));
        JsonNodeAnnotation ann = ctx.getAnnotations().values().iterator().next();
        // The builder may keep the entry with a null value or drop it – both are acceptable.
        // We only assert that no exception was thrown and the map is still present.
        assertTrue(ann.getAttributes().containsKey("nullKey"));
        assertNull(ann.getAttributes().get("nullKey"));
    }

    /* -----------------------------------------------------------
     *  9. ExecutionContext with pre‑populated map – new entry added
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_addsToPrepopulatedAnnotationMap() {
        ExecutionContext ctx = new ExecutionContext();
        // two unrelated annotations already present
        ctx.getAnnotations().put("a1", JsonNodeAnnotation.builder().evaluationPath(mock(JsonNodePath.class)).schemaLocation(mock(SchemaLocation.class)).keyword("k1").build());
        ctx.getAnnotations().put("a2", JsonNodeAnnotation.builder().evaluationPath(mock(JsonNodePath.class)).schemaLocation(mock(SchemaLocation.class)).keyword("k2").build());
        validator.putAnnotation(ctx, b -> b.annotation("extra", "value"));
        assertEquals(3, ctx.getAnnotations().size(), "Map size should increase by one");
    }

    /* -----------------------------------------------------------
     * 10. Null customiser – NPE expected
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_nullCustomizerThrowsNPE() {
        ExecutionContext ctx = new ExecutionContext();
        assertThrows(NullPointerException.class, () -> validator.putAnnotation(ctx, null));
    }

    /* -----------------------------------------------------------
     * 11. ExecutionContext whose getAnnotations() returns null – NPE expected
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_executionContextWithNullAnnotationMapThrowsNPE() {
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getAnnotations()).thenReturn(null);
        Consumer<JsonNodeAnnotation.Builder> dummy = b -> {
        };
        assertThrows(NullPointerException.class, () -> validator.putAnnotation(ctx, dummy));
    }

    /* -----------------------------------------------------------
     * 12. Annotation map that throws on put – exception propagates
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_annotationMapThrowsExceptionPropagates() {
        // a map that throws UnsupportedOperationException on put
        Map<String, JsonNodeAnnotation> throwingMap = new HashMap<>() {

            @Override
            public JsonNodeAnnotation put(String key, JsonNodeAnnotation value) {
                throw new UnsupportedOperationException("put not supported");
            }
        };
        ExecutionContext ctx = mock(ExecutionContext.class);
        when(ctx.getAnnotations()).thenReturn(throwingMap);
        Consumer<JsonNodeAnnotation.Builder> dummy = b -> {
        };
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> validator.putAnnotation(ctx, dummy));
        assertEquals("put not supported", ex.getMessage());
    }

    /* -----------------------------------------------------------
     * 13. Builder cleared required fields – IllegalStateException (or NPE) expected
     * ----------------------------------------------------------- */
    @Test
    void putAnnotation_builderMissingRequiredFieldsThrowsException() {
        ExecutionContext ctx = new ExecutionContext();
        // customiser clears the fields that the builder normally sets
        Consumer<JsonNodeAnnotation.Builder> badCustomizer = b -> b.evaluationPath(null).schemaLocation(null).keyword(null);
        // The exact exception type depends on the builder implementation;
        // we accept IllegalStateException or NullPointerException.
        assertThrows(RuntimeException.class, () -> validator.putAnnotation(ctx, badCustomizer));
    }
}
