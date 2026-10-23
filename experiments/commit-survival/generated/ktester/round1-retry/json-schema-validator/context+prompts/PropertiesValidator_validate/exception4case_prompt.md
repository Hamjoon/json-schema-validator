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
Here is the code of focal method, `validate(ExecutionContext, JsonNode, JsonNode, String)`, with the focal class `PropertiesValidator`:
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
    private static final Logger logger = LoggerFactory.getLogger(PropertiesValidator.class);
    private final Map<String, JsonSchema> schemas = new LinkedHashMap<>();
    @Override
    public Set<ValidationMessage> validate(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at) {
        debug(logger, node, rootNode, at);
        CollectorContext collectorContext = executionContext.getCollectorContext();
        WalkListenerRunner propertyWalkListenerRunner = new DefaultPropertyWalkListenerRunner(this.validationContext.getConfig().getPropertyWalkListeners());
        Set<ValidationMessage> errors = new LinkedHashSet<>();
        // get the Validator state object storing validation data
        ValidatorState state = ((ValidatorState) (collectorContext.get(ValidatorState.VALIDATOR_STATE_KEY)));
        for (Map.Entry<String, JsonSchema> entry : this.schemas.entrySet()) {
            JsonSchema propertySchema = entry.getValue();
            JsonNode propertyNode = node.get(entry.getKey());
            if (propertyNode != null) {
                collectorContext.getEvaluatedProperties().add(atPath(at, entry.getKey()));// TODO: This should happen after validation
    
                // check whether this is a complex validator. save the state
                boolean isComplex = state.isComplexValidator();
                // if this is a complex validator, the node has matched, and all it's child elements, if available, are to be validated
                if (state.isComplexValidator()) {
                    state.setMatchedNode(true);
                }
                // reset the complex validator for child element validation, and reset it after the return from the recursive call
                state.setComplexValidator(false);
                if (!state.isWalkEnabled()) {
                    // validate the child element(s)
                    errors.addAll(propertySchema.validate(executionContext, propertyNode, rootNode, atPath(at, entry.getKey())));
                } else {
                    // check if walker is enabled. If it is enabled it is upto the walker implementation to decide about the validation.
                    walkSchema(executionContext, entry, node, rootNode, at, state.isValidationEnabled(), errors, propertyWalkListenerRunner);
                }
                // reset the complex flag to the original value before the recursive call
                state.setComplexValidator(isComplex);
                // if this was a complex validator, the node has matched and has been validated
                if (state.isComplexValidator()) {
                    state.setMatchedNode(true);
                }
            } else // check whether the node which has not matched was mandatory or not
            if (getParentSchema().hasRequiredValidator()) {
                Set<ValidationMessage> requiredErrors = getParentSchema().getRequiredValidator().validate(executionContext, node, rootNode, at);
                if (!requiredErrors.isEmpty()) {
                    // the node was mandatory, decide which behavior to employ when validator has not matched
                    if (state.isComplexValidator()) {
                        // this was a complex validator (ex oneOf) and the node has not been matched
                        state.setMatchedNode(false);
                        return Collections.emptySet();
                    }
                    errors.addAll(requiredErrors);
                }
            }
        }
        return Collections.unmodifiableSet(errors);
    }
public Map<String, JsonSchema> getSchemas();
    @Override
public void preloadJsonSchema();
    @Override
public Set<ValidationMessage> walk(ExecutionContext executionContext, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema);
    private void walkSchema(ExecutionContext executionContext, Map.Entry<String, JsonSchema> entry, JsonNode node, JsonNode rootNode, String at, boolean shouldValidateSchema, Set<ValidationMessage> validationMessages, WalkListenerRunner propertyWalkListenerRunner);
}
```
@input{existing test case}
here's the existing test cases:
```json
<cases_json>
```

@output{test cases}: output the test cases in the JSON format: