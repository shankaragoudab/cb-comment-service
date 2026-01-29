package com.tarento.commenthub.utility;

import java.util.List;

public class CommentsUtility {

  private CommentsUtility() {
    // Private constructor to prevent instantiation
  }

  public static boolean containsNull(List<?> list) {
    if (list == null) {
      return true;
    }

    for (Object element : list) {
      if (element == null) {
        return true;
      }
    }

    return false;
  }
}
