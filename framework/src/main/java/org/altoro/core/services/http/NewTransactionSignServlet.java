package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.altoro.core.Wallet;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.protos.Protocol.Transaction;
import org.altoro.protos.Protocol.TransactionSign;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


/**
 * @author : little liu
 */
@Component
@Slf4j(topic = "API")
public class NewTransactionSignServlet extends RateLimiterServlet {

    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            JSONObject input = JSONObject.parseObject(contract);

            if (!input.containsKey("password")
                    ||!StringUtils.isEmpty(input.getString("password"))) {
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }

            if (!input.containsKey("address")
                    || StringUtil.isNullOrEmpty(input.getString("address"))) {
                response.getWriter().println(Util.printErrorMsgString("address isn't set."));
                return;
            }

            String password = input.getString("password");
            String address = input.getString("address");

            boolean visible = Util.getVisibleOnlyForSign(input);
            String strTransaction = input.getJSONObject("transaction").toJSONString();
            Transaction transaction = Util.packTransaction(strTransaction, visible);
            JSONObject jsonTransaction = JSONObject.parseObject(JsonFormat.printToString(transaction, visible));
            input.put("transaction", jsonTransaction);

            AccountKeyStore accountKeyStore = GenerateWalletUtil.loadAccountKeyStore(password, address);
            if(accountKeyStore == null){
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }

            input.put("privateKey",accountKeyStore.getPriKey());

            TransactionSign.Builder build = TransactionSign.newBuilder();
            JsonFormat.merge(input.toJSONString(), build, visible);
            TransactionCapsule reply = wallet.getTransactionSign(build.build());
            if (reply != null) {
                response.getWriter().println(Util.printCreateTransaction(reply.getInstance(), visible));
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}
