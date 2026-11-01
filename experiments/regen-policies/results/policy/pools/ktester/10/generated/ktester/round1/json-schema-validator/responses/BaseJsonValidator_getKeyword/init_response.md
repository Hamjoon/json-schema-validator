```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#getKeyword()}.
 *
 * The concrete {@link DummyValidator} is a minimal implementation of the abstract
 * {@link BaseJsonValidator}.  It only implements the {@code validate} methods with
 * empty behaviour because the tests focus exclusively on the {@code getKeyword()}
 * method.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getKeyword_Test {

    /** A mock {@link Keyword} that will be injected into the validator. */
    @Mock
    Keyword mockKeyword;

    /** The validator instance used in the tests. */
    DummyValidator validator;

    /** Simple concrete subclass of {@link BaseJsonValidator}. */
    private static class DummyValidator extends BaseJsonValidator {

        DummyValidator(SchemaLocation schemaLocation,
                       JsonNodePath evaluationPath,
                       JsonNode schemaNode,
                       JsonSchema parentSchema,
                       ErrorMessageType errorMessageType,
                       Keyword keyword,
                       ValidationContext validationContext,
                       boolean suppressSubSchemaRetrieval) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  errorMessageType, keyword, validationContext, suppressSubSchemaRetrieval);
        }

        // The abstract {@code JsonValidator} contract requires several overloads of
        // {@code validate}.  For the purpose of these unit‑tests they can simply
        // return an empty set.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                OutputFormat<?> format) {
            return Collections.emptySet();
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node,
                                                OutputFormat<?> format,
                                                ExecutionContextCustomizer executionCustomizer) {
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Minimal (mostly dummy) objects required by the BaseJsonValidator constructor.
        SchemaLocation schemaLocation = mock(SchemaLocation.class);
        JsonNodePath evaluationPath = new JsonNodePath(PathType.DEFAULT);
        JsonNode schemaNode = mock(JsonNode.class);
        JsonSchema parentSchema = mock(JsonSchema.class);
        ErrorMessageType errorMessageType = mock(ErrorMessageType.class); // enum – mock works with mockito‑inline
        ValidationContext validationContext = null; // not needed for getKeyword()
        boolean suppressSubSchemaRetrieval = false;

        // Build the validator under test.
        validator = new DummyValidator(schemaLocation,
                                       evaluationPath,
                                       schemaNode,
                                       parentSchema,
                                       errorMessageType,
                                       mockKeyword,
                                       validationContext,
                                       suppressSubSchemaRetrieval);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void getKeyword_returnsKeywordValue() {
        // GIVEN
        when(mockKeyword.getValue()).thenReturn("myKeyword");

        // WHEN
        String result = validator.getKeyword();

        // THEN
        assertEquals("myKeyword", result, "The keyword value should be delegated from the Keyword instance");
        verify(mockKeyword, times(1)).getValue();
    }

    @Test
    void getKeyword_returnsNullWhenKeywordValueIsNull() {
        // GIVEN
        when(mockKeyword.getValue()).thenReturn(null);

        // WHEN
        String result = validator.getKeyword();

        // THEN
        assertNull(result, "When the underlying Keyword returns null, getKeyword should also return null");
        verify(mockKeyword, times(1)).getValue();
    }
}
```