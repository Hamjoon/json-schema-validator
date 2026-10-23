
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

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

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Prepare a simple schema node that contains two properties: "a" and "b".
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
        // Stub the ValidationContext so that each call to newSchema returns the same mock.
        // The real implementation would create a new JsonSchema for each property,
        // but for the purpose of this unit test we only need to verify that the map is
        // populated with the expected keys and that the returned objects are the ones
        // supplied by the mock.
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(childSchema);
        // Instantiate the validator under test.
        validator = new PropertiesValidator("testPath", schemaNode, parentSchema, validationContext);
    }

    /**
     * Utility that uses reflection to obtain the private `schemas` field and clear it.
     */
    private void clearInternalSchemasMap() throws Exception {
        Field schemasField = PropertiesValidator.class.getDeclaredField("schemas");
        schemasField.setAccessible(true);
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
        // The values stored in the map must be the objects returned by the mocked ValidationContext.
        assertSame(childSchema, schemas.get("a"), "Schema for 'a' should be the mock returned by ValidationContext");
        assertSame(childSchema, schemas.get("b"), "Schema for 'b' should be the mock returned by ValidationContext");
        // Verify that ValidationContext#newSchema was invoked exactly twice – once per property.
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Initial state of schemas map – should be empty")
    void testInitialStateEmptyMap() {
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertNotNull(schemas, "Map must never be null");
        assertTrue(schemas.isEmpty(), "New validator should expose an empty schemas map");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Reflection insertion – map reflects the injected entry")
    void testAddEntryViaReflection() throws Exception {
        JsonSchema mockSchema = mock(JsonSchema.class);
        putIntoInternalSchemas("user", mockSchema);
        Map<String, JsonSchema> schemas = validator.getSchemas();
        assertEquals(1, schemas.size(), "Map should contain exactly the injected entry");
        assertTrue(schemas.containsKey("user"), "Map must contain the key inserted via reflection");
        assertSame(mockSchema, schemas.get("user"), "Stored value must be the mock injected via reflection");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Returned map is mutable – modifications affect internal state")
    void testMutabilityOfReturnedMap() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema schemaX = mock(JsonSchema.class);
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
                    validator.getSchemas().put("k" + i, mock(JsonSchema.class));
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
        // wait for completion
        w.get();
        // wait for completion
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
        JsonSchema s1 = mock(JsonSchema.class);
        JsonSchema s2 = mock(JsonSchema.class);
        JsonSchema s3 = mock(JsonSchema.class);
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
        JsonSchema temp = mock(JsonSchema.class);
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
        JsonSchema nullKeySchema = mock(JsonSchema.class);
        map.put(null, nullKeySchema);
        assertTrue(map.containsKey(null), "Map must contain the null key");
        assertSame(nullKeySchema, map.get(null), "Value associated with null key must be retrievable");
    }

    // -----------------------------------------------------------------------
    @Test
    @DisplayName("Putting a value under an existing key replaces the previous one")
    void testOverwriteExistingKey() {
        Map<String, JsonSchema> map = validator.getSchemas();
        JsonSchema first = mock(JsonSchema.class);
        JsonSchema second = mock(JsonSchema.class);
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
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:9: error: package org.mockito does not exist
import static org.mockito.ArgumentMatchers.*;
                         ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:10: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:7: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:22: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:25: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:28: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:53: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(childSchema);
                                         ^
  symbol:   method anyString()
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:53: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(childSchema);
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:53: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(childSchema);
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:92: error: cannot find symbol
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
                                  ^
  symbol:   method times(int)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:92: error: cannot find symbol
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
                                                      ^
  symbol:   method anyString()
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:92: error: cannot find symbol
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
                                                                   ^
  symbol:   method any(Class<JsonNode>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:92: error: cannot find symbol
        verify(validationContext, times(2)).newSchema(anyString(), any(JsonNode.class), eq(parentSchema));
                                                                                        ^
  symbol:   method eq(JsonSchema)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:108: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:121: error: cannot find symbol
        JsonSchema schemaX = mock(JsonSchema.class);
                             ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:139: error: cannot find symbol
                    validator.getSchemas().put("k" + i, mock(JsonSchema.class));
                                                        ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:180: error: cannot find symbol
        JsonSchema s1 = mock(JsonSchema.class);
                        ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:181: error: cannot find symbol
        JsonSchema s2 = mock(JsonSchema.class);
                        ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:182: error: cannot find symbol
        JsonSchema s3 = mock(JsonSchema.class);
                        ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:195: error: cannot find symbol
        JsonSchema temp = mock(JsonSchema.class);
                          ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:208: error: cannot find symbol
        JsonSchema nullKeySchema = mock(JsonSchema.class);
                                   ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:219: error: cannot find symbol
        JsonSchema first = mock(JsonSchema.class);
                           ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java:220: error: cannot find symbol
        JsonSchema second = mock(JsonSchema.class);
                            ^
  symbol:   method mock(Class<JsonSchema>)
  location: class PropertiesValidator_getSchemas_Test
Note: src/test/java/com/networknt/schema/PropertiesValidator_getSchemas_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
26 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\trelated functions:\n\t\tmethod `public Map<String, JsonSchema> getSchemas()` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonSchema>`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\n\t\tmethod `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.PropertiesValidator.getSchemas()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


