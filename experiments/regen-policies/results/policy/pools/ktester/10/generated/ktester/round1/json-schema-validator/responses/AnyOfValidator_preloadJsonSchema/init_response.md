```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    @Mock
    private ValidationContext validationContext;

    @Mock
    private SchemaLocation schemaLocation;

    @Mock
    private JsonNodePath evaluationPath;

    @Mock
    private JsonSchema parentSchema;

    private AutoCloseable mocks;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations
        mocks = MockitoAnnotations.openMocks(this);

        // The validator checks the configuration flag – provide a default config
        when(validationContext.getConfig()).thenReturn(new SchemaValidatorsConfig());
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void preloadJsonSchema_ShouldInvokePreloadOnAllContainedSchemas() {
        // ----- Arrange -------------------------------------------------------
        // Create two mock JsonSchema instances that will be returned by the
        // ValidationContext when the AnyOfValidator constructor creates its
        // internal schema list.
        JsonSchema mockSchema1 = mock(JsonSchema.class);
        JsonSchema mockSchema2 = mock(JsonSchema.class);

        // The AnyOfValidator constructor calls validationContext.newSchema(...)
        // for each element of the supplied schemaNode (an array). Stub the method
        // to return the two mocks in order.
        when(validationContext.newSchema(any(SchemaLocation.class),
                                         any(JsonNodePath.class),
                                         any(JsonNode.class),
                                         any(JsonSchema.class)))
                .thenReturn(mockSchema1, mockSchema2);

        // Build a simple JSON array node containing two empty objects – this
        // represents the "anyOf" schema definition.
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        // Construct the validator under test.
        AnyOfValidator validator = new AnyOfValidator(
                schemaLocation,
                evaluationPath,
                schemaArray,
                parentSchema,
                validationContext
        );

        // ----- Act -----------------------------------------------------------
        validator.preloadJsonSchema();

        // ----- Assert --------------------------------------------------------
        // Verify that each mock schema had its preloadJsonSchema() method
        // invoked exactly once by the validator.
        verify(mockSchema1, times(1)).preloadJsonSchema();
        verify(mockSchema2, times(1)).preloadJsonSchema();
    }
}
```