package com.nnmilestoempty.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ResponseUtils {
    public static Map<String, Object> createErrorsMap(String defaultMessage, String objectName, String field) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("defaultMessage", defaultMessage);
        result.put("objectName", objectName);
        result.put("field", field);
        return Collections.singletonMap("errors", result);
    }

    public static Map<String, Object> createErrorsMap(String defaultMessage) {
        HashMap<String, Object> result = new HashMap<>();
        result.put("defaultMessage", defaultMessage);
        return Collections.singletonMap("errors", result);
    }
}
