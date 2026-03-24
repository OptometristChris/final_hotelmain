package com.spring.app.common.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("imagePathUtil")
public class ImagePathUtil {

    private String clean(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

        String value = raw.trim().replace('\', '/');

        if (value.startsWith("/final_hotel")) {
            value = value.substring("/final_hotel".length());
        }
        else if (value.startsWith("final_hotel/")) {
            value = value.substring("final_hotel".length());
        }

        if (value.startsWith("file_images/") || value.startsWith("images/")) {
            value = "/" + value;
        }

        return value;
    }

    private boolean isHttp(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String buildPrimary(String raw, String folder) {
        String value = clean(raw);

        if (!StringUtils.hasText(value)) {
            return "";
        }
        if (isHttp(value)) {
            return value;
        }
        if (value.startsWith("/file_images/") || value.startsWith("/images/")) {
            return value;
        }
        if (value.startsWith("/")) {
            return value;
        }

        return "/file_images/" + folder + "/" + value;
    }

    public String hotel(String raw) {
        return buildPrimary(raw, "hotel");
    }

    public String room(String raw) {
        return buildPrimary(raw, "room");
    }

    public String promo(String raw) {
        return buildPrimary(raw, "js");
    }

    public String promoAlt(String raw) {
        String value = clean(raw);

        if (!StringUtils.hasText(value) || isHttp(value)) {
            return "";
        }
        if (value.startsWith("/file_images/js/")) {
            return value.replaceFirst("^/file_images/js/", "/images/js/");
        }
        if (value.startsWith("/images/js/")) {
            return value.replaceFirst("^/images/js/", "/file_images/js/");
        }
        if (value.startsWith("/")) {
            return value;
        }

        return "/images/js/" + value;
    }
}
