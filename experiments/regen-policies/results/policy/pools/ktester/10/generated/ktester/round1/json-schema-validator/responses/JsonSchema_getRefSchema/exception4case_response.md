{
  "testCases": [
    {
      "group": "Null fragment argument",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "Calling getRefSchema(null) should immediately throw a NullPointerException because the method dereferences fragment to call getPathType() and toString()."
        }
      ]
    },
    {
      "group": "Null schemaLocation in JsonSchema instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"anchor\""
            },
            {
              "parameter": "schemaLocation",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the JsonSchema object's schemaLocation field is null, the method attempts to call getAbsoluteIri() on it, resulting in a NullPointerException before any map lookup."
        }
      ]
    },
    {
      "group": "Null validationContext in JsonSchema instance",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"anchor\""
            },
            {
              "parameter": "validationContext",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the JsonSchema object's validationContext field is null, the call to validationContext.getSchemaResources() triggers a NullPointerException."
        }
      ]
    },
    {
      "group": "Null schemaResources map in ValidationContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"anchor\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "null"
            },
            {
              "parameter": "validationContext.dynamicAnchors",
              "value": "empty map"
            }
          ],
          "expected": "NullPointerException",
          "description": "When getSchemaResources() returns null, the method attempts to call .get(anchor) on a null reference, causing a NullPointerException before checking dynamic anchors."
        }
      ]
    },
    {
      "group": "Null dynamicAnchors map after static lookup fails",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath mock where getPathType() returns PathType.ANCHOR and toString() returns \"missingAnchor\""
            },
            {
              "parameter": "validationContext.schemaResources",
              "value": "empty map"
            },
            {
              "parameter": "validationContext.dynamicAnchors",
              "value": "null"
            }
          ],
          "expected": "NullPointerException",
          "description": "If the static schemaResources map does not contain the anchor and getDynamicAnchors() returns null, the subsequent call to .get(anchor) on a null map throws a NullPointerException rather than the expected JsonSchemaException."
        }
      ]
    }
  ]
}