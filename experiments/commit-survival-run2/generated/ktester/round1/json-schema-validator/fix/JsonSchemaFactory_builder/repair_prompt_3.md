


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchemaFactory_builder_Test {

//     @Mock
    private JsonSchemaFactory // used in the overload test
    mockBlueprint;

    /**
     * A real factory instance used as a blueprint for the overload test.
     */
    private JsonSchemaFactory blueprint;

    @BeforeEach
    void setUp() {
        // Create a default factory that we can use as a blueprint.
        blueprint = JsonSchemaFactory.builder().build();
    }

    /**
     * Utility that walks the class hierarchy to find the first field whose type matches the supplied class.
     */
    private static Field findFieldOfType(Class<?> clazz, Class<?> fieldType) {
        Class<?> current = clazz;
        while (current != null) {
            for (Field f : current.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(f.getType())) {
                    return f;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    @Test
    void builderReturnsNonNullBuilder() {
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder();
        assertNotNull(builder, "builder() should return a non‑null Builder instance");
    }

    @Test
    void builderReturnsNewInstanceOnEachCall() {
        JsonSchemaFactory.Builder first = JsonSchemaFactory.builder();
        JsonSchemaFactory.Builder second = JsonSchemaFactory.builder();
        assertNotSame(first, second, "Each call to builder() should return a new Builder instance");
    }

    @Test
    void builderWithBlueprintReturnsBuilder() {
        // The blueprint is a real factory; the method should still return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(blueprint);
        assertNotNull(builder, "builder(JsonSchemaFactory) should return a non‑null Builder instance");
    }

    @Test
    void builderBuildReturnsNonNullFactory() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().build();
        assertNotNull(factory, "builder().build() should produce a non‑null JsonSchemaFactory");
    }

    @Test
    void successiveBuildsProduceDistinctFactories() {
        JsonSchemaFactory f1 = JsonSchemaFactory.builder().build();
        JsonSchemaFactory f2 = JsonSchemaFactory.builder().build();
        assertNotSame(f1, f2, "Each call to builder().build() must return a distinct factory instance");
    }

    @Test
    void builtFactoryHasDefaultComponents() {
        JsonSchemaFactory factory = JsonSchemaFactory.builder().build();
        // Both components are part of the public API, so we can call them directly.
        assertNotNull(factory.getUriFactory(), "Factory constructed by builder() must provide a non‑null URIFactory");
        assertNotNull(factory.getUriTranslator(), "Factory constructed by builder() must provide a non‑null URITranslator");
    }

//     @Test
//     void builderIsolationAfterCustomConfiguration() throws Exception {
//         // 1) Obtain a builder and inject a custom URIFactory via reflection.
//         JsonSchemaFactory.Builder firstBuilder = JsonSchemaFactory.builder();
//         // Find a field that is likely to hold the URI factory. The exact name may differ
//         // across versions, so we search for a field of type URIFactory.
//         Field uriFactoryField = findFieldOfType(firstBuilder.getClass(), URIFactory.class);
//         assertNotNull(uriFactoryField, "Unable to locate URIFactory field inside Builder (required for isolation test)");
//         uriFactoryField.setAccessible(true);
//         URIFactory customFactory = new URIFactory() {
// 
//             @Override
//             public URI create(String uri) {
//                 return URI.create(uri);
//             }
// 
//             // The interface also defines a two‑argument variant.
//             @Override
//             public URI create(URI uri, String fragment) {
//                 return uri;
//             }
//         };
//         uriFactoryField.set(firstBuilder, customFactory);
//         // 2) Build a factory – it should contain the custom factory we injected.
//         JsonSchemaFactory customFactoryProduct = firstBuilder.build();
//         assertSame(customFactory, customFactoryProduct.getUriFactory(), "Factory built from the customized builder should expose the custom URIFactory");
//         // 3) Obtain a *new* builder and verify that its URIFactory field is NOT the custom instance.
//         JsonSchemaFactory.Builder secondBuilder = JsonSchemaFactory.builder();
//         Field uriFactoryField2 = findFieldOfType(secondBuilder.getClass(), URIFactory.class);
//         assertNotNull(uriFactoryField2, "Second builder must also contain a URIFactory field");
//         uriFactoryField2.setAccessible(true);
//         Object secondFactoryInstance = uriFactoryField2.get(secondBuilder);
//         assertNotSame(customFactory, secondFactoryInstance, "A fresh builder must not retain the custom URIFactory set on a previous builder");
//     }

    @Test
    void builderMethodIsThreadSafe() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            // Thread‑safe collection that stores the returned Builder references.
            Set<JsonSchemaFactory.Builder> builderSet = ConcurrentHashMap.newKeySet();
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    JsonSchemaFactory.Builder b = JsonSchemaFactory.builder();
                    assertNotNull(b, "builder() must never return null even under concurrency");
                    builderSet.add(b);
                }));
            }
            // Await completion and surface any unexpected exceptions.
            for (Future<?> f : futures) {
                // re‑throws any exception from the task
                f.get();
            }
            // Verify that we collected exactly THREAD_COUNT distinct instances.
            assertEquals(THREAD_COUNT, builderSet.size(), "Each concurrent call to builder() should yield a distinct Builder instance");
        } finally {
            executor.shutdownNow();
        }
    }

