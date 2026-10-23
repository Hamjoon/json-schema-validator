
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_walk_Test {

    private static JsonSchemaFactory factory;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void setupBeforeAll() {
        // Use a recent draft version; the exact version is not important for the
        // behaviour we are testing.
        factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    }

    /**
     * Parses a JSON string into a {@link JsonNode}.
     */
    private static JsonNode getJsonNodeFromStringContent(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    /**
     * Retrieves the root {@link JsonValidator} from a {@link JsonSchema} instance
     * via reflection.  The exact field name depends on the library version; we
     * try the known possibilities.
     */
    private static JsonValidator getRootValidator(JsonSchema schema) throws Exception {
        // The field is called "validator" in many versions.
        Field validatorField;
        try {
            validatorField = JsonSchema.class.getDeclaredField("validator");
        } catch (NoSuchFieldException e) {
            // Fallback to "validators" (a list) – pick the first element.
            validatorField = JsonSchema.class.getDeclaredField("validators");
            validatorField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonValidator> list = (List<JsonValidator>) validatorField.get(schema);
            return list.isEmpty() ? null : list.get(0);
        }
        validatorField.setAccessible(true);
        return (JsonValidator) validatorField.get(schema);
    }

    /**
     * Recursively searches the validator tree for an instance of
     * {@link PropertiesValidator}.  The tree can contain composite validators
     * (e.g. {@code ObjectValidator}) that hold child validators in a collection.
     */
    private static PropertiesValidator findPropertiesValidator(JsonValidator validator) throws Exception {
        if (validator instanceof PropertiesValidator) {
            return (PropertiesValidator) validator;
        }
        // Many validators expose a field called "validators" (a List<JsonValidator>)
        // or "schemaValidators" (a Map<String, JsonValidator>).  We inspect both.
        Field listField = null;
        try {
            listField = validator.getClass().getDeclaredField("validators");
        } catch (NoSuchFieldException ignored) {
        }
        if (listField != null) {
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<JsonValidator> children = (List<JsonValidator>) listField.get(validator);
            for (JsonValidator child : children) {
                PropertiesValidator found = findPropertiesValidator(child);
                if (found != null) {
                    return found;
                }
            }
        }
        // Try a map named "schemaValidators"
        Field mapField = null;
        try {
            mapField = validator.getClass().getDeclaredField("schemaValidators");
        } catch (NoSuchFieldException ignored) {
        }
        if (mapField != null) {
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, JsonValidator> map = (java.util.Map<String, JsonValidator>) mapField.get(validator);
            for (JsonValidator child : map.values()) {
                PropertiesValidator found = findPropertiesValidator(child);
                if (found != null) {
                    return found;
                }
            }
        }
        // No matching validator found in this branch.
        return null;
    }

    /**
     * When {@code shouldValidateSchema} is {@code true} the validator must perform
     * the actual validation and return any {@link ValidationMessage}s produced by
     * the underlying schema.
     */
    @Test
    void walkShouldValidateWhenFlagTrue() throws Exception {
        // schema: property "age" must be an integer
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // node violates the schema – "age" is a string, not an integer
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");
        // invoke the walk method with validation enabled
        ValidationResult result = schema.walk(node, true);
        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertFalse(messages.isEmpty(), "Expected validation messages because the instance does not satisfy the schema");
    }

    /**
     * When {@code shouldValidateSchema} is {@code false} the validator must only
     * walk the schema (triggering any walk listeners) but must **not** perform
     * validation. Consequently the returned message set should be empty.
     */
    @Test
    void walkShouldSkipValidationWhenFlagFalse() throws Exception {
        // same schema as above – property "age" must be an integer
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // node still violates the schema, but we ask the walk method *not* to validate
        JsonNode node = getJsonNodeFromStringContent("{\"age\":\"not-an-int\"}");
        // invoke the walk method with validation disabled
        ValidationResult result = schema.walk(node, false);
        Set<ValidationMessage> messages = result.getValidationMessages();
        Assertions.assertTrue(messages.isEmpty(), "Expected no validation messages because validation was disabled");
    }

    /**
     * {@code PropertiesValidator.walk} first checks the {@code applyDefaultsStrategy}
     * field (declared in {@code BaseJsonValidator}).  If that field is {@code null}
     * a {@link NullPointerException} is thrown when the method tries to call
     * {@code applyDefaultsStrategy.shouldApplyPropertyDefaults()}.
     *
     * <p>This test forces the field to {@code null} via reflection and verifies
     * that the expected exception is thrown.</p>
     */
    @Test
    void testWalkApplyDefaultsStrategyNull(@Mock ExecutionContext mockExecutionContext) throws Exception {
        // --------------------------------------------------------------------
        // 1️⃣ Build a schema that contains a "properties" keyword – this guarantees
        //    that a {@code PropertiesValidator} instance exists inside the validator
        //    tree.
        // --------------------------------------------------------------------
        String schemaJson = """
            {
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              }
            }
            """;
        JsonSchema schema = factory.getSchema(schemaJson);
        // --------------------------------------------------------------------
        // 2️⃣ Locate the concrete {@code PropertiesValidator} inside the schema.
        // --------------------------------------------------------------------
        JsonValidator rootValidator = getRootValidator(schema);
        PropertiesValidator propertiesValidator = findPropertiesValidator(rootValidator);
        Assertions.assertNotNull(propertiesValidator, "Unable to locate PropertiesValidator inside the compiled schema – test cannot proceed.");
        // --------------------------------------------------------------------
        // 3️⃣ Set the private field {@code applyDefaultsStrategy} (declared in
        //    {@code BaseJsonValidator}) to {@code null}.
        // --------------------------------------------------------------------
        Field defaultsStrategyField = BaseJsonValidator.class.getDeclaredField("applyDefaultsStrategy");
        defaultsStrategyField.setAccessible(true);
        defaultsStrategyField.set(propertiesValidator, null);
        // --------------------------------------------------------------------
        // 4️⃣ Prepare a non‑null {@code ObjectNode} as required by the test case.
        // --------------------------------------------------------------------
        ObjectNode node = (ObjectNode) MAPPER.readTree("{\"name\":\"John\"}");
        // --------------------------------------------------------------------
        // 5️⃣ Invoke the method under test and assert that a NPE is thrown.
        // --------------------------------------------------------------------
        // deliberately null
        ExecutionContext executionContext = null;
        Assertions.assertThrows(NullPointerException.class, () -> propertiesValidator.walk(executionContext, node, null, "/", true), "Expected NullPointerException because applyDefaultsStrategy was set to null");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/PropertiesValidator_walk_Test.java:11: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/PropertiesValidator_walk_Test.java:12: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/PropertiesValidator_walk_Test.java:14: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/PropertiesValidator_walk_Test.java:165: error: cannot find symbol
    void testWalkApplyDefaultsStrategyNull(@Mock ExecutionContext mockExecutionContext) throws Exception {
                                            ^
  symbol:   class Mock
  location: class PropertiesValidator_walk_Test
4 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\tcom.networknt.schema.ApplyDefaultsStrategy com.networknt.schema.BaseJsonValidator.applyDefaultsStrategy;\n\t\nclass `com.networknt.schema.PropertiesValidator`:\n\tdependent fields:\n\t\tjava.util.Map<java.lang.String, com.networknt.schema.JsonSchema> com.networknt.schema.PropertiesValidator.schemas;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\n\t\tmethod `private void applyPropertyDefaults(ObjectNode)` returns `void`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.ItemsValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\n\t\tmethod `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, api document: \n     * This method both validates and collects the data in a CollectorContext.\n     * Unlike others this methods cleans and removes everything from collector\n     * context before returning.\n     * @param executionContext ExecutionContext\n     * @param jsonNode JsonNode\n     * @param rootNode JsonNode\n     * @param at       String path\n     *\n     * @return ValidationResult\n     ;\n\t\tmethod `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;\nclass `com.networknt.schema.format.DateTimeValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.PropertiesValidator.walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


