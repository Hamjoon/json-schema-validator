{
    "group": "Invalid inputs - mismatched format (YAML supplied as JSON)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "type: object\nproperties:\n  name:\n    type: string"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                }
            ],
            "expected": "JsonProcessingException (or a subclass such as JsonParseException) thrown by the JSON deserializer because the payload is valid YAML but not valid JSON.",
            "description": "The method is instructed to treat the payload as JSON, but the string is written in YAML syntax. Deserialization should fail and the exception must be propagated."
        }
    ]
}