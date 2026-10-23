
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.net.URI;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import java.lang.reflect.Field;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import com.networknt.schema.uri.URIFactory;
import com.networknt.schema.urn.URNFactory;

@ExtendWith(MockitoExtension.class)
class AnyOfValidator_validate_Test {

    /**
     * shared objects for the whole test suite
     */
    private static ObjectMapper mapper;

    private static SchemaValidatorsConfig config;

    /**
     * objects created for each test
     */
    @Mock
    private URIFactory uriFactory;

    @Mock
    private URNFactory urnFactory;

    @Mock
    private JsonMetaSchema metaSchema;

    @Mock
    private JsonSchemaFactory jsonSchemaFactory;

    private ValidationContext validationContext;

    private ExecutionContext executionContext;

    /**
     * the validator under test
     */
    private AnyOfValidator anyOfValidator;

    @Mock
    private CollectorContext collectorContext;

    @Mock
    private ValidatorState validatorState;

    // used for both enter/exit scopes
    @Mock
    private CollectorContext.Scope mockScope;

    @BeforeAll
    static void setupBeforeAll() {
        mapper = new ObjectMapper();
        // we will toggle discriminator flag per test
        config = new SchemaValidatorsConfig();
    }

    @BeforeEach
    void setupBeforeEach() {
        // -----------------------------------------------------------------
        // 1. Build a real ValidationContext – we only need the config part,
        //    all other collaborators are mocked because they are not touched
        //    by the AnyOfValidator.validate logic we are exercising.
        // -----------------------------------------------------------------
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, jsonSchemaFactory, config);
        // -----------------------------------------------------------------
        // 2. ExecutionContext – a simple instance with a fresh CollectorContext.
        // -----------------------------------------------------------------
        // uses default CollectorContext
        executionContext = new ExecutionContext();
        // -----------------------------------------------------------------
        // 3. Prepare a dummy ANY‑OF schema node (an array with two subschemas).
        // -----------------------------------------------------------------
        ArrayNode anyOfArray = mapper.createArrayNode();
        // {} – first subschema
        anyOfArray.add(mapper.createObjectNode());
        // {} – second subschema
        anyOfArray.add(mapper.createObjectNode());
        // -----------------------------------------------------------------
        // 4. Mock the ValidationContext#newSchema method so that it returns
        //    our own mock JsonSchema objects (one per array element).
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = mock(JsonSchema.class);
        JsonSchema subSchema2 = mock(JsonSchema.class);
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// first call → first subschema
        subSchema1).thenReturn(// second call → second subschema
        subSchema2);
        // -----------------------------------------------------------------
        // 5. Instantiate the validator under test.
        // -----------------------------------------------------------------
        anyOfValidator = new AnyOfValidator(// schemaPath (arbitrary)
        "anyOfTest", // the ANY‑OF node
        anyOfArray, // parentSchema – not needed for these tests
        null, validationContext);
    }

    /* --------------------------------------------------------------------- *
     * Helper – inject a mock DiscriminatorContext (used when the config flag
     * is true).  The field is private, so we use reflection.
     * --------------------------------------------------------------------- */
    private void injectDiscriminatorContext(DiscriminatorContext ctx) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("discriminatorContext");
        f.setAccessible(true);
        f.set(anyOfValidator, ctx);
    }

    /* --------------------------------------------------------------------- *
     * Test case 1 – one subschema validates successfully, the other returns
     * errors. According to the implementation, as soon as a subschema validates
     * without errors the validator clears the accumulated errors and returns
     * an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenOneSubschemaValid_returnsEmptySet() {
        // -----------------------------------------------------------------
        // Arrange – first subschema returns no errors, second returns one.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        // first subschema – successful validation
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        // second subschema – produces a single ValidationMessage
        ValidationMessage error = mock(ValidationMessage.class);
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
        // arbitrary instance node
        JsonNode node = mapper.createObjectNode();
        // arbitrary root node
        JsonNode root = mapper.createObjectNode();
        String at = "$";
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        // -----------------------------------------------------------------
        // Assert – because the first subschema succeeded, result must be empty.
        // -----------------------------------------------------------------
        assertTrue(result.isEmpty(), "Result should be empty when any subschema validates");
    }

    /* --------------------------------------------------------------------- *
     * Test case 2 – all subschemas return errors. The validator must aggregate
     * those errors (excluding “required” errors when appropriate) and return
     * them unchanged.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenAllSubschemasInvalid_aggregatesErrors() {
        // -----------------------------------------------------------------
        // Arrange – both subschemas return distinct ValidationMessage objects.
        // -----------------------------------------------------------------
        JsonSchema subSchema1 = validationContext.newSchema("anyOfTest/0", null, null);
        JsonSchema subSchema2 = validationContext.newSchema("anyOfTest/1", null, null);
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        ValidationMessage err1 = mock(ValidationMessage.class);
        ValidationMessage err2 = mock(ValidationMessage.class);
        when(err1.getType()).thenReturn("type1");
        when(err2.getType()).thenReturn("type2");
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$";
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        // -----------------------------------------------------------------
        // Assert – both errors must be present.
        // -----------------------------------------------------------------
        assertEquals(2, result.size(), "Both validation errors should be aggregated");
        assertTrue(result.containsAll(Set.of(err1, err2)), "Result set must contain the two mocked errors");
    }

    /* --------------------------------------------------------------------- *
     * Helper – replace the private 'schemas' list with a list of mocks.
     * --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private void injectSchemas(JsonSchema... schemas) throws Exception {
        Field f = AnyOfValidator.class.getDeclaredField("schemas");
        f.setAccessible(true);
        List<JsonSchema> list = new ArrayList<>(Arrays.asList(schemas));
        f.set(anyOfValidator, list);
    }

    /* --------------------------------------------------------------------- *
     * 1. OpenAPI3StyleDiscriminators disabled – early success (no type
     *    validator, empty error set).  The validator must return an empty
     *    set immediately.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenNoTypeValidatorAndEmptyErrors_returnsEmptySet() throws Exception {
        // explicit – default is false
        config.setOpenAPI3StyleDiscriminators(false);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        injectSchemas(mockSchema);
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "$.foo";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertTrue(result.isEmpty(), "Result must be empty when a subschema validates without errors");
    }

    /* --------------------------------------------------------------------- *
     * 2. Type validator mismatch – schema expects INTEGER but node is STRING.
     *    The validator should add a type‑mismatch message and return it.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenTypeValidatorMismatch_returnsTypeError() throws Exception {
        config.setOpenAPI3StyleDiscriminators(false);
        // ---- mock TypeValidator -------------------------------------------------
        TypeValidator typeValidator = mock(TypeValidator.class);
        // any non‑UNION type
        when(typeValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        // node type does NOT match
        when(typeValidator.equalsToSchemaType(any())).thenReturn(false);
        // ---- mock subschema ----------------------------------------------------
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(true);
        when(mockSchema.getTypeValidator()).thenReturn(typeValidator);
        // validate() will NOT be called because of early continue
        injectSchemas(mockSchema);
        // a textual node – the actual content does not matter
        JsonNode node = mapper.readTree("\"string\"");
        JsonNode root = mapper.createObjectNode();
        String at = "$.bar";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertEquals(1, result.size(), "Exactly one type‑mismatch ValidationMessage should be returned");
    }

    /* --------------------------------------------------------------------- *
     * 3. OpenAPI3StyleDiscriminators enabled – discriminator match found
     *    *with* errors.  The returned set must contain the schema errors plus
     *    the DISCRIMINATOR_REMARK message.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenDiscriminatorMatchFoundWithErrors_returnsErrorsPlusRemark() throws Exception {
        config.setOpenAPI3StyleDiscriminators(true);
        // ---- mock DiscriminatorContext -----------------------------------------
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        injectDiscriminatorContext(discCtx);
        // ---- mock schema that returns a non‑empty error set --------------------
        ValidationMessage schemaError = mock(ValidationMessage.class);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
        injectSchemas(mockSchema);
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/0";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertEquals(2, result.size(), "Should contain the original error and the discriminator remark");
        // the remark is generated via BaseJsonValidator.buildValidationMessage – we verify that at
        // least one of the messages ends with the known suffix.
        boolean remarkFound = result.stream().anyMatch(v -> v.getMessage() != null && v.getMessage().endsWith(AnyOfValidator.DISCRIMINATOR_REMARK));
        assertTrue(remarkFound, "Result must contain the discriminator‑remark message");
    }

    /* --------------------------------------------------------------------- *
     * 4. OpenAPI3StyleDiscriminators enabled – discriminator match found
     *    *without* errors.  The method should return an empty set.
     * --------------------------------------------------------------------- */
    @Test
    void validate_whenDiscriminatorMatchFoundWithoutErrors_returnsEmptySet() throws Exception {
        config.setOpenAPI3StyleDiscriminators(true);
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        injectDiscriminatorContext(discCtx);
        JsonSchema mockSchema = mock(JsonSchema.class);
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
        injectSchemas(mockSchema);
        JsonNode node = mapper.createObjectNode();
        JsonNode root = mapper.createObjectNode();
        String at = "#/anyOf/1";
        Set<ValidationMessage> result = anyOfValidator.validate(executionContext, node, root, at);
        assertTrue(result.isEmpty(), "When discriminator match succeeds without errors, result must be empty");
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:12: error: package org.mockito does not exist
import static org.mockito.Mockito.*;
                         ^
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:15: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:8: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:16: error: package org.mockito does not exist
import org.mockito.*;
^
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:20: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:33: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:36: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:39: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:42: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:54: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:57: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:61: error: cannot find symbol
    @Mock
     ^
  symbol:   class Mock
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:96: error: cannot find symbol
        JsonSchema subSchema1 = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:97: error: cannot find symbol
        JsonSchema subSchema2 = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:98: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// first call → first subschema
                                         ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:98: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// first call → first subschema
                                                      ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:98: error: cannot find symbol
        when(validationContext.newSchema(anyString(), any(JsonNode.class), any(JsonSchema.class))).thenReturn(// first call → first subschema
                                                                           ^
  symbol:   method any(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:133: error: cannot find symbol
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:134: error: cannot find symbol
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:136: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:136: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:136: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:136: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:138: error: cannot find symbol
        ValidationMessage error = mock(ValidationMessage.class);
                                  ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:139: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:139: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:139: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:139: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(error));
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:167: error: cannot find symbol
        when(subSchema1.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:168: error: cannot find symbol
        when(subSchema2.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:169: error: cannot find symbol
        ValidationMessage err1 = mock(ValidationMessage.class);
                                 ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:170: error: cannot find symbol
        ValidationMessage err2 = mock(ValidationMessage.class);
                                 ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:171: error: cannot find symbol
        when(err1.getType()).thenReturn("type1");
        ^
  symbol:   method when(String)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:172: error: cannot find symbol
        when(err2.getType()).thenReturn("type2");
        ^
  symbol:   method when(String)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:173: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:173: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:173: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:173: error: cannot find symbol
        when(subSchema1.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err1));
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:174: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:174: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:174: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:174: error: cannot find symbol
        when(subSchema2.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(err2));
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:209: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:210: error: cannot find symbol
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:211: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:211: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:211: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:211: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:228: error: cannot find symbol
        TypeValidator typeValidator = mock(TypeValidator.class);
                                      ^
  symbol:   method mock(Class<TypeValidator>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:230: error: cannot find symbol
        when(typeValidator.getSchemaType()).thenReturn(JsonType.INTEGER);
        ^
  symbol:   method when(JsonType)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:232: error: cannot find symbol
        when(typeValidator.equalsToSchemaType(any())).thenReturn(false);
                                              ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:234: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:235: error: cannot find symbol
        when(mockSchema.hasTypeValidator()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:236: error: cannot find symbol
        when(mockSchema.getTypeValidator()).thenReturn(typeValidator);
        ^
  symbol:   method when(TypeValidator)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:256: error: cannot find symbol
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
                                       ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:257: error: cannot find symbol
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:260: error: cannot find symbol
        ValidationMessage schemaError = mock(ValidationMessage.class);
                                        ^
  symbol:   method mock(Class<ValidationMessage>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:261: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:262: error: cannot find symbol
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:263: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:263: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:263: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:263: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Set.of(schemaError));
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:272: error: DISCRIMINATOR_REMARK has private access in AnyOfValidator
        boolean remarkFound = result.stream().anyMatch(v -> v.getMessage() != null && v.getMessage().endsWith(AnyOfValidator.DISCRIMINATOR_REMARK));
                                                                                                                            ^
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:283: error: cannot find symbol
        DiscriminatorContext discCtx = mock(DiscriminatorContext.class);
                                       ^
  symbol:   method mock(Class<DiscriminatorContext>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:284: error: cannot find symbol
        when(discCtx.isDiscriminatorMatchFound()).thenReturn(true);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:286: error: cannot find symbol
        JsonSchema mockSchema = mock(JsonSchema.class);
                                ^
  symbol:   method mock(Class<JsonSchema>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:287: error: cannot find symbol
        when(mockSchema.hasTypeValidator()).thenReturn(false);
        ^
  symbol:   method when(boolean)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:288: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                 ^
  symbol:   method any()
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:288: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                        ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:288: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                             ^
  symbol:   method any(Class<JsonNode>)
  location: class AnyOfValidator_validate_Test
src/test/java/com/networknt/schema/AnyOfValidator_validate_Test.java:288: error: cannot find symbol
        when(mockSchema.validate(any(), any(JsonNode.class), any(JsonNode.class), anyString())).thenReturn(Collections.emptySet());
                                                                                  ^
  symbol:   method anyString()
  location: class AnyOfValidator_validate_Test
72 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'dependent classes': 'class `com.networknt.schema.ExecutionContext`:\n\t\nclass `com.networknt.schema.CollectorContext`:\n\t\nclass `com.networknt.schema.ValidatorState`:\n\tdependent fields:\n\t\tjava.lang.String com.networknt.schema.ValidatorState.VALIDATOR_STATE_KEY;\n\t\nclass `com.networknt.schema.ValidatorTypeCode`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidatorTypeCode com.networknt.schema.ValidatorTypeCode.REQUIRED;\n\t\nclass `com.networknt.schema.BaseJsonValidator`:\n\tdependent fields:\n\t\tcom.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;\n\t\nclass `com.networknt.schema.JsonType`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonType com.networknt.schema.JsonType.UNION;\n\t\nclass `com.networknt.schema.AnyOfValidator`:\n\tdependent fields:\n\t\torg.slf4j.Logger com.networknt.schema.AnyOfValidator.logger;\n\t\tcom.networknt.schema.ValidationContext.DiscriminatorContext com.networknt.schema.AnyOfValidator.discriminatorContext;\n\t\tjava.lang.String com.networknt.schema.AnyOfValidator.DISCRIMINATOR_REMARK;\n\t\tjava.util.List<com.networknt.schema.JsonSchema> com.networknt.schema.AnyOfValidator.schemas;\n\t\nclass `com.networknt.schema.OneOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AllOfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.PropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.AdditionalPropertiesValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.IfValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.TypeValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.NotValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.JsonSchema`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;\nclass `com.networknt.schema.RefValidator`:\n\trelated functions:\n\t\tmethod `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.AnyOfValidator.validate(ExecutionContext, JsonNode, JsonNode, String)`;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


