package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.Wallet;
import org.altoro.protos.Protocol.Transaction;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.altoro.protos.contract.MarketContract;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class MarketSellAssetServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    try {
      String contract = request.getReader().lines()
          .collect(Collectors.joining(System.lineSeparator()));
      Util.checkBodySize(contract);

      boolean visible = Util.getVisiblePost(contract);
      MarketContract.MarketSellAssetContract.Builder build = MarketContract.MarketSellAssetContract.newBuilder();
      JsonFormat.merge(contract, build, visible);

      Transaction tx = wallet
          .createTransactionCapsule(build.build(), ContractType.MarketSellAssetContract)
          .getInstance();

      JSONObject jsonObject = JSONObject.parseObject(contract);
      tx = Util.setTransactionPermissionId(jsonObject, tx);
      tx = Util.setTransactionExtraData(jsonObject, tx, visible);

      response.getWriter().println(Util.printCreateTransaction(tx, visible));
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }
}
