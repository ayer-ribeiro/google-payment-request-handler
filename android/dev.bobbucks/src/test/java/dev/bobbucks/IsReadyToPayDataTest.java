package dev.bobbucks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class IsReadyToPayDataTest {
    @Test
    public void parse_recoversStringFalseAndTestField() {
        IsReadyToPayData.Result result =
                IsReadyToPayData.parse("{\"testField\":\"test value\",\"returnValue\":\"false\"}");

        assertFalse(result.returnValue);
        assertEquals("test value", result.testField);
        assertEquals("false", result.suppliedReturnValue);
        assertEquals("recovered", result.status);
    }

    @Test
    public void parse_recoversBooleanTrue() {
        IsReadyToPayData.Result result = IsReadyToPayData.parse("{\"returnValue\":true}");

        assertTrue(result.returnValue);
        assertEquals("true", result.suppliedReturnValue);
        assertEquals("recovered", result.status);
    }

    @Test
    public void parse_malformedJsonUsesSafeDefault() {
        IsReadyToPayData.Result result = IsReadyToPayData.parse("not JSON");

        assertTrue(result.returnValue);
        assertNull(result.testField);
        assertEquals("malformed_json", result.status);
    }

    @Test
    public void parse_missingMethodDataUsesSafeDefault() {
        IsReadyToPayData.Result result = IsReadyToPayData.parse(null);

        assertTrue(result.returnValue);
        assertEquals("missing_method_data", result.status);
    }

    @Test
    public void parse_missingReturnValueUsesSafeDefaultButRecoversTestField() {
        IsReadyToPayData.Result result =
                IsReadyToPayData.parse("{\"testField\":\"test value\"}");

        assertTrue(result.returnValue);
        assertEquals("test value", result.testField);
        assertNull(result.suppliedReturnValue);
        assertEquals("missing_return_value", result.status);
    }

    @Test
    public void parse_invalidReturnValueUsesSafeDefaultButRecoversFields() {
        IsReadyToPayData.Result result =
                IsReadyToPayData.parse("{\"testField\":\"test value\",\"returnValue\":\"maybe\"}");

        assertTrue(result.returnValue);
        assertEquals("test value", result.testField);
        assertEquals("maybe", result.suppliedReturnValue);
        assertEquals("invalid_return_value", result.status);
    }

    @Test
    public void methodNames_exactCurrentIdentifierIsAllowed() {
        assertTrue(IsReadyToPayData.methodNamesAllowCurrentMethod(
                Arrays.asList("unrelated", IsReadyToPayData.METHOD_IDENTIFIER)));
        assertTrue(IsReadyToPayData.methodNamesAllowCurrentMethod(
                new String[] {IsReadyToPayData.METHOD_IDENTIFIER}));
        assertTrue(IsReadyToPayData.methodNamesAllowCurrentMethod(
                IsReadyToPayData.METHOD_IDENTIFIER));
    }

    @Test
    public void methodNames_staleOrEmptyRecognizedValuesAreRejected() {
        assertFalse(IsReadyToPayData.methodNamesAllowCurrentMethod(
                Collections.singletonList("https://bobbucks.dev/pay")));
        assertFalse(IsReadyToPayData.methodNamesAllowCurrentMethod(Collections.emptyList()));
    }

    @Test
    public void methodNames_missingOrUnknownValueAllowsExactKeyFallback() {
        assertTrue(IsReadyToPayData.methodNamesAllowCurrentMethod(null));
        assertTrue(IsReadyToPayData.methodNamesAllowCurrentMethod(42));
    }
}
