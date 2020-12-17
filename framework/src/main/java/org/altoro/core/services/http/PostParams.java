package org.altoro.core.services.http;

import lombok.Getter;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

public class PostParams {

  public static final String S_VALUE = "value";

  @Getter
  private String params;
  @Getter
  private boolean visible;

  public PostParams(String params, boolean visible) {
    this.params = params;
    this.visible = visible;
  }

  public static org.altoro.core.services.http.PostParams getPostParams(HttpServletRequest request) throws Exception {
    String input = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
    Util.checkBodySize(input);
    boolean visible = Util.getVisiblePost(input);
    return new org.altoro.core.services.http.PostParams(input, visible);
  }
}
