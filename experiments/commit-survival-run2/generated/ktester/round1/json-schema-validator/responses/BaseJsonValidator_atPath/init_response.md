```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the protected {@code atPath(String, String)} method of {@link BaseJsonValidator}.
 *
 * <p>The test creates a minimal concrete subclass of {@code BaseJsonValidator} so that the
 * protected method can be exercised. All collaborators are mocked – no private members
 * of the target class are accessed directly.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_atPath_Test {

    // --------------------------------------------------------------------- //
    // Mocked collaborators
    // --------------------------------------------------------------------- //
    @Mock
    private JsonSchema parentSchema;

    @Mock
    private ValidatorTypeCode validatorType;

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaValidatorsConfig config;

    // --------------------------------------------------------------------- //
    // Test fixture
    // --------------------------------------------------------------------- //
    private TestValidator validator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled automatically by @ExtendWith)
        // Configure the ValidationContext hierarchy so that BaseJsonValidator can be
        // constructed without hitting a NullPointerException.
        when(validationContext.getConfig()).thenReturn(config);
        when(config.isFailFast()).thenReturn(false);
        // Use the JSON‑Pointer path type – this is the default in the library.
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        // Apply‑defaults strategy is required by the constructor.
        when(config.getApplyDefaultsStrategy())
                .thenReturn(ApplyDefaultsStrategy.EMPTY_APPLY_DEFAULTS_STRATEGY);

        // Create a concrete instance of the abstract BaseJsonValidator.
        validator = new TestValidator(
                "",                                   // schemaPath (irrelevant for atPath)
                JsonNodeFactory.instance.objectNode(), // empty schema node
                parentSchema,
                validatorType,
                validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    // --------------------------------------------------------------------- //
    // Tests
    // --------------------------------------------------------------------- //

    @Test
    void atPathAppendsTokenWhenCurrentPathIsNonEmpty() {
        // GIVEN
        String currentPath = "/parent";
        String token = "child";

        // WHEN
        String result = validator.atPath(currentPath, token);

        // THEN
        assertEquals("/parent/child", result,
                "atPath should concatenate the token to the current JSON‑Pointer path using '/'");
    }

    @Test
    void atPathAppendsTokenWhenCurrentPathIsRoot() {
        // GIVEN
        String currentPath = "";
        String token = "root";

        // WHEN
        String result = validator.atPath(currentPath, token);

        // THEN
        assertEquals("/root", result,
                "When the current path is empty, atPath should return a leading '/' followed by the token");
    }

    // --------------------------------------------------------------------- //
    // Helper concrete subclass
    // --------------------------------------------------------------------- //

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} used solely for
     * exercising the protected {@code atPath} method.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext);
        }

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            // No validation logic needed for the atPath tests.
            return Collections.emptySet();
        }
    }
}
```