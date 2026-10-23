
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
import java.util.Collections;
import java.util.Set;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JsonSchema_getRequiredValidator_Test {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Mocks required to build a {@link ValidationContext}.
     */
    @Mock
    private URIFactory mockUriFactory;

    @Mock
    private URNFactory mockUrnFactory;

    @Mock
    private JsonMetaSchema mockMetaSchema;

    @Mock
    private JsonSchemaFactory mockSchemaFactory;

    private ValidationContext validationContext;

    /**
     * The schema under test – created freshly for each test method.
     */
    private JsonSchema jsonSchema;

    @BeforeEach
    void setupBeforeEach() {
        // Build a ValidationContext using real collaborators – no side‑effects needed for the tests.
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        // use default factory
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        validationContext = new ValidationContext(// URIFactory – not required for these tests
        null, // URNFactory – not required for these tests
        null, null, metaSchema, schemaFactory, config);
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
        schemaNode, // root node (not needed here)
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
        // GIVEN – a schema that contains a "required" keyword.
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
        // WHEN
        jsonSchema = createSchema(schemaNode);
        // THEN
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Schema should report that it has a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator, "getRequiredValidator() must return a non‑null validator");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator type should be RequiredValidator");
    }

    @Test
    void testGetRequiredValidatorWhenAbsent() throws Exception {
        // GIVEN – a schema without a "required" keyword.
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
        // WHEN
        jsonSchema = createSchema(schemaNode);
        // THEN
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(), "Schema should report that it does NOT have a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(), "getRequiredValidator() must return null when no required validator is present");
    }

    @Test
    @DisplayName("Default behavior without required validator")
    void testDefaultBehaviorWithoutRequiredValidator() throws Exception {
        // GIVEN – a minimal schema node; we do NOT call initializeValidators().
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        // WHEN – getRequiredValidator is queried immediately.
        JsonValidator validator = jsonSchema.getRequiredValidator();
        // THEN – the field is still null.
        Assertions.assertNull(validator, "A newly created JsonSchema should have requiredValidator == null");
    }

    @Test
    @DisplayName("Required validator set via reflection")
    void testRequiredValidatorSetViaReflection() throws Exception {
        // GIVEN – any schema instance.
        JsonNode node = MAPPER.readTree("{\"type\":\"object\"}");
        jsonSchema = createSchema(node);
        // normal init (no required keyword => still null)
        jsonSchema.initializeValidators();
        // Create a simple stub JsonValidator to inject.
        JsonValidator stubValidator = new JsonValidator() {

            @Override
            public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
                return Collections.emptySet();
            }

            // Some older versions also declare a single‑argument validate method.
            @Override
            public Set<ValidationMessage> validate(JsonNode node) {
                return Collections.emptySet();
            }

            @Override
            public void initialize(JsonNode node) {
                // no‑op
            }

            @Override
            public String getSchemaPath() {
                return "";
            }
        };
        injectRequiredValidator(jsonSchema, stubValidator);
        // WHEN – the getter is called.
        JsonValidator result = jsonSchema.getRequiredValidator();
        // THEN – the exact injected instance is returned.
        Assertions.assertSame(stubValidator, result, "Getter must return the validator that was injected via reflection");
    }

    @Test
    @DisplayName("Required validator set by initialise validators")
    void testRequiredValidatorSetByInitializeValidators() throws Exception {
        // GIVEN – a schema containing a non‑empty required array.
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
        // WHEN – initialiseValidators populates the internal field.
        jsonSchema.initializeValidators();
        // THEN – a non‑null RequiredValidator is present.
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Schema should report having a required validator");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator, "getRequiredValidator() must not return null");
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator should be an instance of RequiredValidator");
    }

    @Test
    @DisplayName("Required validator remains null for schema without required keyword")
    void testRequiredValidatorRemainsNullWhenKeywordAbsent() throws Exception {
        // GIVEN – a schema lacking the required keyword.
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
        // THEN – the required validator stays null.
        Assertions.assertFalse(jsonSchema.hasRequiredValidator(), "Schema should report NOT having a required validator");
        Assertions.assertNull(jsonSchema.getRequiredValidator(), "Getter must return null when no required keyword is present");
    }

    @Test
    @DisplayName("Required validator created for schema with empty required array")
    void testRequiredValidatorCreatedForEmptyRequiredArray() throws Exception {
        // GIVEN – a schema with an empty required array.
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
        // THEN – a RequiredValidator instance is still created.
        Assertions.assertTrue(jsonSchema.hasRequiredValidator(), "Even an empty required array should cause a RequiredValidator to be created");
        JsonValidator validator = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(validator);
        Assertions.assertEquals("RequiredValidator", validator.getClass().getSimpleName(), "Validator class should be RequiredValidator");
    }

    @Test
    @DisplayName("Idempotent getRequiredValidator call")
    void testIdempotentGetRequiredValidatorCall() throws Exception {
        // GIVEN – a schema with a required keyword.
        String json = """
            {
              "type": "object",
              "required": ["id"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        jsonSchema.initializeValidators();
        // WHEN – getter is called twice.
        JsonValidator first = jsonSchema.getRequiredValidator();
        JsonValidator second = jsonSchema.getRequiredValidator();
        // THEN – both references point to the same object.
        Assertions.assertNotNull(first);
        Assertions.assertSame(first, second, "Subsequent calls to getRequiredValidator() must return the same instance");
    }

    @Test
    @DisplayName("Child schema without its own required keyword does not inherit parent's validator")
    void testChildSchemaDoesNotInheritParentValidator() throws Exception {
        // Parent schema with required.
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
        // Extract the child node that does NOT contain a required keyword.
        JsonNode childNode = parentNode.at("/properties/child");
        Assertions.assertFalse(childNode.isMissingNode(), "Child node must exist");
        // Create child schema via the protected helper.
        JsonSchema childSchema = parentSchema.createChildSchema("#/properties/child", childNode);
        childSchema.initializeValidators();
        // THEN – child must not have a required validator.
        Assertions.assertNull(childSchema.getRequiredValidator(), "Child schema without a required keyword must not inherit the parent's RequiredValidator");
    }

    @Test
    @DisplayName("Re‑initialising validators does not clear required validator")
    void testReinitialisingValidatorsDoesNotClear() throws Exception {
        // GIVEN – a schema with a required keyword.
        String json = """
            {
              "type": "object",
              "required": ["a"]
            }
            """;
        JsonNode schemaNode = MAPPER.readTree(json);
        jsonSchema = createSchema(schemaNode);
        // First initialisation.
        jsonSchema.initializeValidators();
        JsonValidator first = jsonSchema.getRequiredValidator();
        Assertions.assertNotNull(first, "First initialisation must produce a RequiredValidator");
        // Second initialisation.
        jsonSchema.initializeValidators();
        JsonValidator second = jsonSchema.getRequiredValidator();
        // THEN – validator must still be non‑null (may be same or new, but never null).
        Assertions.assertNotNull(second, "RequiredValidator must remain non‑null after re‑initialisation");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:12: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:13: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:16: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:24: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRequiredValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:27: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRequiredValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:30: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRequiredValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class JsonSchema_getRequiredValidator_Test
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:50: error: constructor ValidationContext in class ValidationContext cannot be applied to given types;
        validationContext = new ValidationContext(// URIFactory – not required for these tests
                            ^
  required: URIFactory,URNFactory,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  found:    <null>,<null>,<null>,JsonMetaSchema,JsonSchemaFactory,SchemaValidatorsConfig
  reason: actual and formal argument lists differ in length
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:165: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:170: error: method does not override or implement a method from a supertype
            @Override
            ^
src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java:175: error: method does not override or implement a method from a supertype
            @Override
            ^
Note: src/test/java/com/networknt/schema/JsonSchema_getRequiredValidator_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
11 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'com.networknt.schema.JsonValidator', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonValidator com.networknt.schema.JsonSchema.requiredValidator;\n\trelated functions:\n\t\tmethod `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`, api document: \n     * Please note that the key in {@link #validators} map is a schema path. It is\n     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.\n     ;\n\t\tmethod `public boolean hasRequiredValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.getRequiredValidator()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


