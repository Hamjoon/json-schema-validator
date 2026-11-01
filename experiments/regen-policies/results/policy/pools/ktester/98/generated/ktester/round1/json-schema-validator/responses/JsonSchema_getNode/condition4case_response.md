{
    "group": "getNode(Object) – property name (String)",
    "cases": [
        {
            "input": [
                {
                    "parameter": "propertyOrIndex",
                    "value": "\"title\""
                }
            ],
            "expected": "\"Example Schema\"",
            "description": "When the schemaNode is an ObjectNode containing a field \"title\" with value \"Example Schema\", getNode(\"title\") should return the JsonNode representing that value."
        },
        {
            "input": [
                {
                    "parameter": "propertyOrIndex",
                    "value": "\"missing\""
                }
            ],
            "expected": "null",
            "description": "If the requested field does not exist in the ObjectNode, getNode should return null (the branch that handles a missing property)."
        }
    ]
}