{
  "testCases": [
    {
      "group": "Id matches a known VersionFlag",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The supplied id equals the id of one of the VersionFlag enum constants, therefore the method should set `found` to true and return the original id unchanged."
        }
      ]
    },
    {
      "group": "Id does not match any VersionFlag and does not contain a draft URL",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "urn:custom:schema"
            }
          ],
          "expected": "urn:custom:schema",
          "description": "The id is not a known VersionFlag and does not contain the substring \"://json-schema.org/draft\"; the method should skip all normalisation logic and return the original id."
        }
      ]
    },
    {
      "group": "Unnormalised draft‑07 URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://example.com/any/path://json-schema.org/draft/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The id contains the generic draft marker and the specific \"/draft-07/\" segment, so the method should replace the whole id with `SchemaId.V7`."
        }
      ]
    },
    {
      "group": "Unnormalised draft‑2019‑09 URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://json-schema.org/draft/2019-09/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "The id contains the generic draft marker and the \"/draft/2019-09/\" segment; the method should replace it with `SchemaId.V201909`."
        }
      ]
    },
    {
      "group": "Unnormalised draft‑2020‑12 URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://json-schema.org/draft/2020-12/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2020-12/schema#",
          "description": "The id contains the generic draft marker and the \"/draft/2020-12/\" segment; the method should replace it with `SchemaId.V202012`."
        }
      ]
    },
    {
      "group": "Unnormalised draft‑04 URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/draft-04/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "The id contains the generic draft marker and the \"/draft-04/\" segment; the method should replace it with `SchemaId.V4`."
        }
      ]
    },
    {
      "group": "Unnormalised draft‑06 URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/draft-06/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-06/schema#",
          "description": "The id contains the generic draft marker and the \"/draft-06/\" segment; the method should replace it with `SchemaId.V6`."
        }
      ]
    },
    {
      "group": "Id contains draft marker but none of the recognised sub‑paths",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/unknown/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/unknown/schema#",
          "description": "The id contains the generic draft marker but none of the specific sub‑paths checked by the else‑if chain; the method should leave the id unchanged."
        }
      ]
    }
  ]
}