{
  "testCases": [
    {
      "group": "walkAtNode - invalid $ref URI",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNodeReferencingInvalidUri" },
            { "parameter": "rootNode", "value": "validRootNodeWithDefinitions" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "IllegalArgumentException thrown because $ref contains an illegal URI fragment that cannot be decoded",
          "description": "The schema contains a $ref like \"#/definitions/invalid uri\" (space not percent‑encoded). walkAtNode should attempt to decode the fragment and throw IllegalArgumentException."
        }
      ]
    },
    {
      "group": "walkAtNode - circular $ref without recursive anchor",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContextWithoutRecursiveAnchor" },
            { "parameter": "node", "value": "validObjectNodeCircularRef" },
            { "parameter": "rootNode", "value": "validRootNodeCircularRef" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "StackOverflowError (or a wrapped ValidationException indicating circular reference) thrown due to infinite $ref resolution loop",
          "description": "The schema defines A -> $ref \"#/definitions/B\" and B -> $ref \"#/definitions/A\" without $recursiveAnchor. walkAtNode should recurse until the JVM stack overflows, exposing the circular reference problem."
        }
      ]
    },
    {
      "group": "walkAtNode - unsupported keyword handling",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "validExecutionContext" },
            { "parameter": "node", "value": "validObjectNode" },
            { "parameter": "rootNode", "value": "validRootNodeWithUnsupportedKeyword" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "IllegalArgumentException thrown because the schema contains an unknown keyword that cannot be mapped to a validator",
          "description": "The schema includes a custom keyword \"x‑unknown\" that the library does not recognise. walkAtNode should detect the missing validator and raise an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "walkAtNode - executionContext with null collector",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "executionContextWithNullCollector" },
            { "parameter": "node", "value": "validObjectNode" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "NullPointerException thrown when walkAtNode attempts to record a validation message via a null CollectorContext",
          "description": "A custom ExecutionContext is created where the internal CollectorContext is deliberately set to null. The method will later call collector.addMessage(...), causing NPE."
        }
      ]
    },
    {
      "group": "walkAtNode - unsupported SpecVersion",
      "cases": [
        {
          "input": [
            { "parameter": "executionContext", "value": "executionContextWithUnsupportedSpecVersion" },
            { "parameter": "node", "value": "validObjectNode" },
            { "parameter": "rootNode", "value": "validRootNode" },
            { "parameter": "instanceLocation", "value": "validInstanceLocationRoot" },
            { "parameter": "validate", "value": "true" }
          ],
          "expected": "IllegalArgumentException thrown because the ValidationContext is configured with an unknown SpecVersion flag",
          "description": "The ExecutionContext wraps a ValidationContext that has a SpecVersion.VersionFlag value not recognised by the library (e.g., a future version). walkAtNode should reject it with IllegalArgumentException."
        }
      ]
    }
  ]
}