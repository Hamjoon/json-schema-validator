{
    "group": "Invalid inputs – unsupported json node types",
    "cases": [
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new URI(\"http://example.com/numberNode.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonMapperFactory.getMapper().readTree(\"5\")"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "A JsonNode that is a numeric literal (neither object nor boolean) is not a valid JSON Schema; the factory should reject it with IllegalArgumentException."
        },
        {
            "input": [
                {
                    "parameter": "schemaUri",
                    "value": "new URI(\"http://example.com/arrayNode.json\")"
                },
                {
                    "parameter": "jsonNode",
                    "value": "JsonMapperFactory.getMapper().readTree(\"[1,2,3]\")"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "A JsonNode that is a JSON array (neither object nor boolean) is invalid as a top‑level schema; the factory must throw IllegalArgumentException."
        }
    ]
}