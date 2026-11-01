{
    "test_cases": [
        {
            "group": "Schema node null leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "null"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig.builder().build()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When the supplied JsonNode is null, getMetaSchemaOrDefault cannot resolve a meta‑schema and the method dereferences the null reference, resulting in a NullPointerException."
                }
            ]
        },
        {
            "group": "Schema node not an object leads to IllegalArgumentException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "ArrayNode (e.g., JsonNodeFactory.instance.arrayNode().add(1))"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig.builder().build()"
                        }
                    ],
                    "expected": "IllegalArgumentException",
                    "description": "If the provided JsonNode is not an object (e.g., an array), getMetaSchemaOrDefault is expected to reject it and throw IllegalArgumentException because a schema must be a JSON object."
                }
            ]
        },
        {
            "group": "Meta‑schema keywords map null leads to NullPointerException",
            "cases": [
                {
                    "input": [
                        {
                            "parameter": "schemaNode",
                            "value": "mocked JsonNode (any content, meta‑schema stub will be forced to return a JsonMetaSchema with null keywords map)"
                        },
                        {
                            "parameter": "config",
                            "value": "SchemaValidatorsConfig.builder().discriminatorKeywordEnabled(false).build()"
                        }
                    ],
                    "expected": "NullPointerException",
                    "description": "When getMetaSchemaOrDefault returns a JsonMetaSchema whose getKeywords() method returns null, the subsequent call to containsKey(\"discriminator\") dereferences a null map, causing a NullPointerException."
                }
            ]
        }
    ]
}