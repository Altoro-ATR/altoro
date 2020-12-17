package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.altoro.api.GrpcAPI;
import org.altoro.common.utils.ByteArray;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.Wallet;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.contract.WitnessContract.DelegateContract;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class DelegateAccountServlet extends RateLimiterServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            JSONObject input = JSONObject.parseObject(contract);
            String password = input.getString("password");

            boolean visible = Util.getVisiblePost(contract);
            DelegateContract.Builder build = DelegateContract.newBuilder();
            JsonFormat.merge(contract, build, visible);
            TransactionCapsule transactionCapsule = wallet.createTransactionCapsule(build.build(), ContractType.DelegateContract);

            if (StringUtils.isBlank(password)) {
                Protocol.Transaction tx = transactionCapsule.getInstance();
                JSONObject jsonObject = JSONObject.parseObject(contract);
                tx = Util.setTransactionPermissionId(jsonObject, tx);
                response.getWriter().println(Util.printCreateTransaction(tx, visible));
                return;
            }

            AccountKeyStore accountKeyStore = GenerateWalletUtil.loadAccountKeyStore(password,StringUtil.encode58Check(build.getOwnerAddress().toByteArray()));
            if(accountKeyStore == null){
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }
            GrpcAPI.Return result = wallet.transactionSignAndBroadcastTransaction(transactionCapsule,accountKeyStore);
            String transactionID = ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes());

            JSONObject res = JSONObject.parseObject(JsonFormat.printToString(result, visible));
            res.put("txid", transactionID);
            response.getWriter().println(res.toJSONString());

        } catch (Exception e) {
            Util.processError(e, response);
        }

    }
}
