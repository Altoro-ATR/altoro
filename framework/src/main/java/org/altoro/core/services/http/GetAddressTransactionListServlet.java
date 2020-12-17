package org.altoro.core.services.http;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.Commons;
import org.altoro.common.utils.JsonUtil;
import org.altoro.core.Wallet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Objects;


@Component
@Slf4j(topic = "API")
public class GetAddressTransactionListServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      boolean visible = Util.getVisible(request);
      String address = request.getParameter("address");
      List<org.altoro.common.entity.TransactionInfo> addressTransactionList =
              wallet.getAddressTransactionList(ByteString.copyFrom(
                      Objects.requireNonNull(Commons.decodeFromBase58Check(address))
              ));
      if (addressTransactionList != null) {
        response.getWriter().println(JsonUtil.obj2Json(addressTransactionList));
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request,response);
  }

}