


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import com.networknt.schema.SpecVersion.VersionFlag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

class JsonSchemaFactory_getInstance_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Normalises a meta‑schema URI using the factory's static helper (mirrors internal behaviour).
     */
    private static String normalise(String uri) {
        try {
            // The method is package‑private static; use reflection to invoke it.
            java.lang.reflect.Method m = JsonSchemaFactory.class.getDeclaredMethod("normalizeMetaSchemaUri", String.class);
            m.setAccessible(true);
            return (String) m.invoke(null, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * All supported version flags together with their expected default meta‑schema URI.
     */
    private static Stream<Arguments> supportedVersionFlags() {
        return Stream.of(Arguments.of(VersionFlag.V4, "http://json-schema.org/draft-04/schema#"), Arguments.of(VersionFlag.V6, "http://json-schema.org/draft-06/schema#"), Arguments.of(VersionFlag.V7, "http://json-schema.org/draft-07/schema#"));
    }

    /**
     * The same set as above but only for the meta‑schema‑registration assertions.
     */
    private static Stream<Arguments> metaSchemaRegistrationFlags() {
        return Stream.of(Arguments.of(VersionFlag.V6, "http://json-schema.org/draft-06/schema#"));
    }

    /**
     * Verify that {@link JsonSchemaFactory#getInstance(SpecVersion.VersionFlag)} returns a
     * non‑null factory and that the factory can be used to create a {@link JsonSchema}
     * from a minimal JSON schema string.
     */
    @Test
    void getInstance_ShouldReturnFactoryAbleToParseSchema() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        assertNotNull(factory, "Factory must not be null");
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "JsonSchema instance must not be null");
        JsonNode instance = MAPPER.readTree("{\"foo\":\"bar\"}");
        assertDoesNotThrow(() -> schema.validate(instance), "Validation of a valid instance should not throw");
    }

    /**
     * Verify that passing a {@code null} version flag results in an
     * {@link IllegalArgumentException} (the internal {@code checkVersion} method validates the argument).
     */
    @Test
    void getInstance_NullVersionFlag_ShouldThrowException() {
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> JsonSchemaFactory.getInstance(null), "Calling getInstance with null should throw IllegalArgumentException");
        assertTrue(thrown.getMessage().contains("null"), "Exception message should mention null");
    }

    /**
     * Reads a private (or package‑private) field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(target);
            return type.cast(value);
        } catch (NoSuchFieldException e) {
            fail("Field '" + name + "' not found in class " + target.getClass(), e);
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            fail("Unable to access field '" + name + "' in class " + target.getClass(), e);
            throw new AssertionError(e);
        }
    }

    @ParameterizedTest(name = "{index} => flag={0}, expectedUri={1}")
    @MethodSource("supportedVersionFlags")
    @DisplayName("Valid version flags – defaultMetaSchemaURI and meta‑schema registration")
    void getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema(VersionFlag flag, String expectedMetaSchemaUri) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        assertNotNull(factory, "Factory must not be null");
        // ----- verify defaultMetaSchemaURI -----
        String actualDefaultUri = getField(factory, "defaultMetaSchemaURI", String.class);
        assertEquals(expectedMetaSchemaUri, actualDefaultUri, "defaultMetaSchemaURI must match the expected URI for flag " + flag);
        // ----- verify internal jsonMetaSchemas map -----
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> metaMap = getField(factory, "jsonMetaSchemas", ConcurrentMap.class);
        assertNotNull(metaMap, "jsonMetaSchemas map must be present");
        assertEquals(1, metaMap.size(), "Exactly one meta‑schema must be registered for flag " + flag);
        String normalizedKey = normalise(expectedMetaSchemaUri);
        assertTrue(metaMap.containsKey(normalizedKey), "Meta‑schema map must contain the normalized key '" + normalizedKey + "'");
    }

    @Test
    @DisplayName("Two successive calls with the same flag produce distinct factories")
    void getInstance_TwiceWithSameFlag_ProducesDistinctInstances() {
        JsonSchemaFactory f1 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        JsonSchemaFactory f2 = JsonSchemaFactory.getInstance(VersionFlag.V4);
        assertNotSame(f1, f2, "Factory instances must be different objects");
    }

    @Test
    @DisplayName("Two successive calls with the latest flag produce distinct factories")
    void getInstance_TwiceWithLatestFlag_ProducesDistinctInstances() {
        JsonSchemaFactory f1 = JsonSchemaFactory.getInstance(VersionFlag.V7);
        JsonSchemaFactory f2 = JsonSchemaFactory.getInstance(VersionFlag.V7);
        assertNotSame(f1, f2, "Factory instances must be different objects");
    }

    @ParameterizedTest(name = "{index} => flag={0}, expectedUri={1}")
    @MethodSource("metaSchemaRegistrationFlags")
    @DisplayName("Meta‑schema registration count & key normalisation")
    void getInstance_MetaSchemaMap_HasSingleNormalisedKey(VersionFlag flag, String expectedMetaSchemaUri) {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(flag);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, JsonMetaSchema> metaMap = getField(factory, "jsonMetaSchemas", ConcurrentMap.class);
        assertEquals(1, metaMap.size(), "Factory should contain exactly one registered meta‑schema");
        String expectedKey = normalise(expectedMetaSchemaUri);
        assertTrue(metaMap.containsKey(expectedKey), "Registered key must be the normalised URI: " + expectedKey);
    }

    @Test
    @DisplayName("Builder defaults are untouched – cache enabled & no URI translators")
    void getInstance_BuilderDefaults_Unmodified() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        // enableUriSchemaCache defaults to true
        Boolean cacheEnabled = getField(factory, "enableUriSchemaCache", Boolean.class);
        assertTrue(cacheEnabled, "enableUriSchemaCache should be true by default");
        // uriTranslators is a CompositeURITranslator – its internal collection should be empty
        Object translator = getField(factory, "uriTranslators", Object.class);
        try {
            Field listField = translator.getClass().getDeclaredField("translators");
            listField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<?> list = (List<?>) listField.get(translator);
            assertTrue(list.isEmpty(), "CompositeURITranslator should contain no translators by default");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Unable to inspect CompositeURITranslator internals", e);
        }
    }

    /**
     * Additional sanity check – the factory returned by {@code getInstance}
     * must be usable to compile a minimal schema and validate a trivial JSON
     * document. This mirrors the original test that already existed in the
     * scaffold but is kept here for completeness.
     */
    @Test
    @DisplayName("Factory can parse a minimal schema and validate an instance")
    void getInstance_CanParseAndValidateSimpleSchema() throws Exception {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(VersionFlag.V7);
        String schemaJson = "{}";
        JsonSchema schema = factory.getSchema(schemaJson);
        assertNotNull(schema, "Returned JsonSchema must not be null");
        JsonNode instance = MAPPER.readTree("{\"foo\":\"bar\"}");
        assertDoesNotThrow(() -> schema.validate(instance), "Validation of a conforming instance must not throw");
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_getInstance_Test ✔
│     ├─ Meta‑schema registration count & key normalisation ✔
│     │  └─ 1 => flag=V6, expectedUri=http://json-schema.org/draft-06/schema# ✔
│     ├─ Two successive calls with the same flag produce distinct factories ✔
│     ├─ Builder defaults are untouched – cache enabled & no URI translators ✘ Unable to inspect CompositeURITranslator internals
│     ├─ getInstance_ShouldReturnFactoryAbleToParseSchema() ✔
│     ├─ Two successive calls with the latest flag produce distinct factories ✔
│     ├─ Valid version flags – defaultMetaSchemaURI and meta‑schema registration ✔
│     │  ├─ 1 => flag=V4, expectedUri=http://json-schema.org/draft-04/schema# ✘ defaultMetaSchemaURI must match the expected URI for flag V4 ==> expected: <http://json-schema.org/draft-04/schema#> but was: <https://json-schema.org/draft-04/schema>
│     │  ├─ 2 => flag=V6, expectedUri=http://json-schema.org/draft-06/schema# ✘ defaultMetaSchemaURI must match the expected URI for flag V6 ==> expected: <http://json-schema.org/draft-06/schema#> but was: <https://json-schema.org/draft-06/schema>
│     │  └─ 3 => flag=V7, expectedUri=http://json-schema.org/draft-07/schema# ✘ defaultMetaSchemaURI must match the expected URI for flag V7 ==> expected: <http://json-schema.org/draft-07/schema#> but was: <https://json-schema.org/draft-07/schema>
│     ├─ getInstance_NullVersionFlag_ShouldThrowException() ✘ Calling getInstance with null should throw IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
│     └─ Factory can parse a minimal schema and validate an instance ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:JsonSchemaFactory_getInstance_Test:Builder defaults are untouched – cache enabled & no URI translators
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getInstance_Test', methodName = 'getInstance_BuilderDefaults_Unmodified', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Unable to inspect CompositeURITranslator internals
       org.junit.jupiter.api.AssertionUtils.fail(AssertionUtils.java:42)
       org.junit.jupiter.api.Assertions.fail(Assertions.java:147)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_BuilderDefaults_Unmodified(JsonSchemaFactory_getInstance_Test.java:155)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
     Caused by: java.lang.NoSuchFieldException: translators
       java.base/java.lang.Class.getDeclaredField(Class.java:2612)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_BuilderDefaults_Unmodified(JsonSchemaFactory_getInstance_Test.java:149)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getInstance_Test:Valid version flags – defaultMetaSchemaURI and meta‑schema registration:1 => flag=V4, expectedUri=http://json-schema.org/draft-04/schema#
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getInstance_Test', methodName = 'getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema', methodParameterTypes = 'com.networknt.schema.SpecVersion$VersionFlag, java.lang.String']
    => org.opentest4j.AssertionFailedError: defaultMetaSchemaURI must match the expected URI for flag V4 ==> expected: <http://json-schema.org/draft-04/schema#> but was: <https://json-schema.org/draft-04/schema>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema(JsonSchemaFactory_getInstance_Test.java:101)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getInstance_Test:Valid version flags – defaultMetaSchemaURI and meta‑schema registration:2 => flag=V6, expectedUri=http://json-schema.org/draft-06/schema#
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getInstance_Test', methodName = 'getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema', methodParameterTypes = 'com.networknt.schema.SpecVersion$VersionFlag, java.lang.String']
    => org.opentest4j.AssertionFailedError: defaultMetaSchemaURI must match the expected URI for flag V6 ==> expected: <http://json-schema.org/draft-06/schema#> but was: <https://json-schema.org/draft-06/schema>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema(JsonSchemaFactory_getInstance_Test.java:101)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getInstance_Test:Valid version flags – defaultMetaSchemaURI and meta‑schema registration:3 => flag=V7, expectedUri=http://json-schema.org/draft-07/schema#
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getInstance_Test', methodName = 'getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema', methodParameterTypes = 'com.networknt.schema.SpecVersion$VersionFlag, java.lang.String']
    => org.opentest4j.AssertionFailedError: defaultMetaSchemaURI must match the expected URI for flag V7 ==> expected: <http://json-schema.org/draft-07/schema#> but was: <https://json-schema.org/draft-07/schema>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:182)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1153)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_WithSupportedFlag_ReturnsFactoryWithCorrectMetaSchema(JsonSchemaFactory_getInstance_Test.java:101)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getInstance_Test:getInstance_NullVersionFlag_ShouldThrowException()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getInstance_Test', methodName = 'getInstance_NullVersionFlag_ShouldThrowException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Calling getInstance with null should throw IllegalArgumentException ==> Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <java.lang.NullPointerException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3099)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.getInstance_NullVersionFlag_ShouldThrowException(JsonSchemaFactory_getInstance_Test.java:70)
       [...]
     Caused by: java.lang.NullPointerException: Cannot invoke "com.networknt.schema.JsonSchemaVersion.getInstance()" because "jsonSchemaVersion" is null
       com.networknt.schema.JsonSchemaFactory.getInstance(JsonSchemaFactory.java:288)
       com.networknt.schema.JsonSchemaFactory_getInstance_Test.lambda$getInstance_NullVersionFlag_ShouldThrowException$1(JsonSchemaFactory_getInstance_Test.java:70)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]

Test run finished after 200 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[        10 tests found           ]
[         0 tests skipped         ]
[        10 tests started         ]
[         0 tests aborted         ]
[         5 tests successful      ]
[         5 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

return type: 
com.networknt.schema.JsonSchemaFactory

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`;
		method `public static JsonSchemaFactory getInstance(SpecVersion.VersionFlag)` returns `com.networknt.schema.JsonSchemaFactory`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`;
class `com.networknt.schema.JsonSchemaFactory.Builder`:
	related functions:
		method `public Builder addMetaSchema(JsonMetaSchema)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`;
		method `public Builder addMetaSchemas(Collection<? extends JsonMetaSchema>)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`;
class `com.networknt.schema.utils.JsonNodeUtil`:
	related functions:
		method `private static long detectVersion(ValidationContext)` returns `long`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getInstance(SpecVersion.VersionFlag)`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
