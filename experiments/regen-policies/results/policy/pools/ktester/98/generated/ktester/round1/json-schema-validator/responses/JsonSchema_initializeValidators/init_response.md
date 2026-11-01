```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /** Simple concrete subclass that exposes the protected constructor of {@link JsonSchema}. */
    private static class TestJsonSchema extends JsonSchema {
        TestJsonSchema(List<JsonValidator> validators, boolean validatorsLoaded) {
            // The protected constructor has many parameters; most of them are not needed for this
            // particular test, so we pass null / defaults for the irrelevant ones.
            super(
                    validators,                // validators
                    validatorsLoaded,          // validatorsLoaded
                    false,                     // recursiveAnchor
                    null,                      // typeValidator
                    null,                      // id
                    false,                     // suppressSubSchemaRetrieval
                    null,                      // schemaNode
                    null,                      // validationContext
                    null,                      // errorMessageType
                    null,                      // errorMessageKeyword
                    null,                      // messageSource
                    null,                      // keyword
                    null,                      // parentSchema
                    null,                      // schemaLocation
                    null,                      // evaluationPath
                    null,                      // evaluationParentSchema
                    Collections.emptyMap()    // errorMessage map
            );
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void initializeValidators_callsPreloadWhenNotYetLoaded(@Mock JsonValidator validator1,
                                                          @Mock JsonValidator validator2) {
        // GIVEN a JsonSchema whose validators have not been loaded yet
        List<JsonValidator> validators = Arrays.asList(validator1, validator2);
        JsonSchema schema = new TestJsonSchema(validators, false);

        // WHEN initializeValidators() is invoked
        schema.initializeValidators();

        // THEN each validator's preloadJsonSchema() should be called exactly once
        verify(validator1, times(1)).preloadJsonSchema();
        verify(validator2, times(1)).preloadJsonSchema();
    }

    @Test
    void initializeValidators_doesNotCallPreloadWhenAlreadyLoaded(@Mock JsonValidator validator) {
        // GIVEN a JsonSchema whose validators are already marked as loaded
        List<JsonValidator> validators = Collections.singletonList(validator);
        JsonSchema schema = new TestJsonSchema(validators, true);

        // WHEN initializeValidators() is invoked
        schema.initializeValidators();

        // THEN no preloadJsonSchema() call should occur
        verify(validator, never()).preloadJsonSchema();
    }
}
```