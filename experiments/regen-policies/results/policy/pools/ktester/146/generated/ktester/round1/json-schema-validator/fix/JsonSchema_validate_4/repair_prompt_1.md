
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.api.extension.ExtendWith;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Locale;
import com.networknt.schema.i18n.MessageSource;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class JsonSchema_validate_4_Test {

    private static JsonSchemaFactory factory;

    private ObjectMapper mapper;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a factory for the 2020‑12 draft – this is the public way to obtain a JsonSchema
        factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
    }

    @BeforeEach
    void setupBeforeEach() {
        mapper = new ObjectMapper();
    }

    /**
     * Validates that the {@code validate(JsonNode, ExecutionContextCustomizer)} method
     * correctly returns a validation error when the instance does not conform to the
     * schema and that a customizer can be supplied without interfering with the result.
     */
    @Test
    void validate_WithExecutionContextCustomizer_ReturnsExpectedMessages(@Mock ExecutionContextCustomizer customizer) {
        // GIVEN – a simple schema that requires a string property "name"
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // AND – an input JSON where "name" is an integer (invalid according to the schema)
        String inputJson = """
            {
              "name": 123
            }
            """;
        JsonNode inputNode = mapper.readTree(inputJson);
        // WHEN – the schema is validated with a no‑op customizer
        ExecutionContextCustomizer customizer = ctx -> {
        };
        Set<ValidationMessage> messages = schema.validate(inputNode, customizer);
        // THEN – exactly one validation message is produced and it mentions a type error
        assertEquals(1, messages.size(), "Expected a single validation error");
        ValidationMessage message = messages.iterator().next();
        assertTrue(message.getMessage().toLowerCase().contains("type"), "Validation message should refer to a type mismatch");
    }

    @Test
    void validate_Successful_NoCustomizer_ReturnsEmptySet() throws Exception {
        // schema: name (string) and age (integer) required
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": [ "name", "age" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode validNode = mapper.readTree("""
            { "name":"John","age":30 }""");
        Set<ValidationMessage> messages = schema.validate(validNode, (ExecutionContextCustomizer) null);
        assertTrue(messages.isEmpty(), "A valid document must produce no validation messages");
    }

    @Test
    void validate_Successful_NoOpCustomizer_ReturnsEmptySet(@Mock ExecutionContextCustomizer noop) {
        // same schema as above
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": [ "name", "age" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode validNode = mapper.createObjectNode().put("name", "John").put("age", 30);
        // a no‑op customizer does nothing
        ExecutionContextCustomizer noop = ctx -> {
        };
        Set<ValidationMessage> messages = schema.validate(validNode, noop);
        assertTrue(messages.isEmpty(), "No‑op customizer must not affect a successful validation");
    }

    @Test
    void validate_Failure_TypeMismatches_ReturnsTwoMessages() throws Exception {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" },
                "age":  { "type": "integer" }
              },
              "required": [ "name", "age" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode badNode = mapper.readTree("""
            { "name":123, "age":"thirty" }""");
        Set<ValidationMessage> messages = schema.validate(badNode, (ExecutionContextCustomizer) null);
        assertEquals(2, messages.size(), "Both 'name' and 'age' have wrong types");
        // simple sanity check – messages should contain the property names
        List<String> msgs = messages.stream().map(ValidationMessage::getPath).toList();
        assertTrue(msgs.contains("$.name"), "Message for 'name' must be present");
        assertTrue(msgs.contains("$.age"), "Message for 'age' must be present");
    }

    @Test
    void validate_MissingRequired_DisabledByCustomizer_ReturnsEmptySet() throws Exception {
        String schemaJson = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": [ "name" ]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode emptyNode = mapper.readTree("{}");
        // customizer disables the required‑field validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
        Set<ValidationMessage> messages = schema.validate(emptyNode, customizer);
        assertTrue(messages.isEmpty(), "Disabling the required validator should allow an otherwise invalid instance to pass");
    }

    @Test
    void validate_NullRootNode_ThrowsException() {
        String schemaJson = """
            { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class, () -> schema.validate((JsonNode) null, (ExecutionContextCustomizer) null), "Passing a null JsonNode should result in NPE");
    }

    @Test
    void validate_CustomizerThrowsRuntimeException_Propagates() throws Exception {
        String schemaJson = """
            { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        ExecutionContextCustomizer exploding = ctx -> {
            throw new RuntimeException("customizer failure");
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> schema.validate(node, exploding), "Exception from customizer must propagate");
        assertEquals("customizer failure", ex.getMessage());
    }

    @Test
    void validate_DisableMinimumValidator_AllowsNegativeValue() throws Exception {
        // schema with minimum 0 on property "price"
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"object",
              "properties":{ "price":{ "type":"number", "minimum":0 } },
              "required":["price"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "price": -5 }""");
        // customizer disables the minimum validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMin(false);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertTrue(messages.isEmpty(), "When the minimum validator is disabled the negative value must not produce an error");
    }

    @Test
    void validate_DisableRequiredValidator_AllowsMissingPrice() throws Exception {
        // same schema as above
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"object",
              "properties":{ "price":{ "type":"number", "minimum":0 } },
              "required":["price"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("{}");
        // disable required validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertTrue(messages.isEmpty(), "Disabling the required validator should allow the missing 'price' property");
    }

    @Test
    void validate_FailFastEnabled_ReturnsSingleMessage() throws Exception {
        // schema with three independent constraints
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"object",
              "properties":{
                "name":{ "type":"string" },
                "age": { "type":"integer" },
                "email":{ "type":"string", "format":"email" }
              },
              "required":["name","age","email"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "name":123, "age":"thirty", "email":null }""");
        // enable fail‑fast mode
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setFailFast(true);
        Set<ValidationMessage> messages = schema.validate(node, customizer);
        assertEquals(1, messages.size(), "Fail‑fast should stop after the first encountered validation error");
    }

    @Test
    void validate_ArraySchema_TypeMismatchInSecondElement() throws Exception {
        // schema: array of objects with integer "id"
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"array",
              "items":{
                "type":"object",
                "properties":{ "id":{ "type":"integer" } },
                "required":["id"]
              },
              "minItems":1
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            [ {"id":1}, {"id":"two"} ]""");
        Set<ValidationMessage> messages = schema.validate(node, (ExecutionContextCustomizer) null);
        assertFalse(messages.isEmpty(), "A type mismatch in the second element must be reported");
        // ensure the path points to the second element
        boolean containsSecond = messages.stream().anyMatch(m -> "$[1].id".equals(m.getPath()));
        assertTrue(containsSecond, "Message should reference $[1].id");
    }

    @Test
    void validate_ArraySchema_MinItemsDisabled_EmptyArrayPasses() throws Exception {
        // same schema as above
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"array",
              "items":{
                "type":"object",
                "properties":{ "id":{ "type":"integer" } },
                "required":["id"]
              },
              "minItems":1
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode emptyArray = mapper.readTree("[]");
        // disable the min‑items validator
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMinItems(false);
        Set<ValidationMessage> messages = schema.validate(emptyArray, customizer);
        assertTrue(messages.isEmpty(), "When minItems validation is disabled an empty array must pass");
    }

    @Test
    void validate_RefWithInvalidPercentEncoding_ThrowsUnsupportedEncodingException() {
        // schema that contains a malformed $ref
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"invalid%ZZschema.json"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        // the exception is thrown during validation when the $ref is resolved
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "Malformed percent‑encoding in $ref must result in UnsupportedEncodingException");
    }

    @Test
    void validate_RefToNonexistentSchema_ThrowsIllegalArgumentException() {
        // $ref points to a file that does not exist
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"nonexistent-schema.json"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "A $ref that cannot be resolved must cause an IllegalArgumentException");
    }

    @Test
    void validate_MessageSourceFrench_ProducesFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"object",
              "properties":{ "name":{ "type":"string" } },
              "required":["name"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "name":123 }""");
        // customizer switches the MessageSource to French
        ExecutionContextCustomizer frenchCustomizer = ctx -> ctx.setMessageSource(MessageSource.forLocale(Locale.FRENCH));
        Set<ValidationMessage> messages = schema.validate(node, frenchCustomizer);
        assertFalse(messages.isEmpty(), "Validation should produce a message");
        String text = messages.iterator().next().getMessage();
        // we only assert that the message contains a French word (e.g., "type")
        assertTrue(text.toLowerCase(Locale.FRENCH).contains("type") || text.toLowerCase(Locale.FRENCH).contains("chaîne"), "Message should be localized to French");
    }

    @Test
    void validate_CustomizerSetsNegativeMaxDepth_ThrowsIllegalArgumentException() {
        String schemaJson = """
            { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.getConfig().setMaxDepth(-1);
        assertThrows(IllegalArgumentException.class, () -> schema.validate(node, badCustomizer), "Negative maxDepth must be rejected with IllegalArgumentException");
    }

    @Test
    void validate_CustomizerDisablesAllValidators_ThrowsIllegalStateException() {
        String schemaJson = """
            { "$schema":"https://json-schema.org/draft/2020-12/schema", "type":"object" }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.createObjectNode();
        ExecutionContextCustomizer disableAll = ctx -> ctx.getConfig().setValidateAll(false);
        assertThrows(IllegalStateException.class, () -> schema.validate(node, disableAll), "Disabling all validators should make the schema unusable and raise IllegalStateException");
    }

    @Test
    void validate_NullMessageSource_ThrowsNullPointerException() throws Exception {
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "type":"object",
              "properties":{ "age":{ "type":"integer" } },
              "required":["age"]
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode node = mapper.readTree("""
            { "age":"thirty" }""");
        ExecutionContextCustomizer nullMessageSource = ctx -> ctx.setMessageSource(null);
        assertThrows(NullPointerException.class, () -> schema.validate(node, nullMessageSource), "A null MessageSource must cause NPE when a message is formatted");
    }

    @Test
    void validate_RefWithIncompletePercentEscape_ThrowsUnsupportedEncodingException() {
        String schemaJson = """
            {
              "$schema":"https://json-schema.org/draft/2020-12/schema",
              "$ref":"#/definitions/invalid%2"
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        JsonNode dummy = mapper.createObjectNode();
        assertThrows(UnsupportedEncodingException.class, () -> schema.validate(dummy, (ExecutionContextCustomizer) null), "Incomplete percent‑escape in $ref fragment must raise UnsupportedEncodingException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:14: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:44: error: cannot find symbol
    void validate_WithExecutionContextCustomizer_ReturnsExpectedMessages(@Mock ExecutionContextCustomizer customizer) {
                                                                          ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:96: error: cannot find symbol
    void validate_Successful_NoOpCustomizer_ReturnsEmptySet(@Mock ExecutionContextCustomizer noop) {
                                                             ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:326: error: cannot find symbol
    void validate_MessageSourceFrench_ProducesFrenchMessage(@Mock ExecutionContextCustomizer customizer) throws Exception {
                                                             ^
  symbol:   class Mock
  location: class JsonSchema_validate_4_Test
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:65: error: variable customizer is already defined in method validate_WithExecutionContextCustomizer_ReturnsExpectedMessages(ExecutionContextCustomizer)
        ExecutionContextCustomizer customizer = ctx -> {
                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:65: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> {
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:112: error: variable noop is already defined in method validate_Successful_NoOpCustomizer_ReturnsEmptySet(ExecutionContextCustomizer)
        ExecutionContextCustomizer noop = ctx -> {
                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:112: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer noop = ctx -> {
                                          ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:137: error: invalid method reference
        List<String> msgs = messages.stream().map(ValidationMessage::getPath).toList();
                                                  ^
  cannot find symbol
    symbol:   method getPath()
    location: class ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:157: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:157: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:178: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer exploding = ctx -> {
                                               ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:200: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMin(false);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:200: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMin(false);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:219: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:219: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateRequired(false);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:243: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setFailFast(true);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:243: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setFailFast(true);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:269: error: cannot find symbol
        boolean containsSecond = messages.stream().anyMatch(m -> "$[1].id".equals(m.getPath()));
                                                                                   ^
  symbol:   method getPath()
  location: variable m of type ValidationMessage
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:291: error: cannot find symbol
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMinItems(false);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:291: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer customizer = ctx -> ctx.getConfig().setValidateMinItems(false);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:339: error: cannot find symbol
        ExecutionContextCustomizer frenchCustomizer = ctx -> ctx.setMessageSource(MessageSource.forLocale(Locale.FRENCH));
                                                                                               ^
  symbol:   method forLocale(Locale)
  location: interface MessageSource
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:339: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer frenchCustomizer = ctx -> ctx.setMessageSource(MessageSource.forLocale(Locale.FRENCH));
                                                      ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:354: error: cannot find symbol
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.getConfig().setMaxDepth(-1);
                                                             ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:354: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer badCustomizer = ctx -> ctx.getConfig().setMaxDepth(-1);
                                                   ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:365: error: cannot find symbol
        ExecutionContextCustomizer disableAll = ctx -> ctx.getConfig().setValidateAll(false);
                                                          ^
  symbol:   method getConfig()
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:365: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer disableAll = ctx -> ctx.getConfig().setValidateAll(false);
                                                ^
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:382: error: cannot find symbol
        ExecutionContextCustomizer nullMessageSource = ctx -> ctx.setMessageSource(null);
                                                                 ^
  symbol:   method setMessageSource(<null>)
  location: variable ctx of type ExecutionContext
src/test/java/com/networknt/schema/JsonSchema_validate_4_Test.java:382: error: incompatible types: incompatible parameter types in lambda expression
        ExecutionContextCustomizer nullMessageSource = ctx -> ctx.setMessageSource(null);
                                                       ^
31 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'api document of method validate(JsonNode, ExecutionContextCustomizer)': '\n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param rootNode            the root node\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ', 'dependent classes': 'class `com.networknt.schema.ExecutionContextCustomizer`:\n\t\nclass `com.networknt.schema.OutputFormat`:\n\tdependent fields:\n\t\tcom.networknt.schema.OutputFormat.Default com.networknt.schema.OutputFormat.DEFAULT;\n\t\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public T validate(JsonNode, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validates the given root JsonNode, starting at the root of the data path. The\n     * output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>      the result type\n     * @param rootNode the root node\n     * @param format   the formatter\n     * @return the result\n     ;\n\t\tmethod `public T validate(String, InputFormat, OutputFormat<T>)` returns `T`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validates the given input string using the input format, starting at the root\n     * of the data path. The output will be formatted using the formatter specified.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param <T>         the result type\n     * @param input       the input\n     * @param inputFormat the inputFormat\n     * @param format      the formatter\n     * @return the result\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(String, InputFormat, ExecutionContextCustomizer)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validate the given input string using the input format, starting at the root\n     * of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     *\n     * @param input               the input\n     * @param inputFormat         the inputFormat\n     * @param executionCustomizer the execution customizer\n     * @return the assertions\n     ;\n\t\tmethod `public Set<ValidationMessage> validate(JsonNode)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Validate the given root JsonNode, starting at the root of the data path.\n     * <p>\n     * Note that since Draft 2019-09 by default format generates only annotations\n     * and not assertions.\n     * <p>\n     * Use {@link ExecutionConfig#setFormatAssertionsEnabled(Boolean)} to override\n     * the default.\n     * \n     * @param rootNode the root node\n     * @return A list of ValidationMessage if there is any validation error, or an\n     *         empty list if there is no error.\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.validate(JsonNode, ExecutionContextCustomizer)`, api document: \n     * Returns the singleton instance of this validator.\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


