{
    "group": "JsonSchema.validate(String, InputFormat, Consumer<ExecutionContext>)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "   "
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "ctx -> {}"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "Input string contains only whitespace. The method should treat this as empty input and reject it with an IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"name\":\"John\"}"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": null
                }
            ],
            "expected": "NullPointerException",
            "description": "A null Consumer is passed as the executionCustomizer. The method should throw a NullPointerException when it attempts to invoke the consumer."
        }
    ]
}