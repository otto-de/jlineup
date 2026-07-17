package de.otto.jlineup;

import de.otto.jlineup.browser.JLineupException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    @Test
    void shouldExtractErrorMessageFromLambdaJson() {
        String lambdaResponse = "{\"errorMessage\":\"HTTP check failed for URL: https://example.com\",\"errorType\":\"RuntimeException\"}";
        
        String result = Utils.extractLambdaErrorMessage(lambdaResponse);
        
        assertEquals("HTTP check failed for URL: https://example.com", result);
    }

    @Test
    void shouldExtractErrorMessageFromLambdaJsonWithSpaces() {
        String lambdaResponse = "{\"errorMessage\": \"Connection timeout\", \"errorType\": \"RuntimeException\"}";
        
        String result = Utils.extractLambdaErrorMessage(lambdaResponse);
        
        assertEquals("Connection timeout", result);
    }

    @Test
    void shouldReturnNullWhenNoErrorMessageInJson() {
        String lambdaResponse = "{\"result\":\"success\"}";
        
        String result = Utils.extractLambdaErrorMessage(lambdaResponse);
        
        assertNull(result);
    }

    @Test
    void shouldReturnNullForNullInput() {
        assertNull(Utils.extractLambdaErrorMessage(null));
    }

    @Test
    void shouldExtractJLineupExceptionMessage() {
        String text = "java.lang.RuntimeException: de.otto.jlineup.browser.JLineupException: HTTP check failed - Expected status 200 but got 404";
        
        String result = Utils.extractJLineupExceptionMessage(text);
        
        assertEquals("HTTP check failed - Expected status 200 but got 404", result);
    }

    @Test
    void shouldExtractJLineupExceptionMessageFromNestedLambdaError() {
        String lambdaResponse = "{\"errorMessage\":\"java.lang.RuntimeException: de.otto.jlineup.browser.JLineupException: HTTP check failed for URL https://example.com - Expected status code 200, but got 503\",\"errorType\":\"RuntimeException\"}";
        
        String extractedError = Utils.extractLambdaErrorMessage(lambdaResponse);
        String result = Utils.extractJLineupExceptionMessage(extractedError);
        
        assertEquals("HTTP check failed for URL https://example.com - Expected status code 200, but got 503", result);
    }

    @Test
    void shouldReturnNullWhenNoJLineupException() {
        String text = "Some other error message";
        
        String result = Utils.extractJLineupExceptionMessage(text);
        
        assertNull(result);
    }

    @Test
    void shouldExtractUserFriendlyMessageFromJLineupException() {
        JLineupException jlineupException = new JLineupException("HTTP check failed - Expected status 200 but got 404");
        RuntimeException wrapper = new RuntimeException(jlineupException);
        
        String result = Utils.extractUserFriendlyErrorMessage(wrapper);
        
        assertEquals("HTTP check failed - Expected status 200 but got 404", result);
    }

    @Test
    void shouldExtractUserFriendlyMessageFromLambdaJsonResponse() {
        String lambdaJson = "{\"errorMessage\":\"Task timed out after 30 seconds\",\"errorType\":\"TimeoutException\"}";
        RuntimeException exception = new RuntimeException(lambdaJson);
        
        String result = Utils.extractUserFriendlyErrorMessage(exception);
        
        assertEquals("Task timed out after 30 seconds", result);
    }

    @Test
    void shouldExtractNestedJLineupExceptionFromLambdaResponse() {
        String lambdaJson = "{\"errorMessage\":\"java.lang.RuntimeException: de.otto.jlineup.browser.JLineupException: HTTP check failed for URL https://example.com - Expected status code 200, but got 503\",\"errorType\":\"RuntimeException\"}";
        RuntimeException exception = new RuntimeException(lambdaJson);
        
        String result = Utils.extractUserFriendlyErrorMessage(exception);
        
        assertEquals("HTTP check failed for URL https://example.com - Expected status code 200, but got 503", result);
    }

    @Test
    void shouldReturnSimpleMessageWhenNoSpecialFormat() {
        RuntimeException exception = new RuntimeException("Simple error message");
        
        String result = Utils.extractUserFriendlyErrorMessage(exception);
        
        assertEquals("Simple error message", result);
    }

    @Test
    void shouldReturnUnknownErrorForNullException() {
        String result = Utils.extractUserFriendlyErrorMessage(null);
        
        assertEquals("Unknown error", result);
    }

    @Test
    void shouldReturnClassNameWhenMessageIsNull() {
        RuntimeException exception = new RuntimeException((String) null);
        
        String result = Utils.extractUserFriendlyErrorMessage(exception);
        
        assertEquals("RuntimeException", result);
    }

    @Test
    void shouldGetRootCause() {
        Exception root = new IllegalArgumentException("root cause");
        Exception middle = new RuntimeException("middle", root);
        Exception top = new Exception("top", middle);
        
        Throwable result = Utils.getRootCause(top);
        
        assertSame(root, result);
    }

    @Test
    void shouldReturnSameExceptionWhenNoCause() {
        Exception exception = new RuntimeException("no cause");
        
        Throwable result = Utils.getRootCause(exception);
        
        assertSame(exception, result);
    }

    @Test
    void shouldReturnNullForNullRootCause() {
        assertNull(Utils.getRootCause(null));
    }
}
