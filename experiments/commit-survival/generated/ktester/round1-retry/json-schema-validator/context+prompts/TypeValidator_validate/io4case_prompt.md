As a professional Java software testing expert, your task is to generate test cases for a focal method.

Guidelines:
1. Analyze the method's functionality and identify possible input combinations including normal cases and boundary conditions.
2. Review the provided existing test cases. Avoid duplication and create new test cases for untested input combinations.

Context Provided:
1. The focal method that needs testing.
2. Relevant code and metadata for the class containing the method, including fields and other methods.
3. The existing test cases (in JSON format) to help avoid redundancy.

Output Instructions:
- Organize the test cases by the name of the test function (group).
- Each test case must include:
    - input: A list of parameter names and values. Use an empty list `[]` if no input is required.
    - expected: The return value or expected observable behavior.
    - description: A clear and concise explanation of the scenario and the intent of the test case.

Maintain the following strict JSON format:
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