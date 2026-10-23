You are a professional Java software testing expert. Your task is to generate high-quality unit test code for a focal method, using **JUnit5** and **Mockito** libraries. 

You'll be provided with:
1. The focal method to test.
2. The target class's structure (fields and methods).
3. A pre-defined test class framework with `@Before`/`@After` methods.
4. Test cases in JSON format.
5. Any relevant context.

**Requirements:**

Code Quality:
- Include clear comments explaining each test scenario.
- Ensure code is clean, understandable, and maintainable.
- Do not directly access private methods or fields. **Use reflection if the focal method is private.**
- Include all necessary imports; code must be free of syntax errors.

**Test Structure:**
- Follow the given test class framework; do not modify `@Before`/`@After` methods.
- Create one test function per test case group, ensuring all cases are covered.
- Use parameterized tests where appropriate.
- Reinitialize variables inside test functions if `@Setup` values don't fit.
- Properly mock and set up dependencies.

@input{focal method and target class}
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `JsonSchema`:
```Java
package com.networknt.schema;
import java.util.*;
import java.text.MessageFormat;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.ValidationContext.DiscriminatorContext;
import java.net.URISyntaxException;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import com.networknt.schema.walk.JsonSchemaWalker;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.CollectorContext.Scope;
import com.networknt.schema.utils.StringUtils;
import java.net.URLDecoder;
public class JsonSchema extends BaseJsonValidator {
    WalkListenerRunner keywordWalkListenerRunner = null;
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, String at) {
        SchemaValidatorsConfig config = this.validationContext.getConfig();
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        // Get the collector context.
        CollectorContext collectorContext = executionContext.getCollectorContext();
        // Set the walkEnabled and isValidationEnabled flag in internal validator state.
        setValidatorState(executionContext, false, true);
        for (JsonValidator v : getValidators().values()) {
            Set<ValidationMessage> results = Collections.emptySet();
            Scope parentScope = collectorContext.enterDynamicScope(this);
            try {
                results = v.validate(executionContext, jsonNode, rootNode, at);
            } finally {
                Scope scope = collectorContext.exitDynamicScope();
                if (results.isEmpty()) {
                    parentScope.mergeWith(scope);
                } else {
                    errors.addAll(results);
                    if ((((v instanceof PrefixItemsValidator) || (v instanceof ItemsValidator)) || (v instanceof ItemsValidator202012)) || (v instanceof ContainsValidator)) {
                        collectorContext.getEvaluatedItems().addAll(scope.getEvaluatedItems());
                    }
                    if (((v instanceof PropertiesValidator) || (v instanceof AdditionalPropertiesValidator)) || (v instanceof PatternPropertiesValidator)) {
                        collectorContext.getEvaluatedProperties().addAll(scope.getEvaluatedProperties());
                    }
                }
            }
        }
        if (config.isOpenAPI3StyleDiscriminators()) {
            ObjectNode discriminator = ((ObjectNode) (this.schemaNode.get("discriminator")));
            if (null != discriminator) {
                final DiscriminatorContext discriminatorContext = this.validationContext.getCurrentDiscriminatorContext();
                if (null != discriminatorContext) {
                    final ObjectNode discriminatorToUse;
                    final ObjectNode discriminatorFromContext = discriminatorContext.getDiscriminatorForPath(this.schemaPath);
                    if (null == discriminatorFromContext) {
                        // register the current discriminator. This can only happen when the current context discriminator
                        // was not registered via allOf. In that case we have a $ref to the schema with discriminator that gets
                        // used for validation before allOf validation has kicked in
                        discriminatorContext.registerDiscriminator(this.schemaPath, discriminator);
                        discriminatorToUse = discriminator;
                    } else {
                        discriminatorToUse = discriminatorFromContext;
                    }
                    final String discriminatorPropertyName = discriminatorToUse.get("propertyName").asText();
                    final JsonNode discriminatorNode = jsonNode.get(discriminatorPropertyName);
                    final String discriminatorPropertyValue = (discriminatorNode == null) ? null : discriminatorNode.asText();
                    checkDiscriminatorMatch(discriminatorContext, discriminatorToUse, discriminatorPropertyValue, this);
                }
            }
        }
        return errors;
    }
public JsonSchema createChildSchema(String schemaPath, JsonNode schemaNode);
    public ExecutionContext createExecutionContext();
    public JsonSchema findAncestor();
    JsonSchema findLexicalRoot();
    static JsonSchema from(ValidationContext validationContext, String schemaPath, URI currentUri, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval);
    public URI getCurrentUri();
    public JsonNode getRefSchemaNode(String ref);
    public JsonValidator getRequiredValidator();
    public TypeValidator getTypeValidator();
    ValidationContext getValidationContext();
    private Map<String, JsonValidator> getValidators();
    public boolean hasRequiredValidator();
    public boolean hasTypeValidator();
    public void initializeValidators();
    public boolean isDynamicAnchor();
    private static void setValidatorState(ExecutionContext executionContext, boolean isWalkEnabled, boolean shouldValidateSchema);
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public ValidationResult validateAndCollect(JsonNode node);
    public ValidationResult validateAndCollect(ExecutionContext executionContext, JsonNode node);
    public ValidationResult walk(JsonNode node, boolean shouldValidateSchema);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean shouldValidateSchema);
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
    public ValidationResult walkAtNode(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
}
```

@input{initial test class framework}
```Java
<initial_class>
```

@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```


@input{context information}

api document of class com.networknt.schema.JsonSchema: 

 * This is the core of json constraint implementation. It parses json constraint
 * file and generates JsonValidators. The class is thread safe, once it is
 * constructed, it can be used to validate multiple json data concurrently.
 

dependent classes: 
class `com.networknt.schema.ExecutionContext`:
	
class `com.networknt.schema.CollectorContext`:
	
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;
		com.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;
	related functions:
		method `protected static void registerAndMergeDiscriminator(DiscriminatorContext, ObjectNode, JsonSchema, String)` returns `void`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: 
     * Rolls up all nested and compatible discriminators to the root discriminator of the type. Detects attempts to redefine
     * the <code>propertyName</code> or mappings.
     *
     * @param currentDiscriminatorContext the currently active {@link DiscriminatorContext}
     * @param discriminator               the discriminator to use for the check
     * @param schema                      the value of the <code>discriminator/propertyName</code> field
     * @param at                          the logging prefix
     ;
class `com.networknt.schema.ValidationMessageHandler`:
	dependent fields:
		java.lang.String com.networknt.schema.ValidationMessageHandler.schemaPath;
	
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: 
     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.
     ;
		method `private URI combineCurrentUriWithIds(URI, JsonNode)` returns `java.net.URI`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `public Set<ValidationMessage> walk(ExecutionContext, JsonNode, JsonNode, String, boolean)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `private String getCustomMessage(JsonNode, String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;
		method `private ValidationResult validateAndCollect(ExecutionContext, JsonNode, JsonNode, String)` returns `com.networknt.schema.ValidationResult`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`, api document: 
     * This method both validates and collects the data in a CollectorContext.
     * Unlike others this methods cleans and removes everything from collector
     * context before returning.
     * @param executionContext ExecutionContext
     * @param jsonNode JsonNode
     * @param rootNode JsonNode
     * @param at       String path
     *
     * @return ValidationResult
     ;
class `com.networknt.schema.TypeValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, String)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.validate(ExecutionContext, JsonNode, JsonNode, String)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.