{
    "group": "detect - root node not an object",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "[]"
                }
            ],
            "expected": "JsonSchemaException: '$schema' tag is not present",
            "description": "When the root JSON node is an empty array, the detector cannot find a `$schema` field and throws the standard `JsonSchemaException`."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "123"
                }
            ],
            "expected": "JsonSchemaException: '$schema' tag is not present",
            "description": "When the root JSON node is a numeric literal, the detector treats it as missing `$schema` and throws the standard `JsonSchemaException`."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "true"
                }
            ],
            "expected": "JsonSchemaException: '$schema' tag is not present",
            "description": "When the root JSON node is a boolean literal, the detector cannot locate a `$schema` field and throws the standard `JsonSchemaException`."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "\"just a string\""
                }
            ],
            "expected": "JsonSchemaException: '$schema' tag is not present",
            "description": "When the root JSON node is a plain string (not an object), the detector does not find a `$schema` field and throws the standard `JsonSchemaException`."
        },
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "{ \"$schema\": null }"
                }
            ],
            "expected": "JsonSchemaException: '$schema' tag is not present",
            "description": "When the `$schema` field is present but its value is JSON null, the detector cannot map it to a known version and throws the standard `JsonSchemaException`."
        }
    ]
}