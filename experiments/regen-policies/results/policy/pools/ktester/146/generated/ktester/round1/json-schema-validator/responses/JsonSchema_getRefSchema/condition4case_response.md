{
  "testCases": [
    {
      "group": "JSON Pointer Path",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.JSON_POINTER"
            }
          ],
          "expected": "Returns the JsonSchema instance produced by getSubSchema(fragment). The test verifies that getSubSchema is invoked exactly once and its result is returned.",
          "description": "When the fragment's PathType is JSON_POINTER the method should delegate to getSubSchema and return its result."
        },
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.JSON_POINTER and null schemaLocation"
            }
          ],
          "expected": "Returns the JsonSchema instance from getSubSchema(fragment). No interaction with schemaLocation or validationContext occurs.",
          "description": "Even if the owning JsonSchema has a null absolute IRI, the JSON‑Pointer branch must ignore it and still call getSubSchema."
        }
      ]
    },
    {
      "group": "Anchor Path – Resource Found",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"myAnchor\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored in validationContext.schemaResources under the computed anchor key.",
          "description": "Fragment is an anchor. The schemaLocation has a non‑null absolute IRI (e.g., \"http://example.com/schema.json\"). The method builds the anchor \"http://example.com/schema.json#myAnchor\", finds it in schemaResources and returns it."
        },
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"anchorOnly\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored in validationContext.schemaResources under the computed anchor key (base part empty).",
          "description": "Fragment is an anchor but the owning schema has a null absolute IRI, so base is empty. The anchor becomes \"#anchorOnly\" and the method finds the schema in schemaResources."
        }
      ]
    },
    {
      "group": "Anchor Path – Dynamic Anchor Found",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"dynAnchor\""
            }
          ],
          "expected": "Returns the JsonSchema instance stored in validationContext.dynamicAnchors under the computed anchor key.",
          "description": "Fragment is an anchor. The computed anchor is not present in schemaResources, but a matching entry exists in dynamicAnchors. The method should fall back to dynamicAnchors and return that schema."
        }
      ]
    },
    {
      "group": "Anchor Path – Not Found (Exception)",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock with PathType.ANCHOR and toString() = \"missingAnchor\""
            }
          ],
          "expected": "Throws JsonSchemaException with message containing \"Unable to find anchor\" and the computed anchor string.",
          "description": "Fragment is an anchor and the computed anchor is absent from both schemaResources and dynamicAnchors. The method must raise a JsonSchemaException."
        }
      ]
    }
  ]
}