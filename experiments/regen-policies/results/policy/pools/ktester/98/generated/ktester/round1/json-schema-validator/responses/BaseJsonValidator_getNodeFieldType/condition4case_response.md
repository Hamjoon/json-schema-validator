{
  "group": "BaseJsonValidator.getNodeFieldType - branch and value coverage",
  "cases": [
    {
      "input": [],
      "expected": "object",
      "description": "Parent schema's schemaNode contains a \"type\" field with textual value \"object\". The method should retrieve the node, detect it is non‑null and return its text representation."
    },
    {
      "input": [],
      "expected": "array",
      "description": "Parent schema's schemaNode contains a \"type\" field with textual value \"array\". Verifies that the method correctly returns the string \"array\"."
    },
    {
      "input": [],
      "expected": null,
      "description": "Parent schema's schemaNode does **not** contain a \"type\" field. The call to get(\"type\") returns null, so the method should follow the else‑branch and return null."
    },
    {
      "input": [],
      "expected": "",
      "description": "Parent schema's schemaNode contains a \"type\" field whose value is explicit JSON null (Jackson NullNode). The node is non‑null, so the method enters the true branch and returns NullNode.asText(), which is an empty string."
    },
    {
      "input": [],
      "expected": "5",
      "description": "Parent schema's schemaNode contains a \"type\" field with a numeric value (e.g., 5). Although the value is not textual, the node is non‑null and asText() yields the string representation \"5\". The method should return this string."
    }
  ]
}