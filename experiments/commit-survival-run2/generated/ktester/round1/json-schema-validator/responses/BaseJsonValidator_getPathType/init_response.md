```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.ValidationContext;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import com.networknt.schema.ValidatorTypeCode;
import com.networknt.schema.PathType;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link BaseJsonValidator#getPathType()}.
 *
 * <p>The {@code BaseJsonValidator} class is abstract, therefore a minimal concrete
 * subclass is created inside the test. The subclass only forwards the constructor
 * arguments to the super‑class and provides a trivial implementation of the
 * {@code validate} method required by the {@link JsonValidator} interface.</p>
 *
 * <p>Two scenarios are covered:</p>
 * <ul>
 *   <li>When the {@link ValidationContext} does not provide a {@link SchemaValidatorsConfig},
 *       the default {@link PathType} defined by the library should be used.</li>
 *   <li>When a custom {@link PathType} is supplied via the configuration,
 *       {@code getPathType()} must return that exact value.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getPathType_Test {

    // -------------------------------------------------------------------------
    // Mocks shared by all tests
    // -------------------------------------------------------------------------
    @Mock
    private JsonSchema parentSchema;               // parent schema – not used in the test
    @Mock
    private ValidatorTypeCode validatorType;      // validator type – not used in the test
    @Mock
    private ValidationContext validationContext; // context injected into the validator

    // -------------------------------------------------------------------------
    // Test fixtures
    // -------------------------------------------------------------------------
    private static final String SCHEMA_PATH = "$";
    private static final JsonNode SCHEMA_NODE = mock(JsonNode.class);

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for these unit tests.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        reset(parentSchema, validatorType, validationContext);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release.
    }

    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.
     *
     * <p>The only purpose of this class is to expose the protected
     * {@code getPathType()} method for verification.</p>
     */
    private static final class TestValidator extends BaseJsonValidator {

        TestValidator(String schemaPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext,
                      boolean suppressSubSchemaRetrieval) {
            super(schemaPath, schemaNode, parentSchema, validatorType, validationContext, suppressSubSchemaRetrieval);
        }

        // The {@code JsonValidator} contract requires a {@code validate} method.
        // For the purpose of these unit tests the method can simply return an empty set.
        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node) {
            return Collections.emptySet();
        }
    }

    @Test
    @DisplayName("getPathType() returns the default PathType when no config is supplied")
    void getPathType_returnsDefaultWhenConfigIsNull() {
        // ValidationContext#getConfig() returns null → BaseJsonValidator falls back to PathType.DEFAULT
        when(validationContext.getConfig()).thenReturn(null);

        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);

        PathType actual = validator.getPathType();

        // The library defines DEFAULT as the fallback; we assert that the returned value
        // equals the constant used by the implementation.
        assertEquals(PathType.DEFAULT, actual,
                "When no configuration is provided, getPathType() should return PathType.DEFAULT");
    }

    @Test
    @DisplayName("getPathType() returns the PathType defined in the supplied config")
    void getPathType_returnsConfiguredValue() {
        // Arrange a custom configuration that specifies a non‑default PathType.
        SchemaValidatorsConfig config = mock(SchemaValidatorsConfig.class);
        when(config.getPathType()).thenReturn(PathType.JSON_POINTER);
        when(validationContext.getConfig()).thenReturn(config);

        TestValidator validator = new TestValidator(
                SCHEMA_PATH,
                SCHEMA_NODE,
                parentSchema,
                validatorType,
                validationContext,
                false);

        PathType actual = validator.getPathType();

        assertEquals(PathType.JSON_POINTER, actual,
                "getPathType() should return the PathType supplied by the configuration");
    }
}
```