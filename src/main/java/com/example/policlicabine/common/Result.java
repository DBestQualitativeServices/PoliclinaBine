package com.example.policlicabine.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A Result monad for representing the outcome of operations that may succeed or fail.
 * <p>
 * This class provides a functional approach to error handling without exceptions,
 * supporting both single error messages and multiple validation errors.
 * </p>
 * <p>Example usage:</p>
 * <pre>
 * Result&lt;User&gt; result = userService.findById(id);
 * return result.map(User::getName)
 *              .map(String::toUpperCase)
 *              .toOptional()
 *              .orElse("Unknown");
 * </pre>
 *
 * @param <T> the type of the success value
 */
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Schema(description = "Standard API response wrapper for all operations")
public class Result<T> {

    @Schema(description = "Indicates if the operation was successful", example = "true")
    private boolean success;

    @Schema(description = "The response data (null if operation failed)")
    private T value;

    @Schema(description = "Error message if operation failed (null if successful)", example = "User not found")
    private String errorMessage;

    @Schema(description = "List of validation errors if operation failed (null if successful)")
    private List<String> errors;

    public static <T> Result<T> success(T value) {
        return new Result<T>(true, value, null, null);
    }

    public static Result<Void> success() {
        return new Result<Void>(true, null, null, null);
    }

    public static <T> Result<T> failure(String errorMessage) {
        return new Result<T>(false, null, errorMessage, null);
    }

    public static <T> Result<T> failure(List<String> errors) {
        return new Result<T>(false, null, null, errors);
    }

    public static <T> Result<T> failure(String errorMessage, List<String> errors) {
        return new Result<T>(false, null, errorMessage, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isFailure() {
        return !success;
    }

    public T getValue() {
        if (!success) {
            throw new IllegalStateException("Cannot get value from failed result");
        }
        return value;
    }

    public List<String> getErrors() {
        return errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    // Optional-style methods
    public Optional<T> toOptional() {
        return isSuccess() ? Optional.ofNullable(value) : Optional.empty();
    }

    // Functional methods
    public <U> Result<U> map(Function<T, U> mapper) {
        if (isFailure()) {
            return Result.failure(errorMessage, errors);
        }
        try {
            return Result.success(mapper.apply(value));
        } catch (Exception e) {
            return Result.failure("Mapping failed: " + e.getMessage());
        }
    }

    public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        if (isFailure()) {
            return Result.failure(errorMessage, errors);
        }
        try {
            return mapper.apply(value);
        } catch (Exception e) {
            return Result.failure("FlatMap failed: " + e.getMessage());
        }
    }

}