```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BaseJsonValidator#checkDiscriminatorMatch(DiscriminatorContext, ObjectNode, String, JsonSchema)}.
 *
 * The tests cover:
 * <ul>
 *   <li>early‑return when the discriminator property value is {@code null}</li>
 *   <li>implicit mapping when no explicit {@code mapping} field is present</li>
 *   <li>explicit mapping success</li>
 *   <li>explicit mapping failure with/without implicit fallback depending on parent‑schema override</li>
 *   <li>edge cases such as a {@code null} mapping node, non‑object mapping, empty mapping, empty‑string key</li>
 *   <li>exceptional situations (null arguments)</li>
 * </ul>
 *
 * The real {@link DiscriminatorContext} implementation is used wherever possible
 * (it records matches, ignored state and the resolved schemas).  {@link JsonSchema}
 * is mocked because only {@code getSchemaByReference(...)} and {@code getSchemaNode()}
 * are required for the logic under test.
 */
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_checkDiscriminatorMatch_Test {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /* --------------------------------------------------------------------- */
    /*  Existing tests – kept unchanged (they already compile and pass)      */
    /* --------------------------------------------------------------------- */

    @Test
    void testCheckDiscriminatorMatch_NullPropertyValue_MarksIgnore() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode(); // empty discriminator
        String discriminatorPropertyValue = null;
        JsonSchema jsonSchema = mock(JsonSchema.class);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        assertTrue(context.isIgnore(),
                "Context should be marked as ignore when discriminator property value is null");
        assertFalse(context.isDiscriminatorMatchFound(),
                "No discriminator match should be found when property value is null");
    }

    @Test
    void testCheckDiscriminatorMatch_NoMapping_ImplicitMatch() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode(); // no "mapping" field
        String discriminatorPropertyValue = "mySchema";
        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema referencedSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(discriminatorPropertyValue)).thenReturn(referencedSchema);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via implicit mapping");
        assertSame(referencedSchema, context.getSchema(discriminatorPropertyValue),
                "Implicitly matched schema should be registered in the context");
    }

    @Test
    void testCheckDiscriminatorMatch_ExplicitMappingMatch() {
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

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via explicit mapping");
        assertSame(referencedSchema, context.getSchema(schemaLocation),
                "Explicitly mapped schema should be registered in the context");
    }

    @Test
    void testCheckDiscriminatorMatch_ExplicitMappingNoMatch_ImplicitFallback() {
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

        // parent schema does NOT contain a "dog" property → no explicit override
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.has("dog")).thenReturn(false);
        when(jsonSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        assertTrue(context.isDiscriminatorMatchFound(),
                "Discriminator match should be found via implicit fallback");
        assertSame(implicitSchema, context.getSchema(discriminatorPropertyValue),
                "Implicitly matched schema should be registered in the context");
        assertNull(context.getSchema(otherSchemaLocation),
                "Explicit mapping schema should not be registered when it does not match");
    }

    /* --------------------------------------------------------------------- */
    /*  Additional test groups derived from the JSON specification           */
    /* --------------------------------------------------------------------- */

    @Test
    void testExplicitMappingNoMatch_NoImplicitFallback_WhenParentOverridesKey() {
        // Setup: explicit mapping contains "dog", property value is "cat"
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        ObjectNode mappingNode = OBJECT_MAPPER.createObjectNode();
        String dogSchemaLocation = "#/components/schemas/Dog";
        mappingNode.set("dog", OBJECT_MAPPER.textNode(dogSchemaLocation));
        discriminator.set("mapping", mappingNode);
        String discriminatorPropertyValue = "cat";

        JsonSchema jsonSchema = mock(JsonSchema.class);
        // parent schema *does* contain a property named "dog" → overrides explicit key
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.has("dog")).thenReturn(true);
        when(jsonSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        // No match should be recorded and implicit fallback must NOT be executed
        assertFalse(context.isDiscriminatorMatchFound(),
                "No discriminator match should be found because parent overrides the key");
        assertNull(context.getSchema(dogSchemaLocation),
                "Explicit mapping schema must not be registered when there is no match");
        assertNull(context.getSchema(discriminatorPropertyValue),
                "Implicit mapping must not be attempted");
    }

    @Test
    void testMappingFieldIsJsonNull_TreatedAsAbsent_ImplicitMatch() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        // {"mapping":null}
        discriminator.set("mapping", NullNode.getInstance());

        String discriminatorPropertyValue = "cat";
        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema implicitSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(discriminatorPropertyValue)).thenReturn(implicitSchema);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        // The method should fall back to the implicit logic
        assertTrue(context.isDiscriminatorMatchFound(),
                "Implicit match should be performed when mapping node is JSON null");
        assertSame(implicitSchema, context.getSchema(discriminatorPropertyValue));
    }

    @Test
    void testNonObjectMapping_NoImplicitFallback_WhenParentOverridesKey() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();

        // {"mapping":["cat","dog"]} – an ArrayNode, not an ObjectNode
        ArrayNode arrayMapping = OBJECT_MAPPER.createArrayNode()
                .add("cat")
                .add("dog");
        discriminator.set("mapping", arrayMapping);

        String discriminatorPropertyValue = "cat";

        JsonSchema jsonSchema = mock(JsonSchema.class);
        // parent schema claims an override for the key "cat"
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.has("cat")).thenReturn(true);
        when(jsonSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        // Explicit check runs but cannot match; due to override, implicit is NOT run
        assertFalse(context.isDiscriminatorMatchFound(),
                "No match should be recorded because parent overrides the key");
    }

    @Test
    void testEmptyObjectMapping_ImplicitFallbackWhenNoOverride() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();

        // {"mapping":{}} – empty object
        ObjectNode emptyMapping = OBJECT_MAPPER.createObjectNode();
        discriminator.set("mapping", emptyMapping);

        String discriminatorPropertyValue = "cat";

        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema implicitSchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(discriminatorPropertyValue)).thenReturn(implicitSchema);

        // parent schema does NOT contain a "cat" property → no override
        JsonNode parentSchemaNode = mock(JsonNode.class);
        when(parentSchemaNode.has("cat")).thenReturn(false);
        when(jsonSchema.getSchemaNode()).thenReturn(parentSchemaNode);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        // Implicit fallback should be taken
        assertTrue(context.isDiscriminatorMatchFound(),
                "Implicit match should be performed when explicit mapping is empty and not overridden");
        assertSame(implicitSchema, context.getSchema(discriminatorPropertyValue));
    }

    @Test
    void testExplicitMappingWithEmptyStringKey_MatchSucceeds() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();

        // {"mapping":{"": "#/components/schemas/Empty"}}
        ObjectNode mappingNode = OBJECT_MAPPER.createObjectNode();
        String emptySchemaLocation = "#/components/schemas/Empty";
        mappingNode.set("", OBJECT_MAPPER.textNode(emptySchemaLocation));
        discriminator.set("mapping", mappingNode);

        String discriminatorPropertyValue = ""; // empty string

        JsonSchema jsonSchema = mock(JsonSchema.class);
        JsonSchema emptySchema = mock(JsonSchema.class);
        when(jsonSchema.getSchemaByReference(emptySchemaLocation)).thenReturn(emptySchema);

        BaseJsonValidator.checkDiscriminatorMatch(context, discriminator,
                discriminatorPropertyValue, jsonSchema);

        assertTrue(context.isDiscriminatorMatchFound(),
                "Explicit match should succeed for empty‑string key");
        assertSame(emptySchema, context.getSchema(emptySchemaLocation));
    }

    @Test
    void testNullDiscriminatorArgument_throwsNPE() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = null;
        String discriminatorPropertyValue = "cat";
        JsonSchema jsonSchema = mock(JsonSchema.class);

        Executable call = () -> BaseJsonValidator.checkDiscriminatorMatch(
                context, discriminator, discriminatorPropertyValue, jsonSchema);

        assertThrows(NullPointerException.class, call,
                "Calling with a null discriminator should throw NPE");
    }

    @Test
    void testNullCurrentDiscriminatorContext_throwsNPE_whenPropertyValueNonNull() {
        DiscriminatorContext context = null;
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        String discriminatorPropertyValue = "cat";
        JsonSchema jsonSchema = mock(JsonSchema.class);

        Executable call = () -> BaseJsonValidator.checkDiscriminatorMatch(
                context, discriminator, discriminatorPropertyValue, jsonSchema);

        assertThrows(NullPointerException.class, call,
                "Null DiscriminatorContext should cause NPE during processing");
    }

    @Test
    void testNullCurrentDiscriminatorContext_throwsNPE_whenPropertyValueNull() {
        DiscriminatorContext context = null;
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        String discriminatorPropertyValue = null;
        JsonSchema jsonSchema = mock(JsonSchema.class);

        Executable call = () -> BaseJsonValidator.checkDiscriminatorMatch(
                context, discriminator, discriminatorPropertyValue, jsonSchema);

        assertThrows(NullPointerException.class, call,
                "Even when property value is null, a null context causes NPE on markIgnore()");
    }

    @Test
    void testNullJsonSchema_throwsNPE_implicitPath() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode(); // no "mapping"
        String discriminatorPropertyValue = "cat";
        JsonSchema jsonSchema = null; // deliberately null

        Executable call = () -> BaseJsonValidator.checkDiscriminatorMatch(
                context, discriminator, discriminatorPropertyValue, jsonSchema);

        assertThrows(NullPointerException.class, call,
                "Null JsonSchema should cause NPE inside implicit mapping logic");
    }

    @Test
    void testNullJsonSchema_throwsNPE_explicitPath() {
        DiscriminatorContext context = new DiscriminatorContext();
        ObjectNode discriminator = OBJECT_MAPPER.createObjectNode();
        ObjectNode mappingNode = OBJECT_MAPPER.createObjectNode();
        mappingNode.set("cat", OBJECT_MAPPER.textNode("#/components/schemas/Cat"));
        discriminator.set("mapping", mappingNode);
        String discriminatorPropertyValue = "dog"; // does NOT match explicit key
        JsonSchema jsonSchema = null; // null schema

        Executable call = () -> BaseJsonValidator.checkDiscriminatorMatch(
                context, discriminator, discriminatorPropertyValue, jsonSchema);

        assertThrows(NullPointerException.class, call,
                "Null JsonSchema should cause NPE during explicit mapping processing");
    }
}
```