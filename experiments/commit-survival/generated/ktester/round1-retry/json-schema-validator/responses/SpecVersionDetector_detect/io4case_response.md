{
  "testCases": [
    {
      "group": "detect - null input",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling `detect` with a null JsonNode should raise a NullPointerException because the method attempts to access the node without a null‑check."
        }
      ]
    },
    {
      "group": "detect - non‑text $schema value",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": 12345 }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "When the `$schema` field exists but its value is not a text node (e.g., a number), `detectOptionalVersion` cannot map it to a known version, so `detect` should throw a `JsonSchemaException` with the standard message."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": [\"http://json-schema.org/draft-07/schema#\"] }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "If `$schema` is an array (or any non‑string JSON type), the detector treats it as unknown and throws the same `JsonSchemaException`."
        }
      ]
    },
    {
      "group": "detect - known older drafts",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-06/schema#\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_06",
          "description": "The detector recognises the Draft‑06 URI and returns `VersionFlag.DRAFT_06`."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"https://json-schema.org/draft/2019-09/schema\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_2019_09",
          "description": "The detector recognises the Draft‑2019‑09 URI (newer URL style) and returns `VersionFlag.DRAFT_2019_09`."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-2019-09/schema#\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_2019_09",
          "description": "The detector also recognises the legacy Draft‑2019‑09 URI with a trailing `#`."
        }
      ]
    },
    {
      "group": "detect - whitespace and case variations",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"  http://json-schema.org/draft-07/schema#  \" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "Leading/trailing whitespace is not trimmed by the detector; the exact string does not match any known URI, resulting in an exception."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"HTTP://JSON-SCHEMA.ORG/DRAFT-07/SCHEMA#\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "The detector performs a case‑sensitive match; an upper‑cased URI is considered unknown and triggers the exception."
        }
      ]
    },
    {
      "group": "detect - $schema present but empty string",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "An empty string value for `$schema` cannot be mapped to any version, so `detect` throws the standard `JsonSchemaException`."
        }
      ]
    },
    {
      "group": "detect - $schema with fragment or query parameters",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema#.extra\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "Appending a fragment or extra characters to a known URI makes it unrecognised; the detector should throw the exception."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema?#param=1\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "A known URI with a query string is not matched by the detector, leading to an exception."
        }
      ]
    }
  ]
}