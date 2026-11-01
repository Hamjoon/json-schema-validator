{
    "group": "walk_nullInputFormat",
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
                    "parameter": "validate",
                    "value": true
                }
            ],
            "expected": "NullPointerException is thrown",
            "description": "The InputFormat argument is null. The walk method should detect the null and throw a NullPointerException before attempting deserialization."
        }
    ]
}