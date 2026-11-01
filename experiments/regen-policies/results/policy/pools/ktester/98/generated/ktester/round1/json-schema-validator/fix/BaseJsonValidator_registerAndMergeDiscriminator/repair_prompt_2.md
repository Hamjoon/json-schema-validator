
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
@ExtendWith(MockitoExtension.class)
class BaseJsonValidator_registerAndMergeDiscriminator_Test {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static class RegisterCall {
        final JsonNodePath path;

        final ObjectNode discriminator;

        RegisterCall(JsonNodePath path, ObjectNode discriminator) {
            this.path = path;
            this.discriminator = discriminator;
        }
    }

    /**
     * Simple test double for {@link DiscriminatorContext}.
     */
    private static class SimpleDiscriminatorContext implements DiscriminatorContext {
        /**
         * Value that {@link #getDiscriminatorForPath(JsonNodePath)} should return.
         */
        private ObjectNode discriminatorForPath;

        /**
         * Records of calls to {@link #registerDiscriminator(JsonNodePath, ObjectNode)}.
         */
        private final List<RegisterCall> registerCalls = new ArrayList<>();

        SimpleDiscriminatorContext() {
        }

        void setDiscriminatorForPath(ObjectNode node) {
            this.discriminatorForPath = node;
        }

        @Override
        public ObjectNode getDiscriminatorForPath(JsonNodePath path) {
            return discriminatorForPath;
        }

        @Override
        public void registerDiscriminator(JsonNodePath path, ObjectNode discriminator) {
            registerCalls.add(new RegisterCall(path, discriminator));
        }

        List<RegisterCall> getRegisterCalls() {
            return registerCalls;
        }

        static class RegisterCall {
            final JsonNodePath path;

            final ObjectNode discriminator;

            RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                this.path = path;
                this.discriminator = discriminator;
            }
        }
    }

    /**
     * Helper to inject a value into a private/protected field via reflection.
     */
    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Scenario 1 – the schema does NOT contain a "discriminator" keyword.
     * The method should simply register the (unchanged) discriminator.
     */
    @Test
    void shouldRegisterWhenNoDiscriminatorOnSchema() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());
        // schema without discriminator – empty object node
        JsonNode schemaNode = MAPPER.createObjectNode();
        // default constructor works
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        // Assert – verify that registerDiscriminator was called exactly once with the same discriminator
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 2 – the schema contains a discriminator with a "propertyName".
     * According to the implementation this is illegal and must throw JsonSchemaException.
     */
    @Test
    void shouldThrowWhenDiscriminatorRedefinesPropertyName() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // Build schema node with discriminator.propertyName
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.put("propertyName", "type");
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("attempts redefining the discriminator property"));
    }

    /**
     * Scenario 3 – context discriminator has no "mapping" while schema provides one.
     * The method should copy the mapping from the schema onto the context discriminator.
     */
    @Test
    void shouldCopyMappingWhenContextHasNone() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // no mapping initially
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // schema discriminator with mapping
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        // make the context think a discriminator already exists for this location
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        // Assert – the root discriminator should now contain the mapping from the schema
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        assertNotNull(resultingMapping);
        assertEquals(schemaMapping, resultingMapping);
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 4 – both context and schema have mappings; they are merged without conflict.
     */
    @Test
    void shouldMergeMappingsWhenNoConflict() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        contextMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/A"));
        rootDiscriminator.set("mapping", contextMapping);
        // schema mapping adds a new entry "B"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("B", MAPPER.createObjectNode().put("$ref", "#/components/schemas/B"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        // Assert – both entries should be present
        JsonNode merged = rootDiscriminator.get("mapping");
        assertNotNull(merged);
        assertEquals(2, merged.size());
        assertTrue(merged.has("A"));
        assertTrue(merged.has("B"));
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Scenario 5 – conflict: same mapping key with different values should raise an exception.
     */
    @Test
    void shouldThrowOnMappingRedefinitionConflict() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        // Existing mapping for "A"
        contextMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/OldA"));
        rootDiscriminator.set("mapping", contextMapping);
        // Schema provides a different mapping for "A"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("A", MAPPER.createObjectNode().put("$ref", "#/components/schemas/NewA"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        assertTrue(ex.getMessage().contains("discriminator mapping redefinition"));
        assertEquals(0, ctx.getRegisterCalls().size());
    }

    /**
     * Mapping with the same key/value already present – the method should silently accept it.
     */
    @Test
    void shouldAcceptSameMappingValueWithoutException() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ObjectNode contextMapping = MAPPER.createObjectNode();
        // Existing mapping for "cat"
        contextMapping.set("cat", MAPPER.createObjectNode().put("$ref", "#/schemas/Cat"));
        rootDiscriminator.set("mapping", contextMapping);
        // Schema provides the *identical* mapping for "cat"
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        schemaMapping.set("cat", MAPPER.createObjectNode().put("$ref", "#/schemas/Cat"));
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", schemaMapping);
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act – should not throw
        assertDoesNotThrow(() -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        // Mapping must stay unchanged (still only one entry)
        JsonNode mapping = rootDiscriminator.get("mapping");
        assertNotNull(mapping);
        assertEquals(1, mapping.size());
        assertTrue(mapping.has("cat"));
        assertEquals(1, ctx.getRegisterCalls().size());
        assertSame(rootDiscriminator, ctx.getRegisterCalls().get(0).discriminator);
    }

    /**
     * Passing a {@code null} {@link DiscriminatorContext} should result in a {@link NullPointerException}
     * because the method dereferences the context without a null‑check.
     */
    @Test
    void shouldThrowNPEWhenContextIsNull() {
        // Arrange
        DiscriminatorContext ctx = null;
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        JsonSchema schema = new JsonSchema();
        // any non‑null schemaNode is fine
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * Supplying a {@code null} discriminator node triggers a {@link NullPointerException}
     * when the method attempts to read its {@code "mapping"} field.
     */
    @Test
    void shouldThrowNPEWhenDiscriminatorNodeIsNull() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        // deliberately null
        ObjectNode rootDiscriminator = null;
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", MAPPER.createObjectNode());
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the root discriminator's {@code "mapping"} field exists but is not an {@link ObjectNode},
     * a {@link ClassCastException} is expected during the cast.
     */
    @Test
    void shouldThrowClassCastWhenRootMappingIsNotObjectNode() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // mapping is a plain text node, not an object
        rootDiscriminator.set("mapping", MAPPER.textNode("foo"));
        // schema must contain a discriminator (even empty) to enter the block
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        // make the context return a non‑null discriminator for the location
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        assertThrows(ClassCastException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * When the nested schema's discriminator {@code "mapping"} field is not an {@link ObjectNode},
     * a {@link ClassCastException} should be thrown.
     */
    @Test
    void shouldThrowClassCastWhenSchemaMappingIsNotObjectNode() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        // valid empty mapping
        rootDiscriminator.set("mapping", MAPPER.createObjectNode());
        // schema discriminator with a non‑object mapping (text node)
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        discriminatorNode.set("mapping", MAPPER.textNode("invalid"));
        ObjectNode schemaNode = MAPPER.createObjectNode();
        schemaNode.set("discriminator", discriminatorNode);
        JsonSchema schema = new JsonSchema();
        setField(schema, "schemaNode", schemaNode);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        assertThrows(ClassCastException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@code schemaNode},
     * accessing {@code schema.schemaNode.get("discriminator")} results in a {@link NullPointerException}.
     */
    @Test
    void shouldThrowNPEWhenSchemaNodeIsNull() {
        // Arrange
        SimpleDiscriminatorContext ctx = new SimpleDiscriminatorContext();
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        JsonSchema schema = new JsonSchema();
        // explicitly set schemaNode to null
        setField(schema, "schemaNode", null);
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
 ^
  symbol: class ExtendWith
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:3: error: cannot find symbol
    private static final ObjectMapper MAPPER = new ObjectMapper();
                         ^
  symbol:   class ObjectMapper
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:6: error: cannot find symbol
        final JsonNodePath path;
              ^
  symbol:   class JsonNodePath
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:8: error: cannot find symbol
        final ObjectNode discriminator;
              ^
  symbol:   class ObjectNode
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:10: error: cannot find symbol
        RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                     ^
  symbol:   class JsonNodePath
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:10: error: cannot find symbol
        RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                                        ^
  symbol:   class ObjectNode
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:19: error: cannot find symbol
    private static class SimpleDiscriminatorContext implements DiscriminatorContext {
                                                               ^
  symbol:   class DiscriminatorContext
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:23: error: cannot find symbol
        private ObjectNode discriminatorForPath;
                ^
  symbol:   class ObjectNode
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:28: error: cannot find symbol
        private final List<RegisterCall> registerCalls = new ArrayList<>();
                      ^
  symbol:   class List
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:33: error: cannot find symbol
        void setDiscriminatorForPath(ObjectNode node) {
                                     ^
  symbol:   class ObjectNode
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:38: error: cannot find symbol
        public ObjectNode getDiscriminatorForPath(JsonNodePath path) {
                                                  ^
  symbol:   class JsonNodePath
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:38: error: cannot find symbol
        public ObjectNode getDiscriminatorForPath(JsonNodePath path) {
               ^
  symbol:   class ObjectNode
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:43: error: cannot find symbol
        public void registerDiscriminator(JsonNodePath path, ObjectNode discriminator) {
                                          ^
  symbol:   class JsonNodePath
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:43: error: cannot find symbol
        public void registerDiscriminator(JsonNodePath path, ObjectNode discriminator) {
                                                             ^
  symbol:   class ObjectNode
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:47: error: cannot find symbol
        List<RegisterCall> getRegisterCalls() {
        ^
  symbol:   class List
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:52: error: cannot find symbol
            final JsonNodePath path;
                  ^
  symbol:   class JsonNodePath
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:54: error: cannot find symbol
            final ObjectNode discriminator;
                  ^
  symbol:   class ObjectNode
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:56: error: cannot find symbol
            RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                         ^
  symbol:   class JsonNodePath
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:56: error: cannot find symbol
            RegisterCall(JsonNodePath path, ObjectNode discriminator) {
                                            ^
  symbol:   class ObjectNode
  location: class RegisterCall
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:1: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:80: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:104: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:127: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:159: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:194: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:224: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:260: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:278: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:296: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:321: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:346: error: cannot find symbol
    @Test
     ^
  symbol:   class Test
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:3: error: cannot find symbol
    private static final ObjectMapper MAPPER = new ObjectMapper();
                                                   ^
  symbol:   class ObjectMapper
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:28: error: cannot find symbol
        private final List<RegisterCall> registerCalls = new ArrayList<>();
                                                             ^
  symbol:   class ArrayList
  location: class SimpleDiscriminatorContext
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:37: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:42: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:68: error: cannot find symbol
            Field f = target.getClass().getDeclaredField(fieldName);
            ^
  symbol:   class Field
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:84: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:87: error: cannot find symbol
        JsonNode schemaNode = MAPPER.createObjectNode();
        ^
  symbol:   class JsonNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:89: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
        ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:89: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
                                ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:91: error: cannot find symbol
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
                                               ^
  symbol:   class SchemaLocation
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:92: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:92: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                            ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:92: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                                         ^
  symbol:   variable PathType
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:94: error: cannot find symbol
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        ^
  symbol:   variable BaseJsonValidator
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:108: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:110: error: cannot find symbol
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:112: error: cannot find symbol
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:114: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
        ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:114: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
                                ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:116: error: cannot find symbol
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
                                               ^
  symbol:   class SchemaLocation
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:117: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:117: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                            ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:117: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                                         ^
  symbol:   variable PathType
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:119: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        ^
  symbol:   class JsonSchemaException
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:119: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
                                              ^
  symbol:   class JsonSchemaException
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:119: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
                                                                               ^
  symbol:   variable BaseJsonValidator
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:132: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:134: error: cannot find symbol
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:136: error: cannot find symbol
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:138: error: cannot find symbol
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:140: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
        ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:140: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
                                ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:142: error: cannot find symbol
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
                                               ^
  symbol:   class SchemaLocation
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:144: error: cannot find symbol
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
                                        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:145: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:145: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                            ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:145: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                                         ^
  symbol:   variable PathType
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:147: error: cannot find symbol
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        ^
  symbol:   variable BaseJsonValidator
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:149: error: cannot find symbol
        JsonNode resultingMapping = rootDiscriminator.get("mapping");
        ^
  symbol:   class JsonNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:163: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:164: error: cannot find symbol
        ObjectNode contextMapping = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:168: error: cannot find symbol
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:170: error: cannot find symbol
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:172: error: cannot find symbol
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:174: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
        ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:174: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
                                ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:176: error: cannot find symbol
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
                                               ^
  symbol:   class SchemaLocation
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:177: error: cannot find symbol
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
                                        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:178: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:178: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                            ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:178: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                                         ^
  symbol:   variable PathType
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:180: error: cannot find symbol
        BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation);
        ^
  symbol:   variable BaseJsonValidator
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:182: error: cannot find symbol
        JsonNode merged = rootDiscriminator.get("mapping");
        ^
  symbol:   class JsonNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:198: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:199: error: cannot find symbol
        ObjectNode contextMapping = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:204: error: cannot find symbol
        ObjectNode schemaMapping = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:206: error: cannot find symbol
        ObjectNode discriminatorNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:208: error: cannot find symbol
        ObjectNode schemaNode = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:210: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
        ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:210: error: cannot find symbol
        JsonSchema schema = new JsonSchema();
                                ^
  symbol:   class JsonSchema
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:212: error: cannot find symbol
        setField(schema, "schemaLocation", new SchemaLocation(null, null));
                                               ^
  symbol:   class SchemaLocation
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:213: error: cannot find symbol
        ctx.setDiscriminatorForPath(new ObjectNode(MAPPER.getNodeFactory()));
                                        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:214: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
        ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:214: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                            ^
  symbol:   class JsonNodePath
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:214: error: cannot find symbol
        JsonNodePath instanceLocation = new JsonNodePath(PathType.JSON_POINTER);
                                                         ^
  symbol:   variable PathType
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:216: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
        ^
  symbol:   class JsonSchemaException
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:216: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
                                              ^
  symbol:   class JsonSchemaException
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:216: error: cannot find symbol
        JsonSchemaException ex = assertThrows(JsonSchemaException.class, () -> BaseJsonValidator.registerAndMergeDiscriminator(ctx, rootDiscriminator, schema, instanceLocation));
                                                                               ^
  symbol:   variable BaseJsonValidator
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
src/test/java/com/networknt/schema/BaseJsonValidator_registerAndMergeDiscriminator_Test.java:228: error: cannot find symbol
        ObjectNode rootDiscriminator = MAPPER.createObjectNode();
        ^
  symbol:   class ObjectNode
  location: class BaseJsonValidator_registerAndMergeDiscriminator_Test
100 errors
only showing the first 100 errors, of 166 total; use -Xmaxerrs if you would like to see more

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.BaseJsonValidator': '\n * Base {@link JsonValidator}. \n ', 'api document of method registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)': '\n     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine\n     * the <code>propertyName</code> or mappings.\n     *\n     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}\n     * @param discriminator               the discriminator to use for the check\n     * @param schema                      the value of the <code>discriminator/propertyName</code> field\n     * @param instanceLocation                          the logging prefix\n     ', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.JsonNodePath`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\trelated functions:\n\t\tmethod `protected JsonNode getMessageNode(String, JsonNode, JsonSchema, String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.BaseJsonValidator.registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\trelated functions:\n\t\tmethod `private static boolean noExplicitDiscriminatorKeyOverride(JsonNode, JsonSchema)` returns `boolean`, related with `com.networknt.schema.BaseJsonValidator.registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)`;\n\t\tmethod `private static void checkForExplicitDiscriminatorMappingMatch(DiscriminatorContext, String, JsonNode, JsonSchema)` returns `void`, related with `com.networknt.schema.BaseJsonValidator.registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)`;\nclass `com.networknt.schema.FormatValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validateUnknownFormat(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.BaseJsonValidator.registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, JsonNodePath)`, api document: \r\n     * When the Format-Assertion vocabulary is specified, implementations MUST fail upon encountering unknown formats.\r\n     * \r\n     * @param executionContext the execution context\r\n     * @param node the node\r\n     * @param rootNode the root node\r\n     * @param instanceLocation the instance location\r\n     * @return the messages\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


