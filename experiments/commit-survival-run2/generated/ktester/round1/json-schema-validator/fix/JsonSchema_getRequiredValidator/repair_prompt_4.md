
You are an experienced Java developer tasked with fixing a piece of code that contains compilation errors. Your task is to resolve these issues after analyzing the code and its compilation feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any syntactical or logical errors.
2. Analyze the compilation feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import java.net.URI;
import java.lang.reflect.Field;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

class JsonSchema_getRequiredValidator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    //     @Mock
    private URIFactory mockUriFactory;

    //     @Mock
    private URNFactory mockUrnFactory;

    //     @Mock
    private JsonMetaSchema mockMetaSchema;

    //     @Mock
    private JsonSchemaFactory mockSchemaFactory;

    private ValidationContext validationContext;

    /**
     * The schema under test – created freshly for each test method.
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a ValidationContext using the default factories – they are required
        // for the JsonSchema construction but are not otherwise used in these tests.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        // Use the library‑provided default factories (they are never null).
        URIFactory uriFactory = URIFactory.getInstance();
        URNFactory urnFactory = URNFactory.getInstance();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        // ValidationContext constructor expects:
        // URIFactory, URNFactory, JsonMetaSchema, JsonSchemaFactory, SchemaValidatorsConfig
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    @AfterEach
    void teardownAfterEach() {
        jsonSchema = null;
        validationContext = null;
    }

    /**
     * Creates a {@link JsonSchema} from the supplied JSON schema node.
     *
     * @param schemaNode the JSON representation of the schema
     * @return a fully initialised {@link JsonSchema}
     */
    private JsonSchema createSchema(JsonNode schemaNode) {
        JsonSchema schema = JsonSchema.from(// validation context
        validationContext, // current URI fragment
        "#", // base URI
        URI.create("http://example.com/base"), // the schema node itself
        schemaNode, // root node – not needed for these tests
        null, // isRootSchema
        false);
        // Populate the validator objects (including the required validator, if any).
        schema.initializeValidators();
        return schema;
    }

    /**
     * Reflectively sets the private {@code requiredValidator} field on a
     * {@link JsonSchema} instance.
     *
     * @param schema   the target schema
     * @param validator the validator to inject
     */
    private void injectRequiredValidator(JsonSchema schema, JsonValidator validator) throws Exception {
        Field f = JsonSchema.class.getDeclaredField("requiredValidator");
        f.setAccessible(true);
        f.set(schema, validator);
    }

    @Test
    void testGetRequiredValidatorWhenPresent() throws Exception {
        String json = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "name": { "type": "string" }
              },
              "required": ["name"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Schema should report that it has a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator, "getRequiredValidator() must return a non‑null validator");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator type should be RequiredValidator");
    }

    @Test
    void testGetRequiredValidatorWhenAbsent() throws Exception {
        String json = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": {
                "age": { "type": "integer" }
              }
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(), "Schema should report that it does NOT have a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(), "getRequiredValidator() must return null when no required validator is present");
    }

    @Test
    @DisplayName("Default behavior without required validator")
    void testDefaultBehaviorWithoutRequiredValidator() throws Exception {
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNull(validator, "A newly created JsonSchema should have requiredValidator == null");
    }

    @Test
    @DisplayName("Required validator set by initialise validators")
    void testRequiredValidatorSetByInitializeValidators() throws Exception {
        String json = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": { "name": { "type":"string"} },
              "required": ["name"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        // initialiseValidators already called in createSchema, calling again is safe
        jsonSchema.initializeValidators();
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Schema should report having a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator, "getRequiredValidator() must not return null");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator should be an instance of RequiredValidator");
    }

    @Test
    @DisplayName("Required validator remains null for schema without required keyword")
    void testRequiredValidatorRemainsNullWhenKeywordAbsent() throws Exception {
        String json = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "properties": { "age": { "type":"integer"} }
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(), "Schema should report NOT having a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(), "Getter must return null when no required keyword is present");
    }

    @Test
    @DisplayName("Required validator created for schema with empty required array")
    void testRequiredValidatorCreatedForEmptyRequiredArray() throws Exception {
        String json = """
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "type": "object",
              "required": []
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Even an empty required array should cause a RequiredValidator to be created");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator);
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator class should be RequiredValidator");
    }

    @Test
    @DisplayName("Idempotent getRequiredValidator call")
    void testIdempotentGetRequiredValidatorCall() throws Exception {
        String json = """
            {
              "type": "object",
              "required": ["id"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        JsonValidator first = jsonSchema.getRequiredValidator();
        JsonValidator second = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(first);
        Assertions.assertSame(first, second, "Subsequent calls to getRequiredValidator() must return the same instance");
    }

    @Test
    @DisplayName("Child schema without its own required keyword does not inherit parent's validator")
    void testChildSchemaDoesNotInheritParentValidator() throws Exception {
        String parentJson = """
            {
              "type": "object",
              "required": ["parentProp"],
              "properties": {
                "child": { "type":"object", "properties": { "foo": {"type":"string"} } }
              }
            }
            """;
        JsonNode parentNode = MAPPER.readTree(parentJson);
        JsonSchema parentSchema = createSchema(parentNode);
        parentSchema.initializeValidators();
        JsonNode childNode = parentNode.at("/properties/child");
        Assertions.assertFalse(childNode.isMissingNode(), "Child node must exist");
        // Create a child schema based on the parent – the method uses the same
        // ValidationContext, so the factories are already correctly set.
        JsonSchema childSchema = parentSchema.createChildSchema("#/properties/child", childNode);
        childSchema.initializeValidators();
        Assertions.assertNull(childSchema.getRequiredValidator(), "Child schema without a required keyword must not inherit the parent's RequiredValidator");
    }

    @Test
    @DisplayName("Re‑initialising validators does not clear required validator")
    void testReinitialisingValidatorsDoesNotClear() throws Exception {
        String json = """
            {
              "type": "object",
              "required": ["a"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        JsonValidator first = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(first, "First initialisation must produce a RequiredValidator");
        // Re‑initialise – the validator should stay present.
        jsonSchema.initializeValidators();
        JsonValidator second = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(second, "RequiredValidator must remain non‑null after re‑initialisation");
        Assertions.assertSame(first, second, "Re‑initialisation must not replace the existing RequiredValidator");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:41: error: cannot find symbol
        URIFactory uriFactory = URIFactory.getInstance();
                                          ^
  symbol:   method getInstance()
  location: interface URIFactory
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:42: error: cannot find symbol
        URNFactory urnFactory = URNFactory.getInstance();
                                          ^
  symbol:   method getInstance()
  location: interface URNFactory
Note: src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
2 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonValidator', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonValidator com.networknt.schema.JsonSchema.requiredValidator;\n\trelated functions:\n\t\tmethod `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`, api document: \n     * Please note that the key in {@link #validators} map is a schema path. It is\n     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.\n     ;\n\t\tmethod `public boolean hasRequiredValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


