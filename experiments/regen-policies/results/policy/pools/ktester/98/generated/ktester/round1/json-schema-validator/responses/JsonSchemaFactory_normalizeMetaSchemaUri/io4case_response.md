{
  "test_cases": [
    {
      "group": "NullInput",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing a null reference should cause a NullPointerException when the method attempts to call `contains` on the argument."
        }
      ]
    },
    {
      "group": "EmptyString",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": ""
            }
          ],
          "expected": "",
          "description": "An empty string does not match any known VersionFlag and does not contain the draft marker, so the method should return it unchanged."
        }
      ]
    },
    {
      "group": "MultipleDraftSubstrings",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The id contains both \"/draft-07/\" and \"/draft-04/\". Because the method checks for \"/draft-07/\" first, it should normalise to `SchemaId.V7`."
        }
      ]
    },
    {
      "group": "OverlappingDraftPatterns",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2019-09/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "Both \"/draft/2019-09/\" and \"/draft-04/\" are present; the earlier check for 2019‑09 wins, so the result should be `SchemaId.V201909`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2020-12/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "Even though \"/draft/2020-12/\" appears first, the method checks for \"/draft-07/\" before it, so the id normalises to `SchemaId.V7`."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-04/draft-06/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "Both \"/draft-04/\" and \"/draft-06/\" are present; the check for \"/draft-04/\" occurs before \"/draft-06/\", resulting in `SchemaId.V4`."
        }
      ]
    },
    {
      "group": "NonMatchingDraftPattern",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft07/schema#",
          "description": "The substring \"/draft-07/\" is missing the separating slash, so the method should not treat it as an unnormalised draft URI and must return the original id."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07schema#",
          "description": "The pattern does not contain the required \"/draft-07/\" segment; therefore no normalisation occurs."
        }
      ]
    },
    {
      "group": "CaseSensitivity",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://JSON-SCHEMA.ORG/draft-07/schema#"
            }
          ],
          "expected": "http://JSON-SCHEMA.ORG/draft-07/schema#",
          "description": "The check for the marker \"://json-schema.org/draft\" is case‑sensitive; a different case prevents detection, so the id stays unchanged."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "HTTP://json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "HTTP://json-schema.org/draft-07/schema#",
          "description": "Upper‑case scheme part breaks the case‑sensitive substring search, resulting in no normalisation."
        }
      ]
    }
  ]
}