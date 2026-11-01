```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link JsonSchema#initializeValidators()}.
 *
 * The test verifies that:
 * <ul>
 *   <li>Each {@link JsonValidator} in the schema has its {@code preloadJsonSchema()}
 *       method invoked exactly once when {@code initializeValidators()} is called the first time.</li>
 *   <li>A subsequent call to {@code initializeValidators()} does not invoke {@code preloadJsonSchema()}
 *       again because the internal {@code validatorsLoaded} flag is set.</li>
 * </ul>
 *
 * The {@link JsonSchema} instance is created via the protected constructor that allows us
 * to inject a custom list of validators. All other constructor arguments are mocked or
 * supplied with minimal values because they are not used by the method under test.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    // -----------------------------------------------------------------------
    // Mocks and test data
    // -----------------------------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaLocation schemaLocation;

    @Mock
    JsonNodePath evaluationPath;

    @Mock
    JsonNode schemaNode;

    JsonValidator mockValidator;          // the validator we will verify
    JsonSchema schemaUnderTest;           // instance of JsonSchema to test

    // -----------------------------------------------------------------------
    // Lifecycle methods
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock JsonValidator.  The concrete type is irrelevant – we only
        // care that its {@code preloadJsonSchema()} method is called.
        mockValidator = mock(JsonValidator.class);

        // Build the list of validators that will be injected into the JsonSchema.
        List<JsonValidator> validators = new ArrayList<>();
        validators.add(mockValidator);

        // Construct the JsonSchema using the protected constructor.  Because this
        // test class lives in the same package, the protected constructor is
        // directly accessible.
        schemaUnderTest = new JsonSchema(
                validators,                // validators
                false,                     // validatorsLoaded (initially false)
                false,                     // recursiveAnchor
                null,                      // typeValidator
                null,                      // id
                false,                     // suppressSubSchemaRetrieval
                schemaNode,                // schemaNode
                validationContext,         // validationContext
                null,                      // errorMessageType
                null,                      // errorMessageKeyword
                null,                      // messageSource
                null,                      // keyword
                null,                      // parentSchema
                schemaLocation,            // schemaLocation
                evaluationPath,            // evaluationPath
                null,                      // evaluationParentSchema
                Collections.emptyMap()    // errorMessage map
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid interaction leakage between tests.
        reset(mockValidator);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------
    @Test
    void initializeValidators_ShouldPreloadEachValidatorOnlyOnce() {
        // -------------------------------------------------------------------
        // First invocation – validators are not yet loaded.
        // -------------------------------------------------------------------
        schemaUnderTest.initializeValidators();

        // The mock validator must have received exactly one call.
        verify(mockValidator, times(1)).preloadJsonSchema();

        // -------------------------------------------------------------------
        // Second invocation – the internal flag should prevent re‑loading.
        // -------------------------------------------------------------------
        schemaUnderTest.initializeValidators();

        // No additional interactions should have occurred.
        verifyNoMoreInteractions(mockValidator);
    }
}
```