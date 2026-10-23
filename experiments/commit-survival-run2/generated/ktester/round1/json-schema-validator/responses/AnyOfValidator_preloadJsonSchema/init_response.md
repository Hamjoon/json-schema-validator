```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_preloadJsonSchema_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    SchemaValidatorsConfig config;

    @Mock
    JsonSchema mockSchema1;

    @Mock
    JsonSchema mockSchema2;

    AnyOfValidator anyOfValidator;

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialise.
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- stubbed behaviour for ValidationContext -----
        // config required by the constructor
        when(validationContext.getConfig()).thenReturn(config);
        // discriminator flag – not needed for this test, just return false
        when(config.isOpenAPI3StyleDiscriminators()).thenReturn(false);

        // newSchema is called once per element in the schema array.
        // Return different mocks for successive calls.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class)))
                .thenReturn(mockSchema1, mockSchema2);

        // ----- build a simple schema node that contains two subschemas -----
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode schemaArray = mapper.createArrayNode();
        // each subschema can be an empty object – the content is irrelevant for this test
        schemaArray.add(mapper.createObjectNode());
        schemaArray.add(mapper.createObjectNode());

        // parent schema is not used in the constructor for this test; pass null
        anyOfValidator = new AnyOfValidator(
                "testPath",
                schemaArray,
                null,
                validationContext
        );
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(validationContext, config, mockSchema1, mockSchema2);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global resources to clean up.
    }

    @Test
    void testPreloadJsonSchemaInvokesPreloadOnAllContainedSchemas() {
        // Act – invoke the method under test
        anyOfValidator.preloadJsonSchema();

        // Assert – each JsonSchema created by the constructor must have its
        // preloadJsonSchema() method called exactly once.
        verify(mockSchema1, times(1)).preloadJsonSchema();
        verify(mockSchema2, times(1)).preloadJsonSchema();

        // Also verify that no extra interactions happened.
        verifyNoMoreInteractions(mockSchema1, mockSchema2);
    }
}
```