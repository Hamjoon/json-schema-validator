{
  "groups": [
    {
      "group": "Sub‑schema found – segment is numeric and parent node is an array",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /0"
            }
          ],
          "expected": "Returns a JsonSchema instance whose schemaLocation and evaluationPath have been appended with index 0. No exception is thrown.",
          "description": "The fragment points to the first element of an array. The method finds a non‑null subSchemaNode, recognises the segment as a Number while the current parent node is an array, updates schemaLocation/evaluationPath with the numeric index and creates a new sub‑schema because the resolved $id is either non‑null or this is the last segment."
        }
      ]
    },
    {
      "group": "Sub‑schema found – segment is a property name (object field)",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /properties/name"
            }
          ],
          "expected": "Returns a JsonSchema instance whose schemaLocation/evaluationPath have been appended with the string \"properties\" then \"name\". No exception is thrown.",
          "description": "The fragment points to a regular object property. The segment is not a Number, so the else‑branch updates the location with the segment’s string value and creates a sub‑schema because the $id is resolved (or it is the last segment)."
        }
      ]
    },
    {
      "group": "Sub‑schema found – id is null and segment is not the last one",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /definitions/person/properties/age"
            }
          ],
          "expected": "Returns the JsonSchema that corresponds to the last segment (\"age\"). The intermediate segment \"person\" does **not** cause a new sub‑schema creation because id == null and it is not the final segment.",
          "description": "The method iterates over three segments. For the first segment (\"definitions\") id is null and x != nameCount‑1, therefore the inner `if ((id != null) || (x == nameCount‑1))` is false and `subSchema` remains null. When the last segment (\"age\") is processed, the condition becomes true because x == nameCount‑1, so a sub‑schema is finally created and returned."
        }
      ]
    },
    {
      "group": "Sub‑schema found – id is null but segment is the last one",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /singleLevel"
            }
          ],
          "expected": "Returns a JsonSchema instance for the node at \"/singleLevel\" even though the resolved id is null, because the segment is the last one.",
          "description": "When the fragment consists of a single segment, the loop runs once (x == 0 == nameCount‑1). The condition `((id != null) || (x == nameCount‑1))` evaluates to true, forcing creation of the sub‑schema despite a null id."
        }
      ]
    },
    {
      "group": "Fragment not found – fetchSubSchemaNode returns null (exception path)",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /nonexistent"
            }
          ],
          "expected": "InvalidSchemaRefException is thrown with a ValidationMessage of type REF and code internal.unresolvedRef.",
          "description": "The fragment cannot be resolved in the current document. `subSchemaNode` is null, `document.findSchemaResourceRoot().fetchSubSchemaNode` returns null, so the method throws an InvalidSchemaRefException."
        }
      ]
    },
    {
      "group": "Fragment not found – fetchSubSchemaNode returns a schema that can resolve the fragment",
      "cases": [
        {
          "input": [
            {
              "parameter": "fragment",
              "value": "JsonNodePath representing /externalRef"
            }
          ],
          "expected": "Returns the JsonSchema obtained from the recursive call `found.getSubSchema(fragment)`.",
          "description": "When the fragment is not present in the current document, the method delegates to the root schema resource (`document.findSchemaResourceRoot().fetchSubSchemaNode`). That call returns a non‑null JsonSchema (`found`), which then successfully resolves the fragment via its own `getSubSchema` call, and the resulting sub‑schema is returned."
        }
      ]
    }
  ]
}