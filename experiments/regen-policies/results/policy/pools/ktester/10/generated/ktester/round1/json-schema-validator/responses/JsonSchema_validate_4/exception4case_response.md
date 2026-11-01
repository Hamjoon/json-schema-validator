{
    "group": "Invalid schema – malformed $ref",
    "cases": [
        {
            "input": [
                {
                    "parameter": "rootNode",
                    "value": "{ \"$ref\": \"http://[invalid-url]\" }"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "null"
                }
            ],
            "expected": "IllegalArgumentException (or JsonSchemaException) is thrown due to malformed URI in $ref",
            "description": "The schema contains a $ref with an invalid URI. When validate(JsonNode, ExecutionContextCustomizer) attempts to resolve the reference it should detect the malformed reference and throw an appropriate exception rather than silently ignoring it."
        }
    ]
}