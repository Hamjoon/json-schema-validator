
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
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
            public void customize(ExecutionContext executionContext) {
                // no‑op for the test
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

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:167: error: <anonymous com.networknt.schema.JsonSchema_walk_3_Test$1> is not abstract and does not override abstract method customize(ExecutionContext,ValidationContext) in ExecutionContextCustomizer
        ExecutionContextCustomizer customizer = new ExecutionContextCustomizer() {
                                                                                 ^
src/test/java/com/networknt/schema/JsonSchema_walk_3_Test.java:169: error: method does not override or implement a method from a supertype
            @Override
            ^
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method walk(ExecutionContext, JsonNode, boolean)': '\n     * Walk the JSON node.\n     * \n     * @param executionContext the execution context\n     * @param node             the input\n     * @param validate         true to validate the input against the schema\n     *\n     * @return the validation result\n     ', 'return type': 'com.networknt.schema.ValidationResult', 'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Result com.networknt.schema.OutputFormat.RESULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public ValidationResult walk(ExecutionContext, String, InputFormat, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: \n     * Walk the input.\n     * \n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public ValidationResult walkAtNode(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: \n     * Walk at the node.\n     * \n     * @param executionContext the execution content\n     * @param node             the current node\n     * @param rootNode         the root node\n     * @param instanceLocation the instance location\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, JsonNode, OutputFormat<T>, boolean, ExecutionContextCustomizer)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: \n     * Walk the JSON node.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param node             the input\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @param executionCustomizer the customizer\n     *\n     * @return the validation result\n     ;\n\t\tmethod `public T walk(ExecutionContext, String, InputFormat, OutputFormat<T>, boolean)` returns `T`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`, api document: \n     * Walk the input.\n     * \n     * @param <T>         the result type\n     * @param executionContext the execution context\n     * @param input            the input\n     * @param inputFormat      the input format\n     * @param outputFormat     the output format\n     * @param validate         true to validate the input against the schema\n     * @return the validation result\n     ;\nclass `com.networknt.schema.BaseJsonValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


