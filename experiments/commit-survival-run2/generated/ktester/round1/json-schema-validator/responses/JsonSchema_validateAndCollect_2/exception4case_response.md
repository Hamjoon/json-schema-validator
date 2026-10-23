{
  "testCases": [
    {
      "group": "URISyntaxException on malformed $ref URI",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"ht!tp://[invalid]\" }"
            }
          ],
          "expected": "URISyntaxException thrown when the validator attempts to resolve the malformed URI in the $ref keyword",
          "description": "The $ref value contains characters that make the URI syntactically invalid. During validateAndCollect the method calls URI.create or new URI(...), which should raise a URISyntaxException."
        }
      ]
    },
    {
      "group": "UnsupportedEncodingException on malformed percent‑encoding in $ref",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": \"#/components/schemas/Item%ZZ\" }"
            }
          ],
          "expected": "UnsupportedEncodingException thrown while decoding the percent‑encoded $ref fragment",
          "description": "The $ref contains an illegal percent‑encoding sequence (%ZZ). When the validator uses URLDecoder.decode(..., \"UTF-8\"), the JDK throws an UnsupportedEncodingException because the byte sequence cannot be decoded."
        }
      ]
    },
    {
      "group": "RuntimeException from WalkListenerRunner callback",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"name\": \"Bob\" }"
            }
          ],
          "expected": "RuntimeException propagated from WalkListenerRunner.onWalkStart (or onWalkEnd)",
          "description": "A custom WalkListenerRunner is injected into the JsonSchema instance whose onWalkStart method deliberately throws a RuntimeException. validateAndCollect must propagate this exception."
        }
      ]
    },
    {
      "group": "NullPointerException due to null ExecutionContext",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"field\": \"value\" }"
            }
          ],
          "expected": "NullPointerException when validateAndCollect attempts to use a null ExecutionContext returned by createExecutionContext()",
          "description": "The JsonSchema subclass overrides createExecutionContext() to return null. The public validateAndCollect(JsonNode) then passes this null to the private overload, causing a NPE when the context is accessed."
        }
      ]
    },
    {
      "group": "IllegalArgumentException when required validator receives non‑object node",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "\"just a string\""
            }
          ],
          "expected": "IllegalArgumentException indicating that a required‑property validator cannot operate on a non‑object instance",
          "description": "The schema defines a RequiredValidator for property \"id\", but the supplied instance is a JSON string. RequiredValidator expects an ObjectNode and should throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "NullPointerException when getRefSchemaNode receives null reference",
      "cases": [
        {
          "input": [
            {
              "parameter": "node",
              "value": "{ \"$ref\": null }"
            }
          ],
          "expected": "NullPointerException because getRefSchemaNode(null) is invoked",
          "description": "The $ref keyword is present but its value is JSON null. The validator calls getRefSchemaNode(ref) with a null argument, leading to a NPE."
        }
      ]
    }
  ]
}