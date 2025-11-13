package com.example.policlicabine.util;

import com.example.policlicabine.common.Result;
import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.function.Consumer;

/**
 * Custom AssertJ assertion class for Result&lt;T&gt; monad.
 * <p>
 * Provides fluent API for testing Result objects with clear, readable assertions.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Result&lt;User&gt; result = userService.findById(id);
 * ResultAssertions.assertThat(result)
 *     .isSuccess()
 *     .hasValue()
 *     .extractingValue()
 *     .satisfies(user -&gt; {
 *         assertThat(user.getName()).isEqualTo("John");
 *     });
 * </pre>
 *
 * @param <T> the type of the Result value
 */
public class ResultAssert<T> extends AbstractAssert<ResultAssert<T>, Result<T>> {

    protected ResultAssert(Result<T> actual) {
        super(actual, ResultAssert.class);
    }

    /**
     * Asserts that the Result is a success.
     *
     * @return this assertion object
     */
    public ResultAssert<T> isSuccess() {
        isNotNull();
        if (!actual.isSuccess()) {
            failWithMessage("Expected result to be successful but it failed with error: <%s> and errors: <%s>",
                    actual.getErrorMessage(), actual.getErrors());
        }
        return this;
    }

    /**
     * Asserts that the Result is a failure.
     *
     * @return this assertion object
     */
    public ResultAssert<T> isFailure() {
        isNotNull();
        if (!actual.isFailure()) {
            failWithMessage("Expected result to be failed but it succeeded with value: <%s>", actual.getValue());
        }
        return this;
    }

    /**
     * Asserts that the Result has a non-null value.
     * Note: This does not check if the Result is successful.
     *
     * @return this assertion object
     */
    public ResultAssert<T> hasValue() {
        isSuccess();
        if (actual.getValue() == null) {
            failWithMessage("Expected result to have a non-null value but it was null");
        }
        return this;
    }

    /**
     * Asserts that the Result has a null value (success with null).
     *
     * @return this assertion object
     */
    public ResultAssert<T> hasNullValue() {
        isSuccess();
        if (actual.getValue() != null) {
            failWithMessage("Expected result to have a null value but it was: <%s>", actual.getValue());
        }
        return this;
    }

    /**
     * Asserts that the Result value is equal to the expected value.
     *
     * @param expectedValue the expected value
     * @return this assertion object
     */
    public ResultAssert<T> hasValue(T expectedValue) {
        isSuccess();
        if (!actual.getValue().equals(expectedValue)) {
            failWithMessage("Expected result value to be <%s> but was <%s>",
                    expectedValue, actual.getValue());
        }
        return this;
    }

    /**
     * Asserts that the Result contains the specified error message.
     *
     * @param expectedErrorMessage the expected error message
     * @return this assertion object
     */
    public ResultAssert<T> hasErrorMessage(String expectedErrorMessage) {
        isFailure();
        if (actual.getErrorMessage() == null) {
            failWithMessage("Expected error message to be <%s> but it was null", expectedErrorMessage);
        }
        if (!actual.getErrorMessage().equals(expectedErrorMessage)) {
            failWithMessage("Expected error message to be <%s> but was <%s>",
                    expectedErrorMessage, actual.getErrorMessage());
        }
        return this;
    }

    /**
     * Asserts that the Result error message contains the specified substring.
     *
     * @param substring the substring to look for
     * @return this assertion object
     */
    public ResultAssert<T> hasErrorMessageContaining(String substring) {
        isFailure();
        if (actual.getErrorMessage() == null || !actual.getErrorMessage().contains(substring)) {
            failWithMessage("Expected error message to contain <%s> but it was <%s>",
                    substring, actual.getErrorMessage());
        }
        return this;
    }

    /**
     * Asserts that the Result has the specified errors list.
     *
     * @param expectedErrors the expected errors
     * @return this assertion object
     */
    public ResultAssert<T> hasErrors(List<String> expectedErrors) {
        isFailure();
        if (!actual.getErrors().equals(expectedErrors)) {
            failWithMessage("Expected errors to be <%s> but was <%s>",
                    expectedErrors, actual.getErrors());
        }
        return this;
    }

    /**
     * Asserts that the Result errors list contains the specified error.
     *
     * @param error the error to look for
     * @return this assertion object
     */
    public ResultAssert<T> hasErrorContaining(String error) {
        isFailure();
        if (actual.getErrors() == null || !actual.getErrors().contains(error)) {
            failWithMessage("Expected errors to contain <%s> but they were <%s>",
                    error, actual.getErrors());
        }
        return this;
    }

    /**
     * Asserts that the Result has exactly the specified number of errors.
     *
     * @param expectedCount the expected error count
     * @return this assertion object
     */
    public ResultAssert<T> hasErrorCount(int expectedCount) {
        isFailure();
        int actualCount = actual.getErrors() != null ? actual.getErrors().size() : 0;
        if (actualCount != expectedCount) {
            failWithMessage("Expected error count to be <%d> but was <%d>",
                    expectedCount, actualCount);
        }
        return this;
    }

    /**
     * Extracts the value for further assertions.
     * Fails if the Result is not successful or value is null.
     *
     * @return the extracted value
     */
    public T extractingValue() {
        hasValue();
        return actual.getValue();
    }

    /**
     * Allows custom assertions on the Result value.
     *
     * @param valueConsumer consumer that performs assertions on the value
     * @return this assertion object
     */
    public ResultAssert<T> hasValueSatisfying(Consumer<T> valueConsumer) {
        hasValue();
        valueConsumer.accept(actual.getValue());
        return this;
    }
}
