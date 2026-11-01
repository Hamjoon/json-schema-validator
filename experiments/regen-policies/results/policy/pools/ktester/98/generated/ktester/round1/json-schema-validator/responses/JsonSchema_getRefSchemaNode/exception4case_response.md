{
    "group": "getRefSchemaNode_exceptions",
    "cases": [
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/any/path"
                }
            ],
            "expected": "NullPointerException",
            "description": "findSchemaResourceRoot() returns null, causing a NullPointerException when the method attempts to call schema.getId() or schema.getSchemaNode()."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/any/path"
                }
            ],
            "expected": "NullPointerException",
            "description": "findSchemaResourceRoot() returns a non‑null JsonSchema, but its getSchemaNode() returns null. When the method reaches node.at(jsonPointer) it throws a NullPointerException because node is null."
        },
        {
            "input": [
                {
                    "parameter": "ref",
                    "value": "/invalid~pointer"
                }
            ],
            "expected": "IllegalArgumentException",
            "description": "The jsonPointer starts with '/' so the third if block is entered. The pointer contains an unescaped '~' character, which makes JsonNode.at() compile the pointer and throw an IllegalArgumentException. The method does not catch this exception, so it propagates."
        }
    ]
}