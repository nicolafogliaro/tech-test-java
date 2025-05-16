package com.nicolafogliaro.orderservice.api.util;

import java.util.Collection;
import org.springframework.lang.Nullable;

public final class MyCollectionUtils {

  public static boolean isEmpty(@Nullable Collection<?> collection) {
    return org.springframework.util.CollectionUtils.isEmpty(collection);
  }

  public static boolean nonEmpty(@Nullable Collection<?> collection) {
    return !isEmpty(collection);
  }

  private MyCollectionUtils() {}
}
