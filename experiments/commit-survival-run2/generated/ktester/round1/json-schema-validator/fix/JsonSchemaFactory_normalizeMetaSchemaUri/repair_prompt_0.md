


You are an experienced Java developer tasked with fixing a piece of code that contains execution errors. Your task is to resolve these issues after analyzing the code and its execution feedback.

Use the following steps to guide your analysis and correction process:
1. Review the provided Java code for any logical errors.
2. Analyze the execution feedback to understand the specific errors and their locations within the code.
3. Apply necessary corrections to the code based on the feedback, and output the compelete correct code.
The Java code you need to fix is provided below:
```java
package com.networknt.schema;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

class JsonSchemaFactory_normalizeMetaSchemaUri_Test {

    private static Stream<Arguments> provideSuccessfulUris() {
        return Stream.of(// ----- group: normalizeMetaSchemaUri - successful conversions -----
        Arguments.of("http://example.com/schema", "https://example.com/schema"), Arguments.of("http://user:pass@example.com:8080/path?query#frag", "https://user:pass@example.com:8080/path"), Arguments.of("https://example.org", "https://example.org"), Arguments.of("http://example.com", "https://example.com"), // ----- group: scheme conversion edge cases -----
        Arguments.of("ftp://example.com/resource.txt?download=true#section", "https://example.com/resource.txt"), Arguments.of("http://[2001:db8::1]:8080/abc/def", "https://[2001:db8::1]:8080/abc/def"), Arguments.of("http://example.com:0", "https://example.com:0"), Arguments.of("https://user@example.com:443/path?query=1#frag", "https://user@example.com:443/path"), Arguments.of("http://example.com?", "https://example.com"), Arguments.of("http://example.com#fragmentOnly", "https://example.com"), Arguments.of("http://example.com:65535/path", "https://example.com:65535/path"), // ----- group: path and encoding handling -----
        Arguments.of("http://example.com/%7Euser", "https://example.com/%7Euser"), Arguments.of("http://example.com/space%20here", "https://example.com/space%20here"), Arguments.of("http://example.com/../a/b/..", "https://example.com/../a/b/.."), Arguments.of("", "https:"), Arguments.of("/relative/path", "https:/relative/path"));
    }

    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// ----- group: normalizeMetaSchemaUri - error handling -----
        Arguments.of("http://[invalid", JsonSchemaException.class, "Wrong MetaSchema URI: http://[invalid"), Arguments.of(null, NullPointerException.class, null), // ----- group: scheme conversion edge cases (error variant) -----
        Arguments.of("http://example.com:65536/path", JsonSchemaException.class, "Wrong MetaSchema URI: http://example.com:65536/path"), // ----- group: illegal argument handling -----
        Arguments.of("http://exa mple.com/path", IllegalArgumentException.class, null), Arguments.of("http://user:pa ss@example.com", IllegalArgumentException.class, null), Arguments.of("http://example.com/invalid path", IllegalArgumentException.class, null), Arguments.of("http://example.com:-1", IllegalArgumentException.class, null));
    }

    @Test
    void shouldConvertHttpToHttpsAndPreserveComponents() {
        // simple case without user info, port, query or fragment
        String input = "http://example.com/schema.json";
        String expected = "https://example.com/schema.json";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldPreserveUserInfoAndPortWhileSwitchingScheme() {
        // case with user info, port and path
        String input = "http://user:pass@example.com:8080/path/to/resource";
        String expected = "https://user:pass@example.com:8080/path/to/resource";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldStripQueryAndFragmentWhenNormalizing() {
        // case with query and fragment – they must be removed
        String input = "http://example.com:9090/path?query=123#section";
        String expected = "https://example.com:9090/path";
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual);
    }

    @Test
    void shouldThrowJsonSchemaExceptionForInvalidUri() {
        String invalidInput = "http://[invalid";
        JsonSchemaException thrown = assertThrows(JsonSchemaException.class, () -> JsonSchemaFactory.normalizeMetaSchemaUri(invalidInput));
        assertTrue(thrown.getMessage().contains("Wrong MetaSchema URI"));
        assertTrue(thrown.getMessage().contains(invalidInput));
    }

    /**
     * Verifies that valid URIs are normalised to HTTPS and that query/fragment
     * components are stripped while preserving authority, user‑info, port and
     * the raw path exactly as the implementation copies it.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) ⇒ {1}")
    @MethodSource("provideSuccessfulUris")
    void shouldNormalizeMetaSchemaUriSuccessfully(String input, String expected) {
        String actual = JsonSchemaFactory.normalizeMetaSchemaUri(input);
        assertEquals(expected, actual, () -> "Normalization failed for input: " + input);
    }

    /**
     * Verifies that malformed or illegal inputs cause the appropriate exception
     * to be thrown and that the exception message (when applicable) contains the
     * expected text.
     */
    @ParameterizedTest(name = "[{index}] normalizeMetaSchemaUri({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    void shouldThrowExpectedException(String input, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        Throwable thrown = assertThrows(expectedException, () -> JsonSchemaFactory.normalizeMetaSchemaUri(input));
        if (expectedMessageFragment != null) {
            assertTrue(thrown.getMessage().contains(expectedMessageFragment), () -> "Exception message [" + thrown.getMessage() + "] does not contain expected fragment [" + expectedMessageFragment + "]");
        }
    }
}

```

