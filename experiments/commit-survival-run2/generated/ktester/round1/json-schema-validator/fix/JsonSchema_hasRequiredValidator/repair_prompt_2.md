
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.Collections;
import java.util.Set;
import com.networknt.schema.urn.URNFactory;
import com.networknt.schema.uri.URIFactory;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JsonSchema_hasRequiredValidator_Test {

    /**
     * Shared objects for the whole test suite
     */
    private static ObjectMapper mapper;

    /**
     * Objects created for each test
     */
    private ValidationContext validationContext;

    private JsonSchema jsonSchemaNoRequired;

    private JsonSchema jsonSchemaWithRequired;

    /**
     * Simple dummy implementation of {@link JsonValidator}.  It does nothing
     * and is only used to verify that {@link JsonSchema#hasRequiredValidator()}
     * reacts to a non‑null validator being present.
     */
    private static class DummyValidator implements JsonValidator {

        @Override
        public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
            return Collections.emptySet();
        }

        @Override
        public String getKeyword() {
            return "dummy";
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Jackson mapper is cheap and thread‑safe – initialise once.
        mapper = new ObjectMapper();
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        /*
         * Build a minimal but functional ValidationContext. All the
         * arguments have sensible defaults that are sufficient for the
         * validator initialisation performed in the tests.
         */
        URIFactory uriFactory = createURIFactory();
        URNFactory urnFactory = createURNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        validationContext = new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
        // -----------------------------------------------------------------
        // 1️⃣  Schema **without** a "required" keyword – the validator should
        //     report that no required validator exists.
        // -----------------------------------------------------------------
        JsonNode schemaNodeNoRequired = mapper.readTree("{ \"type\": \"object\" }");
        jsonSchemaNoRequired = JsonSchema.from(validationContext, "schemaNoRequired", new URI("urn:test:schemaNoRequired"), schemaNodeNoRequired, null, false);
        // -----------------------------------------------------------------
        // 2️⃣  Schema **with** a "required" keyword – after initialisation the
        //     required validator will be created and hasRequiredValidator()
        //     must return true.
        // -----------------------------------------------------------------
        JsonNode schemaNodeWithRequired = mapper.readTree("{ \"type\": \"object\", \"required\": [\"name\"] }");
        jsonSchemaWithRequired = JsonSchema.from(validationContext, "schemaWithRequired", new URI("urn:test:schemaWithRequired"), schemaNodeWithRequired, null, false);
        // Initialise the validators for both schemas – this populates the
        // internal `requiredValidator` field when the schema contains a
        // "required" keyword.
        jsonSchemaNoRequired.initializeValidators();
        jsonSchemaWithRequired.initializeValidators();
    }

    /**
     * Sets the private {@code requiredValidator} field via reflection.
     */
    private static void setRequiredValidator(JsonSchema schema, JsonValidator validator) throws Exception {
        Field field = JsonSchema.class.getDeclaredField("requiredValidator");
        field.setAccessible(true);
        field.set(schema, validator);
    }

    private static Stream<Arguments> provideHasRequiredValidatorDirectManipulation() throws Exception {
        // 1️⃣  Fresh schema – no required validator.
        JsonSchema freshSchema = JsonSchema.from(createValidationContext(), "fresh", new URI("urn:test:fresh"), new ObjectMapper().readTree("{}"), null, false);
        // Do NOT call initialiseValidators(); we want the field to stay null.
        // 2️⃣  Schema where we inject a dummy validator.
        JsonSchema injectedSchema = JsonSchema.from(createValidationContext(), "injected", new URI("urn:test:injected"), new ObjectMapper().readTree("{}"), null, false);
        JsonValidator dummyValidator = new DummyValidator();
        setRequiredValidator(injectedSchema, dummyValidator);
        return Stream.of(Arguments.of("Fresh instance – no required validator assigned", freshSchema, false), Arguments.of("After injecting a non‑null JsonValidator via reflection", injectedSchema, true));
    }

    private static Stream<Arguments> provideInitializationScenarios() {
        return Stream.of(// 1️⃣ object with required → true
        Arguments.of("Object schema with a non‑empty \"required\" array creates RequiredValidator", null, "{\"type\":\"object\",\"required\":[\"name\"]}", false, true), // 2️⃣ object without required → false
        Arguments.of("Object schema without \"required\" keyword must not create RequiredValidator", null, "{\"type\":\"object\"}", false, false), // 3️⃣ non‑object type with required → false
        Arguments.of("\"required\" on a non‑object type (string) is ignored; no RequiredValidator", null, "{\"type\":\"string\",\"required\":[\"len\"]}", false, false), // 4️⃣ object with required then reset → false
        Arguments.of("After creating a RequiredValidator, resetting the field via reflection should return false", null, "{\"type\":\"object\",\"required\":[\"id\"]}", true, false), // 5️⃣ child schema without its own required (parent has) → false
        Arguments.of("Child schema without its own \"required\" must not inherit parent’s RequiredValidator", "{\"type\":\"object\",\"required\":[\"parentProp\"]}", "{\"type\":\"object\"}", false, false), // 6️⃣ child schema with its own required (parent also has) → true
        Arguments.of("Both parent and child define \"required\"; child gets its own RequiredValidator", "{\"type\":\"object\",\"required\":[\"parentProp\"]}", "{\"type\":\"object\",\"required\":[\"childProp\"]}", false, true));
    }

    /* --------------------------------------------------------------
     * Utility to create a fresh ValidationContext for static method sources.
     * -------------------------------------------------------------- */
    private static ValidationContext createValidationContext() {
        URIFactory uriFactory = createURIFactory();
        URNFactory urnFactory = createURNFactory();
        JsonMetaSchema metaSchema = JsonMetaSchema.getV7();
        JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance();
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        return new ValidationContext(uriFactory, urnFactory, metaSchema, schemaFactory, config);
    }

    /* -----------------------------------------------------------------
     * Simple concrete implementations of the abstract factories used by
     * ValidationContext.  The library provides default implementations
     * (DefaultURIFactory and DefaultURNFactory) that can be instantiated
     * directly.
     * ----------------------------------------------------------------- */
    private static URIFactory createURIFactory() {
        return new URIFactory() {

            @Override
            public URI create(String uri) {
                return URI.create(uri);
            }
        };
    }

    private static URNFactory createURNFactory() {
        return new URNFactory() {

            @Override
            public URI create(String urn) {
                return URI.create(urn);
            }
        };
    }

    @Test
    void hasRequiredValidator_returnsFalseWhenNoRequiredKeyword() {
        Assertions.assertFalse(jsonSchemaNoRequired.hasRequiredValidator(), "hasRequiredValidator() should be false when the schema does not contain a 'required' keyword");
    }

    @Test
    void hasRequiredValidator_returnsTrueWhenRequiredKeywordPresent() {
        Assertions.assertTrue(jsonSchemaWithRequired.hasRequiredValidator(), "hasRequiredValidator() should be true when the schema contains a 'required' keyword");
    }

    /**
     * Returns the current value of the private {@code requiredValidator} field (for debugging).
     */
    @SuppressWarnings("unused")
    private static JsonValidator getRequiredValidator(JsonSchema schema) throws Exception {
        Field field = JsonSchema.class.getDeclaredField("requiredValidator");
        field.setAccessible(true);
        return (JsonValidator) field.get(schema);
    }

    /**
     * Covers the two basic scenarios described in the original test‑case JSON:
     *
     * <ul>
     *   <li>A newly created {@link JsonSchema} instance – no validator has been assigned
     *       yet, so the method must return {@code false}.</li>
     *   <li>After injecting a non‑null {@link JsonValidator} (a Mockito mock) via reflection,
     *       the method must recognise the presence of the validator and return {@code true}.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("provideHasRequiredValidatorDirectManipulation")
    void hasRequiredValidator_directManipulation(String description, JsonSchema schema, boolean expected) throws Exception {
        Assertions.assertEquals(expected, schema.hasRequiredValidator(), "hasRequiredValidator() did not match expectation: " + description);
    }

    /**
     * Parameterised test that exercises {@link JsonSchema#initializeValidators()}
     * with a variety of schema definitions, including parent/child relationships
     * and a reset‑via‑reflection scenario.
     */
    @ParameterizedTest(name = "{index} ⇒ {4}")
    @MethodSource("provideInitializationScenarios")
    void hasRequiredValidator_initialization(String description, String parentSchemaJson, String schemaJson, boolean resetRequiredValidator, boolean expected) throws Exception {
        // --------------------------------------------------------------
        // 1️⃣  Build the *parent* schema if one is supplied.
        // --------------------------------------------------------------
        JsonSchema parent = null;
        if (parentSchemaJson != null) {
            JsonNode parentNode = mapper.readTree(parentSchemaJson);
            parent = JsonSchema.from(validationContext, "parent", new URI("urn:test:parent"), parentNode, null, false);
            // ensure any parent validator is instantiated
            parent.initializeValidators();
        }
        // --------------------------------------------------------------
        // 2️⃣  Build the *target* schema (either root or child).
        // --------------------------------------------------------------
        JsonNode schemaNode = mapper.readTree(schemaJson);
        JsonSchema target;
        if (parent != null) {
            // Child schema – use the public factory method.
            target = parent.createChildSchema("child", schemaNode);
        } else {
            target = JsonSchema.from(validationContext, "root", new URI("urn:test:root"), schemaNode, null, false);
        }
        target.initializeValidators();
        // --------------------------------------------------------------
        // 3️⃣  Optionally reset the requiredValidator field via reflection.
        // --------------------------------------------------------------
        if (resetRequiredValidator) {
            setRequiredValidator(target, null);
        }
        // --------------------------------------------------------------
        // 4️⃣  Assertion.
        // --------------------------------------------------------------
        Assertions.assertEquals(expected, target.hasRequiredValidator(), "Unexpected hasRequiredValidator() result for scenario: " + description);
    }
}
```

Corresponding compilation feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8
src/test/java/com/networknt/schema/JsonSchema_hasRequiredValidator_Test.java:17: error: package org.mockito.junit.jupiter does not exist
import org.mockito.junit.jupiter.MockitoExtension;
                                ^
src/test/java/com/networknt/schema/JsonSchema_hasRequiredValidator_Test.java:19: error: cannot find symbol
@ExtendWith(MockitoExtension.class)
            ^
  symbol: class MockitoExtension
src/test/java/com/networknt/schema/JsonSchema_hasRequiredValidator_Test.java:48: error: method does not override or implement a method from a supertype
        @Override
        ^
src/test/java/com/networknt/schema/JsonSchema_hasRequiredValidator_Test.java:142: error: <anonymous com.networknt.schema.JsonSchema_hasRequiredValidator_Test$1> is not abstract and does not override abstract method create(URI,String) in URIFactory
        return new URIFactory() {
                                ^
Note: src/test/java/com/networknt/schema/JsonSchema_hasRequiredValidator_Test.java uses or overrides a deprecated API.
Note: Recompile with -Xlint:deprecation for details.
4 errors

```

