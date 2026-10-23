{
    "group": "Default URI Factory",
    "cases": [
        {
            "input": [],
            "expected": "Non‑null instance of URIFactory that equals the factory created by the default builder",
            "description": "When a JsonSchemaFactory is created via the default `JsonSchemaFactory.builder().build()` the internal `uriFactory` is instantiated with the library's default implementation. The test verifies that `getUriFactory()` returns a non‑null object and that the returned reference is the same object that the builder used to construct the factory."
        }
    ]
}