


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class JsonSchema_walk_3_Test {

    private static JsonSchemaFactory factory;

    private static JsonSchema schema;

    private static ObjectMapper mapper;

    // reused in many tests
    private static JsonSchema baseJsonSchema;

    @BeforeAll
    static void setupBeforeAll() {
        // a simple base schema – an object with a required string property "name"
        String baseSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"type\": \"object\"," + "\"properties\": {" + "    \"name\": {\"type\": \"string\"}" + "}," + "\"required\": [\"name\"]" + "}";
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        mapper = new ObjectMapper();
        baseJsonSchema = factory.getSchema(baseSchema);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods to work with ValidationResult without compile‑time
     *  dependencies on its concrete API (different library versions expose
     *  different method names).
     * --------------------------------------------------------------------- */
    private boolean isValid(ValidationResult result) {
        try {
            // newer versions expose isSuccess()
            Method m = result.getClass().getMethod("isSuccess");
            return (boolean) m.invoke(result);
        } catch (Exception e) {
            try {
                // older versions may expose isValid()
                Method m = result.getClass().getMethod("isValid");
                return (boolean) m.invoke(result);
            } catch (Exception ex) {
                throw new RuntimeException("Unable to determine validity of ValidationResult", ex);
            }
        }
    }

    @Test
    void walkValidNodeReturnsNoMessages() throws Exception {
        JsonNode validNode = mapper.readTree("{\"name\":\"John\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, validNode, true);
        Set<ValidationMessage> messages = getMessages(result);
        assertTrue(messages.isEmpty(), "Expected no validation messages for a valid node");
    }

    @Test
    void walkInvalidNodeReturnsMessages() throws Exception {
        JsonNode invalidNode = mapper.readTree("{\"name\":1}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, invalidNode, true);
        Set<ValidationMessage> messages = getMessages(result);
        assertFalse(messages.isEmpty(), "Expected validation messages for an invalid node");
        boolean containsTypeError = messages.stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(containsTypeError, "Expected a type‑error message");
    }

    /* --------------------------------------------------------------------- *
     *  1. walk_validNode – a valid instance should produce a successful result
     * --------------------------------------------------------------------- */
    @Test
    void walk_validNode() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"John\",\"age\":30}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        assertTrue(isValid(result), "Result must be valid for a conforming instance");
        assertTrue(getMessages(result).isEmpty(), "No validation messages expected");
    }

    /* --------------------------------------------------------------------- *
     *  2. walk_invalidNode – an invalid instance should produce errors
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidNode() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":123,\"age\":\"thirty\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, true);
        assertFalse(isValid(result), "Result must be invalid when instance violates schema");
        assertFalse(getMessages(result).isEmpty(), "At least one validation message expected");
        boolean typeError = getMessages(result).stream().anyMatch(m -> m.getMessage().toLowerCase().contains("type"));
        assertTrue(typeError, "A type‑error message should be present");
    }

    /* --------------------------------------------------------------------- *
     *  3. walk_validateFlagFalse – validation is skipped, always valid
     * --------------------------------------------------------------------- */
    @Test
    void walk_validateFlagFalse() throws Exception {
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, node, false);
        assertTrue(isValid(result), "When validate=false the result must be valid");
        assertTrue(getMessages(result).isEmpty(), "No messages should be produced when validation is disabled");
    }

    /* --------------------------------------------------------------------- *
     *  4. walk_nullNode – null instance node should be treated as invalid
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullNode() {
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ValidationResult result = baseJsonSchema.walk(ctx, null, true);
        assertFalse(isValid(result), "Null node must produce an invalid result");
        assertFalse(getMessages(result).isEmpty(), "A message describing the missing node is expected");
    }

    /* --------------------------------------------------------------------- *
     *  5. walk_nullExecutionContext – passing null should throw NPE
     * --------------------------------------------------------------------- */
    @Test
    void walk_nullExecutionContext() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Alice\"}");
        assertThrows(NullPointerException.class, () -> baseJsonSchema.walk(null, node, true), "A null ExecutionContext should cause NullPointerException");
    }

    /* --------------------------------------------------------------------- *
     *  6. walk_recursiveAnchor_true – schema with $recursiveAnchor resolves $recursiveRef
     * --------------------------------------------------------------------- */
    @Test
    void walk_recursiveAnchor_true() throws Exception {
        String recursiveSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$recursiveAnchor\": true," + "\"type\": \"object\"," + "\"properties\": {" + "  \"self\": {\"$recursiveRef\": \"#\"}" + "}" + "}";
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode node = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(isValid(result), "Recursive anchor should be processed without infinite recursion");
        assertTrue(getMessages(result).isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  7. walk_schemaResourceRoot – schema marked as a resource root works
     * --------------------------------------------------------------------- */
    @Test
    void walk_schemaResourceRoot() throws Exception {
        String resourceRootSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$id\": \"http://example.com/root.json\"," + "\"type\": \"object\"," + "\"properties\": {\"name\": {\"type\": \"string\"}}" + "}";
        JsonSchema schema = factory.getSchema(resourceRootSchema);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        ValidationResult result = schema.walk(ctx, node, true);
        assertTrue(isValid(result), "Schema resource root should not affect normal validation");
        assertTrue(getMessages(result).isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  8. walk_withCustomExecutionContextCustomizer – customizer does not alter outcome
     * --------------------------------------------------------------------- */
    @Test
    void walk_withCustomExecutionContextCustomizer() throws Exception {
        JsonNode node = mapper.readTree("{\"name\":\"Charlie\"}");
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {

            @Override
            public void customize(ExecutionContext executionContext, ValidationContext validationContext) {
                // no‑op for the test – we deliberately ignore both parameters
            }
        };
        ValidationResult resultWithCustomizer = baseJsonSchema.walk(ctx, node, true, customizer);
        ValidationResult resultWithoutCustomizer = baseJsonSchema.walk(ctx, node, true);
        assertEquals(isValid(resultWithoutCustomizer), isValid(resultWithCustomizer), "Customizer must not change the validity");
        assertEquals(getMessages(resultWithoutCustomizer).size(), getMessages(resultWithCustomizer).size(), "Customizer must not change the number of messages");
    }

    /* --------------------------------------------------------------------- *
     *  9. walk_invalidRefSyntax – malformed $ref should raise an exception
     * --------------------------------------------------------------------- */
    @Test
    void walk_invalidRefSyntax() throws Exception {
        String badRefSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"http://[invalid-uri]\"" + "}";
        JsonSchema schema = factory.getSchema(badRefSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Malformed $ref must cause JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 10. walk_unresolvableRef – $ref to a non‑existent definition should fail
     * --------------------------------------------------------------------- */
    @Test
    void walk_unresolvableRef() throws Exception {
        String unresolvableRefSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"#/components/schemas/Unknown\"" + "}";
        JsonSchema schema = factory.getSchema(unresolvableRefSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Unresolvable $ref must raise JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 11. walk_recursiveAnchorDepthExceeded – low recursion depth should be detected
     * --------------------------------------------------------------------- */
    @Test
    void walk_recursiveAnchorDepthExceeded() throws Exception {
        String recursiveSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$recursiveAnchor\": true," + "\"type\": \"object\"," + "\"properties\": {" + "  \"self\": {\"$recursiveRef\": \"#\"}" + "}" + "}";
        JsonSchema schema = factory.getSchema(recursiveSchema);
        JsonNode node = mapper.readTree("{\"self\":{}}");
        ExecutionContext ctx = schema.createExecutionContext();
        // set max recursion depth to 1 via reflection
        Field vcField = ExecutionContext.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        ValidationContext vc = (ValidationContext) vcField.get(ctx);
        Method setDepth = ValidationContext.class.getMethod("setMaxRecursionDepth", int.class);
        setDepth.invoke(vc, 1);
        assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "Recursion depth limit should trigger JsonSchemaException");
    }

    /* --------------------------------------------------------------------- *
     * 12. walk_executionContextMissingValidationContext – null ValidationContext
     * --------------------------------------------------------------------- */
    @Test
    void walk_executionContextMissingValidationContext() throws Exception {
        ExecutionContext ctx = baseJsonSchema.createExecutionContext();
        // make the internal ValidationContext null via reflection
        Field vcField = ExecutionContext.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(ctx, null);
        JsonNode node = mapper.readTree("{\"name\":\"Bob\"}");
        assertThrows(IllegalStateException.class, () -> baseJsonSchema.walk(ctx, node, true), "Missing ValidationContext inside ExecutionContext should cause IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     * 13. walk_unsupportedEncodingDuringRefResolution – percent‑encoded bad charset
     * --------------------------------------------------------------------- */
    @Test
    void walk_unsupportedEncodingDuringRefResolution() throws Exception {
        String badEncodingSchema = "{" + "\"$schema\": \"https://json-schema.org/draft/2019-09/schema\"," + "\"$ref\": \"%FF%FE\"" + "}";
        JsonSchema schema = factory.getSchema(badEncodingSchema);
        JsonNode node = mapper.readTree("{\"any\":\"value\"}");
        ExecutionContext ctx = schema.createExecutionContext();
        Exception ex = assertThrows(JsonSchemaException.class, () -> schema.walk(ctx, node, true), "UnsupportedEncodingException should be wrapped in JsonSchemaException");
        assertTrue(ex.getCause() instanceof UnsupportedEncodingException, "Root cause must be UnsupportedEncodingException");
    }

    @SuppressWarnings("unchecked")
    private Set<ValidationMessage> getMessages(ValidationResult result) {
        try {
            Method m = result.getClass().getMethod("getMessages");
            return (Set<ValidationMessage>) m.invoke(result);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve messages from ValidationResult", e);
        }
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_walk_3_Test ✔
│     ├─ walk_invalidNode() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_nullExecutionContext() ✔
│     ├─ walk_recursiveAnchor_true() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_withCustomExecutionContextCustomizer() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_invalidRefSyntax() ✘ Malformed $ref must cause JsonSchemaException ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
│     ├─ walk_schemaResourceRoot() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_validateFlagFalse() ✘ Unable to determine validity of ValidationResult
│     ├─ walkInvalidNodeReturnsMessages() ✘ Unable to retrieve messages from ValidationResult
│     ├─ walk_validNode() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_unresolvableRef() ✔
│     ├─ walk_nullNode() ✘ Unable to determine validity of ValidationResult
│     ├─ walk_unsupportedEncodingDuringRefResolution() ✘ UnsupportedEncodingException should be wrapped in JsonSchemaException ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
│     ├─ walk_executionContextMissingValidationContext() ✘ validationContext
│     ├─ walk_recursiveAnchorDepthExceeded() ✘ validationContext
│     └─ walkValidNodeReturnsNoMessages() ✘ Unable to retrieve messages from ValidationResult
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (13):
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_invalidNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_invalidNode', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_invalidNode(JsonSchema_walk_3_Test.java:94)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_recursiveAnchor_true()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_recursiveAnchor_true', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_recursiveAnchor_true(JsonSchema_walk_3_Test.java:142)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_withCustomExecutionContextCustomizer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_withCustomExecutionContextCustomizer', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_withCustomExecutionContextCustomizer(JsonSchema_walk_3_Test.java:176)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_invalidRefSyntax()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_invalidRefSyntax', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Malformed $ref must cause JsonSchemaException ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_invalidRefSyntax(JsonSchema_walk_3_Test.java:189)
       [...]
     Caused by: java.lang.IllegalArgumentException: Malformed IPv6 address at index 8: http://[invalid-uri]
       java.base/java.net.URI.create(URI.java:906)
       com.networknt.schema.resource.UriSchemaLoader.toURI(UriSchemaLoader.java:52)
       com.networknt.schema.resource.UriSchemaLoader.getSchema(UriSchemaLoader.java:35)
       com.networknt.schema.resource.DefaultSchemaLoader.getSchema(DefaultSchemaLoader.java:65)
       com.networknt.schema.JsonSchemaFactory.getMappedSchema(JsonSchemaFactory.java:670)
       [...]
     Caused by: java.net.URISyntaxException: Malformed IPv6 address at index 8: http://[invalid-uri]
       java.base/java.net.URI$Parser.fail(URI.java:2976)
       java.base/java.net.URI$Parser.parseIPv6Reference(URI.java:3613)
       java.base/java.net.URI$Parser.parseServer(URI.java:3357)
       java.base/java.net.URI$Parser.parseAuthority(URI.java:3284)
       java.base/java.net.URI$Parser.parseHierarchical(URI.java:3221)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_schemaResourceRoot()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_schemaResourceRoot', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_schemaResourceRoot(JsonSchema_walk_3_Test.java:156)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_validateFlagFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_validateFlagFalse', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_validateFlagFalse(JsonSchema_walk_3_Test.java:108)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walkInvalidNodeReturnsMessages()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walkInvalidNodeReturnsMessages', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to retrieve messages from ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.getMessages(JsonSchema_walk_3_Test.java:255)
       com.networknt.schema.JsonSchema_walk_3_Test.walkInvalidNodeReturnsMessages(JsonSchema_walk_3_Test.java:68)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.getMessages()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.getMessages(JsonSchema_walk_3_Test.java:252)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_validNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_validNode', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_validNode(JsonSchema_walk_3_Test.java:82)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_nullNode()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_nullNode', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to determine validity of ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:49)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_nullNode(JsonSchema_walk_3_Test.java:119)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.isValid()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.isValid(JsonSchema_walk_3_Test.java:46)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_unsupportedEncodingDuringRefResolution()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_unsupportedEncodingDuringRefResolution', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: UnsupportedEncodingException should be wrapped in JsonSchemaException ==> Unexpected exception type thrown, expected: <com.networknt.schema.JsonSchemaException> but was: <java.lang.IllegalArgumentException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_unsupportedEncodingDuringRefResolution(JsonSchema_walk_3_Test.java:245)
       [...]
     Caused by: java.lang.IllegalArgumentException: URI is not absolute
       java.base/java.net.URL.fromURI(URL.java:721)
       java.base/java.net.URI.toURL(URI.java:1139)
       com.networknt.schema.resource.UriSchemaLoader.toURL(UriSchemaLoader.java:66)
       com.networknt.schema.resource.UriSchemaLoader.getSchema(UriSchemaLoader.java:36)
       com.networknt.schema.resource.DefaultSchemaLoader.getSchema(DefaultSchemaLoader.java:65)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_executionContextMissingValidationContext()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_executionContextMissingValidationContext', methodParameterTypes = '']
    => java.lang.NoSuchFieldException: validationContext
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_executionContextMissingValidationContext(JsonSchema_walk_3_Test.java:229)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walk_recursiveAnchorDepthExceeded()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walk_recursiveAnchorDepthExceeded', methodParameterTypes = '']
    => java.lang.NoSuchFieldException: validationContext
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_walk_3_Test.walk_recursiveAnchorDepthExceeded(JsonSchema_walk_3_Test.java:214)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]
  JUnit Jupiter:JsonSchema_walk_3_Test:walkValidNodeReturnsNoMessages()
    MethodSource [className = 'com.networknt.schema.JsonSchema_walk_3_Test', methodName = 'walkValidNodeReturnsNoMessages', methodParameterTypes = '']
    => java.lang.RuntimeException: Unable to retrieve messages from ValidationResult
       com.networknt.schema.JsonSchema_walk_3_Test.getMessages(JsonSchema_walk_3_Test.java:255)
       com.networknt.schema.JsonSchema_walk_3_Test.walkValidNodeReturnsNoMessages(JsonSchema_walk_3_Test.java:59)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
     Caused by: java.lang.NoSuchMethodException: com.networknt.schema.ValidationResult.getMessages()
       java.base/java.lang.Class.getMethod(Class.java:2229)
       com.networknt.schema.JsonSchema_walk_3_Test.getMessages(JsonSchema_walk_3_Test.java:252)
       [...]

Test run finished after 435 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[        15 tests found           ]
[         0 tests skipped         ]
[        15 tests started         ]
[         0 tests aborted         ]
[         2 tests successful      ]
[        13 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * Used for creating a schema with validators for validating inputs. This is
 * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}
 * and should be cached for performance.
 * <p>
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 * <p>
 * JsonSchema instances are thread-safe provided its configuration is not
 * modified.
 

api document of method walk(ExecutionContext, JsonNode, boolean): 

     * Walk the JSON node.
     * 
     * @param executionContext the execution context
     * @param node             the input
     * @param validate         true to validate the input against the schema
     *
     * @return the validation result
     

return type: 
com.networknt.schema.ValidationResult

dependent classes: 
class `com.networknt.schema.ExecutionContext`:
	
class `com.networknt.schema.OutputFormat`:
	dependent fields:
		com.networknt.schema.OutputFormat.Result com.networknt.schema.OutputFormat.RESULT;
	
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: 
     * Walk the input.
     * 
     * @param executionContext the execution context
     * @param input            the input
     * @param inputFormat      the input format
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: 
     * Walk at the node.
     * 
     * @param executionContext the execution content
     * @param node             the current node
     * @param rootNode         the root node
     * @param instanceLocation the instance location
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
		method `public T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: 
     * Walk the JSON node.
     * 
     * @param <T>         the result type
     * @param executionContext the execution context
     * @param node             the input
     * @param outputFormat     the output format
     * @param validate         true to validate the input against the schema
     * @param executionCustomizer the customizer
     *
     * @return the validation result
     ;
		method `public T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: 
     * Walk the input.
     * 
     * @param <T>         the result type
     * @param executionContext the execution context
     * @param input            the input
     * @param inputFormat      the input format
     * @param outputFormat     the output format
     * @param validate         true to validate the input against the schema
     * @return the validation result
     ;
class `com.networknt.schema.BaseJsonValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
