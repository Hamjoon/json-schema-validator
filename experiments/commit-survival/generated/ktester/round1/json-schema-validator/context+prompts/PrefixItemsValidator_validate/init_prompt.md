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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `PrefixItemsValidator_validate_Test`:
```Java
package com.networknt.schema;
import java.util.Set;
import com.networknt.schema.walk.DefaultItemWalkListenerRunner;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.networknt.schema.walk.WalkListenerRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
public class PrefixItemsValidator extends BaseJsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(PrefixItemsValidator.class);
    private final List<JsonSchema> tupleSchema;
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        // ignores non-arrays
        if (node.isArray()) {
            Collection<String> evaluatedItems = executionContext.getCollectorContext().getEvaluatedItems();
            for (int i = 0; i < Math.min(node.size(), this.tupleSchema.size()); ++i) {
                String path = atPath(at, i);
                Set<ValidationMessage> results = this.tupleSchema.get(i).validate(executionContext, node.get(i), rootNode, path);
                if (results.isEmpty()) {
                    evaluatedItems.add(path);
                } else {
                    errors.addAll(results);
                }
            }
        }
        return Collections.unmodifiableSet(errors);
    }
public List<JsonSchema> getTupleSchema();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
}
```


@input{context information}

constructors for class `com.networknt.schema.PrefixItemsValidator`: 
params: com.networknt.schema.JsonSchema parentSchema 
com.networknt.schema.ValidationContext validationContext 
body:
```java
    public PrefixItemsValidator(String schemaPath, JsonNode schemaNode, JsonSchema parentSchema, ValidationContext validationContext) {
        super(schemaPath, schemaNode, parentSchema, ValidatorTypeCode.PREFIX_ITEMS, validationContext);

        this.tupleSchema = new ArrayList<>();

        if (schemaNode instanceof ArrayNode && 0 < schemaNode.size()) {
            for (JsonNode s : schemaNode) {
                this.tupleSchema.add(validationContext.newSchema(schemaPath, s, parentSchema));
            }
        } else {
            throw new IllegalArgumentException("The value of 'prefixItems' MUST be a non-empty array of valid JSON Schemas.");
        }

        this.arrayItemWalkListenerRunner = new DefaultItemWalkListenerRunner(validationContext.getConfig().getArrayItemWalkListeners());

        this.validationContext = validationContext;

        parseErrorCode(getValidatorType().getErrorCodeKey());
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
class `com.networknt.schema.ExecutionContext`:
api document : 
 * Stores the execution context for the validation run.
 
constructor:
```java
    public ExecutionContext() {
        this(new CollectorContext());
    }

    public ExecutionContext(CollectorContext collectorContext) {
        this.collectorContext = collectorContext;
    }

```

existing test class: 
```java
package com.networknt.schema;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * This class handles exception case for {@link PrefixItemsValidator}
 */
public class PrefixItemsValidatorTest extends AbstractJsonSchemaTestSuite {

    /**
     * this method create test cases from JSON and run those test cases with assertion
     */
    @Test
    void testEmptyPrefixItemsException() {
        Stream<DynamicNode> dynamicNodeStream = createTests(SpecVersion.VersionFlag.V7, "src/test/suite/tests/prefixItemsException");
        dynamicNodeStream.forEach(
                dynamicNode -> {
                    ((DynamicContainer) dynamicNode).getChildren().forEach(dynamicNode1 -> {
                        if (dynamicNode1 instanceof DynamicTest) {
                            assertThrows(JsonSchemaException.class, () -> {
                                ((DynamicTest) dynamicNode1).getExecutable().execute();
                            });
                        }
                    });
                }
        );
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
class PrefixItemsValidator_validate_Test {
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