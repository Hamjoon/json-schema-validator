
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import java.net.URI;
import com.fasterxml.jackson.databind.JsonNode;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getValidationContext_Test {

    @Mock
    ValidationContext validationContext;

    @Mock
    JsonMetaSchema metaSchema;

    @Mock
    JsonNode schemaNode;

    JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Use the V7 meta‑schema – the most common default.
        metaSchema = JsonMetaSchema.getV7();
        // Create a simple ValidationContext instance. The library provides a
        // public no‑arg constructor for this purpose.
        validationContext = new ValidationContext();
        // Minimal schema node – an empty JSON object is sufficient.
        ObjectMapper mapper = new ObjectMapper();
        schemaNode = mapper.readTree("{}");
        URI currentUri = URI.create("http://example.com/schema");
        // Create the JsonSchema instance via the public factory method.
        jsonSchema = JsonSchema.from(validationContext, "testSchemaPath", currentUri, schemaNode, // do not suppress sub‑schema retrieval
        null, false);
    }

    @Test
    void getValidationContext_returnsTheSameInstanceProvidedInConstructor() {
        ValidationContext result = jsonSchema.getValidationContext();
        assertSame(validationContext, result, "JsonSchema#getValidationContext should return the ValidationContext supplied at construction");
    }

    /**
     * Group: testGetValidationContextOnChildSchema **
     */
    @Test
    void getValidationContext_onChildSchema_returnsParentContext() {
        // Arrange – create a child schema from the parent.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode childNode = mapper.readTree("{\"type\":\"string\"}");
        JsonSchema childSchema = jsonSchema.createChildSchema("childPath", childNode);
        // Act
        ValidationContext childContext = childSchema.getValidationContext();
        // Assert – the child must expose the exact same ValidationContext instance as its parent.
        assertSame(validationContext, childContext, "Child JsonSchema#getValidationContext should return the same ValidationContext as its parent");
    }

    /**
     * Group: testGetValidationContextWhenNullContext **
     */
    @Test
    void getValidationContext_whenFactoryCalledWithNullContext_returnsNull() {
        // Arrange – call the static factory with a null ValidationContext.
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        URI currentUri = URI.create("http://example.com/nullContext");
        JsonSchema schemaWithNullContext = JsonSchema.from(null, "nullPath", currentUri, node, null, false);
        // Act
        ValidationContext result = schemaWithNullContext.getValidationContext();
        // Assert – the method should simply return null and not throw.
        assertNull(result, "JsonSchema#getValidationContext should return null when the schema was created with a null ValidationContext");
    }

    /**
     * Group: testGetValidationContextAfterReusingContextAcrossMultipleSchemas **
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 5, 10 })
    void getValidationContext_multipleSchemas_shareSameContext(int schemaCount) {
        // Arrange – create several independent schemas using the same ValidationContext instance.
        JsonSchema[] schemas = new JsonSchema[schemaCount];
        URI baseUri = URI.create("http://example.com/multi");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree("{}");
        for (int i = 0; i < schemaCount; i++) {
            schemas[i] = JsonSchema.from(validationContext, "path" + i, baseUri.resolve("schema" + i), node, null, false);
        }
        // Act & Assert – each schema must return the identical ValidationContext instance.
        for (int i = 0; i < schemaCount; i++) {
            assertSame(validationContext, schemas[i].getValidationContext(), "Schema #" + i + " should share the same ValidationContext instance");
        }
    }

    /**
     * Group: testGetValidationContextImmutability **
     */
    @Test
    void getValidationContext_immutability_noNewInstanceCreated() {
        // Act – retrieve the context twice.
        ValidationContext firstCall = jsonSchema.getValidationContext();
        ValidationContext secondCall = jsonSchema.getValidationContext();
        // Assert – both calls must return the exact same object reference.
        assertSame(firstCall, secondCall, "Subsequent calls to getValidationContext() should return the original instance, not a copy");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:15: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:18: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:21: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getValidationContext_Test
src/test/java/com/networknt/schema/JsonSchema_getValidationContext_Test.java:35: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        validationContext = new ValidationContext();
                            ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    no arguments
  reason: actual and formal argument lists differ in length
7 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.ValidationContext', 'dependent classes': 'class `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod ` ValidationContext getValidationContext()` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.getValidationContext()`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getValidationContext()`;\nclass `com.networknt.schema.MaxItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.getValidationContext()`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


