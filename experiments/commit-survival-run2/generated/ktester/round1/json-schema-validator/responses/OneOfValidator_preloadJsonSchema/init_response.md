```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OneOfValidator_preloadJsonSchema_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    ValidationContext validationContext;          // mock for the ValidationContext dependency

    @Mock
    JsonSchema parentSchema;                       // parent schema can be null or a mock – not used in the test

    @Mock
    JsonSchema childSchema1;                       // first child schema returned by ValidationContext.newSchema

    @Mock
    JsonSchema childSchema2;                       // second child schema returned by ValidationContext.newSchema

    OneOfValidator validator;                       // instance under test

    /**
     * Helper that creates an {@link OneOfValidator} from a JSON array string.
     *
     * @param jsonArray the JSON text representing an array of subschemas
     */
    private OneOfValidator createValidator(String jsonArray) throws IOException {
        JsonNode schemaNode = MAPPER.readTree(jsonArray);
        // The constructor will invoke validationContext.newSchema for each array element.
        // We stub those calls here – the stubbing itself is done in the individual test methods
        // because the number of expected calls differs per test.
        return new OneOfValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset mocks before each test to avoid cross‑test interference
        reset(validationContext, childSchema1, childSchema2);
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up for these unit tests
    }

    @Test
    void preloadJsonSchema_callsInitializeValidatorsOnAllChildSchemas() throws Exception {
        // GIVEN – a OneOfValidator built from an array with two subschemas
        validator = createValidator("[{}, {}]");

        // stub ValidationContext.newSchema to return our two mock child schemas
        when(validationContext.newSchema(anyString(), any(JsonNode.class), eq(parentSchema)))
                .thenReturn(childSchema1, childSchema2);

        // Re‑create the validator now that the stubbing is in place – the constructor will
        // populate the internal `schemas` list with the two mocks.
        validator = new OneOfValidator("testPath", MAPPER.readTree("[{}, {}]"), parentSchema, validationContext);

        // WHEN – preloadJsonSchema is invoked
        validator.preloadJsonSchema();

        // THEN – each child schema should have its initializeValidators() method called exactly once
        verify(childSchema1, times(1)).initializeValidators();
        verify(childSchema2, times(1)).initializeValidators();
        // No other interactions should happen on the child schemas
        verifyNoMoreInteractions(childSchema1, childSchema2);
    }

    @Test
    void preloadJsonSchema_withEmptySchemaArray_doesNotInvokeInitializeValidators() throws Exception {
        // GIVEN – a OneOfValidator built from an empty array
        validator = createValidator("[]");

        // No need to stub newSchema because the constructor will not call it (array is empty)

        // WHEN – preloadJsonSchema is invoked
        validator.preloadJsonSchema();

        // THEN – there are no child schemas, therefore no interactions with any mock
        verifyNoInteractions(childSchema1, childSchema2);
    }
}
```