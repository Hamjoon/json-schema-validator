
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.net.URI;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class JsonSchema_initializeValidators_Test {

    /**
     * Minimal JSON schema used for constructing a {@link JsonSchema} instance.
     */
    private static final String SIMPLE_SCHEMA = "{ \"type\": \"object\" }";

    /**
     * The {@link JsonSchema} instance under test.
     */
    private JsonSchema jsonSchema;

    /**
     * Mocked {@link ValidationContext} required by the factory method.
     */
    @Mock
    private ValidationContext validationContext;

    /**
     * Mocked {@link JsonMetaSchema} returned from the validation context.
     */
    @Mock
    private JsonMetaSchema metaSchema;

    /**
     * Mocked {@link SchemaValidatorsConfig} – not needed for this test, can be {@code null}.
     */
    @Mock
    private SchemaValidatorsConfig config;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Configure the ValidationContext mock to satisfy the constructor requirements.
        // Only getMetaSchema() and getConfig() are accessed during construction.
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        // No keyword‑walk listeners needed.
        when(validationContext.getConfig()).thenReturn(null);
        // Parse a very small JSON schema.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);
        // Build the JsonSchema instance using the package‑private factory method.
        jsonSchema = JsonSchema.from(validationContext, // schemaPath – empty for the root schema
        "", new URI("http://example.com/simple-schema.json"), schemaNode, // no parent schema
        null, // do not suppress sub‑schema retrieval
        false);
        // minimal mock behaviour required for factory method
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        when(validationContext.getConfig()).thenReturn(null);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);
        jsonSchema = JsonSchema.from(validationContext, // schemaPath
        "", new URI("http://example.com/simple-schema.json"), schemaNode, // parent
        null, // suppressSubSchemaRetrieval
        false);
        // Ensure the internal validators map starts empty (or null) for a clean slate.
        setValidatorsMap(jsonSchema, new LinkedHashMap<>());
    }

    /**
     * Helper: obtains the private {@code validators} field (type {@code Map<String, JsonValidator>}).
     */
    private static Field getValidatorsField() throws NoSuchFieldException {
        Field f = JsonSchema.class.getDeclaredField("validators");
        f.setAccessible(true);
        return f;
    }

    /**
     * Helper: reads the private {@code validatorsLoaded} flag.
     */
    private static boolean isValidatorsLoaded(JsonSchema schema) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("validatorsLoaded");
        f.setAccessible(true);
        return (boolean) f.get(schema);
    }

    /**
     * Helper: injects a map into the private {@code validators} field.
     */
    private static void setValidatorsMap(JsonSchema schema, Map<String, JsonValidator> map) throws Exception {
        Field f = getValidatorsField();
        f.set(schema, map);
    }

    @Test
    @DisplayName("initializeValidators runs without throwing")
    void testInitializeValidatorsRunsWithoutException() {
        assertDoesNotThrow(() -> jsonSchema.initializeValidators(), "Calling initializeValidators should not throw any exception");
    }

    @Test
    @DisplayName("initializeValidators is idempotent")
    void testInitializeValidatorsIdempotent() {
        JsonValidator v = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("v", v);
        setValidatorsMap(jsonSchema, map);
        // first call – preloads
        jsonSchema.initializeValidators();
        verify(v, times(1)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchema));
        // reset interactions and call again
        reset(v);
        jsonSchema.initializeValidators();
        // second call must not invoke preloadJsonSchema again
        verifyNoInteractions(v);
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  1. first call with non‑empty validators map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – first call with non‑empty validators map")
    void testInitializeValidatorsNonEmptyMap() throws Exception {
        JsonValidator v1 = mock(JsonValidator.class);
        JsonValidator v2 = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("v1", v1);
        map.put("v2", v2);
        setValidatorsMap(jsonSchema, map);
        assertFalse(isValidatorsLoaded(jsonSchema), "validatorsLoaded should start as false");
        jsonSchema.initializeValidators();
        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded must become true after first call");
        verify(v1, times(1)).preloadJsonSchema();
        verify(v2, times(1)).preloadJsonSchema();
    }

    /* -------------------------------------------------------------
     *  2. first call with empty validators map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – first call with empty validators map")
    void testInitializeValidatorsEmptyMap() throws Exception {
        // map is already empty from setup()
        assertFalse(isValidatorsLoaded(jsonSchema));
        jsonSchema.initializeValidators();
        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded must become true even when map is empty");
        // No validator to verify – just ensure no exception is thrown
    }

    /* -------------------------------------------------------------
     *  4. validator throws RuntimeException during preload
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validator throws RuntimeException")
    void testInitializeValidatorsValidatorThrowsRuntimeException() throws Exception {
        JsonValidator bad = mock(JsonValidator.class);
        doThrow(new RuntimeException("boom")).when(bad).preloadJsonSchema();
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("bad", bad);
        setValidatorsMap(jsonSchema, map);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> jsonSchema.initializeValidators(), "Exception from validator must propagate");
        assertEquals("boom", ex.getMessage());
        // flag is set before iterating over validators
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  5. getValidators returns null -> NullPointerException
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators returns null")
    void testInitializeValidatorsGetValidatorsNull() throws Exception {
        // Directly set the private field to null
        setValidatorsMap(jsonSchema, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchema.initializeValidators(), "Calling .values() on a null map should throw NPE");
        assertTrue(isValidatorsLoaded(jsonSchema), "validatorsLoaded should be true even when NPE occurs");
    }

    /* -------------------------------------------------------------
     *  6. validators map contains a null entry
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validators map contains null entry")
    void testInitializeValidatorsNullEntryInMap() throws Exception {
        Map<String, JsonValidator> map = new HashMap<>();
        map.put("good", mock(JsonValidator.class));
        // deliberate null
        map.put("nullEntry", null);
        setValidatorsMap(jsonSchema, map);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> jsonSchema.initializeValidators(), "Calling preloadJsonSchema on a null value should NPE");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  7. duplicate validator instances in map
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – duplicate validator instances")
    void testInitializeValidatorsDuplicateValidatorInstances() throws Exception {
        JsonValidator shared = mock(JsonValidator.class);
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        map.put("first", shared);
        // same instance under another key
        map.put("second", shared);
        setValidatorsMap(jsonSchema, map);
        jsonSchema.initializeValidators();
        // Method is called once per entry, therefore twice for the same instance
        verify(shared, times(2)).preloadJsonSchema();
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  8. getValidators throws unchecked exception
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – getValidators throws unchecked exception")
    void testInitializeValidatorsGetValidatorsThrows() throws Exception {
        // Replace the internal map with a proxy that throws when values() is called
        Map<String, JsonValidator> throwingMap = (Map<String, JsonValidator>) java.lang.reflect.Proxy.newProxyInstance(Map.class.getClassLoader(), new Class[] { Map.class }, (proxy, method, args) -> {
            if ("values".equals(method.getName())) {
                throw new IllegalStateException("unexpected state");
            }
            return method.invoke(new HashMap<>(), args);
        });
        setValidatorsMap(jsonSchema, throwingMap);
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> jsonSchema.initializeValidators(), "Unchecked exception from getValidators must propagate");
        assertEquals("unexpected state", ise.getMessage());
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     *  9. concurrent modification during iteration
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – concurrent modification during iteration")
    void testInitializeValidatorsConcurrentModification() throws Exception {
        // Use a non‑thread‑safe map that we modify from within the preload call
        Map<String, JsonValidator> map = new LinkedHashMap<>();
        JsonValidator victim = mock(JsonValidator.class);
        doAnswer(invocation -> {
            // mutate the map while iteration is in progress
            map.put("new", mock(JsonValidator.class));
            return null;
        }).when(victim).preloadJsonSchema();
        map.put("victim", victim);
        setValidatorsMap(jsonSchema, map);
        assertThrows(ConcurrentModificationException.class, () -> jsonSchema.initializeValidators(), "Modifying the map during iteration must cause CME");
        assertTrue(isValidatorsLoaded(jsonSchema));
    }

    /* -------------------------------------------------------------
     * 10. validator throws an Error (e.g., AssertionError)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("initializeValidators – validator throws Error")
    void testInitializeValidatorsValidatorThrowsError() throws Exception {
        JsonValidator bad = mock(JsonValidator.class);
        doThrow(new AssertionError("assertion failed")).when(bad).preloadJsonSchema();
        Map<String, JsonValidator> map = Collections.singletonMap("bad", bad);
        setValidatorsMap(jsonSchema, map);
        AssertionError err = assertThrows(AssertionError.class, () -> jsonSchema.initializeValidators(), "Error thrown by validator must propagate");
        assertEquals("assertion failed", err.getMessage());
        assertTrue(isValidatorsLoaded(jsonSchema));
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:7: error: package org.mockito does not exist
import org.mockito.Mock;
                  ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:14: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:18: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:34: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:40: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:46: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:53: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:55: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:65: error: cannot find symbol
        when(validationContext.getMetaSchema()).thenReturn(metaSchema);
        ^
  symbol:   method when(JsonMetaSchema)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:66: error: cannot find symbol
        when(validationContext.getConfig()).thenReturn(null);
        ^
  symbol:   method when(SchemaValidatorsConfig)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:67: error: variable mapper is already defined in method setupBeforeEach()
        ObjectMapper mapper = new ObjectMapper();
                     ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:68: error: variable schemaNode is already defined in method setupBeforeEach()
        JsonNode schemaNode = mapper.readTree(SIMPLE_SCHEMA);
                 ^
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:112: error: cannot find symbol
        JsonValidator v = mock(JsonValidator.class);
                          ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:118: error: cannot find symbol
        verify(v, times(1)).preloadJsonSchema();
                  ^
  symbol:   method times(int)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:121: error: cannot find symbol
        reset(v);
        ^
  symbol:   method reset(JsonValidator)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:124: error: cannot find symbol
        verifyNoInteractions(v);
        ^
  symbol:   method verifyNoInteractions(JsonValidator)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:134: error: cannot find symbol
        JsonValidator v1 = mock(JsonValidator.class);
                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:135: error: cannot find symbol
        JsonValidator v2 = mock(JsonValidator.class);
                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:143: error: cannot find symbol
        verify(v1, times(1)).preloadJsonSchema();
                   ^
  symbol:   method times(int)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:144: error: cannot find symbol
        verify(v2, times(1)).preloadJsonSchema();
                   ^
  symbol:   method times(int)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:166: error: cannot find symbol
        JsonValidator bad = mock(JsonValidator.class);
                            ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:167: error: cannot find symbol
        doThrow(new RuntimeException("boom")).when(bad).preloadJsonSchema();
        ^
  symbol:   method doThrow(RuntimeException)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:196: error: cannot find symbol
        map.put("good", mock(JsonValidator.class));
                        ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:210: error: cannot find symbol
        JsonValidator shared = mock(JsonValidator.class);
                               ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:218: error: cannot find symbol
        verify(shared, times(2)).preloadJsonSchema();
                       ^
  symbol:   method times(int)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:249: error: cannot find symbol
        JsonValidator victim = mock(JsonValidator.class);
                               ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:250: error: cannot find symbol
        doAnswer(invocation -> {
        ^
  symbol:   method doAnswer((invocatio[...]ll; })
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:252: error: cannot find symbol
            map.put("new", mock(JsonValidator.class));
                           ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:267: error: cannot find symbol
        JsonValidator bad = mock(JsonValidator.class);
                            ^
  symbol:   method mock(Class<JsonValidator>)
  location: class JsonSchema_initializeValidators_Test
src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java:268: error: cannot find symbol
        doThrow(new AssertionError("assertion failed")).when(bad).preloadJsonSchema();
        ^
  symbol:   method doThrow(AssertionError)
  location: class JsonSchema_initializeValidators_Test
Note: src/test/java/com/networknt/schema/JsonSchema_initializeValidators_Test.java uses unchecked or unsafe operations.
Note: Recompile with -Xlint:unchecked for details.
33 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'api document of method initializeValidators()': "\n     * Initializes the validators' {@link com.networknt.schema.JsonSchema} instances.\n     * For avoiding issues with concurrency, in 1.0.49 the {@link com.networknt.schema.JsonSchema} instances affiliated with\n     * validators were modified to no more preload the schema and lazy loading is used instead.\n     * <p>This comes with the issue that this way you cannot rely on validating important schema features, in particular\n     * <code>$ref</code> resolution at instantiation from {@link com.networknt.schema.JsonSchemaFactory}.</p>\n     * <p>By calling <code>initializeValidators</code> you can enforce preloading of the {@link com.networknt.schema.JsonSchema}\n     * instances of the validators.</p>\n     ", 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.JsonSchema.validatorsLoaded;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.initializeValidators()`;\nclass `com.networknt.schema.UnionTypeValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.JsonSchema.initializeValidators()`;\nclass `com.networknt.schema.PatternPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public void preloadJsonSchema()` returns `void`, related with `com.networknt.schema.JsonSchema.initializeValidators()`;\nclass `com.networknt.schema.DependentSchemas`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.initializeValidators()`;\nclass `com.networknt.schema.JsonMetaSchema`:\n\trelated functions:\n\t\tmethod `public static Builder builder(String, JsonMetaSchema)` returns `com.networknt.schema.JsonMetaSchema.Builder`, related with `com.networknt.schema.JsonSchema.initializeValidators()`, api document: \r\n     * @param uri       the URI of your new JsonMetaSchema that will be defined via this builder.\r\n     * @param blueprint the JsonMetaSchema to base your custom JsonMetaSchema on.\r\n     * @return a builder instance preconfigured to be the same as blueprint, but with a different uri.\r\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