And you can use the following related information to assist you in your analysis and correction:
```
{'api document of class com.networknt.schema.JsonSchema': '\n * This is the core of json constraint implementation. It parses json constraint\n * file and generates JsonValidators. The class is thread safe, once it is\n * constructed, it can be used to validate multiple json data concurrently.\n ', 'return type': 'boolean', 'dependent classes': 'class `com.networknt.schema.JsonSchema`:\n\tdependent fields:\n\t\tcom.networknt.schema.JsonValidator com.networknt.schema.JsonSchema.requiredValidator;\n\trelated functions:\n\t\tmethod `private Map<String, JsonValidator> read(JsonNode)` returns `java.util.Map<java.lang.String, com.networknt.schema.JsonValidator>`, related with `com.networknt.schema.JsonSchema.hasRequiredValidator()`, api document: \n     * Please note that the key in {@link #validators} map is a schema path. It is\n     * used in {@link com.networknt.schema.walk.DefaultKeywordWalkListenerRunner} to derive the keyword.\n     ;\n\t\tmethod `public boolean hasRequiredValidator()` returns `boolean`, related with `com.networknt.schema.JsonSchema.hasRequiredValidator()`;\nclass `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`:\n\trelated functions:\n\t\tmethod `public static InetAddressValidator getInstance()` returns `com.networknt.org.apache.commons.validator.routines.InetAddressValidator`, related with `com.networknt.schema.JsonSchema.hasRequiredValidator()`, api document: \n     * Returns the singleton instance of this validator.\n     *\n     * @return the singleton instance of this validator\n     ;'}
```

**Note**: If the method-to-test in the @Test function is a private method, you can use **reflection** to call it.

Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 


