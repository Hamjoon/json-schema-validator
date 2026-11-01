


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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
// added for spy, doReturn, etc.
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_getRefSchemaNode_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * The schema under test.
     */
    private JsonSchema schema;

    /**
     * Sample schema node used by the tests.
     */
    private JsonNode schemaNode;

    /**
     * Schema without a $id – used for most test cases.
     */
    private JsonSchema schemaWithoutId;

    /**
     * Schema that contains a $id – used for id‑related test cases.
     */
    private JsonSchema schemaWithId;

    /**
     * Sample JSON document that contains the nodes referenced by the tests.
     */
    private static final String JSON_WITH_ID = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "http://example.com/schemas/main",
          "components": {
            "schemas": {
              "Person": {
                "type": "object",
                "properties": {}
              }
            }
          },
          "defs": {
            "thing": {
              "type": "string"
            }
          },
          "definitions": {
            "foo": {
              "type": "string",
              "description": "A simple string"
            },
            "bar": {
              "type": "integer"
            }
          }
        }
        """;

    /**
     * Same document but **without** the $id property.
     */
    private static final String JSON_WITHOUT_ID = """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "components": {
            "schemas": {
              "Person": {
                "type": "object",
                "properties": {}
              }
            }
          },
          "defs": {
            "thing": {
              "type": "string"
            }
          },
          "definitions": {
            "foo": {
              "type": "string",
              "description": "A simple string"
            },
            "bar": {
              "type": "integer"
            }
          }
        }
        """;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // -----------------------------------------------------------------
        // 1. Minimal schema (used by the non‑parameterised tests)
        // -----------------------------------------------------------------
        String minimal = """
            {
              "$schema": "https://json-schema.org/draft/2020-12/schema",
              "definitions": {
                "foo": {
                  "type": "string",
                  "description": "A simple string"
                },
                "bar": {
                  "type": "integer"
                }
              }
            }
            """;
        JsonNode minimalNode = MAPPER.readTree(minimal);
        schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(minimalNode);
        // -----------------------------------------------------------------
        // 2. Schema without $id
        // -----------------------------------------------------------------
        JsonNode withoutIdNode = MAPPER.readTree(JSON_WITHOUT_ID);
        schemaWithoutId = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(withoutIdNode);
        // -----------------------------------------------------------------
        // 3. Schema with $id
        // -----------------------------------------------------------------
        JsonNode withIdNode = MAPPER.readTree(JSON_WITH_ID);
        schemaWithId = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(withIdNode);
    }

    /**
     * Provides all normal test scenarios described in the JSON test‑case list.
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>the {@link JsonSchema} instance to use (with/without $id)</li>
     *   <li>the {@code ref} argument</li>
     *   <li>the expected JSON‑Pointer path relative to the root node (or {@code null}
     *       when the original root node is expected)</li>
     *   <li>boolean flag – {@code true} if the method is expected to return a
     *       {@link com.fasterxml.jackson.databind.node.MissingNode}</li>
     *   <li>a description (used only for readability)</li>
     * </ul>
     */
    private static Stream<Arguments> provideRefScenarios() {
        return Stream.of(// 1. simpleRef – no id, no special prefix → original schema node
        arguments("schemaWithoutId", "simpleRef", null, false, "original schema node"), // 2. id‑aware ref with "#"
        arguments("schemaWithId", "http://example.com/schemas/main#/defs/thing", "/defs/thing", false, "node at path \"/defs/thing\" (id stripped, hash present)"), // 3. id‑aware ref without "#"
        arguments("schemaWithId", "http://example.com/schemas/main/defs/thing", "/defs/thing", false, "node at path \"/defs/thing\" (id stripped, no hash)"), // 4. standard JSON‑Pointer with leading "#/"
        arguments("schemaWithoutId", "#/components/schemas/Person", "/components/schemas/Person", false, "node at path \"/components/schemas/Person\""), // 5. missing path → fallback (MissingNode)
        arguments("schemaWithoutId", "/missing/path", null, true, "node returned by handleNullNode (missing)"), // 6. ref exactly equals schema id → original node
        arguments("schemaWithoutId", "http://example.com/schemas/main", null, false, "original schema node (id matches exactly)"), // 7. id matches exactly (with $id)
        arguments("schemaWithId", "http://example.com/schemas/main", null, false, "original schema node (id matches exactly)"), // 8. hash without slash → original node
        arguments("schemaWithoutId", "#plainFragment", null, false, "original schema node (hash without slash)"), // 9. hash followed only by slash → original node (root)
        arguments("schemaWithoutId", "#/", null, false, "original schema node (hash followed by slash)"), //10. absolute pointer without hash
        arguments("schemaWithoutId", "/components/schemas/Person", "/components/schemas/Person", false, "node at path \"/components/schemas/Person\""), //11. URL‑encoded slash after hash
        arguments("schemaWithoutId", "#/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //12. URL‑encoded slash without hash
        arguments("schemaWithoutId", "/defs%2Fthing", "/defs/thing", false, "node at path \"/defs/thing\" (URL‑encoded)"), //13. empty string → original node
        arguments("schemaWithoutId", "", null, false, "original schema node (empty ref)"), //14. solitary "#" → original node
        arguments("schemaWithoutId", "#", null, false, "original schema node (sole '#')"), //15. ref with non‑matching id prefix → original node
        arguments("schemaWithId", "http://example.com/schemas/mainExtra#/defs/thing", null, false, "original schema node (id does not match)"), //16. missing node after hash → fallback (MissingNode)
        arguments("schemaWithoutId", "#/missing/node", null, true, "node returned by handleNullNode (missing)"));
    }

    /**
     * Verify that a valid JSON‑Pointer (starting with {@code "#/"} ) is resolved
     * correctly to the target node.
     */
    @Test
    void testGetRefSchemaNode_ValidPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/foo");
        assertNotNull(result, "Resulting node must not be null");
        assertFalse(result.isMissingNode(), "Result should not be a missing node");
        assertEquals("string", result.get("type").asText(), "The resolved node should contain the expected type");
        assertEquals("A simple string", result.get("description").asText(), "The resolved node should contain the expected description");
    }

    /**
     * Verify that a pointer without the leading {@code "#"} is also handled
     * (the method strips the leading {@code "#"} internally).
     */
    @Test
    void testGetRefSchemaNode_PointerWithoutHash() {
        JsonNode result = schema.getRefSchemaNode("/definitions/bar");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("integer", result.get("type").asText(), "Should resolve to the integer definition");
    }

    /**
     * Verify that an unknown pointer results in a missing node and that the
     * private {@code handleNullNode} fallback is exercised (it returns a missing
     * node in our minimal setup).
     */
    @Test
    void testGetRefSchemaNode_UnknownPointer() {
        JsonNode result = schema.getRefSchemaNode("#/definitions/unknown");
        assertTrue(result.isMissingNode(), "An unknown pointer should yield a missing node after fallback");
    }

    /**
     * Verify that URL‑encoded pointers are decoded before navigation.
     */
    @Test
    void testGetRefSchemaNode_UrlEncodedPointer() {
        JsonNode result = schema.getRefSchemaNode("#%2Fdefinitions%2Ffoo");
        assertNotNull(result);
        assertFalse(result.isMissingNode());
        assertEquals("string", result.get("type").asText(), "URL‑decoded pointer should resolve correctly");
    }

    /**
     * Executes the parameterised scenarios defined in {@link #provideRefScenarios()}.
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("provideRefScenarios")
    void testGetRefSchemaNode_VariousScenarios(String schemaKey, String ref, String expectedPath, boolean expectMissing, String description) throws Exception {
        JsonSchema target = "schemaWithId".equals(schemaKey) ? schemaWithId : schemaWithoutId;
        JsonNode result = target.getRefSchemaNode(ref);
        if (expectMissing) {
            assertTrue(result.isMissingNode(), "Expected a MissingNode for ref \"" + ref + "\"");
            return;
        }
        if (expectedPath == null) {
            // Expect the original root node.
            assertSame(target.getSchemaNode(), result, "Expected the original schema node for ref \"" + ref + "\"");
        } else {
            JsonNode expected = target.getSchemaNode().at(expectedPath);
            assertEquals(expected, result, "Resolved node does not match expected node for ref \"" + ref + "\"");
        }
    }

    @Test
    void testGetRefSchemaNode_NullReference_ThrowsNPE() {
        assertThrows(NullPointerException.class, () -> schemaWithoutId.getRefSchemaNode(null), "Passing null should raise NullPointerException");
    }

//     @Test
//     void testGetRefSchemaNode_FindSchemaResourceRootReturnsNull_ThrowsNPE() {
//         JsonSchema spy = spy(schemaWithoutId);
//         // Stub the internal call to return null.
//         doReturn(null).when(spy).findSchemaResourceRoot();
//         assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/path"), "When findSchemaResourceRoot() returns null a NPE should be thrown");
//     }

//     @Test
//     void testGetRefSchemaNode_RootNodeIsNull_ThrowsNPE() {
//         JsonSchema spy = spy(schemaWithoutId);
//         // Stub the root node to be null.
//         doReturn(null).when(spy).getSchemaNode();
//         assertThrows(NullPointerException.class, () -> spy.getRefSchemaNode("/any/path"), "When the root schema node is null a NPE should be thrown");
//     }

    @Test
    void testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException() {
        // The "~" character is illegal in a JSON‑Pointer unless escaped.
        assertThrows(IllegalArgumentException.class, () -> schemaWithoutId.getRefSchemaNode("/invalid~pointer"), "An illegal JSON‑Pointer should propagate IllegalArgumentException");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

17:52:10.898 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword components - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
17:52:10.899 [main] WARN com.networknt.schema.UnknownKeywordFactory -- Unknown keyword defs - you should define your own Meta Schema. If the keyword is irrelevant for validation, just use a NonValidationKeyword or if it should generate annotations AnnotationKeyword
╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_getRefSchemaNode_Test ✔
│     ├─ testGetRefSchemaNode_ValidPointer() ✔
│     ├─ testGetRefSchemaNode_VariousScenarios(String, String, String, boolean, String) ✔
│     │  ├─ [1] original schema node ✔
│     │  ├─ [2] node at path "/defs/thing" (id stripped, hash present) ✔
│     │  ├─ [3] node at path "/defs/thing" (id stripped, no hash) ✔
│     │  ├─ [4] node at path "/components/schemas/Person" ✔
│     │  ├─ [5] node returned by handleNullNode (missing) ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local7>" is null
│     │  ├─ [6] original schema node (id matches exactly) ✔
│     │  ├─ [7] original schema node (id matches exactly) ✔
│     │  ├─ [8] original schema node (hash without slash) ✔
│     │  ├─ [9] original schema node (hash followed by slash) ✘ Expected the original schema node for ref "#/" ==> expected: <{"$schema":"https://json-schema.org/draft/2020-12/schema","components":{"schemas":{"Person":{"type":"object","properties":{}}}},"defs":{"thing":{"type":"string"}},"definitions":{"foo":{"type":"string","description":"A simple string"},"bar":{"type":"integer"}}}> but was: <null>
│     │  ├─ [10] node at path "/components/schemas/Person" ✔
│     │  ├─ [11] node at path "/defs/thing" (URL‑encoded) ✔
│     │  ├─ [12] node at path "/defs/thing" (URL‑encoded) ✔
│     │  ├─ [13] original schema node (empty ref) ✔
│     │  ├─ [14] original schema node (sole '#') ✔
│     │  ├─ [15] original schema node (id does not match) ✔
│     │  └─ [16] node returned by handleNullNode (missing) ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local7>" is null
│     ├─ testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException() ✘ An illegal JSON‑Pointer should propagate IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
│     ├─ testGetRefSchemaNode_UrlEncodedPointer() ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.asText()" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
│     ├─ testGetRefSchemaNode_NullReference_ThrowsNPE() ✔
│     ├─ testGetRefSchemaNode_PointerWithoutHash() ✔
│     └─ testGetRefSchemaNode_UnknownPointer() ✘ Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local1>" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (6):
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_VariousScenarios(String, String, String, boolean, String):[5] node returned by handleNullNode (missing)
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_VariousScenarios', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local7>" is null
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_VariousScenarios(JsonSchema_getRefSchemaNode_Test.java:225)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_VariousScenarios(String, String, String, boolean, String):[9] original schema node (hash followed by slash)
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_VariousScenarios', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String']
    => org.opentest4j.AssertionFailedError: Expected the original schema node for ref "#/" ==> expected: <{"$schema":"https://json-schema.org/draft/2020-12/schema","components":{"schemas":{"Person":{"type":"object","properties":{}}}},"defs":{"thing":{"type":"string"}},"definitions":{"foo":{"type":"string","description":"A simple string"},"bar":{"type":"integer"}}}> but was: <null>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertSame.failNotSame(AssertSame.java:50)
       org.junit.jupiter.api.AssertSame.assertSame(AssertSame.java:35)
       org.junit.jupiter.api.Assertions.assertSame(Assertions.java:2873)
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_VariousScenarios(JsonSchema_getRefSchemaNode_Test.java:230)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_VariousScenarios(String, String, String, boolean, String):[16] node returned by handleNullNode (missing)
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_VariousScenarios', methodParameterTypes = 'java.lang.String, java.lang.String, java.lang.String, boolean, java.lang.String']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local7>" is null
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_VariousScenarios(JsonSchema_getRefSchemaNode_Test.java:225)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: An illegal JSON‑Pointer should propagate IllegalArgumentException ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:39)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3131)
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_InvalidJsonPointer_ThrowsIllegalArgumentException(JsonSchema_getRefSchemaNode_Test.java:261)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_UrlEncodedPointer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_UrlEncodedPointer', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.asText()" because the return value of "com.fasterxml.jackson.databind.JsonNode.get(String)" is null
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_UrlEncodedPointer(JsonSchema_getRefSchemaNode_Test.java:213)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]
  JUnit Jupiter:JsonSchema_getRefSchemaNode_Test:testGetRefSchemaNode_UnknownPointer()
    MethodSource [className = 'com.networknt.schema.JsonSchema_getRefSchemaNode_Test', methodName = 'testGetRefSchemaNode_UnknownPointer', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.fasterxml.jackson.databind.JsonNode.isMissingNode()" because "<local1>" is null
       com.networknt.schema.JsonSchema_getRefSchemaNode_Test.testGetRefSchemaNode_UnknownPointer(JsonSchema_getRefSchemaNode_Test.java:202)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:728)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       org.junit.jupiter.engine.extension.TimeoutExtension.interceptTestableMethod(TimeoutExtension.java:147)
       [...]

Test run finished after 396 ms
[         5 containers found      ]
[         0 containers skipped    ]
[         5 containers started    ]
[         0 containers aborted    ]
[         5 containers successful ]
[         0 containers failed     ]
[        22 tests found           ]
[         0 tests skipped         ]
[        22 tests started         ]
[         0 tests aborted         ]
[        16 tests successful      ]
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
 

api document of method getRefSchemaNode(String): 

     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     

return type: 
com.fasterxml.jackson.databind.JsonNode

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `public JsonNode getRefSchemaNode(String)` returns `com.fasterxml.jackson.databind.JsonNode`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Find the schema node for $ref attribute.
     *
     * @param ref String
     * @return JsonNode
     ;
class `com.networknt.schema.resource.PrefixSchemaMapper`:
	related functions:
		method `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.resource.MetaSchemaMapper`:
	related functions:
		method `public AbsoluteIri map(AbsoluteIri)` returns `com.networknt.schema.AbsoluteIri`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.org.apache.commons.validator.routines.DomainValidator`:
	related functions:
		method `private String chompLeadingDot(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.SchemaLocation.Fragment`:
	related functions:
		method `public static JsonNodePath of(String)` returns `com.networknt.schema.JsonNodePath`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
         * Parses a string representing a fragment.
         * 
         * @param fragmentString the fragment
         * @return the path
         ;
class `com.networknt.schema.output.OutputUnitData`:
	related functions:
		method `public static String formatMessage(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public boolean isValidInet4Address(String)` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Validates an IPv4 address. Returns true if valid.
     * @param inet4Address the IPv4 address to validate
     * @return true if the argument contains a valid IPv4 address
     ;
class `com.networknt.schema.resource.ClasspathSchemaLoader`:
	related functions:
		method `public InputStreamSource getSchema(AbsoluteIri)` returns `com.networknt.schema.resource.InputStreamSource`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`;
class `com.networknt.schema.SchemaLocation`:
	related functions:
		method `public static SchemaLocation of(String)` returns `com.networknt.schema.SchemaLocation`, related with `com.networknt.schema.JsonSchema.getRefSchemaNode(String)`, api document: 
     * Parses a string representing an IRI of the schema location.
     * 
     * @param iri the IRI
     * @return the schema location
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
