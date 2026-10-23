```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_preloadJsonSchemas_Test {

    /** Simple concrete subclass exposing the protected method for testing. */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator() {
            // All arguments are harmlessly null for the purpose of this test.
            super("testSchemaPath", null, null, null, null);
        }

        /** Expose protected method to the test. */
        void exposePreloadJsonSchemas(Collection<JsonSchema> schemas) {
            preloadJsonSchemas(schemas);
        }

        /** Minimal implementation of the abstract validate method. */
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed.
    }

    @BeforeEach
    void setupBeforeEach() {
        validator = new TestValidator();
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
    void preloadJsonSchemas_callsInitializeValidators_onEachSchema() {
        // Arrange – create two mock JsonSchema instances
        JsonSchema schema1 = mock(JsonSchema.class);
        JsonSchema schema2 = mock(JsonSchema.class);
        Collection<JsonSchema> schemas = Arrays.asList(schema1, schema2);

        // Act – invoke the method under test via the exposing wrapper
        validator.exposePreloadJsonSchemas(schemas);

        // Assert – each mock must have received exactly one call to initializeValidators()
        verify(schema1, times(1)).initializeValidators();
        verify(schema2, times(1)).initializeValidators();
        // No other interactions should have occurred
        verifyNoMoreInteractions(schema1, schema2);
    }
}
```