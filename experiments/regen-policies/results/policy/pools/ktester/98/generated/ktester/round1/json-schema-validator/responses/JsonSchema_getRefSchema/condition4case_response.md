{
  "testCases": [
    {
      "group": "JSON Pointer Path",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath with PathType.JSON_POINTER"
            }
          ],
          "expected": "Returns the JsonSchema instance produced by getSubSchema(fragment)",
          "description": "When the fragment's PathType is JSON_POINTER the method should delegate to getSubSchema and return its result."
        }
      ]
    },
    {
      "group": "Anchor found in schemaResources",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/definitions/foo\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored in validationContext.schemaResources under the computed anchor",
          "description": "Fragment is an anchor. The base IRI is empty, so anchor becomes \"#/definitions/foo\". The schemaResources map contains this key, so the method must return the associated JsonSchema without checking dynamicAnchors."
        }
      ]
    },
    {
      "group": "Anchor found in dynamicAnchors",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/defs/bar\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored in validationContext.dynamicAnchors under the computed anchor",
          "description": "Fragment is an anchor. The key is not present in schemaResources but is present in dynamicAnchors, so the method should fall back to the dynamicAnchors map and return that schema."
        }
      ]
    },
    {
      "group": "Anchor not found – exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "Mocked JsonNodePath with PathType.ANCHOR, toString() = \"/missing\""
            }
          ],
          "expected": "Throws JsonSchemaException with message containing \"Unable to find anchor\" and the computed anchor",
          "description": "Fragment is an anchor and the computed anchor is absent from both schemaResources and dynamicAnchors. The method must throw a JsonSchemaException."
        }
      ]
    }
  ]
}