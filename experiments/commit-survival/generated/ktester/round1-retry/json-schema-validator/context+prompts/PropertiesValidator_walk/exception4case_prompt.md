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
Here is the code of focal method, `walk(ExecutionContext, JsonNode, JsonNode, String, boolean)`, with the focal class `PropertiesValidator`:
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
    public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema) {
        HashSet<ValidationMessage> validationMessages = new LinkedHashSet<>();
        if ((this.applyDefaultsStrategy.shouldApplyPropertyDefaults() && (null != node)) && (node.getNodeType() == JsonNodeType.OBJECT)) {
            applyPropertyDefaults(((ObjectNode) (node)));
        }
        if (shouldValidateSchema) {
            validationMessages.addAll(validate(executionContext, node, rootNode, at));
        } else {
            WalkListenerRunner propertyWalkListenerRunner = new DefaultPropertyWalkListenerRunner(this.validationContext.getConfig().getPropertyWalkListeners());
            for (Map.Entry<String, JsonSchema> entry : this.schemas.entrySet()) {
                walkSchema(executionContext, entry, node, rootNode, at, shouldValidateSchema, validationMessages, propertyWalkListenerRunner);
            }
        }
        return validationMessages;
    }
private void applyPropertyDefaults(ObjectNode node);
    public Map<String, JsonSchema> getSchemas();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at);
    private void walkSchema(ExecutionContext executionContext, Map.Entry<String, JsonSchema> entry, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema, Set<ValidationMessage> validationMessages, WalkListenerRunner propertyWalkListenerRunner);
}
```
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: