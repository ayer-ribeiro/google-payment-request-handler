package dev.bobbucks;

import org.json.JSONException;
import org.json.JSONObject;

final class IsReadyToPayData {
    static final String METHOD_IDENTIFIER =
            "https://pay-nine-tan.vercel.app/payment-method";
    static final String LOG_PREFIX = "IRTP_METHOD_DATA";
    static final boolean DEFAULT_RETURN_VALUE = true;

    private IsReadyToPayData() {}

    static boolean methodNamesAllowCurrentMethod(Object methodNames) {
        if (methodNames == null) {
            return true;
        }

        if (methodNames instanceof String) {
            return METHOD_IDENTIFIER.equals(methodNames);
        }

        if (methodNames instanceof Object[]) {
            for (Object methodName : (Object[]) methodNames) {
                if (METHOD_IDENTIFIER.equals(methodName)) {
                    return true;
                }
            }
            return false;
        }

        if (methodNames instanceof Iterable<?>) {
            for (Object methodName : (Iterable<?>) methodNames) {
                if (METHOD_IDENTIFIER.equals(methodName)) {
                    return true;
                }
            }
            return false;
        }

        // An unfamiliar representation should not prevent exact-key fallback in methodData.
        return true;
    }

    static Result parse(String json) {
        if (json == null) {
            return new Result(DEFAULT_RETURN_VALUE, null, null, "missing_method_data");
        }

        final JSONObject data;
        try {
            data = new JSONObject(json);
        } catch (JSONException e) {
            return new Result(DEFAULT_RETURN_VALUE, null, null, "malformed_json");
        }

        String testField = valueAsString(data.opt("testField"));
        Object rawReturnValue = data.opt("returnValue");
        String suppliedReturnValue = valueAsString(rawReturnValue);
        Boolean parsedReturnValue = parseBoolean(rawReturnValue);
        if (parsedReturnValue == null) {
            String status = rawReturnValue == null || rawReturnValue == JSONObject.NULL
                    ? "missing_return_value"
                    : "invalid_return_value";
            return new Result(
                    DEFAULT_RETURN_VALUE, testField, suppliedReturnValue, status);
        }

        return new Result(parsedReturnValue, testField, suppliedReturnValue, "recovered");
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            if ("true".equalsIgnoreCase((String) value)) {
                return true;
            }
            if ("false".equalsIgnoreCase((String) value)) {
                return false;
            }
        }
        return null;
    }

    private static String valueAsString(Object value) {
        return value == null || value == JSONObject.NULL ? null : String.valueOf(value);
    }

    static final class Result {
        final boolean returnValue;
        final String testField;
        final String suppliedReturnValue;
        final String status;

        Result(
                boolean returnValue,
                String testField,
                String suppliedReturnValue,
                String status) {
            this.returnValue = returnValue;
            this.testField = testField;
            this.suppliedReturnValue = suppliedReturnValue;
            this.status = status;
        }
    }
}
