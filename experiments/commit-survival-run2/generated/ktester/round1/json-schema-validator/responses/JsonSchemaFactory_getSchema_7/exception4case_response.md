{
    "test_cases": [
        {
            "group": "Invalid input – jsonNode is a string primitive",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "\"just a string\""
                        },
                        {
                            "parameter": "config",
                            "value": "new SchemaValidatorsConfig()"
                        }
                    ],
                    "expected": "IllegalArgumentException indicating that the root schema node must be an object or boolean",
                    "description": "A JSON string literal is not a valid JSON Schema root; the factory should reject it with an IllegalArgumentException."
                }
            ]
        },
        {
            "group": "Invalid input – jsonNode is a JSON null literal",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "jsonNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "null"
                        }
                    ],
                    "expected": "IllegalArgumentException indicating that the schema JsonNode cannot be a JSON null value",
                    "description": "A JSON null node is not a valid schema root; the method should throw an IllegalArgumentException rather than creating a JsonSchema."
                }
            ]
        }
    ]
}