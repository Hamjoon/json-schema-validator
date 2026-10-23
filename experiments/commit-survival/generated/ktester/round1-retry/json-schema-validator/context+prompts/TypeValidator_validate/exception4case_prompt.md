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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `TypeValidator`:
```Java
package com.networknt.schema;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.utils.JsonNodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class TypeValidator extends BaseJsonValidator {
    private static final Logger logger = LoggerFactory.getLogger(TypeValidator.class);
    private JsonType schemaType;
    private UnionTypeValidator unionTypeValidator;
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);
        if (this.schemaType == JsonType.UNION) {
            return this.unionTypeValidator.validate(executionContext, node, rootNode, at);
        }
        if (!equalsToSchemaType(node)) {
            JsonType nodeType = TypeFactory.getValueNodeType(node, this.validationContext.getConfig());
            return Collections.singleton(buildValidationMessage(at, nodeType.toString(), this.schemaType.toString()));
        }
        // TODO: Is this really necessary?
        // Hack to catch evaluated properties if additionalProperties is given as "additionalProperties":{"type":"string"}
        // Hack to catch patternProperties like "^foo":"value"
        if (this.schemaPath.endsWith("/type")) {
            if (rootNode.isArray()) {
                executionContext.getCollectorContext().getEvaluatedItems().add(at);
            } else if (rootNode.isObject()) {
                executionContext.getCollectorContext().getEvaluatedProperties().add(at);
            }
        }
        return Collections.emptySet();
    }
public boolean equalsToSchemaType(JsonNode node);
    public JsonType getSchemaType();
}
```
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: