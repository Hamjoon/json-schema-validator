{
    "group": "TypeValidator.getSchemaType() initialized via schema node",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"string\"}"
                }
            ],
            "expected": "JsonType.STRING",
            "description": "When a TypeValidator is constructed with a schema node that defines \"type\":\"string\", the internal schemaType should be set to JsonType.STRING and getSchemaType() must return JsonType.STRING."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"number\"}"
                }
            ],
            "expected": "JsonType.NUMBER",
            "description": "When the schema node defines \"type\":\"number\", the validator should initialise schemaType to JsonType.NUMBER; getSchemaType() should return JsonType.NUMBER."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"integer\"}"
                }
            ],
            "expected": "JsonType.INTEGER",
            "description": "When the schema node defines \"type\":\"integer\", getSchemaType() must return JsonType.INTEGER."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"boolean\"}"
                }
            ],
            "expected": "JsonType.BOOLEAN",
            "description": "When the schema node defines \"type\":\"boolean\", getSchemaType() must return JsonType.BOOLEAN."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"object\"}"
                }
            ],
            "expected": "JsonType.OBJECT",
            "description": "When the schema node defines \"type\":\"object\", getSchemaType() must return JsonType.OBJECT."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"array\"}"
                }
            ],
            "expected": "JsonType.ARRAY",
            "description": "When the schema node defines \"type\":\"array\", getSchemaType() must return JsonType.ARRAY."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"null\"}"
                }
            ],
            "expected": "JsonType.NULL",
            "description": "When the schema node defines \"type\":\"null\", getSchemaType() must return JsonType.NULL."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{}"
                }
            ],
            "expected": "null",
            "description": "When the schema node does not contain a \"type\" property, the validator cannot infer a schema type; getSchemaType() should return null."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":\"unknown\"}"
                }
            ],
            "expected": "null",
            "description": "When the schema node contains an unrecognised type string, the validator should leave schemaType unset; getSchemaType() must return null."
        },
        {
            "input": [
                {
                    "parameter": "schemaJson",
                    "value": "{\"type\":[\"string\",\"null\"]}"
                }
            ],
            "expected": "null",
            "description": "When the schema node defines a type array (multiple possible types), the validator does not map it to a single JsonType; getSchemaType() should return null."
        }
    ]
}