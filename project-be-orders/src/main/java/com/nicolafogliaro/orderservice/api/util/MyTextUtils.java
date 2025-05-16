package com.nicolafogliaro.orderservice.api.util;

import org.springframework.util.StringUtils;

public final class MyTextUtils {

  public static boolean isEmpty(String s) {
    return !nonEmpty(s);
  }

  public static boolean nonEmpty(String s) {
    return StringUtils.hasText(s);
  }

  public static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private MyTextUtils() {
    throw new UnsupportedOperationException();
  }
}
