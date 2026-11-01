{
    "group": "walkWhenShouldValidateFalseWithChildMessages",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "mockExecutionContext"
                },
                {
                    "parameter": "node",
                    "value": "mockNode"
                },
                {
                    "parameter": "rootNode",
                    "value": "mockRootNode"
                },
                {
                    "parameter": "instanceLocation",
                    "value": "mockInstanceLocation"
                },
                {
                    "parameter": "shouldValidateSchema",
                    "value": false
                }
            ],
            "expected": "Empty Set; each child schema's walk method is invoked once with the same parameters and may return ValidationMessage sets, but those messages are ignored by OneOfValidator.walk",
            "description": "When shouldValidateSchema is false and the validator holds several child schemas that each produce ValidationMessage objects, OneOfValidator.walk must still return an empty Set because it never aggregates child messages."
        }
    ]
}