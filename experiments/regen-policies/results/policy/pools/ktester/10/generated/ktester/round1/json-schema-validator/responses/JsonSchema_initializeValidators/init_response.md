```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    @Mock ValidationContext validationContext;
    @Mock SchemaLocation schemaLocation;
    @Mock JsonNodePath evaluationPath;
    @Mock JsonNode schemaNode;

    /** The JsonSchema instance under test (a spy so we can stub getValidators()). */
    private JsonSchema jsonSchemaSpy;

    /** A mock validator that will be returned by getValidators(). */
    @Mock JsonValidator mockValidator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for these tests.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣  Create a real JsonSchema instance via reflection (its constructor is private)
        // --------------------------------------------------------------------
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(
                ValidationContext.class,
                SchemaLocation.class,
                JsonNodePath.class,
                JsonNode.class,
                JsonSchema.class,
                boolean.class);
        ctor.setAccessible(true);
        JsonSchema realInstance = ctor.newInstance(
                validationContext,
                schemaLocation,
                evaluationPath,
                schemaNode,
                null,                     // parent
                false);                   // suppressSubSchemaRetrieval

        // --------------------------------------------------------------------
        // 2️⃣  Spy the instance so we can stub getValidators()
        // --------------------------------------------------------------------
        jsonSchemaSpy = spy(realInstance);

        // --------------------------------------------------------------------
        // 3️⃣  Stub getValidators() to return a list containing our mock validator
        // --------------------------------------------------------------------
        List<JsonValidator> validators = Collections.singletonList(mockValidator);
        doReturn(validators).when(jsonSchemaSpy).getValidators();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks between tests
        reset(mockValidator);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    /** Helper to set the private field {@code validatorsLoaded} via reflection. */
    private void setValidatorsLoaded(JsonSchema schema, boolean value) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        f.setBoolean(schema, value);
    }

    /** Helper to read the private field {@code validatorsLoaded} via reflection. */
    private boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return f.getBoolean(schema);
    }

    @Test
    void initializeValidators_firstCall_loadsValidatorsAndSetsFlag() throws Exception {
        // Pre‑condition: flag must be false (default)
        assertFalse(isValidatorsLoaded(jsonSchemaSpy));

        // WHEN
        jsonSchemaSpy.initializeValidators();

        // THEN
        // 1️⃣  The validator's preloadJsonSchema() must have been invoked exactly once
        verify(mockValidator, times(1)).preloadJsonSchema();

        // 2️⃣  The internal flag must now be true
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }

    @Test
    void initializeValidators_subsequentCall_doesNotReloadWhenFlagIsTrue() throws Exception {
        // GIVEN: flag already true
        setValidatorsLoaded(jsonSchemaSpy, true);
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));

        // WHEN
        jsonSchemaSpy.initializeValidators();

        // THEN
        // No interaction with the validator because loading is skipped
        verify(mockValidator, never()).preloadJsonSchema();

        // Flag stays true
        assertTrue(isValidatorsLoaded(jsonSchemaSpy));
    }
}
```