```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_hasRequiredValidator_Test {

    /*** Mocks needed for the package‑private factory method ***/
    @Mock
    ValidationContext validationContext;
    @Mock
    SchemaLocation schemaLocation;
    @Mock
    JsonNodePath evaluationPath;
    @Mock
    JsonNode schemaNode;          // a dummy schema – its content is irrelevant for this test
    @Mock
    JsonSchema parentSchema;      // no parent is required for the scenarios below

    /*** The instance under test – created once per test method ***/
    JsonSchema jsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources are required for these unit tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Use the package‑private *from* factory to obtain a real JsonSchema instance.
        // All arguments are mocks – the constructor logic only stores them; the
        // validator list is lazily initialised and will not be touched by the
        // *hasRequiredValidator* method.
        jsonSchema = JsonSchema.from(validationContext,
                                    schemaLocation,
                                    evaluationPath,
                                    schemaNode,
                                    parentSchema,
                                    false);
        // Ensure the requiredValidator field is *null* before each test case.
        setRequiredValidator(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the test creates only in‑memory objects.
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global teardown required.
    }

    /**
     * Helper that uses reflection to set the private {@code requiredValidator}
     * field of {@link JsonSchema}. This is the only way to inject a value
     * without invoking any private methods of the class under test.
     */
    private void setRequiredValidator(JsonValidator validator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(jsonSchema, validator);
    }

    @Test
    void hasRequiredValidator_ReturnsFalse_WhenValidatorIsNull() {
        // The field was cleared in {@code setupBeforeEach()}, so we expect false.
        assertFalse(jsonSchema.hasRequiredValidator(),
                "hasRequiredValidator() should return false when no required validator is present");
    }

    @Test
    void hasRequiredValidator_ReturnsTrue_WhenValidatorIsPresent() throws Exception {
        // Prepare a simple mock validator – its behaviour is irrelevant.
        JsonValidator dummyValidator = mock(JsonValidator.class);
        setRequiredValidator(dummyValidator);

        assertTrue(jsonSchema.hasRequiredValidator(),
                "hasRequiredValidator() should return true when a required validator is set");
    }
}
```