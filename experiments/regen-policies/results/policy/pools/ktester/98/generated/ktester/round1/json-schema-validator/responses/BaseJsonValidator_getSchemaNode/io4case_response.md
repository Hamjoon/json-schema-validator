{
  "groups": [
    {
      "group": "BaseJsonValidator#getSchemaNode - different JsonNode implementations",
      "cases": [
        {
          "input": [],
          "expected": "the same ObjectNode instance representing {\"type\":\"string\"}",
          "description": "Validator constructed with an ObjectNode (e.g., {\"type\":\"string\"}) should return that exact ObjectNode instance, confirming the getter does not create a copy."
        },
        {
          "input": [],
          "expected": "the same ArrayNode instance representing [1,2,3]",
          "description": "Validator constructed with an ArrayNode ([1,2,3]) must return the identical ArrayNode reference."
        },
        {
          "input": [],
          "expected": "the same TextNode instance representing the string \"hello\"",
          "description": "When the schemaNode is a TextNode (\"hello\"), getSchemaNode() should return that same TextNode object."
        },
        {
          "input": [],
          "expected": "the same NumericNode instance representing the number 42",
          "description": "Validator built with a NumericNode (42) must return the exact same NumericNode reference."
        },
        {
          "input": [],
          "expected": "the same BooleanNode instance representing true",
          "description": "If the schemaNode is a BooleanNode (true), getSchemaNode() should return that identical BooleanNode."
        },
        {
          "input": [],
          "expected": "the same NullNode instance (JsonNode representing null)",
          "description": "When the validator is constructed with Jackson's NullNode, getSchemaNode() must return that same NullNode reference."
        }
      ]
    },
    {
      "group": "BaseJsonValidator#getSchemaNode - reference mutability",
      "cases": [
        {
          "input": [],
          "expected": "the same mutable ObjectNode instance reflecting the added field {\"type\":\"string\",\"maxLength\":5}",
          "description": "After constructing the validator with an ObjectNode {\"type\":\"string\"}, a field is added to the original node (maxLength). Because getSchemaNode() returns the stored reference, the returned node should reflect the mutation."
        },
        {
          "input": [],
          "expected": "the same mutable ArrayNode instance reflecting the appended element 4, i.e., [1,2,3,4]",
          "description": "Validator created with an ArrayNode [1,2,3]; after appending 4 to the original array, getSchemaNode() must return the same ArrayNode now containing the new element, proving the getter does not clone the node."
        }
      ]
    }
  ]
}