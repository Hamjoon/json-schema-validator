```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * Tests for {@link JsonSchemaFactory#getSchema(String)}.
 *
 * <p>The method under test is a simple delegator:
 * <pre>
 *     public JsonSchema getSchema(final String schema) {
 *         return getSchema(schema, null);
 *     }
 * </pre>
 * The test verifies that the delegating call is performed exactly once
 * with the expected arguments and that the returned {@link JsonSchema}
 * instance is the one produced by the overloaded method.</p>
 */
@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getSchema_4_Test {

    /** Spy of the factory created via the public builder API. */
    private JsonSchemaFactory factorySpy;

    /** A dummy {@link JsonSchema} instance returned by the overloaded method. */
    private JsonSchema dummySchema;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources required for this test class.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a real factory using the public builder – this avoids dealing with the
        // private constructor and all its required arguments.
        JsonSchemaFactory realFactory = JsonSchemaFactory.builder().build();

        // Spy the real instance so that we can verify internal delegation while still
        // using the real implementation for everything else.
        factorySpy = spy(realFactory);

        // Prepare a mock JsonSchema that will be returned by the overloaded method.
        dummySchema = mock(JsonSchema.class);

        // Stub the overloaded method (String, SchemaValidatorsConfig) to return the mock.
        // The second argument must be exactly null, matching the contract of the delegator.
        doReturn(dummySchema).when(factorySpy).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset Mockito interactions to keep each test isolated.
        clearInvocations(factorySpy, dummySchema);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup needed.
    }

    @Test
    void getSchema_delegatesToOverload_withNullConfig() {
        // Act – call the method under test.
        JsonSchema result = factorySpy.getSchema("dummy-schema");

        // Assert – the returned value must be exactly the stubbed dummy schema.
        assertSame(dummySchema, result, "The delegating method should return the schema produced by the overloaded method.");

        // Verify – the overloaded method must have been invoked once with the same
        // schema string and a null configuration object.
        verify(factorySpy, times(1)).getSchema(Mockito.eq("dummy-schema"), Mockito.isNull());
    }

    @Test
    void getSchema_passesThroughNullWhenConfigIsAbsent() {
        // Arrange – stub a different schema string to ensure the interaction is not
        // hard‑coded to "dummy-schema".
        String schemaId = "http://example.com/my-schema.json";
        JsonSchema anotherDummy = mock(JsonSchema.class);
        doReturn(anotherDummy).when(factorySpy).getSchema(Mockito.eq(schemaId), Mockito.isNull());

        // Act
        JsonSchema result = factorySpy.getSchema(schemaId);

        // Assert
        assertSame(anotherDummy, result);

        // Verify delegation.
        verify(factorySpy, times(1)).getSchema(Mockito.eq(schemaId), Mockito.isNull());
    }
}
```