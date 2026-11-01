{
    "group": "Null InputFormat",
    "cases": [
        {
            "input": [
                {
                    "parameter": "input",
                    "value": "{\"key\":\"value\"}"
                },
                {
                    "parameter": "inputFormat",
                    "value": null
                }
            ],
            "expected": "IllegalArgumentException (or NullPointerException) indicating that the InputFormat argument cannot be null.",
            "description": "Passing a non‑null JSON string while the InputFormat argument is null should cause the method to reject the call early, throwing an IllegalArgumentException (or NPE) before any deserialization occurs."
        }
    ]
}