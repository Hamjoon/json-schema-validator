{
    "group": "$ref resolution handling",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNode representing schema with $ref to an external definition (e.g., {\"$ref\":\"#/definitions/person\"}, definitions.person = {\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}) validated against instance {\"name\":\"Bob\"}"
                },
                {
                    "parameter": "format",
                    "value": "CollectingOutputFormat"
                }
            ],
            "expected": "Empty List (the $ref is resolved correctly and the instance conforms)",
            "description": "Validate that a schema containing a $ref to an internal definition is resolved and validated without errors."
        },
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "new ExecutionContext()"
                },
                {
                    "parameter": "node",
                    "value": "JsonNode representing schema with $ref to external file (e.g., {\"$ref\":\"http://example.com/schemas/address.json\"}) where the external schema requires a string property \"street\"; instance is {\"street\":123}"
                },
                {
                    "parameter": "format",
                    "value": "CollectingOutputFormat"
                }
            ],
            "expected": "List containing a ValidationMessage indicating type mismatch for property 'street'",
            "description": "Ensure that external $ref resolution works (via default URI resolver) and that validation errors in the referenced schema are reported."
        }
    ]
}