You are tasked with creating a framework for a test class in Java. Now analyze the following information carefully:

1. The method that needs to be tested and its parameters.
2. The constructor of the target class and its parameters.
3. Any dependencies or external resources used by the target class.

Based on this analysis, complete the test class template in the provided structure:

1. Initialize necessary fields and dependencies within setupBeforeAll() and setupBeforeEach().
2. Handle any required cleanup in teardownAfterEach() and teardownAfterAll().
3. Make sure not to invoke any private methods or fields of the target class and dependent class.
4. Refer to the "given" phase in the existing test class for guidance.
5. Add necessary import statements and ensure there are no syntax errors in the generated code.

@input{focal method and target class}
Here is the code of focal method, `preloadJsonSchema()`, with the focal class `PropertiesValidator_preloadJsonSchema_Test`:
```Java
package com.networknt.schema;
import java.util.*;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.DefaultPropertyWalkListenerRunner;
import com.networknt.schema.walk.WalkListenerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class PropertiesValidator extends BaseJsonValidator {
    public static final String PROPERTY = "properties";
    private final Map<String, JsonSchema> schemas = new LinkedHashMap<>();
    @Override
    public void preloadJsonSchema() {
        preloadJsonSchemas(this.schemas.values());
    }
public Map<String, JsonSchema> getSchemas();
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
}
```


@input{context information}

constructors for class `com.networknt.schema.PropertiesValidator`: 
params: com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public PropertiesValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.PROPERTIES, validationContext);
        this.validationContext = validationContext;
        for (Iterator<String> it = schemaNode.fieldNames(); it.hasNext(); ) {
            String pname = it.next();
            this.schemas.put(pname, validationContext.newSchema(schemaPath + "/" + pname, schemaNode.get(pname), parentSchema));
        }
    }

```

parameters in constructors and focal method: 
class `com.networknt.schema.JsonSchema`:
api document : 
 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 
constructor:
```java
    private JsonSchema(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode,
                       JsonSchema parent, boolean suppressSubSchemaRetrieval) {
        super(schemaPath, schemaNode, parent, null, validationContext, suppressSubSchemaRetrieval);
        this.validationContext = validationContext;
        this.metaSchema = validationContext.getMetaSchema();
        this.currentUri = combineCurrentUriWithIds(currentUri, schemaNode);
        if (uriRefersToSubschema(currentUri, schemaPath)) {
            updateThisAsSubschema(currentUri);
        }
        if (validationContext.getConfig() != null) {
            this.keywordWalkListenerRunner = new DefaultKeywordWalkListenerRunner(this.validationContext.getConfig().getKeywordWalkListenersMap());
            if (validationContext.getConfig().isOpenAPI3StyleDiscriminators()) {
                ObjectNode discriminator = (ObjectNode) schemaNode.get("discriminator");
                if (null != discriminator && null != validationContext.getCurrentDiscriminatorContext()) {
                    validationContext.getCurrentDiscriminatorContext().registerDiscriminator(schemaPath, discriminator);
                }
            }
        }
    }

```
class `com.networknt.schema.ValidationContext`:
constructor:
```java
    public ValidationContext(URIFactory uriFactory, URNFactory urnFactory, JsonMetaSchema metaSchema,
                             JsonSchemaFactory jsonSchemaFactory, SchemaValidatorsConfig config) {
        if (uriFactory == null) {
            throw new IllegalArgumentException("URIFactory must not be null");
        }
        if (metaSchema == null) {
            throw new IllegalArgumentException("JsonMetaSchema must not be null");
        }
        if (jsonSchemaFactory == null) {
            throw new IllegalArgumentException("JsonSchemaFactory must not be null");
        }
        this.uriFactory = uriFactory;
        this.urnFactory = urnFactory;
        this.metaSchema = metaSchema;
        this.jsonSchemaFactory = jsonSchemaFactory;
        this.config = config;
    }

```

existing test class: 
```java
package com.networknt.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Created by josejulio on 25/04/22.
 */
public class PropertiesValidatorTest extends BaseJsonSchemaValidatorTest {

    @Test
    public void testDoesNotThrowWhenApplyingDefaultPropertiesToNonObjects() throws Exception {
        Assertions.assertDoesNotThrow(() -> {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);

            SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig();
            schemaValidatorsConfig.setApplyDefaultsStrategy(new ApplyDefaultsStrategy(
                    true,
                    true,
                    true
            ));

            JsonSchema schema = factory.getSchema("{\"type\":\"object\",\"properties\":{\"foo\":{\"type\":\"object\", \"properties\": {} },\"i-have-default\":{\"type\":\"string\",\"default\":\"foo\"}}}", schemaValidatorsConfig);
            JsonNode node = getJsonNodeFromStringContent("{\"foo\": \"bar\"}");
            ValidationResult result = schema.walk(node, true);
            Assertions.assertEquals(result.getValidationMessages().size(), 1);
        });
    }
}

```



@input{test class template}
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// Add other imports if necessary

@ExtendWith(MockitoExtension.class)
class PropertiesValidator_preloadJsonSchema_Test {
    @BeforeAll
    static void setupBeforeAll() {
    }

    @BeforeEach
    void setupBeforeEach() {
    }

    @AfterEach
    void teardownAfterEach() {
    }

    @AfterAll
    static void teardownAfterAll() {
    }
}
```

@output{initial test class}: now consider the invoke examples output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.