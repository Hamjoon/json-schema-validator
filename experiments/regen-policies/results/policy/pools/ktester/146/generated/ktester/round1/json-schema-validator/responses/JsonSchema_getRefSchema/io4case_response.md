{
    "group": "JSON Pointer Path – SubSchema Returns Null",
    "cases": [
        {
            "input": [
                {
                    "parameter": "fragment",
                    "value": "JsonNodePath mock with PathType.JSON_POINTER (any non‑null fragment)"
                }
            ],
            "expected": "Returns null because getSubSchema(fragment) is stubbed to return null. No exception is thrown and no further lookup is performed.",
            "description": "When the fragment is a JSON‑Pointer the method should delegate to getSubSchema. If getSubSchema returns null the method must propagate that null result."
        }
    ]
}