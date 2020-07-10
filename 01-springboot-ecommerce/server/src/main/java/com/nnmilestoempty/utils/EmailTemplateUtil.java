package com.nnmilestoempty.utils;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class EmailTemplateUtil {
    private static final EmailTemplateUtil INSTANCE = new EmailTemplateUtil();

    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateUtil.class);

    public String loadEmailTemplate(String templateName, Map<String, String> values) {
        InputStream is = getInstance().getClass().getResourceAsStream(templateName);
        String result = null;
        try {
            result = IOUtils.toString(is, Charset.defaultCharset());
        } catch (IOException e) {
            logger.error("Error loading email template {}", templateName, e);
        }

        if (result != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                result = result.replace("${" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "<Missing>");
            }
        }

        return result;
    }

    public static EmailTemplateUtil getInstance() {
        return INSTANCE;
    }
}
