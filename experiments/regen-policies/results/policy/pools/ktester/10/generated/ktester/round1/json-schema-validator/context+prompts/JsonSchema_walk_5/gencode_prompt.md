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
Here is the code of focal method, `walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`, with the focal class `JsonSchema`:
```Java
package com.networknt.schema;
import com.networknt.schema.serialization.JsonMapperFactory;
import com.networknt.schema.serialization.YamlMapperFactory;
import java.util.*;
import java.io.UnsupportedEncodingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.walk.WalkListenerRunner;
import com.networknt.schema.walk.DefaultKeywordWalkListenerRunner;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.CollectorContext.Scope;
import java.util.function.Consumer;
import java.net.URLDecoder;
import com.fasterxml.jackson.core.JsonProcessingException;
public class JsonSchema extends BaseJsonValidator {
    WalkListenerRunner keywordWalkListenerRunner = null;
    @Override
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean shouldValidateSchema) {
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        CollectorContext collectorContext = executionContext.getCollectorContext();
        // Walk through all the JSONWalker's.
        for (JsonValidator v : getValidators()) {
            JsonNodePath evaluationPathWithKeyword = v.getEvaluationPath();
            try {
                // Call all the pre-walk listeners. If at least one of the pre walk listeners
                // returns SKIP, then skip the walk.
                if (this.keywordWalkListenerRunner.runPreWalkListeners(executionContext, evaluationPathWithKeyword.getName(-1), node, rootNode, instanceLocation, v.getEvaluationPath(), v.getSchemaLocation(), this.schemaNode, this.parentSchema, this.validationContext, this.validationContext.getJsonSchemaFactory())) {
                    Set<ValidationMessage> results = null;
                    Scope parentScope = collectorContext.enterDynamicScope(this);
                    try {
                        results = v.walk(executionContext, node, rootNode, instanceLocation, shouldValidateSchema);
                    } finally {
                        Scope scope = collectorContext.exitDynamicScope();
                        if ((results == null) || results.isEmpty()) {
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
            } finally {
                // Call all the post-walk listeners.
                this.keywordWalkListenerRunner.runPostWalkListeners(executionContext, evaluationPathWithKeyword.getName(-1), node, rootNode, instanceLocation, v.getEvaluationPath(), v.getSchemaLocation(), this.schemaNode, this.parentSchema, this.validationContext, this.validationContext.getJsonSchemaFactory(), errors);
            }
        }
        return errors;
    }
public ExecutionContext createExecutionContext();
    public JsonSchema findAncestor();
    public JsonSchema findLexicalRoot();
    public JsonSchema findSchemaResourceRoot();
    static JsonSchema from(ValidationContext validationContext, SchemaLocation schemaLocation, JsonNodePath evaluationPath, JsonNode schemaNode, JsonSchema parent, boolean suppressSubSchemaRetrieval);
    public JsonSchema fromRef(JsonSchema refEvaluationParentSchema, JsonNodePath refEvaluationPath);
    public String getId();
    protected JsonNode getNode(Object propertyOrIndex);
    public JsonSchema getRefSchema(JsonNodePath fragment);
    public JsonNode getRefSchemaNode(String ref);
    public JsonValidator getRequiredValidator();
    public JsonSchema getSubSchema(JsonNodePath fragment);
    public TypeValidator getTypeValidator();
    ValidationContext getValidationContext();
    public List<JsonValidator> getValidators();
    public boolean hasRequiredValidator();
    public boolean hasTypeValidator();
    public void initializeValidators();
    public boolean isRecursiveAnchor();
    public boolean isSchemaResourceRoot();
    @Override
public String toString();
    public Set<ValidationMessage> validate(JsonNode rootNode);
    public Set<ValidationMessage> validate(JsonNode rootNode, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(JsonNode rootNode, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(JsonNode rootNode, Consumer<ExecutionContext> executionCustomizer);
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode jsonNode, JsonNode rootNode, JsonNodePath instanceLocation);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, ExecutionContextCustomizer executionCustomizer);
    public <T> T validate(String input, InputFormat inputFormat, OutputFormat<T> format, Consumer<ExecutionContext> executionCustomizer);
    public Set<ValidationMessage> validate(String input, InputFormat inputFormat, Consumer<ExecutionContext> executionCustomizer);
    public ValidationResult validateAndCollect(JsonNode node);
    public ValidationResult validateAndCollect(ExecutionContext executionContext, JsonNode node);
    public ValidationResult walk(JsonNode node, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, JsonNode node, boolean validate);
    public ValidationResult walk(ExecutionContext executionContext, String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walk(String input, InputFormat inputFormat, boolean validate);
    public ValidationResult walkAtNode(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, JsonNodePath instanceLocation, boolean validate);
    public JsonSchema withConfig(SchemaValidatorsConfig config);
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
	
class `com.networknt.schema.JsonNodePath`:
	
class `com.networknt.schema.CollectorContext`:
	
class `com.networknt.schema.JsonSchema`:
	dependent fields:
		com.networknt.schema.walk.WalkListenerRunner com.networknt.schema.JsonSchema.keywordWalkListenerRunner;
	related functions:
		method `private void initializeConfig()` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
class `com.networknt.schema.BaseJsonValidator`:
	dependent fields:
		com.networknt.schema.ValidationContext com.networknt.schema.BaseJsonValidator.validationContext;
		com.fasterxml.jackson.databind.JsonNode com.networknt.schema.BaseJsonValidator.schemaNode;
	
class `com.networknt.schema.ValidationMessageHandler`:
	dependent fields:
		com.networknt.schema.JsonSchema com.networknt.schema.ValidationMessageHandler.parentSchema;
	
class `com.networknt.schema.AllOfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
class `com.networknt.schema.PropertiesValidator`:
	related functions:
		method `private void walkSchema(ExecutionContext, Map.Entry<String, JsonSchema>, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>, WalkListenerRunner)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
class `com.networknt.schema.UnevaluatedItemsValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
class `com.networknt.schema.IfValidator`:
	related functions:
		method `public Set<ValidationMessage> validate(ExecutionContext, JsonNode, JsonNode, JsonNodePath)` returns `java.util.Set<com.networknt.schema.ValidationMessage>`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;
class `com.networknt.schema.ItemsValidator`:
	related functions:
		method `private void walkSchema(ExecutionContext, JsonSchema, JsonNode, JsonNode, JsonNodePath, boolean, Set<ValidationMessage>)` returns `void`, related with `com.networknt.schema.JsonSchema.walk(ExecutionContext, JsonNode, JsonNode, JsonNodePath, boolean)`;



@output{test class}: output the code wrapped in a Markdown code block ```java at the beginning and ``` at the end.