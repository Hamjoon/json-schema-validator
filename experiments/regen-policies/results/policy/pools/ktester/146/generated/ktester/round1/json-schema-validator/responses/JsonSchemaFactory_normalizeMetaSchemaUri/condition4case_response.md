{
  "testCases": [
    {
      "group": "Normalization when ID matches a known VersionFlag",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft-07/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The supplied id exactly matches one of the VersionFlag ids, therefore the method should detect it in the first loop (found = true) and return the original id without any further processing."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://json-schema.org/draft/2019-09/schema#"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "The supplied id matches the VersionFlag for Draft‑2019‑09. The method should return the id unchanged."
        }
      ]
    },
    {
      "group": "Normalization for unnormalized $schema URIs (contains \"json-schema.org/draft\")",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://example.com/some/path/draft-07/schema.json"
            }
          ],
          "expected": "http://json-schema.org/draft-07/schema#",
          "description": "The id does not match any VersionFlag, but contains \"://json-schema.org/draft\" and the substring \"/draft-07/\". The method should replace the whole id with SchemaId.V7."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://myhost.org/anything/draft/2019-09/schema"
            }
          ],
          "expected": "http://json-schema.org/draft/2019-09/schema#",
          "description": "Unnormalized id containing \"/draft/2019-09/\" should be normalized to SchemaId.V201909."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "file:///tmp/draft/2020-12/meta"
            }
          ],
          "expected": "http://json-schema.org/draft/2020-12/schema#",
          "description": "Unnormalized id containing \"/draft/2020-12/\" should be normalized to SchemaId.V202012."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "http://anyhost/draft-04/schema"
            }
          ],
          "expected": "http://json-schema.org/draft-04/schema#",
          "description": "Unnormalized id containing \"/draft-04/\" should be normalized to SchemaId.V4."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://anywhere/draft-06/schema"
            }
          ],
          "expected": "http://json-schema.org/draft-06/schema#",
          "description": "Unnormalized id containing \"/draft-06/\" should be normalized to SchemaId.V6."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": "https://example.com/draft-05/schema"
            }
          ],
          "expected": "https://example.com/draft-05/schema",
          "description": "The id contains \"json-schema.org/draft\" but none of the specific substrings matched; the method should leave the id unchanged."
        }
      ]
    },
    {
      "group": "No normalization needed (does not contain draft marker)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": "urn:custom:schema"
            }
          ],
          "expected": "urn:custom:schema",
          "description": "The id does not contain \"json-schema.org/draft\" and also does not match any VersionFlag, so it should be returned unchanged."
        },
        {
          "input": [
            {
              "parameter": "id",
              "value": ""
            }
          ],
          "expected": "",
          "description": "Empty string is not a match for any VersionFlag and does not contain the draft marker; the method should simply return the empty string."
        }
      ]
    },
    {
      "group": "Edge cases (null input)",
      "cases": [
        {
          "input": [
            {
              "parameter": "id",
              "value": null
            }
          ],
          "expected": "NullPointerException",
          "description": "Passing null causes a NullPointerException when the method attempts to call id.contains(...). The test should verify that the exception is thrown."
        }
      ]
    }
  ]
}