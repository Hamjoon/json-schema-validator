{
  "test_cases": [
    {
      "group": "SpecVersionDetector.detectVersion - JSON overrides path (draft-06 vs draft-04)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"\\$schema\": \"http://json-schema.org/draft-06/schema#\" }"
            },
            {
              "parameter": "specification",
              "value": "/tmp/schemas/draft-04/schema.json"
            },
            {
              "parameter": "defaultVersion",
              "value": "VersionFlag.V7"
            }
          ],
          "expected": "VersionFlag.V6",
          "description": "Both detections are present but the JSON‑based detection (draft‑06) should take precedence over the path‑based detection (draft‑04). The method must return V6."
        }
      ]
    },
    {
      "group": "SpecVersionDetector.detectVersion - Both detections return same version (draft-06)",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{ \"\\$schema\": \"http://json-schema.org/draft-06/schema#\" }"
            },
            {
              "parameter": "specification",
              "value": "/tmp/schemas/draft-06/schema.json"
            },
            {
              "parameter": "defaultVersion",
              "value": "VersionFlag.V4"
            }
          ],
          "expected": "VersionFlag.V6",
          "description": "JSON detection yields V6 and the path also yields V6; the method should return V6 (the first present optional version)."
        }
      ]
    },
    {
      "group": "SpecVersionDetector.detectVersion - Unknown path version falls back to default",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "specification",
              "value": "/tmp/schemas/draft-99/schema.json"
            },
            {
              "parameter": "defaultVersion",
              "value": "VersionFlag.V4"
            }
          ],
          "expected": "VersionFlag.V4",
          "description": "Path contains an unrecognised draft number (draft‑99). JSON has no $schema, so both optional detections are empty and the method must return the supplied default version V4."
        }
      ]
    },
    {
      "group": "SpecVersionDetector.detectVersion - Null defaultVersion results in null return",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "specification",
              "value": "null"
            },
            {
              "parameter": "defaultVersion",
              "value": "null"
            }
          ],
          "expected": "null",
          "description": "Both JSON and path detections are empty and the caller supplies a null defaultVersion. The method should propagate the null and return null."
        }
      ]
    },
    {
      "group": "SpecVersionDetector.detectVersion - Specification is a directory (no file name) falls back to default",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "{}"
            },
            {
              "parameter": "specification",
              "value": "/tmp/schemas/draft-07/"
            },
            {
              "parameter": "defaultVersion",
              "value": "VersionFlag.V4"
            }
          ],
          "expected": "VersionFlag.V4",
          "description": "The Path points to a directory, so version detection from the path yields empty. JSON also provides no $schema, therefore the method should return the default version V4."
        }
      ]
    },
    {
      "group": "SpecVersionDetector.detectVersion - JSON node is an array, path provides version",
      "cases": [
        {
          "input": [
            {
              "parameter": "jsonNode",
              "value": "[]"
            },
            {
              "parameter": "specification",
              "value": "/tmp/schemas/draft-07/schema.json"
            },
            {
              "parameter": "defaultVersion",
              "value": "VersionFlag.V4"
            }
          ],
          "expected": "VersionFlag.V7",
          "description": "The JSON node is an array, so detectOptionalVersion cannot find a $schema property and returns empty. The path contains \"draft-07\", therefore the method should return V7."
        }
      ]
    }
  ]
}