{
  "groups": [
    {
      "group": "Null Schema Node",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "simple"
            }
          ],
          "expected": "NullPointerException",
          "description": "When the JsonSchema instance returns null from getSchemaNode(), any attempt to call node.get(...) (whether for a Number or a String key) throws a NullPointerException. This test forces getSchemaNode() to return null (e.g., via a test subclass or mock) and supplies a normal string key."
        }
      ]
    },
    {
      "group": "Number with Faulty intValue",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "new Number(){ public int intValue(){ throw new IllegalArgumentException(\"bad int\"); } public long longValue(){ return 0L; } public double doubleValue(){ return 0.0; } }"
            }
          ],
          "expected": "IllegalArgumentException",
          "description": "A custom Number implementation throws an exception from intValue(). The method casts the argument to Number and calls intValue(), so the exception propagates. This verifies that the method does not swallow such runtime exceptions."
        }
      ]
    },
    {
      "group": "Object with Faulty toString",
      "cases": [
        {
          "input": [
            {
              "parameter": "propertyOrIndex",
              "value": "new Object(){ public String toString(){ throw new IllegalStateException(\"toString failed\"); } }"
            }
          ],
          "expected": "IllegalStateException",
          "description": "An object whose toString() method throws an exception. The method invokes propertyOrIndex.toString() for non‑Number arguments, so the exception is propagated. This tests the method's behaviour when the supplied object's string conversion is broken."
        }
      ]
    }
  ]
}