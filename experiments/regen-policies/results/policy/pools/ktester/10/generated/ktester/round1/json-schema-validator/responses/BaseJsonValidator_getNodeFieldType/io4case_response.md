{
    "testCases": [
        {
            "group": "BaseJsonValidator.getNodeFieldType - type field absent",
            "cases": [
                {
                    "input": [],
                    "expected": null,
                    "description": "Parent schema's JsonNode does not contain a \"type\" property. The method should return null."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getNodeFieldType - type field is numeric node",
            "cases": [
                {
                    "input": [],
                    "expected": "",
                    "description": "Parent schema's JsonNode contains a \"type\" property whose value is a numeric node (e.g., 5). JsonNode.asText() on a non‑text node returns an empty string, so the method should return \"\"."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getNodeFieldType - type field is array node",
            "cases": [
                {
                    "input": [],
                    "expected": "",
                    "description": "Parent schema's JsonNode contains a \"type\" property whose value is an array node (e.g., [\"object\", \"string\"]). asText() on an ArrayNode yields an empty string, so the method should return \"\"."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getNodeFieldType - type field is null node",
            "cases": [
                {
                    "input": [],
                    "expected": "null",
                    "description": "Parent schema's JsonNode contains a \"type\" property whose value is a JSON null node. NullNode.asText() returns the string \"null\", so the method should return \"null\"."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getNodeFieldType - parent schema node is null",
            "cases": [
                {
                    "input": [],
                    "expected": null,
                    "description": "The parent schema's getSchemaNode() returns null. The call to get(\"type\") therefore yields null, and the method should return null."
                }
            ]
        },
        {
            "group": "BaseJsonValidator.getNodeFieldType - parent schema is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "BaseJsonValidator.getParentSchema() returns null. Attempting to call getSchemaNode() on a null reference throws a NullPointerException. The test expects this exception."
                }
            ]
        }
    ]
}