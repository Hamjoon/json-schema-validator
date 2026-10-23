{
  "testCases": [
    {
      "group": "detect - successful version detection",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-07/schema#\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_07",
          "description": "When the JSON node contains a `$schema` tag with a known Draft‑07 URI, `detect` should return the corresponding `VersionFlag.DRAFT_07`."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://json-schema.org/draft-04/schema#\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_04",
          "description": "When the JSON node contains a `$schema` tag with a known Draft‑04 URI, `detect` should return the corresponding `VersionFlag.DRAFT_04`."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"https://json-schema.org/draft/2020-12/schema\" }"
            }
          ],
          "expected": "VersionFlag.DRAFT_2020_12",
          "description": "When the JSON node contains a `$schema` tag with a known 2020‑12 URI, `detect` should return the corresponding `VersionFlag.DRAFT_2020_12`."
        }
      ]
    },
    {
      "group": "detect - missing or unknown $schema tag",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"title\": \"No schema tag here\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "If the JSON node does not contain a `$schema` field, `detect` must throw a `JsonSchemaException` with the message `'$schema' tag is not present`."
        },
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"$schema\": \"http://example.com/unknown-schema\" }"
            }
          ],
          "expected": "JsonSchemaException: '$schema' tag is not present",
          "description": "If the `$schema` field is present but its value is not recognised by `detectOptionalVersion`, the optional result is empty and `detect` should throw a `JsonSchemaException` with the same message."
        }
      ]
    }
  ]
}