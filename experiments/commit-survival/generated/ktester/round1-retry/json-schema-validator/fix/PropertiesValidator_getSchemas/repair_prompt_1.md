
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_getSchemas_Test {

    // ----- Mocks ------------------------------------------------------------
    @Mock
    ValidationContext validationContext;

    @Mock
    JsonSchema parentSchema;

    @Mock
    JsonSchema // the schema that will be returned by ValidationContext#newSchema
    childSchema;

    // ----- System under test -----------------------------------------------
    private PropertiesValidator validator;

    // may be null – we only need a reference
    private JsonSchema stubChildSchema;

    // ----- Stubs ------------------------------------------------------------
    private static class StubValidationContext implements ValidationContext {

        private final JsonSchema stubSchema;

        private int callCount = 0;

        StubValidationContext(JsonSchema stubSchema) {
            this.stubSchema = stubSchema;
        }

        @Override
        public JsonSchema newSchema(String path, JsonNode node, JsonSchema parent) {
            callCount++;
            return stubSchema;
        }

        int getCallCount() {
            return callCount;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Simple schema JSON containing two properties: "a" (string) and "b" (integer)
        String schemaJson = """
            {
              "properties": {
                "a": { "type": "string" },
                "b": { "type": "integer" }
              }
            }
            """;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(schemaJson);
        // The stub child schema – we do not need any behaviour, only a reference.
        // using null keeps the test lightweight
        stubChildSchema = null;
        // ValidationContext that always returns the same stub schema and records calls.
        validationContext = new StubValidationContext(stubChildSchema);
        // Parent schema can also be null – it is only passed through to the context.
        JsonSchema parentSchema = null;
        // Create the validator under test.
        validator = new PropertiesValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /**
     * Utility that uses reflection to obtain the private `schemas` field and clear it.
     */
    private void clearInternalSchemasMap() throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.clear();
    }

    /**
     * Utility that injects a map entry via reflection.
     */
    private void putIntoInternalSchemas(String key, JsonSchema schema) throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, JsonSchema> map = (Map<String, JsonSchema>) schemasField.get(validator);
        map.put(key, schema);
    }

    @Test
    void testGetSchemasReturnsMapWithAllDeclaredProperties() {
        // Act
        Map<String, JsonSchema> schemas = validator.getSchemas();
        // Assert – the map must contain exactly the two property names defined in the JSON.
        assertNotNull(schemas, "The schemas map should never be null");
        assertEquals(2, schemas.size(), "There should be two entries in the schemas map");
        assertTrue(schemas.containsKey("a"), "Map should contain property 'a'");
        assertTrue(schemas.containsKey("b"), "Map should contain property 'b'");
        // The values stored in the map must be the objects returned by the stub ValidationContext.
        // In this test the stub returns {@code null}, so we expect null values.
        assertNull(schemas.get("a"), "Schema for 'a' should be the stub (null) value");
        assertNull(schemas.get("b"), "Schema for 'b' should be the stub (null) value");
        // Verify that ValidationContext#newSchema was invoked exactly twice – once per property.
        assertEquals(2, validationContext.getCallCount(), "ValidationContext#newSchema should have been called once for each property");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Initial state of schemas map – should be empty")
    void testInitialStateEmptyMap() {
        // The validator is freshly created in @BeforeEach, so the map is still empty.
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertNotNull(schemas, "Map must never be null");
        assertTrue(schemas.isEmpty(), "New validator should expose an empty schemas map");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Reflection insertion – map reflects the injected entry")
    void testAddEntryViaReflection() throws Exception {
        JsonSchema injected = new JsonSchema() {
        };
        putIntoInternalSchemas("user", injected);
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals(1, schemas.size(), "Map should contain exactly the injected entry");
        assertTrue(schemas.containsKey("user"), "Map must contain the key inserted via reflection");
        assertSame(injected, schemas.get("user"), "Stored value must be the mock injected via reflection");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Returned map is mutable – modifications affect internal state")
    void testMutabilityOfReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema schemaX = new JsonSchema() {
        };
        map.put("x", schemaX);
        // Re‑obtain the map and verify the entry persists.
        Map<String, JsonSchema> again = validator.getSchemas();
        assertEquals(1, again.size());
        assertSame(schemaX, again.get("x"));
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Concurrent modifications and reads do not throw unexpected exceptions")
    void testThreadSafetySanityCheck() throws Exception {
        final int iterations = 10_000;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicBoolean failed = new AtomicBoolean(false);
        Runnable writer = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    validator.getSchemas().put("k" + i, new JsonSchema() {
                    });
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };
        Runnable reader = () -> {
            try {
                for (int i = 0; i < iterations; i++) {
                    // just read size
                    validator.getSchemas().size();
                }
            } catch (Exception e) {
                failed.set(true);
            }
        };
        Future<?> w = executor.submit(writer);
        Future<?> r = executor.submit(reader);
        w.get();
        r.get();
        executor.shutdownNow();
        assertFalse(failed.get(), "No exception should occur during concurrent access");
        assertTrue(validator.getSchemas().size() >= iterations, "At least the written entries should be present");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Two consecutive calls return the exact same Map instance")
    void testIdentityOfReturnedMap() {
        Map<String, JsonSchema> first = validator.getSchemas();
        Map<String, JsonSchema> second = validator.getSchemas();
        assertSame(first, second, "Getter must return the live internal map, not a copy");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Insertion order is preserved")
    void testInsertionOrderPreservation() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema s1 = new JsonSchema() {
        };
        JsonSchema s2 = new JsonSchema() {
        };
        JsonSchema s3 = new JsonSchema() {
        };
        map.put("a", s1);
        map.put("b", s2);
        map.put("c", s3);
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(List.of("a", "b", "c"), keys, "LinkedHashMap should preserve the order of insertion");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Removing a key through the returned map updates internal state")
    void testRemovalViaReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema temp = new JsonSchema() {
        };
        map.put("temp", temp);
        assertEquals(1, map.size());
        map.remove("temp");
        assertFalse(validator.getSchemas().containsKey("temp"));
        assertEquals(0, validator.getSchemas().size());
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Map accepts a null key")
    void testNullKeyHandling() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema nullKeySchema = new JsonSchema() {
        };
        map.put(null, nullKeySchema);
        assertTrue(map.containsKey(null), "Map must contain the null key");
        assertSame(nullKeySchema, map.get(null), "Value associated with null key must be retrievable");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Putting a value under an existing key replaces the previous one")
    void testOverwriteExistingKey() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema first = new JsonSchema() {
        };
        JsonSchema second = new JsonSchema() {
        };
        map.put("duplicate", first);
        assertEquals(1, map.size());
        assertSame(first, map.get("duplicate"));
        map.put("duplicate", second);
        assertEquals(1, map.size(), "Map size must stay unchanged when overwriting a key");
        assertSame(second, map.get("duplicate"), "Value should be replaced by the second schema");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:10: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:11: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:35: error: interface expected here
    private static class StubValidationContext implements ValidationContext {
                                                          ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:45: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:73: error: incompatible types: StubValidationContext cannot be converted to ValidationContext
        validationContext = new StubValidationContext(stubChildSchema);
                            ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:107: error: cannot find symbol
        assertNotNull(schemas, "The schemas map should never be null");
        ^
  symbol:   method assertNotNull(Map<String,JsonSchema>,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:108: error: cannot find symbol
        assertEquals(2, schemas.size(), "There should be two entries in the schemas map");
        ^
  symbol:   method assertEquals(int,int,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:109: error: cannot find symbol
        assertTrue(schemas.containsKey("a"), "Map should contain property 'a'");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:110: error: cannot find symbol
        assertTrue(schemas.containsKey("b"), "Map should contain property 'b'");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:113: error: cannot find symbol
        assertNull(schemas.get("a"), "Schema for 'a' should be the stub (null) value");
        ^
  symbol:   method assertNull(JsonSchema,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:114: error: cannot find symbol
        assertNull(schemas.get("b"), "Schema for 'b' should be the stub (null) value");
        ^
  symbol:   method assertNull(JsonSchema,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:116: error: cannot find symbol
        assertEquals(2, validationContext.getCallCount(), "ValidationContext#newSchema should have been called once for each property");
                                         ^
  symbol:   method getCallCount()
  location: variable validationContext of type ValidationContext
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:125: error: cannot find symbol
        assertNotNull(schemas, "Map must never be null");
        ^
  symbol:   method assertNotNull(Map<String,JsonSchema>,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:126: error: cannot find symbol
        assertTrue(schemas.isEmpty(), "New validator should expose an empty schemas map");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:133: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema injected = new JsonSchema() {
                              ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:137: error: cannot find symbol
        assertEquals(1, schemas.size(), "Map should contain exactly the injected entry");
        ^
  symbol:   method assertEquals(int,int,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:138: error: cannot find symbol
        assertTrue(schemas.containsKey("user"), "Map must contain the key inserted via reflection");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:139: error: cannot find symbol
        assertSame(injected, schemas.get("user"), "Stored value must be the mock injected via reflection");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:147: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema schemaX = new JsonSchema() {
                             ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:152: error: cannot find symbol
        assertEquals(1, again.size());
        ^
  symbol:   method assertEquals(int,int)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:153: error: cannot find symbol
        assertSame(schemaX, again.get("x"));
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:166: error: no suitable constructor found for JsonSchema(no arguments)
                    validator.getSchemas().put("k" + i, new JsonSchema() {
                                                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:188: error: cannot find symbol
        assertFalse(failed.get(), "No exception should occur during concurrent access");
        ^
  symbol:   method assertFalse(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:189: error: cannot find symbol
        assertTrue(validator.getSchemas().size() >= iterations, "At least the written entries should be present");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:198: error: cannot find symbol
        assertSame(first, second, "Getter must return the live internal map, not a copy");
        ^
  symbol:   method assertSame(Map<String,JsonSchema>,Map<String,JsonSchema>,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:206: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema s1 = new JsonSchema() {
                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:208: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema s2 = new JsonSchema() {
                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:210: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema s3 = new JsonSchema() {
                        ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:216: error: cannot find symbol
        assertEquals(List.of("a", "b", "c"), keys, "LinkedHashMap should preserve the order of insertion");
        ^
  symbol:   method assertEquals(List<String>,List<String>,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:224: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema temp = new JsonSchema() {
                          ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:227: error: cannot find symbol
        assertEquals(1, map.size());
        ^
  symbol:   method assertEquals(int,int)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:229: error: cannot find symbol
        assertFalse(validator.getSchemas().containsKey("temp"));
        ^
  symbol:   method assertFalse(boolean)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:230: error: cannot find symbol
        assertEquals(0, validator.getSchemas().size());
        ^
  symbol:   method assertEquals(int,int)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:238: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema nullKeySchema = new JsonSchema() {
                                   ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:241: error: cannot find symbol
        assertTrue(map.containsKey(null), "Map must contain the null key");
        ^
  symbol:   method assertTrue(boolean,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:242: error: cannot find symbol
        assertSame(nullKeySchema, map.get(null), "Value associated with null key must be retrievable");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:250: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema first = new JsonSchema() {
                           ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:252: error: no suitable constructor found for JsonSchema(no arguments)
        JsonSchema second = new JsonSchema() {
                            ^
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,URI,JsonNode,boolean) is not applicable
      (actual and formal argument lists differ in length)
    constructor JsonSchema.JsonSchema(ValidationContext,String,URI,JsonNode,JsonSchema,boolean) is not applicable
      (actual and formal argument lists differ in length)
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:255: error: cannot find symbol
        assertEquals(1, map.size());
        ^
  symbol:   method assertEquals(int,int)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:256: error: cannot find symbol
        assertSame(first, map.get("duplicate"));
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:258: error: cannot find symbol
        assertEquals(1, map.size(), "Map size must stay unchanged when overwriting a key");
        ^
  symbol:   method assertEquals(int,int,String)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:259: error: cannot find symbol
        assertSame(second, map.get("duplicate"), "Value should be replaced by the second schema");
        ^
  symbol:   method assertSame(JsonSchema,JsonSchema,String)
  location: class PropertiesValidator_getSchemas_Test
46 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\trelated functions:\n\t\tmethod `public Map<String, JsonSchema> getSchemas()` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonSchema>`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\n\t\tmethod `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


