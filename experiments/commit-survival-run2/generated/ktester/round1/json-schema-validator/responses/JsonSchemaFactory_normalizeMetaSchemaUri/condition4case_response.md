{
  "testCases": [
    {
      "group": "normalizeMetaSchemaUri - successful conversions",
      "cases": [
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com/schema"
            }
          ],
          "expected": "https://example.com/schema",
          "description": "Converts a simple HTTP URI to HTTPS while preserving host and path."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://user:pass@example.com:8080/path?query#frag"
            }
          ],
          "expected": "https://user:pass@example.com:8080/path",
          "description": "Keeps user info, host, port and path, but drops query and fragment, and forces HTTPS scheme."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "https://example.org"
            }
          ],
          "expected": "https://example.org",
          "description": "Input already uses HTTPS; method should return the same URI (no query/fragment removal needed)."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://example.com"
            }
          ],
          "expected": "https://example.com",
          "description": "URI without explicit path – result should be HTTPS with empty path component."
        }
      ]
    },
    {
      "group": "normalizeMetaSchemaUri - error handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "u",
              "value": "http://[invalid"
            }
          ],
          "expected": "JsonSchemaException with message \"Wrong MetaSchema URI: http://[invalid\"",
          "description": "Invalid URI syntax triggers URISyntaxException which is wrapped in JsonSchemaException."
        },
        {
          "input": [
            {
              "parameter": "u",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing null to the method causes a NullPointerException before URI construction; this is not caught by the method."
        }
      ]
    }
  ]
}