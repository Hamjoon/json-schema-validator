{
    "group": "validateNullInputFormat",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"name\":\"John\"}"
                },
                {
                    "parameter": "inputFormat",
                    "value": null
                },
                {
                    "parameter": "format",
                    "value": "OutputFormat.SET"
                },
                {
                    "parameter": "executionCustomizer",
                    "value": "consumer -> {}"
                }
            ],
            "expected": "NullPointerException",
            "description": "Passing a null InputFormat should cause a NullPointerException before any deserialization or validation occurs."
        }
    ]
}