{
    "group": "Unsupported InputFormat enum constant (e.g., InputFormat.XML)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "executionContext",
                    "value": "JsonSchema.createExecutionContext()"
                },
                {
                    "parameter": "input",
                    "value": "\"<root><name>John</name></root>\""
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.XML"
                },
                {
                    "parameter": "validate",
                    "value": "true"
                }
            ],
            "expected": "IllegalArgumentException (or a subclass) because the supplied InputFormat is not supported by JsonSchema.deserialize",
            "description": "Passing a valid InputFormat enum constant that the library does not handle (e.g., XML) should cause deserialize to reject the request and the walk method to throw an IllegalArgumentException."
        }
    ]
}