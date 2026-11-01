


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import java.lang.reflect.Field;
import org.junit.jupiter.api.*;

// @ExtendWith(MockitoExtension.class)
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
        SchemaValidatorsConfig disabledConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(// also disabled to verify it gets enabled automatically
        false).nullableKeywordEnabled(false).build();
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
        SchemaValidatorsConfig newConfig = SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(// keep nullable the same to isolate the change
        false).nullableKeywordEnabled(true).build();
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

17:23:37.716 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword discriminator - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_withConfig_Test ✔
│     ├─ withConfigReturnsSameInstanceWhenConfigUnchanged() ✘ Expected the same JsonSchema instance when config is unchanged ==> expected: com.networknt.schema.JsonSchema@10cf09e8<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}> but was: com.networknt.schema.JsonSchema@1921ad94<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}>
│     ├─ withConfigEnablesDiscriminatorKeywordWhenDisabled() ✘ Expected a new JsonSchema instance when the supplied config disables a required keyword ==> expected: not same but was: <"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"type":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/definitions/A"}},"definitions":{"A":{"type":"object","properties":{"a":{"type":"string"}}}}}>
│     ├─ nullValidationContextThrowsNPE() ✘ validationContext
│     ├─ sameConfigReturnsOriginalInstance() ✘ Expected the same JsonSchema instance when the supplied config is equal to the current one ==> expected: com.networknt.schema.JsonSchema@4b3ed2f0<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}> but was: com.networknt.schema.JsonSchema@4fad9bb2<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}>
│     ├─ nullConfigThrowsNPE() ✘ Calling withConfig(null) should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
│     ├─ discriminatorKeywordAutoEnabled() ✘ Expected a new JsonSchema instance when the supplied config disables a required keyword ==> expected: not same but was: <"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"type":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/definitions/A"}},"definitions":{"A":{"type":"object","properties":{"a":{"type":"string"}}}}}>
│     └─ differentConfigCreatesNewInstance() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:JsonSchema_withConfig_Test:withConfigReturnsSameInstanceWhenConfigUnchanged()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'withConfigReturnsSameInstanceWhenConfigUnchanged', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected the same JsonSchema instance when config is unchanged ==> expected: com.networknt.schema.JsonSchema@10cf09e8<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}> but was: com.networknt.schema.JsonSchema@1921ad94<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertSame.failNotSame(AssertSame.java:50)
       org.junit.jupiter.api.AssertSame.assertSame(AssertSame.java:35)
       org.junit.jupiter.api.Assertions.assertSame(Assertions.java:2873)
       com.networknt.schema.JsonSchema_withConfig_Test.withConfigReturnsSameInstanceWhenConfigUnchanged(JsonSchema_withConfig_Test.java:28)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_withConfig_Test:withConfigEnablesDiscriminatorKeywordWhenDisabled()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'withConfigEnablesDiscriminatorKeywordWhenDisabled', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected a new JsonSchema instance when the supplied config disables a required keyword ==> expected: not same but was: <"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"type":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/definitions/A"}},"definitions":{"A":{"type":"object","properties":{"a":{"type":"string"}}}}}>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotSame.failSame(AssertNotSame.java:49)
       org.junit.jupiter.api.AssertNotSame.assertNotSame(AssertNotSame.java:35)
       org.junit.jupiter.api.Assertions.assertNotSame(Assertions.java:2912)
       com.networknt.schema.JsonSchema_withConfig_Test.withConfigEnablesDiscriminatorKeywordWhenDisabled(JsonSchema_withConfig_Test.java:51)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_withConfig_Test:nullValidationContextThrowsNPE()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'nullValidationContextThrowsNPE', methodParameterTypes = '']
    => java.lang.NoSuchFieldException: validationContext
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchema_withConfig_Test.nullValidationContextThrowsNPE(JsonSchema_withConfig_Test.java:148)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:767)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]
  JUnit Jupiter:JsonSchema_withConfig_Test:sameConfigReturnsOriginalInstance()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'sameConfigReturnsOriginalInstance', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected the same JsonSchema instance when the supplied config is equal to the current one ==> expected: com.networknt.schema.JsonSchema@4b3ed2f0<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}> but was: com.networknt.schema.JsonSchema@4fad9bb2<"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object"}>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertSame.failNotSame(AssertSame.java:50)
       org.junit.jupiter.api.AssertSame.assertSame(AssertSame.java:35)
       org.junit.jupiter.api.Assertions.assertSame(Assertions.java:2873)
       com.networknt.schema.JsonSchema_withConfig_Test.sameConfigReturnsOriginalInstance(JsonSchema_withConfig_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_withConfig_Test:nullConfigThrowsNPE()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'nullConfigThrowsNPE', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling withConfig(null) should throw NullPointerException ==> Expected java.lang.NullPointerException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3153)
       com.networknt.schema.JsonSchema_withConfig_Test.nullConfigThrowsNPE(JsonSchema_withConfig_Test.java:132)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:767)
       [...]
  JUnit Jupiter:JsonSchema_withConfig_Test:discriminatorKeywordAutoEnabled()
    MethodSource [className = 'com.networknt.schema.JsonSchema_withConfig_Test', methodName = 'discriminatorKeywordAutoEnabled', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Expected a new JsonSchema instance when the supplied config disables a required keyword ==> expected: not same but was: <"" : {"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"type":{"type":"string"}},"discriminator":{"propertyName":"type","mapping":{"a":"#/definitions/A"}},"definitions":{"A":{"type":"object","properties":{"a":{"type":"string"}}}}}>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertNotSame.failSame(AssertNotSame.java:49)
       org.junit.jupiter.api.AssertNotSame.assertNotSame(AssertNotSame.java:35)
       org.junit.jupiter.api.Assertions.assertNotSame(Assertions.java:2912)
       com.networknt.schema.JsonSchema_withConfig_Test.discriminatorKeywordAutoEnabled(JsonSchema_withConfig_Test.java:117)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 272 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         7 tests found           ]
[         0 tests skipped         ]
[         7 tests started         ]
[         0 tests aborted         ]
[         1 tests successful      ]
[         6 tests failed          ]


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
 

return type: 
com.networknt.schema.JsonSchema

dependent classes: 
class `com.networknt.schema.SchemaValidatorsConfig`:
	
class `com.networknt.schema.ValidationMessageHandler`:
	dependent fields:
		com.networknt.schema.JsonNodePath com.networknt.schema.ValidationMessageHandler.evaluationPath;
		com.networknt.schema.ErrorMessageType com.networknt.schema.ValidationMessageHandler.errorMessageType;
		com.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.evaluationParentSchema;
		java.util.Map<java.lang.String, java.lang.String> com.networknt.schema.ValidationMessageHandler.errorMessage;
		java.lang.String com.networknt.schema.ValidationMessageHandler.errorMessageKeyword;
		com.networknt.schema.Keyword com.networknt.schema.ValidationMessageHandler.keyword;
		com.networknt.schema.i18n.MessageSource com.networknt.schema.ValidationMessageHandler.messageSource;
		com.networknt.schema.SchemaLocation com.networknt.schema.ValidationMessageHandler.schemaLocation;
		com.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;
	related functions:
		method `protected MessageSourceValidationMessage.Builder message()` returns `com.networknt.schema.MessageSourceValidationMessage.Builder`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
class `com.networknt.schema.JsonSchema`:
	dependent fields:
		java.lang.String com.networknt.schema.JsonSchema.id;
		boolean com.networknt.schema.JsonSchema.recursiveAnchor;
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
		method `public JsonSchema fromRef(JsonSchema, JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: 
     * Creates a schema using the current one as a template with the parent as the
     * ref.
     * <p>
     * This is typically used if this schema is a schema resource that can be
     * pointed to by various references.
     *
     * @param refEvaluationParentSchema the parent ref
     * @param refEvaluationPath the ref evaluation path
     * @return the schema
     ;
		method `public JsonSchema getRefSchema(JsonNodePath)` returns `com.networknt.schema.JsonSchema`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		boolean com.networknt.schema.BaseJsonValidator.suppressSubSchemaRetrieval;
		com.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;
	
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `private ValidationContext withMetaSchema(ValidationContext, JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`, api document: 
     * Determines the validation context to use for the schema given the parent
     * validation context.
     * <p>
     * This is typically the same validation context unless the schema has a
     * different $schema from the parent.
     * <p>
     * If the schema does not define a $schema, the parent should be used.
     * 
     * @param validationContext the parent validation context
     * @param schemaNode        the schema node
     * @return the validation context to use
     ;
		method `protected ValidationContext createValidationContext(JsonNode, SchemaValidatorsConfig)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
class `com.networknt.schema.RefValidator`:
	related functions:
		method `private static void copySchemaResources(ValidationContext, JsonSchema)` returns `void`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `protected Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;
class `com.networknt.schema.UnevaluatedPropertiesValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.withConfig(SchemaValidatorsConfig)`;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
