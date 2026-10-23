


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
        // Create a default factory (using the Draft‑V4 blueprint) that we can use as a blueprint.
        // The builder that takes a blueprint copies its configuration, so the resulting factory
        // is fully initialised and does not throw the “defaultMetaSchemaURI” exception.
//         blueprint = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
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

//     @Test
//     void builderReturnsNonNullBuilder() {
//         JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         assertNotNull(builder, "builder() should return a non‑null Builder instance");
//     }

//     @Test
//     void builderReturnsNewInstanceOnEachCall() {
//         JsonSchemaFactory.Builder first = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         JsonSchemaFactory.Builder second = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//         assertNotSame(first, second, "Each call to builder() should return a new Builder instance");
//     }

    @Test
    void builderWithBlueprintReturnsBuilder() {
        // The blueprint is a real factory; the method should still return a Builder.
        JsonSchemaFactory.Builder builder = JsonSchemaFactory.builder(blueprint);
        assertNotNull(builder, "builder(JsonSchemaFactory) should return a non‑null Builder instance");
    }

//     @Test
//     void builderBuildReturnsNonNullFactory() {
//         JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         assertNotNull(factory, "builder().build() should produce a non‑null JsonSchemaFactory");
//     }

//     @Test
//     void successiveBuildsProduceDistinctFactories() {
//         JsonSchemaFactory f1 = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         JsonSchemaFactory f2 = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         assertNotSame(f1, f2, "Each call to builder().build() must return a distinct factory instance");
//     }

//     @Test
//     void builtFactoryHasDefaultComponents() {
//         JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4()).build();
//         // Both components are part of the public API, so we can call them directly.
//         assertNotNull(factory.getUriFactory(), "Factory constructed by builder() must provide a non‑null URIFactory");
//         assertNotNull(factory.getUriTranslator(), "Factory constructed by builder() must provide a non‑null URITranslator");
//     }

//     @Test
//     void builderMethodIsThreadSafe() throws Exception {
//         final int THREAD_COUNT = 10;
//         ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
//         try {
//             // Thread‑safe collection that stores the returned Builder references.
//             Set<JsonSchemaFactory.Builder> builderSet = ConcurrentHashMap.newKeySet();
//             List<Future<?>> futures = new ArrayList<>();
//             for (int i = 0; i < THREAD_COUNT; i++) {
//                 futures.add(executor.submit(() -> {
//                     JsonSchemaFactory.Builder b = JsonSchemaFactory.builder(JsonSchemaFactory.getDraftV4());
//                     assertNotNull(b, "builder() must never return null even under concurrency");
//                     builderSet.add(b);
//                 }));
//             }
//             // Await completion and surface any unexpected exceptions.
//             for (Future<?> f : futures) {
//                 // re‑throws any exception from the task
//                 f.get();
//             }
//             // Verify that we collected exactly THREAD_COUNT distinct instances.
//             assertEquals(THREAD_COUNT, builderSet.size(), "Each concurrent call to builder() should yield a distinct Builder instance");
//         } finally {
//             executor.shutdownNow();
//         }
//     }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_builder_Test ✔
│     └─ builderWithBlueprintReturnsBuilder() ✘ Cannot read field "jsonMetaSchemas" because "blueprint" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchemaFactory_builder_Test:builderWithBlueprintReturnsBuilder()
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_builder_Test', methodName = 'builderWithBlueprintReturnsBuilder', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot read field "jsonMetaSchemas" because "blueprint" is null
       com.networknt.schema.JsonSchemaFactory.builder(JsonSchemaFactory.java:308)
       com.networknt.schema.JsonSchemaFactory_builder_Test.builderWithBlueprintReturnsBuilder(JsonSchemaFactory_builder_Test.java:61)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]

Test run finished after 102 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         1 tests found           ]
[         0 tests skipped         ]
[         1 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         1 tests failed          ]


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
