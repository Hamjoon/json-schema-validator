```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getSchemaPath()}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * implementation {@code TestValidator} is defined inside the test class. The
 * implementation only needs to provide the {@code validate} method because
 * {@code getSchemaPath()} does not depend on any validation logic.</p>
 *
 * <p>All collaborators required by the constructor are mocked with Mockito.
 * The tests verify that the value supplied to the constructor is returned
 * unchanged by {@code getSchemaPath()}.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getSchemaPath_Test {

    /* --------------------------------------------------------------------- */
    /* Test fixtures                                                       */
    /* --------------------------------------------------------------------- */

    private static final String SCHEMA_PATH = "/example/schema/path";

    private JsonNode mockSchemaNode;
    private JsonSchema mockParentSchema;
    private ValidatorTypeCode mockValidatorType;
    private ValidationContext mockValidationContext;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh mocks for each test to avoid cross‑test interference.
        mockSchemaNode = mock(JsonNode.class);
        mockParentSchema = mock(JsonSchema.class);
        mockValidatorType = mock(ValidatorTypeCode.class);
        mockValidationContext = mock(ValidationContext.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    /* --------------------------------------------------------------------- */
    /* Tests                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    void getSchemaPath_returnsPathProvidedInConstructor() {
        // GIVEN – a concrete validator instantiated with a known schema path
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext
        );

        // WHEN – the method under test is invoked
        String actualPath = validator.getSchemaPath();

        // THEN – the returned path matches the one supplied to the constructor
        assertEquals(SCHEMA_PATH, actualPath,
                "BaseJsonValidator.getSchemaPath() should return the schemaPath supplied at construction");
    }

    @Test
    void getSchemaPath_isImmutableAfterConstruction() {
        // GIVEN – a validator created with an initial path
        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorType,
                mockValidationContext
        );

        // WHEN – the method is called multiple times
        String firstCall = validator.getSchemaPath();
        String secondCall = validator.getSchemaPath();

        // THEN – each call returns the same value
        assertAll(
                () -> assertEquals(SCHEMA_PATH, firstCall, "First call should return the original path"),
                () -> assertEquals(SCHEMA_PATH, secondCall, "Second call should return the same original path")
        );
    }

    /* --------------------------------------------------------------------- */
    /* Helper concrete class                                               */
    /* --------------------------------------------------------------------- */

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator} required
     * for testing the non‑abstract behaviour of {@code getSchemaPath()}.
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
            // Validation is irrelevant for the getSchemaPath() tests.
            return Collections.emptySet();
        }
    }
}
```