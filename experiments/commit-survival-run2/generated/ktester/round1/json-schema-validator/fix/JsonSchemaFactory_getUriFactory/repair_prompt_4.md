


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
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
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import com.networknt.schema.uri.URIFactory;
import org.junit.jupiter.api.extension.ExtendWith;

// @ExtendWith(MockitoExtension.class)
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
        String defaultMetaSchemaURI = "http://example.com/meta/schema";
        // Empty meta‑schemas map – the constructor does not require any entries for
        // the behaviour we are testing (getUriFactory)
        Map<String, JsonMetaSchema> jsonMetaSchemas = Collections.emptyMap();
        // Empty URI‑to‑URI mapping (allowed by the constructor)
        Map<String, String> uriMap = Collections.emptyMap();
        // The objects we want to verify are returned unchanged
        expectedUriFactory = new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }

            @Override
            public URI create(URI base, String reference) {
                // Simple implementation that resolves against the base using URI.resolve
                return base.resolve(reference);
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

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
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

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
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

            @Override
            public URI create(URI base, String reference) {
                return base.resolve(reference);
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

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_getUriFactory_Test ✔
│     ├─ customUriFactory_isReturnedUnmodified() ✘ Json Meta Schemas must not be null or empty
│     ├─ builder_nullArgument_throwsNullPointerException() ✘ Json Meta Schemas must not be null or empty
│     ├─ defaultBuilder_returnsNonNullDefaultUriFactory() ✘ Json Meta Schemas must not be null or empty
│     ├─ blueprintInheritance_customFactory_isPreserved() ✘ Json Meta Schemas must not be null or empty
│     ├─ getUriFactory_returnsConfiguredFactory() ✘ Json Meta Schemas must not be null or empty
│     ├─ multipleCalls_consistencyAcrossDifferentFactories() ✘ Json Meta Schemas must not be null or empty
│     └─ blueprintInheritance_defaultFactory_isShared() ✘ Json Meta Schemas must not be null or empty
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (7):
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:customUriFactory_isReturnedUnmodified()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'customUriFactory_isReturnedUnmodified', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:builder_nullArgument_throwsNullPointerException()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'builder_nullArgument_throwsNullPointerException', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:defaultBuilder_returnsNonNullDefaultUriFactory()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'defaultBuilder_returnsNonNullDefaultUriFactory', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:blueprintInheritance_customFactory_isPreserved()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'blueprintInheritance_customFactory_isPreserved', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:getUriFactory_returnsConfiguredFactory()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'getUriFactory_returnsConfiguredFactory', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:multipleCalls_consistencyAcrossDifferentFactories()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'multipleCalls_consistencyAcrossDifferentFactories', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]
  JUnit Jupiter:JsonSchemaFactory_getUriFactory_Test:blueprintInheritance_defaultFactory_isShared()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_getUriFactory_Test', methodName = 'blueprintInheritance_defaultFactory_isShared', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: Json Meta Schemas must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:241)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchemaFactory_getUriFactory_Test.setupBeforeEach(JsonSchemaFactory_getUriFactory_Test.java:74)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       [...]

Test run finished after 108 ms
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
[         0 tests successful      ]
[         7 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of method getUriFactory(): 

     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.
     

return type: 
com.networknt.schema.uri.URIFactory

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	dependent fields:
		com.networknt.schema.uri.URISchemeFactory com.networknt.schema.JsonSchemaFactory.uriFactory;
	related functions:
		method `public static Builder builder(JsonSchemaFactory)` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`;
		method `protected ValidationContext createValidationContext(JsonNode)` returns `com.networknt.schema.ValidationContext`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`;
		method `public URIFactory getUriFactory()` returns `com.networknt.schema.uri.URIFactory`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`, api document: 
     * @return A shared {@link URI} factory that is used for creating the URI references in schemas.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.getUriFactory()`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
