{
  "test_cases": [
    {
      "group": "External $ref resolution – I/O error",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"$ref\":\"http://nonexistent.example.com/missing-schema.json\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "IOException is caught and ValidationResult.isValid() == false with a message containing \"I/O error\" or \"cannot read\"",
          "description": "When the schema contains a $ref that points to an external resource which cannot be accessed, walk should propagate the IOException (or wrap it) and return an invalid ValidationResult indicating the I/O problem."
        }
      ]
    },
    {
      "group": "Internal $ref resolution – missing definition",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"$ref\":\"#/definitions/unknown\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "JsonSchemaException (or ValidationException) is thrown indicating that the reference '#/definitions/unknown' cannot be resolved",
          "description": "If a $ref points to a fragment that does not exist in the current schema, walk must raise a schema‑resolution exception rather than silently succeed."
        }
      ]
    },
    {
      "group": "Malformed $ref URI – illegal characters",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"$ref\":\"http://example.com/invalid[ref].json\"}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "IllegalArgumentException is thrown with a message about malformed URI or illegal characters in $ref",
          "description": "A $ref containing characters that are not allowed in a URI should cause the parser to throw an IllegalArgumentException before any validation is performed."
        }
      ]
    },
    {
      "group": "Schema with unsupported keyword – validator initialization failure",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing {\"unexpectedKeyword\":123}"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "IllegalArgumentException is thrown during validator initialization because the schema contains an unknown/unsupported keyword",
          "description": "When the JsonSchema instance is built from a schema that includes a keyword not recognized by the library, walk should fail early with an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "Recursive anchor without proper detection – StackOverflowError fallback",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "ObjectNode representing a deeply nested self‑referencing structure that exceeds recursion guard limits"
            },
            {
              "parameter": "validate",
              "value": true
            }
          ],
          "expected": "StackOverflowError is thrown (indicating that the recursive‑anchor guard failed)",
          "description": "If the schema contains a recursive anchor but the implementation fails to recognise it (e.g., due to a bug), walking a self‑referencing instance should eventually cause a StackOverflowError."
        }
      ]
    }
  ]
}