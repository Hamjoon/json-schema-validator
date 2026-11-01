```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests the overload {@code JsonSchema.validate(JsonNode)} which should delegate to
 * {@code JsonSchema.validate(JsonNode, OutputFormat)} with {@code OutputFormat.DEFAULT}.
 *
 * The test creates a real {@link JsonSchema} instance via {@link JsonSchemaFactory},
 * spies on it and stubs the delegating method. This way we avoid dealing with the
 * complex constructor arguments while still exercising the public overload.
 */
@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_2_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonSchema schema;          // real instance created by the factory
    private JsonSchema spySchema;       // spy on the real instance
    private JsonNode   sampleDataNode;  // a simple JSON document to validate

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for this test class.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // 1️⃣  Build a minimal, valid JSON‑Schema (an empty object is a valid schema).
        String schemaJson = "{}";

        // 2️⃣  Obtain a JsonSchema instance from the public factory.
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        schema = factory.getSchema(schemaJson);

        // 3️⃣  Spy on the instance so we can verify the delegation.
        spySchema = Mockito.spy(schema);

        // 4️⃣  Prepare a simple JSON document that will be passed to validate().
        sampleDataNode = MAPPER.readTree("{\"any\":\"value\"}");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the spy to avoid cross‑test interference.
        if (spySchema != null) {
            Mockito.reset(spySchema);
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required.
    }

    @Test
    void validate_ShouldDelegateToValidateWithDefaultOutputFormat() {
        // Arrange – stub the overloaded method that the simple overload should call.
        Set<ValidationMessage> stubbedResult = Collections.emptySet();
        doReturn(stubbedResult)
                .when(spySchema)
                .validate(eq(sampleDataNode), eq(OutputFormat.DEFAULT));

        // Act – call the method under test.
        Set<ValidationMessage> result = spySchema.validate(sampleDataNode);

        // Assert – the delegating method was invoked exactly once with the expected arguments.
        verify(spySchema, times(1))
                .validate(eq(sampleDataNode), eq(OutputFormat.DEFAULT));

        // And the result is exactly what the stub returned.
        assertSame(stubbedResult, result, "The result should be the stubbed empty set");
    }
}
```