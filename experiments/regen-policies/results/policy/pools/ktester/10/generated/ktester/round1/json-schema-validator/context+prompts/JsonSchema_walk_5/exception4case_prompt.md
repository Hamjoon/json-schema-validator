You are a professional software tester. Write test cases for a focal method in a target class.

**Instructions:**
1. Analyze method behavior and identify ALL possible exception scenarios
2. Design test cases that trigger each identified exception
3. Follow existing test case structure, Avoid duplication and create new test cases for uncovered exception scenarios

Test Case Format:
1. Group test cases by test function name
2. Each test case MUST include:
   - Input parameters with values (use empty list `[]` if none)
   - Expected exception or observable behavior
   - Clear description of test scenario
3. Return empty array if no exception scenarios found

Output Schema (STRICT JSON format):
```json
[
    {
        "group": "test name",
        "cases": [
            {
                "input": [
                    {
                        "parameter": "param name",
                        "value": "param value"
                    }
                ],
                "expected": "expected exception or behavior",
                "description": "test scenario description"
            }
        ]
    },
    {...}
]
```

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
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: