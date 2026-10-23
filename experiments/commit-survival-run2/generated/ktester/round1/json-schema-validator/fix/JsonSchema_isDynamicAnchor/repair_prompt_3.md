


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

// @ExtendWith(MockitoExtension.class)
class JsonSchema_isDynamicAnchor_Test {

//     @Mock
    ValidationContext // mock – no real behavior needed
    validationContext;

//     @Mock
    JsonNode // mock – schema content not required for this test
    schemaNode;

    private JsonSchema jsonSchema;

//     @Mock
    JsonNode // dummy – used only for the private‑ctor path
    dummySchemaNode;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // By default create a schema with dynamicAnchor = false.
        jsonSchema = createJsonSchema(false);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
    }

    /**
     * Creates a {@link JsonSchema} instance using the private constructor.
     *
     * @param dynamicAnchor the value that the {@code isDynamicAnchor()} method should return
     * @throws Exception if reflection fails
     */
    private JsonSchema createJsonSchema(boolean dynamicAnchor) throws Exception {
        // private constructor signature:
        // JsonSchema(ValidationContext, String, URI, JsonNode, JsonSchema, boolean)
        Constructor<JsonSchema> ctor = JsonSchema.class.getDeclaredConstructor(ValidationContext.class, String.class, URI.class, JsonNode.class, JsonSchema.class, boolean.class);
        ctor.setAccessible(true);
        JsonSchema instance = ctor.newInstance(// ValidationContext – not needed for these tests
        null, // schema path
        "dummySchemaPath", // currentUri – not required
        null, // schema node – not required
        null, // parent – top‑level schema
        null, // suppressSubSchemaRetrieval – default
        false);
        // Set the private {@code dynamicAnchor} field to the desired value.
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(instance, dynamicAnchor);
        return instance;
    }

    @Test
    void whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() {
        // jsonSchema was created in @BeforeEach with dynamicAnchor = false
        assertFalse(jsonSchema.isDynamicAnchor(), "isDynamicAnchor() should return false when the internal flag is false");
    }

    @Test
    void whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Re‑create the instance with the flag set to true
        JsonSchema schemaWithDynamicAnchor = createJsonSchema(true);
        assertTrue(schemaWithDynamicAnchor.isDynamicAnchor(), "isDynamicAnchor() should return true when the internal flag is true");
    }

    /* -------------------------------------------------------------
     *  3. Creation via JsonSchema.from – $dynamicAnchor = true
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() throws Exception {
        // Build a JSON node: { "$dynamicAnchor": true }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("$dynamicAnchor", true);
        JsonSchema schema = JsonSchema.from(// ValidationContext – not needed for this unit test
        null, // schema path (parent)
        "schemaWithDynamicAnchor", // currentUri – not needed
        null, // schema node containing $dynamicAnchor
        node, // suppressSubSchemaRetrieval (parent node)
        null, // suppressSubSchemaRetrieval flag
        false);
        assertTrue(schema.isDynamicAnchor(), "Schema created from a node containing \"$dynamicAnchor\": true must report true");
    }

    /* -------------------------------------------------------------
     *  4. Creation via JsonSchema.from – $dynamicAnchor absent (defaults to false)
     * ------------------------------------------------------------- */
    @Test
    void fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() throws Exception {
        // Empty schema node: { }
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        JsonSchema schema = JsonSchema.from(null, "schemaWithoutDynamicAnchor", null, node, null, false);
        assertFalse(schema.isDynamicAnchor(), "Schema created from a node without \"$dynamicAnchor\" must report false");
    }

    /* -------------------------------------------------------------
     *  5. Child schema must NOT inherit the parent's dynamicAnchor value
     * ------------------------------------------------------------- */
    @Test
    void childSchemaDoesNotInheritParentDynamicAnchor() throws Exception {
        // 1️⃣ Create a parent schema where dynamicAnchor is true (via reflection)
        JsonSchema parent = createJsonSchema(true);
        assertTrue(parent.isDynamicAnchor(), "Pre‑condition: parent dynamicAnchor should be true");
        // 2️⃣ Create a child schema through the public API
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode childNode = mapper.createObjectNode();
        childNode.put("type", "string");
        JsonSchema child = parent.createChildSchema("#/properties/name", childNode);
        // 3️⃣ Verify that the child starts with false, i.e. it does not inherit the flag
        assertFalse(child.isDynamicAnchor(), "Child schemas must initialise dynamicAnchor to false independent of the parent");
    }
}
```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchema_isDynamicAnchor_Test ✔
│     ├─ fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     ├─ childSchemaDoesNotInheritParentDynamicAnchor() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
│     └─ whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() ✘ Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (5):
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.createJsonSchema(JsonSchema_isDynamicAnchor_Test.java:51)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.setupBeforeEach(JsonSchema_isDynamicAnchor_Test.java:32)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.createJsonSchema(JsonSchema_isDynamicAnchor_Test.java:51)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.setupBeforeEach(JsonSchema_isDynamicAnchor_Test.java:32)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.createJsonSchema(JsonSchema_isDynamicAnchor_Test.java:51)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.setupBeforeEach(JsonSchema_isDynamicAnchor_Test.java:32)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:childSchemaDoesNotInheritParentDynamicAnchor()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'childSchemaDoesNotInheritParentDynamicAnchor', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.createJsonSchema(JsonSchema_isDynamicAnchor_Test.java:51)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.setupBeforeEach(JsonSchema_isDynamicAnchor_Test.java:32)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse', methodParameterTypes = '']
    => java.lang.NullPointerException: Cannot invoke "com.networknt.schema.ValidationContext.getMetaSchema()" because "validationContext" is null
       com.networknt.schema.JsonSchema.<init>(JsonSchema.java:110)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
       java.base/jdk.internal.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
       java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:500)
       java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:481)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.createJsonSchema(JsonSchema_isDynamicAnchor_Test.java:51)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.setupBeforeEach(JsonSchema_isDynamicAnchor_Test.java:32)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       [...]

Test run finished after 44 ms
[         4 containers found      ]
[         0 containers skipped    ]
[         4 containers started    ]
[         0 containers aborted    ]
[         4 containers successful ]
[         0 containers failed     ]
[         5 tests found           ]
[         0 tests skipped         ]
[         5 tests started         ]
[         0 tests aborted         ]
[         0 tests successful      ]
[         5 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

return type: 
boolean

dependent classes: 
class `com.networknt.schema.JsonSchema`:
	dependent fields:
		boolean com.networknt.schema.JsonSchema.dynamicAnchor;
	related functions:
		method `public boolean isDynamicAnchor()` returns `boolean`, related with `com.networknt.schema.JsonSchema.isDynamicAnchor()`;
		method `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.isDynamicAnchor()`, api document: 
     * Please note that the key in {@link #validators} map is a schema path. It is
     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.
     ;
class `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:
	related functions:
		method `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.isDynamicAnchor()`, api document: 
     * Returns the singleton instance of this validator.
     *
     * @return the singleton instance of this validator
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
