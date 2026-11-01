{
  "group": "JsonSchema.getId",
  "cases": [
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"   \""
        }
      ],
      "expected": "\"   \"",
      "description": "Identifier consisting only of whitespace characters – getId() should return the exact whitespace string without trimming."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"urn:example:12345\""
        }
      ],
      "expected": "\"urn:example:12345\"",
      "description": "URN style identifier – verifies that getId() correctly returns a non‑URL identifier."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema#section\""
        }
      ],
      "expected": "\"http://example.com/schema#section\"",
      "description": "URI containing a fragment component – ensures the fragment is preserved in the returned id."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://例子.测试/模式\""
        }
      ],
      "expected": "\"http://例子.测试/模式\"",
      "description": "Identifier with Unicode characters – checks that getId() returns the Unicode string unchanged."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema?query=param%20value\""
        }
      ],
      "expected": "\"http://example.com/schema?query=param%20value\"",
      "description": "URI containing percent‑encoded characters – validates that the encoded value is returned verbatim."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"%s\""
        }
      ],
      "expected": "\"%s\"",
      "description": "Identifier that looks like a format placeholder – confirms that getId() does not interpret or alter placeholder syntax."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"\" + \"a\".repeat(5000)"
        }
      ],
      "expected": "\"\" + \"a\".repeat(5000)",
      "description": "Very long identifier (5,000 characters) – tests that getId() can handle large strings without truncation or performance issues."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema\\n\""
        }
      ],
      "expected": "\"http://example.com/schema\\n\"",
      "description": "Identifier ending with a newline character – ensures newline is preserved in the returned value."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema\\t\""
        }
      ],
      "expected": "\"http://example.com/schema\\t\"",
      "description": "Identifier ending with a tab character – verifies that tab characters are retained."
    },
    {
      "input": [
        {
          "parameter": "id",
          "value": "\"http://example.com/schema?param=1&param=2\""
        }
      ],
      "expected": "\"http://example.com/schema?param=1&param=2\"",
      "description": "URI with repeated query parameters – checks that the entire query string is returned unchanged."
    }
  ]
}