Corresponding execution feedback:
```
Picked up _JAVA_OPTIONS: -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8

╷
├─ JUnit Jupiter ✔
│  └─ JsonSchemaFactory_normalizeMetaSchemaUri_Test ✔
│     ├─ shouldThrowExpectedException(String, Class, String) ✔
│     │  ├─ [1] normalizeMetaSchemaUri(http://[invalid) throws class com.networknt.schema.Js... ✔
│     │  ├─ [2] normalizeMetaSchemaUri(null) throws class java.lang.NullPointerException ✔
│     │  ├─ [3] normalizeMetaSchemaUri(http://example.com:65536/path) throws class com.netwo... ✘ Expected com.networknt.schema.JsonSchemaException to be thrown, but nothing was thrown.
│     │  ├─ [4] normalizeMetaSchemaUri(http://exa mple.com/path) throws class java.lang.Ille... ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
│     │  ├─ [5] normalizeMetaSchemaUri(http://user:pa ss@example.com) throws class java.lang... ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
│     │  ├─ [6] normalizeMetaSchemaUri(http://example.com/invalid path) throws class java.la... ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
│     │  └─ [7] normalizeMetaSchemaUri(http://example.com:-1) throws class java.lang.Illegal... ✘ Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
│     ├─ shouldConvertHttpToHttpsAndPreserveComponents() ✔
│     ├─ shouldNormalizeMetaSchemaUriSuccessfully(String, String) ✔
│     │  ├─ [1] normalizeMetaSchemaUri(http://example.com/schema) ⇒ https://example.com/sche... ✔
│     │  ├─ [2] normalizeMetaSchemaUri(http://user:pass@example.com:8080/path?query#frag) ⇒ ... ✔
│     │  ├─ [3] normalizeMetaSchemaUri(https://example.org) ⇒ https://example.org ✔
│     │  ├─ [4] normalizeMetaSchemaUri(http://example.com) ⇒ https://example.com ✔
│     │  ├─ [5] normalizeMetaSchemaUri(ftp://example.com/resource.txt?download=true#section)... ✔
│     │  ├─ [6] normalizeMetaSchemaUri(http://[2001:db8::1]:8080/abc/def) ⇒ https://[2001:db... ✔
│     │  ├─ [7] normalizeMetaSchemaUri(http://example.com:0) ⇒ https://example.com:0 ✔
│     │  ├─ [8] normalizeMetaSchemaUri(https://user@example.com:443/path?query=1#frag) ⇒ htt... ✔
│     │  ├─ [9] normalizeMetaSchemaUri(http://example.com?) ⇒ https://example.com ✔
│     │  ├─ [10] normalizeMetaSchemaUri(http://example.com#fragmentOnly) ⇒ https://example.c... ✔
│     │  ├─ [11] normalizeMetaSchemaUri(http://example.com:65535/path) ⇒ https://example.com... ✔
│     │  ├─ [12] normalizeMetaSchemaUri(http://example.com/%7Euser) ⇒ https://example.com/%7... ✘ Normalization failed for input: http://example.com/%7Euser ==> expected: <https://example.com/%7Euser> but was: <https://example.com/~user>
│     │  ├─ [13] normalizeMetaSchemaUri(http://example.com/space%20here) ⇒ https://example.c... ✔
│     │  ├─ [14] normalizeMetaSchemaUri(http://example.com/../a/b/..) ⇒ https://example.com/... ✔
│     │  ├─ [15] normalizeMetaSchemaUri() ⇒ https: ✘ Wrong MetaSchema URI: 
│     │  └─ [16] normalizeMetaSchemaUri(/relative/path) ⇒ https:/relative/path ✔
│     ├─ shouldStripQueryAndFragmentWhenNormalizing() ✔
│     ├─ shouldThrowJsonSchemaExceptionForInvalidUri() ✔
│     └─ shouldPreserveUserInfoAndPortWhileSwitchingScheme() ✔
├─ JUnit Vintage ✔
└─ JUnit Platform Suite ✔

Failures (7):
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldThrowExpectedException(String, Class, String):[3] normalizeMetaSchemaUri(http://example.com:65536/path) throws class com.networknt.schema.JsonSchemaException
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldThrowExpectedException', methodParameterTypes = 'java.lang.String, java.lang.Class, java.lang.String']
    => org.opentest4j.AssertionFailedError: Expected com.networknt.schema.JsonSchemaException to be thrown, but nothing was thrown.
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:152)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:73)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldThrowExpectedException(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldThrowExpectedException(String, Class, String):[4] normalizeMetaSchemaUri(http://exa mple.com/path) throws class java.lang.IllegalArgumentException
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldThrowExpectedException', methodParameterTypes = 'java.lang.String, java.lang.Class, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldThrowExpectedException(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: http://exa mple.com/path
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.lambda$shouldThrowExpectedException$2(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldThrowExpectedException(String, Class, String):[5] normalizeMetaSchemaUri(http://user:pa ss@example.com) throws class java.lang.IllegalArgumentException
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldThrowExpectedException', methodParameterTypes = 'java.lang.String, java.lang.Class, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldThrowExpectedException(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: http://user:pa ss@example.com
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.lambda$shouldThrowExpectedException$2(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldThrowExpectedException(String, Class, String):[6] normalizeMetaSchemaUri(http://example.com/invalid path) throws class java.lang.IllegalArgumentException
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldThrowExpectedException', methodParameterTypes = 'java.lang.String, java.lang.Class, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldThrowExpectedException(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: http://example.com/invalid path
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.lambda$shouldThrowExpectedException$2(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldThrowExpectedException(String, Class, String):[7] normalizeMetaSchemaUri(http://example.com:-1) throws class java.lang.IllegalArgumentException
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldThrowExpectedException', methodParameterTypes = 'java.lang.String, java.lang.Class, java.lang.String']
    => org.opentest4j.AssertionFailedError: Unexpected exception type thrown, expected: <java.lang.IllegalArgumentException> but was: <com.networknt.schema.JsonSchemaException>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:67)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:35)
       org.junit.jupiter.api.Assertions.assertThrows(Assertions.java:3083)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldThrowExpectedException(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       [...]
     Caused by: com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: http://example.com:-1
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.lambda$shouldThrowExpectedException$2(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:81)
       org.junit.jupiter.api.AssertThrows.assertThrows(AssertThrows.java:53)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldNormalizeMetaSchemaUriSuccessfully(String, String):[12] normalizeMetaSchemaUri(http://example.com/%7Euser) ⇒ https://example.com/%7Euser
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldNormalizeMetaSchemaUriSuccessfully', methodParameterTypes = 'java.lang.String, java.lang.String']
    => org.opentest4j.AssertionFailedError: Normalization failed for input: http://example.com/%7Euser ==> expected: <https://example.com/%7Euser> but was: <https://example.com/~user>
       org.junit.jupiter.api.AssertionFailureBuilder.build(AssertionFailureBuilder.java:151)
       org.junit.jupiter.api.AssertionFailureBuilder.buildAndThrow(AssertionFailureBuilder.java:132)
       org.junit.jupiter.api.AssertEquals.failNotEqual(AssertEquals.java:197)
       org.junit.jupiter.api.AssertEquals.assertEquals(AssertEquals.java:188)
       org.junit.jupiter.api.Assertions.assertEquals(Assertions.java:1164)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldNormalizeMetaSchemaUriSuccessfully(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:70)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       [...]
  JUnit Jupiter:JsonSchemaFactory_normalizeMetaSchemaUri_Test:shouldNormalizeMetaSchemaUriSuccessfully(String, String):[15] normalizeMetaSchemaUri() ⇒ https:
    MethodSource [className = 'com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test', methodName = 'shouldNormalizeMetaSchemaUriSuccessfully', methodParameterTypes = 'java.lang.String, java.lang.String']
    => com.networknt.schema.JsonSchemaException: Wrong MetaSchema URI: 
       com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(JsonSchemaFactory.java:514)
       com.networknt.schema.JsonSchemaFactory_normalizeMetaSchemaUri_Test.shouldNormalizeMetaSchemaUriSuccessfully(JsonSchemaFactory_normalizeMetaSchemaUri_Test.java:69)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
       java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77)
       java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
       java.base/java.lang.reflect.Method.invoke(Method.java:569)
       org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
       org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
       org.junit.jupiter.engine.execution.InvocationInterceptorChain$ValidatingInvocation.proceed(InvocationInterceptorChain.java:131)
       org.junit.jupiter.engine.extension.TimeoutExtension.intercept(TimeoutExtension.java:156)
       [...]

Test run finished after 82 ms
[         6 containers found      ]
[         0 containers skipped    ]
[         6 containers started    ]
[         0 containers aborted    ]
[         6 containers successful ]
[         0 containers failed     ]
[        27 tests found           ]
[         0 tests skipped         ]
[        27 tests started         ]
[         0 tests aborted         ]
[        20 tests successful      ]
[         7 tests failed          ]


```


