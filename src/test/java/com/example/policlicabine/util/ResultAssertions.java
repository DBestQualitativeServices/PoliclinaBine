package com.example.policlicabine.util;

import com.example.policlicabine.common.Result;

/**
 * Entry point for Result&lt;T&gt; assertions using a fluent API.
 * <p>
 * This class provides static factory methods to create ResultAssert instances,
 * enabling readable and type-safe assertions on Result objects.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * import static com.example.policlicabine.util.ResultAssertions.assertThat;
 *
 * Result&lt;User&gt; result = userService.createUser(dto);
 *
 * // Success assertions
 * assertThat(result).isSuccess().hasValue();
 * assertThat(result).isSuccess().hasValue(expectedUser);
 * assertThat(result).isSuccess().extractingValue()
 *     .satisfies(user -&gt; {
 *         assertThat(user.getEmail()).isEqualTo("test@example.com");
 *     });
 *
 * // Failure assertions
 * assertThat(result).isFailure().hasErrorMessage("User already exists");
 * assertThat(result).isFailure().hasErrorMessageContaining("already exists");
 * assertThat(result).isFailure().hasErrorCount(3);
 * </pre>
 */
public class ResultAssertions {

    /**
     * Creates a new ResultAssert instance for the given Result.
     *
     * @param actual the Result to assert on
     * @param <T>    the type of the Result value
     * @return a new ResultAssert instance
     */
    public static <T> ResultAssert<T> assertThat(Result<T> actual) {
        return new ResultAssert<>(actual);
    }

    // Private constructor to prevent instantiation
    private ResultAssertions() {
        throw new AssertionError("ResultAssertions is a utility class and should not be instantiated");
    }
}
