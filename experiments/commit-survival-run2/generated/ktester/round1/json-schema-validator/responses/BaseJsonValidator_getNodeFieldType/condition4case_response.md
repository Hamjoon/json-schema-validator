{
    "testCases": [
        {
            "group": "getNodeFieldType - type field present",
            "cases": [
                {
                    "input": [],
                    "expected": "object",
                    "description": "The parent schema's schema node contains a \"type\" field with the textual value \"object\". The method should return the string \"object\"."
                }
            ]
        },
        {
            "group": "getNodeFieldType - type field absent",
            "cases": [
                {
                    "input": [],
                    "expected": null,
                    "description": "The parent schema's schema node does not contain a \"type\" field. The method should fall through the if‑statement and return null."
                }
            ]
        },
        {
            "group": "getNodeFieldType - parent schema is null",
            "cases": [
                {
                    "input": [],
                    "expected": "NullPointerException",
                    "description": "If getParentSchema() returns null, the call to getParentSchema().getSchemaNode() triggers a NullPointerException. This test confirms the current implementation does not guard against a null parent schema."
                }
            ]
        }
    ]
}