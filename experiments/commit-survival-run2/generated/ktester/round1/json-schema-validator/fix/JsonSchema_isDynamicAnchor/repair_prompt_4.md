


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;

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
        // Create a minimal (empty) schema node – the content does not matter for
        // the dynamic‑anchor flag.
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode emptyNode = mapper.createObjectNode();
        // Use the public factory to obtain a fully‑initialised JsonSchema.
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(emptyNode);
        // Force the private 'dynamicAnchor' field to the desired value.
        Field field = JsonSchema.class.getDeclaredField("dynamicAnchor");
        field.setAccessible(true);
        field.setBoolean(schema, dynamicAnchor);
        return schema;
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
        // Use the factory – it will read the $dynamicAnchor keyword.
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(node);
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
        JsonSchema schema = JsonSchemaFactory.getInstance().getSchema(node);
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
│     ├─ fromSchemaWithoutDynamicAnchor_isDynamicAnchorReturnsFalse() ✔
│     ├─ fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue() ✘ Schema created from a node containing "$dynamicAnchor": true must report true ==> expected: <true> but was: <false>
│     ├─ whenDynamicAnchorIsTrue_isDynamicAnchorReturnsTrue() ✔
│     ├─ childSchemaDoesNotInheritParentDynamicAnchor() ✔
│     └─ whenDynamicAnchorIsFalse_isDynamicAnchorReturnsFalse() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (1):
  JUnit Jupiter:JsonSchema_isDynamicAnchor_Test:fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue()
    MethodSource [className = 'com.networknt.schema.JsonSchema_isDynamicAnchor_Test', methodName = 'fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue', methodParameterTypes = '']
    => org.opentest4j.AssertionFailedError: Schema created from a node containing "$dynamicAnchor": true must report true ==> expected: <true> but was: <false>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertTrue.failNotTrue(AssertTrue.java:63)
       org.junit.jupiter.api.AssertTrue.assertTrue(AssertTrue.java:36)
       org.junit.jupiter.api.Assertions.assertTrue(Assertions.java:211)
       com.networknt.schema.JsonSchema_isDynamicAnchor_Test.fromSchemaWithDynamicAnchorTrue_isDynamicAnchorReturnsTrue(JsonSchema_isDynamicAnchor_Test.java:81)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]

Test run finished after 142 ms
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
[         4 tests successful      ]
[         1 tests failed          ]


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
