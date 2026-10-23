{
    "group": "Malformed $schema URI",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper().readTree('{\"$schema\":\"http://[invalid]/schema#\",\"type\":\"string\"}')"
                }
            ],
            "expected": "JsonSchemaException (or RuntimeException) caused by URISyntaxException because the $schema value is not a valid URI",
            "description": "The $schema keyword contains a syntactically invalid URI; the factory should attempt to parse it and fail, propagating an exception."
        }
    ]
}