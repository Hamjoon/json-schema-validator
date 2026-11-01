{
    "group": "BaseJsonValidator#getNodeFieldType",
    "cases": [
        {
            "input": [],
            "expected": "\"string\"",
            "description": "When the parent schema's JSON node contains a \"type\" property with the value \"string\", getNodeFieldType() should return the text of that property."
        },
        {
            "input": [],
            "expected": "null",
            "description": "When the parent schema's JSON node does not contain a \"type\" property, getNodeFieldType() should return null."
        },
        {
            "input": [],
            "expected": "NullPointerException",
            "description": "If getParentSchema() unexpectedly returns null, the method will throw a NullPointerException. This case verifies the method's failure mode for a missing parent schema."
        }
    ]
}