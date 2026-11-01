{
    "group": "validate_String_InputFormat_exceptions",
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
                }
            ],
            "expected": "throws NullPointerException",
            "description": "The InputFormat argument is null. The method should fail fast with a NullPointerException before attempting deserialization."
        },
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "%ZZ%invalid%encoding"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.JSON"
                }
            ],
            "expected": "throws UnsupportedEncodingException",
            "description": "The input string contains malformed percent‑encoding. If the implementation URL‑decodes the string during deserialization, it should raise an UnsupportedEncodingException."
        },
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"name\":\"John\"}"
                },
                {
                    "parameter": "inputFormat",
                    "value": "InputFormat.UNKNOWN"
                }
            ],
            "expected": "throws IllegalArgumentException",
            "description": "An InputFormat value that is not supported by the deserializer (e.g., a hypothetical UNKNOWN constant) should cause the method to reject the request with an IllegalArgumentException."
        }
    ]
}