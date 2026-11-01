{
  "testCases": [
    {
      "group": "KnownVersionFlag",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The supplied id exactly matches a known VersionFlag identifier (draft‑07). The method should detect it in the first loop, set `found` to true and return the original id unchanged."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "The supplied id matches the VersionFlag identifier for draft‑04. Even though the string also contains the generic \"draft\" pattern, the method should stop after the first loop and return the original id."
        }
      ]
    },
    {
      "group": "UnnormalizedDraftURIs",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "When the id does not match any VersionFlag (simulated by using a value that is not in the enum) but contains the substring \"/draft-07/\", the method should normalise it to the constant `SchemaId.V7`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://json-schema.org/draft/2019-09/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "Unnormalized $schema containing \"/draft/2019-09/\" should be normalised to `SchemaId.V201909`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2020-12/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2020-12/schema#",
          "description": "Unnormalized $schema containing \"/draft/2020-12/\" should be normalised to `SchemaId.V202012`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "Unnormalized $schema containing \"/draft-04/\" should be normalised to `SchemaId.V4`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-06/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-06/schema#",
          "description": "Unnormalized $schema containing \"/draft-06/\" should be normalised to `SchemaId.V6`."
        }
      ]
    },
    {
      "group": "NoNormalization",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-99/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-99/schema#",
          "description": "The id contains the generic \"://json-schema.org/draft\" pattern but none of the specific draft substrings; the method should leave the id unchanged."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "urn:example:custom-schema"
            }
          ],
          "expected": "urn:example:custom-schema",
          "description": "The id does not contain the \"json-schema.org/draft\" marker at all; the method should return the original id unchanged."
        }
      ]
    }
  ]
}