And you can use the following related information to assist you in your analysis and correction:

dependent classes: 
class `com.networknt.schema.JsonSchemaFactory`:
	related functions:
		method `private JsonMetaSchema fromId(String)` returns `com.networknt.schema.JsonMetaSchema`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
		method `protected static String normalizeMetaSchemaUri(String)` returns `java.lang.String`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
		method `private boolean idMatchesSourceUri(JsonMetaSchema, JsonNode, URI)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.format.AbstractRFC3986Format`:
	related functions:
		method `public final boolean matches(String)` returns `boolean`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`;
class `com.networknt.schema.SpecVersionDetector`:
	related functions:
		method `public static VersionFlag detect(JsonNode)` returns `com.networknt.schema.SpecVersion.VersionFlag`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, throws
     * {@link JsonSchemaException} with the corresponding message, otherwise - returns the detected spec version.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise throws an exception
     ;
		method `public static Optional<VersionFlag> detectOptionalVersion(JsonNode)` returns `java.util.Optional<com.networknt.schema.SpecVersion.VersionFlag>`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Detects schema version based on the schema tag: if the schema tag is not present, returns an empty {@link
     * Optional} value, otherwise - returns the detected spec version wrapped into {@link Optional}.
     *
     * @param jsonNode JSON Node to read from
     * @return Spec version if present, otherwise empty
     ;
class `com.networknt.schema.JsonSchema`:
	related functions:
		method `private void updateThisAsSubschema(URI)` returns `void`, related with `com.networknt.schema.JsonSchemaFactory.normalizeMetaSchemaUri(String)`, api document: 
     * Creates a new parent schema from the current state and updates this object to refer to the subschema instead.
     ;



Now you should output the corrected Java code wrapped in a Markdown code block ```java at the beginning and ``` at the end: 
