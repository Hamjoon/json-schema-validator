{
  "group": "BaseJsonValidator.getNodeFieldType - additional type variations",
  "cases": [
    {
      "input": [],
      "expected": "true",
      "description": "Parent schema's schemaNode contains a \"type\" field with a boolean true value. The node is non‑null, so the method returns the result of asText() which for a BooleanNode is the string \"true\"."
    },
    {
      "input": [],
      "expected": "false",
      "description": "Parent schema's schemaNode contains a \"type\" field with a boolean false value. The method should return the string \"false\"."
    },
    {
      "input": [],
      "expected": "",
      "description": "Parent schema's schemaNode contains a \"type\" field whose value is a JSON object (e.g., {\"a\":1}). For container nodes Jackson's asText() yields an empty string, so the method should return \"\"."
    },
    {
      "input": [],
      "expected": "",
      "description": "Parent schema's schemaNode contains a \"type\" field whose value is a JSON array (e.g., [1,2,3]). asText() on an ArrayNode returns an empty string, therefore the method returns \"\"."
    },
    {
      "input": [],
      "expected": "  object  ",
      "description": "Parent schema's schemaNode contains a \"type\" field with a textual value that includes leading and trailing whitespace. The method should return the exact text as stored, preserving the whitespace."
    },
    {
      "input": [],
      "expected": "π",
      "description": "Parent schema's schemaNode contains a \"type\" field with a Unicode character (Greek pi). The method should return the Unicode string unchanged."
    }
  ]
}