{
    "group": "BaseJsonValidator#getNodeFieldType - additional scenarios",
    "cases": [
        {
            "input": [],
            "expected": "\"\"",
            "description": "When the parent schema's JSON node contains a \"type\" property whose value is an array (e.g., [\"string\", \"null\"]), JsonNode.asText() returns an empty string. The method should therefore return an empty string."
        },
        {
            "input": [],
            "expected": "\"\"",
            "description": "When the parent schema's JSON node contains a \"type\" property whose value is a numeric node (e.g., 42), JsonNode.asText() returns an empty string. The method should return an empty string."
        },
        {
            "input": [],
            "expected": "\"\"",
            "description": "When the parent schema's JSON node contains a \"type\" property whose value is the JSON literal null, JsonNode.asText() returns an empty string. The method should return an empty string."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "If getParentSchema() returns a non‑null schema but getSchemaNode() returns null, invoking get(\"type\") on a null JsonNode causes a NullPointerException. This case verifies that failure mode."
        }
    ]
}