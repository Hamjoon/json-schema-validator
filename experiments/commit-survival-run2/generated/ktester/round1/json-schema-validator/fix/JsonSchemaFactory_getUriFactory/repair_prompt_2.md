
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.schema.uri.URITranslator;
import com.networknt.schema.uri.URISchemeFetcher;
import com.networknt.schema.uri.URISchemeFactory;
import com.networknt.schema.urn.URNFactory;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.mockito.junit.jupiter.MockitoExtension;
import com.networknt.schema.uri.URIFactory;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_getUriFactory_Test {

    /**
     * Instance under test
     */
    private JsonSchemaFactory jsonSchemaFactory;

    /**
     * The factory we expect to be returned by {@code getUriFactory()}
     */
    private URIFactory expectedUriFactory;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // ----- Minimal but valid collaborators ---------------------------------
        // real mapper – no external I/O
        ObjectMapper jsonMapper = new ObjectMapper();
        // real mapper – no external I/O
        YAMLMapper yamlMapper = new YAMLMapper();
        // The default meta‑schema URI used by the factory (any string is fine for the test)
        String defaultMetaSchemaURI = "http://example.com/meta/schema";
        // Empty map of meta‑schemas – the constructor does not require any entries for the
        // behaviour we are testing (getUriFactory)
        Map<String, JsonMetaSchema> jsonMetaSchemas = Collections.emptyMap();
        // Empty URI‑to‑URI mapping (allowed by the constructor)
        Map<String, String> uriMap = Collections.emptyMap();
        // The objects we want to verify are returned unchanged
        expectedUriFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };
        // The scheme‑factory and fetcher can be empty – they are not exercised here
        URISchemeFactory uriSchemeFactory = new URISchemeFactory(Collections.emptyMap());
        URISchemeFetcher uriSchemeFetcher = new URISchemeFetcher(Collections.emptyMap());
        // Provide a minimal concrete URNFactory implementation
        URNFactory urnFactory = new URNFactory() {

            @Override
            public URI create(String urn) {
                return URI.create(urn);
            }
        };
        // Composite translator – we can use the default constructor
        URITranslator.CompositeURITranslator uriTranslators = new URITranslator.CompositeURITranslator();
        // ----- Instantiate JsonSchemaFactory via reflection --------------------
        Constructor<JsonSchemaFactory> ctor = JsonSchemaFactory.class.getDeclaredConstructor(ObjectMapper.class, YAMLMapper.class, String.class, URISchemeFactory.class, URISchemeFetcher.class, URNFactory.class, Map.class, Map.class, boolean.class, URITranslator.CompositeURITranslator.class);
        ctor.setAccessible(true);
        jsonSchemaFactory = ctor.newInstance(jsonMapper, yamlMapper, defaultMetaSchemaURI, uriSchemeFactory, uriSchemeFetcher, urnFactory, jsonMetaSchemas, uriMap, false, uriTranslators);
        // Replace the private field `uriFactory` with our expected instance using reflection.
        java.lang.reflect.Field uriFactoryField = JsonSchemaFactory.class.getDeclaredField("uriFactory");
        uriFactoryField.setAccessible(true);
        uriFactoryField.set(jsonSchemaFactory, expectedUriFactory);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchemaFactory = null;
        expectedUriFactory = null;
    }

    @Test
    void getUriFactory_returnsConfiguredFactory() {
        // When
        URIFactory actual = jsonSchemaFactory.getUriFactory();
        // Then
        assertNotNull(actual, "The returned URIFactory should not be null");
        assertSame(expectedUriFactory, actual, "JsonSchemaFactory#getUriFactory should return the instance supplied at construction");
    }

    /* --------------------------------------------------------------
     *  1. Default builder creates a non‑null default URIFactory.
     * -------------------------------------------------------------- */
    @Test
    void defaultBuilder_returnsNonNullDefaultUriFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().build();
        URIFactory uriFactory = factory.getUriFactory();
        assertNotNull(uriFactory, "Default builder must provide a non‑null URIFactory");
        // Repeated calls must give the same reference.
        assertSame(uriFactory, factory.getUriFactory(), "Repeated getUriFactory() calls should return the identical instance");
    }

    /* --------------------------------------------------------------
     *  2. Custom URIFactory supplied via builder is returned unchanged.
     * -------------------------------------------------------------- */
    @Test
    void customUriFactory_isReturnedUnmodified() {
        // Arrange – a lightweight custom implementation
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };
        // Act
        JsonSchemaFactory factory = JsonSchemaFactory.builder().uriFactory(// builder API assumed to exist
        customFactory).build();
        // Assert
        assertSame(customFactory, factory.getUriFactory(), "The factory returned by getUriFactory() must be the exact instance supplied to the builder");
    }

    /* --------------------------------------------------------------
     *  3. Blueprint inheritance – custom factory.
     * -------------------------------------------------------------- */
    @Test
    void blueprintInheritance_customFactory_isPreserved() {
        // Arrange – build a blueprint with a custom factory
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().uriFactory(customFactory).build();
        // Act – create a new factory from the blueprint
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        // Assert
        assertSame(customFactory, derived.getUriFactory(), "Derived factory must inherit the custom URIFactory from its blueprint");
    }

    /* --------------------------------------------------------------
     *  4. Blueprint inheritance – default factory.
     * -------------------------------------------------------------- */
    @Test
    void blueprintInheritance_defaultFactory_isShared() {
        // Arrange – blueprint created via the default builder
        JsonSchemaFactory blueprint = JsonSchemaFactory.builder().build();
        // Act – derive a second factory from the blueprint
        JsonSchemaFactory derived = JsonSchemaFactory.builder(blueprint).build();
        // Assert – both must expose the *same* URIFactory reference
        assertSame(blueprint.getUriFactory(), derived.getUriFactory(), "Derived factory should share the default URIFactory instance with its blueprint");
    }

    /* --------------------------------------------------------------
     *  5. Multiple calls consistency for various factory configurations.
     * -------------------------------------------------------------- */
    @Test
    void multipleCalls_consistencyAcrossDifferentFactories() {
        // Default factory
        JsonSchemaFactory defaultFactory = JsonSchemaFactory.builder().build();
        assertSame(defaultFactory.getUriFactory(), defaultFactory.getUriFactory(), "Default factory must return identical URIFactory on successive calls");
        // Custom factory
        URIFactory customFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };
        JsonSchemaFactory custom = JsonSchemaFactory.builder().uriFactory(customFactory).build();
        assertSame(custom.getUriFactory(), custom.getUriFactory(), "Custom factory must return identical URIFactory on successive calls");
        // Blueprint‑derived factory
        JsonSchemaFactory derived = JsonSchemaFactory.builder(custom).build();
        assertSame(derived.getUriFactory(), derived.getUriFactory(), "Blueprint‑derived factory must return identical URIFactory on successive calls");
    }

    /* --------------------------------------------------------------
     *  6. Builder(null) must reject a null blueprint argument.
     * -------------------------------------------------------------- */
    @Test
    void builder_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> JsonSchemaFactory.builder((JsonSchemaFactory) null), "Calling builder(null) should throw NullPointerException");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:16: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:48: error: <anonymous com.networknt.schema.JsonSchemaFactory_getUriFactory_Test$1> is not abstract and does not override abstract method create(URI,String) in URIFactory
        expectedUriFactory = new URIFactory() {
                                              ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:111: error: <anonymous com.networknt.schema.JsonSchemaFactory_getUriFactory_Test$3> is not abstract and does not override abstract method create(URI,String) in URIFactory
        URIFactory customFactory = new URIFactory() {
                                                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:131: error: <anonymous com.networknt.schema.JsonSchemaFactory_getUriFactory_Test$4> is not abstract and does not override abstract method create(URI,String) in URIFactory
        URIFactory customFactory = new URIFactory() {
                                                    ^
src/test/java/com/networknt/schema/JsonSchemaFactory_getUriFactory_Test.java:167: error: <anonymous com.networknt.schema.JsonSchemaFactory_getUriFactory_Test$5> is not abstract and does not override abstract method create(URI,String) in URIFactory
        URIFactory customFactory = new URIFactory() {
                                                    ^
6 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of method getUriFactory()': '\n     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.\n     ', 'return type': 'com.networknt.schema.uri.URIFactory', 'dependent classes': 'class `com.networknt.schema.JsonSchemaFactory`:\n\tdependent fields:\n\t\tcom.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;\n\trelated functions:\n\t\tmethod `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`;\n\t\tmethod `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`;\n\t\tmethod `public URIFactory getUriFactory()` returns `com.networknt.schema.uri.URIFactory`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`, api document: \n     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.\n     ;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


