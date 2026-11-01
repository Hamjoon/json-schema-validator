```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // No global resources to initialize
    }

    @BeforeEach
    void setupBeforeEach() {
        // No per‑test initialization required
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
    }

    @AfterAll
    static void teardownAfterAll() {
        // No global cleanup required
    }

    @Test
    void testCheckDiscriminatorMatch_NullPropertyValue_MarksIgnore() {
        // Arrange
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode(); // empty discriminator
        String discriminatorPropertyValue = null;
        JsonSchema jsonSchema = mock(JsonSchema.class);

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema);

        // Assert
        assertTrue(context.isIgnore(),
                "Context should be marked as ignore when discriminator property value is null");
        assertFalse(context.isDiscriminatorMatchFound(),
                "No discriminator match should be found when property value is null");
    }

    @Test
    void testCheckDiscriminatorMatch_NoMapping_ImplicitMatch() {
        // Arrange
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode(); // no "mapping" field
        String discriminatorPropertyValue = "mySchema";
        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema referencedSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(discriminatorPropertyValue)).thenReturn(referencedSchema);

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema);

        // Assert
        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via implicit mapping");
        assertSame(referencedSchema, context.getSchema(discriminatorPropertyValue),
                "Implicitly matched schema should be registered in the context");
    }

    @Test
    void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
        // Arrange
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        ObjectNode mappingNode = OBJECT_MAPPER.createObjectNode();
        String schemaLocation = "#/definitions/CatSchema";
        mappingNode.set("cat", OBJECT_MAPPER.textNode(schemaLocation));
        discriminator.set("mapping", mappingNode);
        String discriminatorPropertyValue = "cat";
        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema referencedSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(schemaLocation)).thenReturn(referencedSchema);

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema);

        // Assert
        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via explicit mapping");
        assertSame(referencedSchema, context.getSchema(schemaLocation),
                "Explicitly mapped schema should be registered in the context");
    }

    @Test
    void testCheckDiscriminatorMatch_ExplicitMappingNoMatch_ImplicitFallback() {
        // Arrange
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        ObjectNode mappingNode = OBJECT_MAPPER.createObjectNode();
        String otherSchemaLocation = "#/definitions/DogSchema";
        mappingNode.set("dog", OBJECT_MAPPER.textNode(otherSchemaLocation));
        discriminator.set("mapping", mappingNode);
        String discriminatorPropertyValue = "cat"; // does NOT match the explicit key "dog"
        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema implicitSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(discriminatorPropertyValue)).thenReturn(implicitSchema);

        // Mock parent schema node so that noExplicitDiscriminatorKeyOverride returns true
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.has("dog")).thenReturn(false);
        when(jsonSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        // Act
        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator, discriminatorPropertyValue, jsonSchema);

        // Assert
        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via implicit fallback");
        assertSame(implicitSchema, context.getSchema(discriminatorPropertyValue),
                "Implicitly matched schema should be registered in the context");
        // Explicit mapping should not have been registered because the key did not match
        assertNull(context.getSchema(otherSchemaLocation),
                "Explicit mapping schema should not be registered when it does not match");
    }
}
```