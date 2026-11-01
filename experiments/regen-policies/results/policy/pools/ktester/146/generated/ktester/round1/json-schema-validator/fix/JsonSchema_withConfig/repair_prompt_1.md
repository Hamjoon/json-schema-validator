
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class JsonSchema_withConfig_Test {

    /**
     * Verify that {@code withConfig} returns the same {@link JsonSchema}
     * instance when the supplied configuration is equal to the one already
     * used by the schema.
     */
    @Test
    void withConfigReturnsSameInstanceWhenConfigUnchanged() {
        // a simple schema without a discriminator keyword
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // configuration with discriminator enabled (the default for the meta‑schema)
        SchemaValidatorsConfig config = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, config);
        // invoke withConfig using an *equal* configuration (different instance)
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        JsonSchema result = schema.withConfig(equalConfig);
        // The method should return the original instance.
        assertSame(schema, result, "Expected the same JsonSchema instance when config is unchanged");
    }

    /**
     * Verify that {@code withConfig} creates a new {@link JsonSchema}
     * instance when the supplied configuration disables the {@code discriminator}
     * keyword while the meta‑schema contains this keyword. The method must
     * automatically enable the discriminator (and nullable) keyword.
     */
    @Test
    void withConfigEnablesDiscriminatorKeywordWhenDisabled() {
        // Schema that contains a discriminator keyword (supported by draft‑2020‑12)
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"," + "\"properties\": {" + "    \"type\": {\"type\": \"string\"}" + "}," + "\"discriminator\": {" + "    \"propertyName\": \"type\"," + "    \"mapping\": {" + "        \"a\": \"#/definitions/A\"" + "    }" + "}," + "\"definitions\": {" + "    \"A\": {" + "        \"type\": \"object\"," + "        \"properties\": {" + "            \"a\": {\"type\": \"string\"}" + "        }" + "    }" + "}" + "}";
        // Start with a configuration that explicitly disables the discriminator keyword
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).// also disabled to verify it gets enabled automatically
        nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        // Call withConfig using the same (disabled) configuration.
        // According to the implementation, the method should replace it with a new
        // configuration that enables both discriminator and nullable keywords.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        // The returned schema must be a *different* instance.
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config disables a required keyword");
        // Verify that the new schema's validation context now has the discriminator
        // keyword enabled (and nullable enabled as a side‑effect).
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should be enabled in the resulting configuration");
    }

    /**
     * The {@code withConfig} method must return the original {@link JsonSchema}
     * instance when the supplied configuration is equal (by {@code equals}) to
     * the configuration stored in the current {@link ValidationContext}.
     */
    @Test
    void sameConfigReturnsOriginalInstance() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // Configuration used to create the schema.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson, originalConfig);
        // Build a *different* instance that is equal to the original one.
        SchemaValidatorsConfig equalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchema result = schema.withConfig(equalConfig);
        assertSame(schema, result, "Expected the same JsonSchema instance when the supplied config is equal to the current one");
    }

    /**
     * When the supplied configuration differs from the one stored in the
     * {@link ValidationContext} and the meta‑schema does not force an automatic
     * change, {@code withConfig} must build a new {@link ValidationContext} and
     * return a freshly constructed {@link JsonSchema}.
     */
    @Test
    void differentConfigCreatesNewInstance() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        // Original configuration – discriminator enabled.
        SchemaValidatorsConfig originalConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).nullableKeywordEnabled(true).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, originalConfig);
        // New configuration – discriminator disabled (different from original).
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).// keep nullable the same to isolate the change
        nullableKeywordEnabled(true).build();
        JsonSchema newSchema = originalSchema.withConfig(newConfig);
        // Must be a different instance.
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config differs");
        // The new schema's ValidationContext must hold the *exact* configuration we passed.
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertEquals(newConfig, resultingConfig, "The resulting ValidationContext should contain the supplied configuration");
    }

    /**
     * If the meta‑schema contains the {@code discriminator} keyword but the
     * supplied configuration disables it, {@code withConfig} must automatically
     * enable both {@code discriminator} and {@code nullable} and return a new
     * {@link JsonSchema} instance.
     */
    @Test
    void discriminatorKeywordAutoEnabled() {
        // Schema that contains a discriminator keyword (supported by draft‑2020‑12).
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"," + "\"properties\": {\"type\": {\"type\": \"string\"}}," + "\"discriminator\": {" + "    \"propertyName\": \"type\"," + "    \"mapping\": {\"a\": \"#/definitions/A\"}" + "}," + "\"definitions\": {" + "    \"A\": {" + "        \"type\": \"object\"," + "        \"properties\": {\"a\": {\"type\": \"string\"}}" + "    }" + "}" + "}";
        // Configuration that explicitly disables the discriminator (and nullable).
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).nullableKeywordEnabled(false).build();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema originalSchema = factory.getSchema(schemaJson, disabledConfig);
        // The call should trigger the auto‑enable logic.
        JsonSchema newSchema = originalSchema.withConfig(disabledConfig);
        assertNotSame(originalSchema, newSchema, "Expected a new JsonSchema instance when the supplied config disables a required keyword");
        SchemaValidatorsConfig resultingConfig = newSchema.getValidationContext().getConfig();
        assertTrue(resultingConfig.isDiscriminatorKeywordEnabled(), "Discriminator keyword should be enabled in the resulting configuration");
        assertTrue(resultingConfig.isNullableKeywordEnabled(), "Nullable keyword should be enabled in the resulting configuration");
    }

    /**
     * Passing {@code null} as the {@link SchemaValidatorsConfig} must result in a
     * {@link NullPointerException}.
     */
    @Test
    void nullConfigThrowsNPE() {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(schemaJson);
        assertThrows(NullPointerException.class, () -> schema.withConfig(null), "Calling withConfig(null) should throw NullPointerException");
    }

    /**
     * If the {@link JsonSchema} instance has a {@code null} {@link ValidationContext},
     * calling {@code withConfig} must throw a {@link NullPointerException}.
     * This situation is simulated by creating a real schema and then using
     * reflection (or a Mockito spy) to make {@code getValidationContext()} return
     * {@code null}.
     */
    @Test
    void nullValidationContextThrowsNPE() throws Exception {
        String schemaJson = "{" + "\"$schema\": \"https://json-schema.org/draft/2020-12/schema\"," + "\"type\": \"object\"" + "}";
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V202012);
        JsonSchema realSchema = factory.getSchema(schemaJson);
        // Use reflection to nullify the internal validationContext.
        Field vcField = JsonSchema.class.getDeclaredField("validationContext");
        vcField.setAccessible(true);
        vcField.set(realSchema, null);
        SchemaValidatorsConfig anyConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(true).build();
        assertThrows(NullPointerException.class, () -> realSchema.withConfig(anyConfig), "Calling withConfig when ValidationContext is null should throw NullPointerException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:8: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_withConfig_Test.java:11: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * Used for creating a schema with validators for validating inputs. This is\n * created using {@link JsonSchemaFactory#getInstance(VersionFlag, Consumer)}\n * and should be cached for performance.\n * <p>\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n * <p>\n * JsonSchema instances are thread-safe provided its configuration is not\n * modified.\n ', 'return type': 'com.networknt.schema.JsonSchema', 'dependent classes': 'class `com.networknt.schema.SchemaValidatorsConfig`:\n\t\nclass `com.networknt.schema.ValidationMessageHandler`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonNodePath com.networknt.schema.ValidationMessageHandler.evaluationPath;\n\t\tcom.networknt.schema.ErrorMessageType com.networknt.schema.ValidationMessageHandler.errorMessageType;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.evaluationParentSchema;\n\t\tjava.util.Map<java.lang.String, java.lang.String> com.networknt.schema.ValidationMessageHandler.errorMessage;\n\t\tjava.lang.String com.networknt.schema.ValidationMessageHandler.errorMessageKeyword;\n\t\tcom.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;\n\t\tcom.networknt.schema.i18n.MessageSource com.networknt.schema.ValidationMessageHandler.messageSource;\n\t\tcom.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;\n\t\tcom.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;\n\trelated functions:\n\t\tmethod `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.JsonSchema.id;\n\t\tboolean com.networknt.schema.JsonSchema.recursiveAnchor;\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\n\t\tmethod `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: \n     * Creates a schema using the current one as a template with the parent as the\n     * ref.\n     * <p>\n     * This is typically used if this schema is a schema resource that can be\n     * pointed to by various references.\n     *\n     * @param refEvaluationParentSchema the parent ref\n     * @param refEvaluationPath the ref evaluation path\n     * @return the schema\n     ;\n\t\tmethod `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tboolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;\n\t\tcom.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;\n\t\nclass `com.networknt.schema.JsonSchemaFactory`:\n\trelated functions:\n\t\tmethod `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: \n     * Determines the validation context to use for the schema given the parent\n     * validation context.\n     * <p>\n     * This is typically the same validation context unless the schema has a\n     * different $schema from the parent.\n     * <p>\n     * If the schema does not define a $schema, the parent should be used.\n     * \n     * @param validationContext the parent validation context\n     * @param schemaNode        the schema node\n     * @return the validation context to use\n     ;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;\nclass `com.networknt.schema.UnevaluatedPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


