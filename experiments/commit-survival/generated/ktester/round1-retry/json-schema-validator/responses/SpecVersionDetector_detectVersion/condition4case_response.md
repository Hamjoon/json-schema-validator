{
  "group": "SpecVersionDetector.detectVersion - Branch Coverage",
  "cases": [
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"\\$schema\": \"http://json-schema.org/draft-07/schema#\" }"
        },
        {
          "parameter": "specification",
          "value": "null"
        },
        {
          "parameter": "defaultVersion",
          "value": "VersionFlag.V4"
        }
      ],
      "expected": "VersionFlag.V7",
      "description": "First optional version is present (detectOptionalVersion returns V7). The method should return this version without consulting the path."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{}"
        },
        {
          "parameter": "specification",
          "value": "\"/tmp/schemas/draft-06/schema.json\""
        },
        {
          "parameter": "defaultVersion",
          "value": "VersionFlag.V4"
        }
      ],
      "expected": "VersionFlag.V6",
      "description": "detectOptionalVersion returns empty, but detectVersionFromPath extracts V6 from the file name. The method should return V6."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{}"
        },
        {
          "parameter": "specification",
          "value": "\"/tmp/schemas/unknown/schema.json\""
        },
        {
          "parameter": "defaultVersion",
          "value": "VersionFlag.V4"
        }
      ],
      "expected": "VersionFlag.V4",
      "description": "Both optional detections are empty (no $schema in JSON and path does not contain a known version). The method should fall back to the supplied default version."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "null"
        },
        {
          "parameter": "specification",
          "value": "\"/tmp/schemas/draft-07/schema.json\""
        },
        {
          "parameter": "defaultVersion",
          "value": "VersionFlag.V4"
        }
      ],
      "expected": "VersionFlag.V7",
      "description": "jsonNode is null, detectOptionalVersion should handle the null gracefully and return empty (or be mocked to return empty). Path detection yields V7, so the method returns V7."
    },
    {
      "input": [
        {
          "parameter": "jsonNode",
          "value": "{ \"\\$schema\": \"http://json-schema.org/draft-04/schema#\" }"
        },
        {
          "parameter": "specification",
          "value": "\"/tmp/schemas/draft-07/schema.json\""
        },
        {
          "parameter": "defaultVersion",
          "value": "VersionFlag.V6"
        }
      ],
      "expected": "VersionFlag.V4",
      "description": "Both detections are present, but the stream order gives priority to the first (JSON‑based) detection. The method must return V4 even though the path indicates V7."
    },
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
          "value": "VersionFlag.V6"
        }
      ],
      "expected": "VersionFlag.V6",
      "description": "Both jsonNode and specification are empty/null, resulting in no optional version. The method should return the provided default version V6."
    }
  ]
}