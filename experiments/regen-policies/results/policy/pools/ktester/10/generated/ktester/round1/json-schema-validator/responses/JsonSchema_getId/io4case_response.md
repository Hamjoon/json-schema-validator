{
  "group": "JsonSchema.getId() - edge cases",
  "cases": [
    {
      "input": [],
      "expected": "",
      "description": "Verify that getId() returns an empty string when the JsonSchema instance is created with an empty identifier."
    },
    {
      "input": [],
      "expected": "   ",
      "description": "Verify that getId() returns a string consisting only of whitespace characters when the identifier is set to spaces."
    },
    {
      "input": [],
      "expected": "http://example.com/schema.json?query=param%20value",
      "description": "Verify that getId() returns the exact identifier containing URL‑encoded characters without performing any decoding."
    },
    {
      "input": [],
      "expected": "urn:example:schema#fragment",
      "description": "Verify that getId() correctly returns an identifier that includes a fragment component."
    },
    {
      "input": [],
      "expected": "ПримерСхемы",
      "description": "Verify that getId() returns a Unicode identifier (Cyrillic characters) unchanged."
    },
    {
      "input": [],
      "expected": "schema\nwith\nnewlines",
      "description": "Verify that getId() returns an identifier containing newline characters exactly as supplied."
    }
  ]
}