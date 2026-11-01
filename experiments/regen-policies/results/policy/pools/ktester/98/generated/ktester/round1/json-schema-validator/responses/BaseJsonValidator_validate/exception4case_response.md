{
  "testCases": [
    {
      "group": "Null schemaNode handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            }
          ],
          "expected": "Throws NullPointerException because the validator's schemaNode field is null and the method dereferences it while processing the node.",
          "description": "Create a concrete subclass of BaseJsonValidator that does not initialise the protected `schemaNode` field (leaves it null). Calling validate should hit a NPE when the implementation tries to inspect the schema definition."
        }
      ]
    },
    {
      "group": "Null validationContext handling",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.textNode(\"test\")"
            }
          ],
          "expected": "Throws NullPointerException because the validator's validationContext field is null and the method attempts to use it (e.g., to fetch a sub‑schema).",
          "description": "Instantiate a subclass of BaseJsonValidator where the protected `validationContext` is deliberately set to null. The call to `validate` will invoke `fetchSubSchemaNode(validationContext)` leading to a NPE."
        }
      ]
    },
    {
      "group": "collectAnnotations exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"annot\", true)"
            }
          ],
          "expected": "Throws IllegalStateException with message \"annotation‑failure\" and the exception is not swallowed by the top‑level validate method.",
          "description": "Override `collectAnnotations(ExecutionContext)` in a test subclass to throw an IllegalStateException. The top‑level `validate` must propagate this exception unchanged."
        }
      ]
    },
    {
      "group": "atRoot exception propagation",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode()"
            }
          ],
          "expected": "Throws RuntimeException with message \"atRoot‑failed\" and the exception bubbles up to the caller.",
          "description": "Create a subclass that overrides `atRoot()` to throw a RuntimeException. The wrapper `validate` method should not catch it, so the test expects the exception to be propagated."
        }
      ]
    },
    {
      "group": "Discriminator mismatch exception",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"unknown\")"
            }
          ],
          "expected": "Throws IllegalArgumentException with message containing \"discriminator property value does not match any schema\".",
          "description": "Configure the validator's `schemaNode` to contain a `discriminator` object that expects a property value that is not present in the supplied JSON instance. The internal call to `checkDiscriminatorMatch` should detect the mismatch and throw an IllegalArgumentException."
        }
      ]
    },
    {
      "group": "registerAndMergeDiscriminator duplicate registration",
      "cases": [
        {
          "input": [
            {
              "parameter": "executionContext",
              "value": "new ExecutionContext()"
            },
            {
              "parameter": "node",
              "value": "JsonNodeFactory.instance.objectNode().put(\"type\", \"A\")"
            }
          ],
          "expected": "Throws IllegalStateException with message \"Discriminator already registered for property 'type'\".",
          "description": "In a test subclass, invoke `registerAndMergeDiscriminator` twice with the same discriminator property during validation. The second registration should trigger an IllegalStateException according to the library contract."
        }
      ]
    }
  ]
}