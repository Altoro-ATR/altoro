package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.altoro.api.GrpcAPI.BytesMessage;
import org.altoro.common.utils.ByteArray;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

import static org.altoro.core.services.http.PostParams.S_VALUE;


@Component
@Slf4j(topic = "API")
public class CreateAddressServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String input = request.getParameter(S_VALUE);
      if (visible) {
        input = Util.getHexString(input);
      }
      JSONObject jsonObject = new JSONObject();
      jsonObject.put(S_VALUE, input);
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(jsonObject.toJSONString(), build, visible);
      fillResponse(build.getValue(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String input = request.getReader().lines()
              .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(input);
      boolean visible = Util.getVisiblePost(input);
      if (visible) {
        input = covertStringToHex(input);
      }
      BytesMessage.Builder build = BytesMessage.newBuilder();
      JsonFormat.merge(input, build, visible);
      fillResponse(build.getValue(), response);
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  private String covertStringToHex(String input) {
    JSONObject jsonObject = JSONObject.parseObject(input);
    String value = jsonObject.getString(S_VALUE);
    jsonObject.put(S_VALUE, Util.getHexString(value));
    return jsonObject.toJSONString();
  }

  private void fillResponse(ByteString value, HttpServletResponse response) throws IOException {
    byte[] address = wallet.createAddress(value.toByteArray());
    String base58check = StringUtil.encode58Check(address);
    String hexString = ByteArray.toHexString(address);
    JSONObject jsonAddress = new JSONObject();
    jsonAddress.put("base58checkAddress", base58check);
    jsonAddress.put(S_VALUE, hexString);
    response.getWriter().println(jsonAddress.toJSONString());
  }
}