//     @Test
//     void builderDoesNotRetainStateAfterBuild() throws Exception {
//         // Create a builder and set a custom URI translator (or any mutable field we can locate).
//         JsonSchemaFactory.Builder firstBuilder = JsonSchemaFactory.builder();
//         // Locate a mutable field of type URITranslator (common in the builder).
//         Field translatorField = findFieldOfType(firstBuilder.getClass(), URITranslator.class);
//         assertNotNull(translatorField, "Unable to locate a URITranslator field inside Builder");
//         translatorField.setAccessible(true);
//         URITranslator customTranslator = new URITranslator() {
// 
//             @Override
//             public URI translate(URI uri) {
//                 return uri;
//             }
//         };
//         translatorField.set(firstBuilder, customTranslator);
//         // Build a factory – the custom translator should be in effect for this factory.
//         JsonSchemaFactory factoryWithCustom = firstBuilder.build();
//         assertSame(customTranslator, factoryWithCustom.getUriTranslator(), "Factory built from customized builder must contain the custom URITranslator");
//         // Obtain a *new* builder and verify that its translator field is NOT the custom one.
//         JsonSchemaFactory.Builder secondBuilder = JsonSchemaFactory.builder();
//         Field translatorField2 = findFieldOfType(secondBuilder.getClass(), URITranslator.class);
//         assertNotNull(translatorField2, "Second builder must also contain a URITranslator field");
//         translatorField2.setAccessible(true);
//         Object secondTranslator = translatorField2.get(secondBuilder);
//         assertNotSame(customTranslator, secondTranslator, "A fresh builder must not retain the custom URITranslator set on a previous builder");
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_builder_Test ✔
│     ├─ builderReturnsNonNullBuilder() ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ successiveBuildsProduceDistinctFactories() ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builderWithBlueprintReturnsBuilder() ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builderBuildReturnsNonNullFactory() ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builderMethodIsThreadSafe() ✘ defaultMetaSchemaURI must not be null or empty
│     ├─ builderReturnsNewInstanceOnEachCall() ✘ defaultMetaSchemaURI must not be null or empty
│     └─ builtFactoryHasDefaultComponents() ✘ defaultMetaSchemaURI must not be null or empty
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (7):
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderReturnsNonNullBuilder()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderReturnsNonNullBuilder', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:successiveBuildsProduceDistinctFactories()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'successiveBuildsProduceDistinctFactories', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderWithBlueprintReturnsBuilder()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderWithBlueprintReturnsBuilder', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderBuildReturnsNonNullFactory()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderBuildReturnsNonNullFactory', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderMethodIsThreadSafe()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderMethodIsThreadSafe', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderReturnsNewInstanceOnEachCall()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderReturnsNewInstanceOnEachCall', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builtFactoryHasDefaultComponents()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builtFactoryHasDefaultComponents', methodParameterTypes = '']
    => java.lang.IllegalArgumentException: defaultMetaSchemaURI must not be null or empty
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:235)
       com.networknt.schema.JsonSchemaFactory.<init>(JsonSchemaFactory.java:39)
       com.networknt.schema.JsonSchemaFactory$Builder.build(JsonSchemaFactory.java:193)
       com.networknt.schema.JsonSchemaFactory_builder_Test.setUp(JsonSchemaFactory_builder_Test.java:25)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       [...]

Test run finished after 110 ms
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

api document of method builder(): 

     * Builder without keywords or formats.
     *
     * <code>
     * JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
     * </code>
     *
     * @return a builder instance without any keywords or formats - usually not what one needs.
     

return type: 
com.networknt.schema.JsonSchemaFactory.Builder

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `public static Builder builder()` returns `com.networknt.schema.JsonSchemaFactory.Builder`, related with `com.networknt.schema.JsonSchemaFactory.builder()`, api document: 
     * Builder without keywords or formats.
     *
     * <code>
     * JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
     * </code>
     *
     * @return a builder instance without any keywords or formats - usually not what one needs.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchemaFactory.builder()`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
