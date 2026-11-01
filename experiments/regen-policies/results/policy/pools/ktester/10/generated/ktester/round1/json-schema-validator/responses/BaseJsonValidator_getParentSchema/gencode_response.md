```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#getParentSchema()}.
 *
 * <p>The tests verify that the getter simply returns the {@code parentSchema}
 * instance that was supplied to the {@link BaseJsonValidator} constructor or
 * later set via reflection.  The test suite covers a wide range of scenarios
 * described in the JSON test‑case specification:</p>
 *
 * <ul>
 *   <li>null parent schema</li>
 *   <li>mock parent schema (reference equality)</li>
 *   <li>real {@link JsonSchema} instance created by {@link JsonSchemaFactory}</li>
 *   <li>resetting the field to {@code null} after a non‑null assignment</li>
 *   <li>self‑reference (the validator's own schema as its parent)</li>
 *   <li>dynamic proxy implementing {@link JsonSchema}</li>
 *   <li>different thread visibility</li>
 * </ul>
 *
 * <p>A minimal concrete subclass {@code TestValidator} is defined inside the
 * test class because {@code BaseJsonValidator} is abstract.  All abstract
 * methods are stubbed – they are irrelevant for the behaviour under test.</p>
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_getParentSchema_Test {

    // -----------------------------------------------------------------------
    // Mocks required for the BaseJsonValidator constructor
    // -----------------------------------------------------------------------
    @Mock
    private SchemaLocation mockSchemaLocation;
    @Mock
    private JsonNodePath mockEvaluationPath;
    @Mock
    private JsonNode mockSchemaNode;
    @Mock
    private ValidationContext mockValidationContext;
    @Mock
    private ValidatorTypeCode mockValidatorTypeCode;

    // -----------------------------------------------------------------------
    // Helper concrete validator used in the tests
    // -----------------------------------------------------------------------
    /**
     * Minimal concrete implementation of {@link BaseJsonValidator}.  All abstract
     * methods are implemented with trivial stubs because the tests only need the
     * {@code getParentSchema()} behaviour.
     */
    private static class TestValidator extends BaseJsonValidator {

        TestValidator(SchemaLocation schemaLocation,
                      JsonNodePath evaluationPath,
                      JsonNode schemaNode,
                      JsonSchema parentSchema,
                      ValidatorTypeCode validatorType,
                      ValidationContext validationContext) {
            super(schemaLocation, evaluationPath, schemaNode, parentSchema,
                  validatorType, validationContext);
        }

        @Override
        public String getKeyword() {
            return "testKeyword";
        }

        @Override
        public JsonNodePath getEvaluationPath() {
            return null;
        }

        @Override
        public SchemaLocation getSchemaLocation() {
            return null;
        }

        @Override
        public JsonSchema getEvaluationParentSchema() {
            return null;
        }
    }

    // -----------------------------------------------------------------------
    // Test lifecycle – no special global state, so methods are empty
    // -----------------------------------------------------------------------
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are initialised automatically by the extension
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to release
    }

    // -----------------------------------------------------------------------
    // Helper to set the protected `parentSchema` field via reflection
    // -----------------------------------------------------------------------
    private static void setParentSchema(BaseJsonValidator validator, JsonSchema value) throws Exception {
        Field f = ValidationMessageHandler.class.getDeclaredField("parentSchema");
        f.setAccessible(true);
        f.set(validator, value);
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void getParentSchema_returnsProvidedParentSchema() {
        // Arrange
        JsonSchema mockParentSchema = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParentSchema,
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(mockParentSchema, result,
                "BaseJsonValidator#getParentSchema() should return the exact instance supplied to the constructor");
    }

    @Test
    void getParentSchema_returnsNullWhenNoParentProvided() {
        // Arrange
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,                       // <-- no parent schema
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertNull(result,
                "BaseJsonValidator#getParentSchema() should return null when the validator was constructed with a null parent");
    }

    @Test
    void getParentSchema_returnsRealJsonSchemaFromFactory() throws Exception {
        // Arrange: create a real JsonSchema using the public factory
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        JsonSchema realParent = factory.getSchema("{\"type\":\"object\"}");

        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                realParent,
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(realParent, result,
                "The getter must return the exact JsonSchema instance created by JsonSchemaFactory");
    }

    @Test
    void getParentSchema_returnsNullAfterFieldResetViaReflection() throws Exception {
        // Arrange: start with a non‑null mock parent
        JsonSchema mockParent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                mockParent,
                mockValidatorTypeCode,
                mockValidationContext);

        // Verify initial state
        assertSame(mockParent, validator.getParentSchema(),
                "Pre‑condition: parentSchema should be the mock instance");

        // Act: reset the protected field to null using reflection
        setParentSchema(validator, null);

        // Assert
        assertNull(validator.getParentSchema(),
                "After resetting the field to null via reflection, getter must return null");
    }

    @Test
    void getParentSchema_returnsSelfReference() throws Exception {
        // Arrange: use the validator's own schemaNode wrapped as a JsonSchema
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance();
        JsonSchema selfSchema = factory.getSchema("{\"type\":\"string\"}");

        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,
                mockValidatorTypeCode,
                mockValidationContext);

        // Set the parentSchema to the validator's own JsonSchema instance
        setParentSchema(validator, selfSchema);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(selfSchema, result,
                "When the validator's own JsonSchema is set as parent, the getter should return that same instance");
    }

    @Test
    void getParentSchema_returnsDynamicProxyInstance() throws Exception {
        // Arrange: create a dynamic proxy that implements JsonSchema
        JsonSchema proxyInstance = (JsonSchema) Proxy.newProxyInstance(
                JsonSchema.class.getClassLoader(),
                new Class<?>[]{JsonSchema.class},
                (proxy, method, args) -> {
                    // simple stub: return defaults for Object methods, null otherwise
                    if (method.getName().equals("toString")) {
                        return "JsonSchemaProxy";
                    }
                    return null;
                });

        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                proxyInstance,
                mockValidatorTypeCode,
                mockValidationContext);

        // Act
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(proxyInstance, result,
                "The getter must return the dynamic proxy instance unchanged");
    }

    @Test
    void getParentSchema_returnsInstanceSetFromAnotherThread() throws Exception {
        // Arrange
        JsonSchema mockParent = mock(JsonSchema.class);
        TestValidator validator = new TestValidator(
                mockSchemaLocation,
                mockEvaluationPath,
                mockSchemaNode,
                null,
                mockValidatorTypeCode,
                mockValidationContext);

        // Set the field in a separate thread
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> setter = executor.submit(() -> {
            try {
                setParentSchema(validator, mockParent);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        setter.get(); // wait for completion
        executor.shutdown();

        // Act (in the main test thread)
        JsonSchema result = validator.getParentSchema();

        // Assert
        assertSame(mockParent, result,
                "Even when parentSchema is set from another thread, the getter should return the assigned instance");
    }
}
```