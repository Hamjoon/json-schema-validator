{
    "group": "JsonSchemaFactory.getObjectMapper",
    "cases": [
        {
            "input": [
                {
                    "parameter": "inputFormat",
                    "value": "JSON"
                }
            ],
            "expected": "ObjectMapper instance returned by getJsonMapper() (mocked JsonMapper)",
            "description": "When the supplied InputFormat is JSON the method should delegate to getJsonMapper() and return the JSON ObjectMapper."
        },
        {
            "input": [
                {
                    "parameter": "inputFormat",
                    "value": "YAML"
                }
            ],
            "expected": "ObjectMapper instance returned by getYamlMapper() (mocked YamlMapper)",
            "description": "When the supplied InputFormat is YAML the method should delegate to getYamlMapper() and return the YAML ObjectMapper."
        },
        {
            "input": [
                {
                    "parameter": "inputFormat",
                    "value": null
                }
            ],
            "expected": "IllegalArgumentException with message containing \"Unsupported input format null\"",
            "description": "Passing a null InputFormat should trigger the default branch and throw an IllegalArgumentException indicating the format is unsupported."
        },
        {
            "input": [
                {
                    "parameter": "inputFormat",
                    "value": "UNSUPPORTED"
                }
            ],
            "expected": "IllegalArgumentException with message containing \"Unsupported input format UNSUPPORTED\"",
            "description": "If a future enum value that is neither JSON nor YAML is supplied, the method must still fall through to the default branch and throw an IllegalArgumentException."
        }
    ]
}