{
    "group": "JsonSchemaFactory.getSchema(JsonNode) - numeric schema node",
    "cases": [
        {
            "input": [
                {
                    "parameter": "jsonNode",
                    "value": "ObjectMapper.readTree(\"123\")"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "A numeric JSON node is not a valid top‑level JSON‑Schema representation; the factory must reject it with an IllegalArgumentException."
        }
    ]
}