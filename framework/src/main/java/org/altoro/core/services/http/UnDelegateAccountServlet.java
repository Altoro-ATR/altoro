package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.api.GrpcAPI;
import org.altoro.common.utils.ByteArray;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.Wallet;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.contract.WitnessContract.UnDelegateContract;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@Component
@Slf4j(topic = "API")
public class UnDelegateAccountServlet extends RateLimiterServlet {

    @Autowired
    private Wallet wallet;

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {

            PostParams params = PostParams.getPostParams(request);

            JSONObject input = JSONObject.parseObject(params.getParams());
            String password = input.getString("password");

            UnDelegateContract.Builder build = UnDelegateContract.newBuilder();
            JsonFormat.merge(params.getParams(), build, params.isVisible());
            TransactionCapsule transactionCapsule = wallet.createTransactionCapsule(build.build(), ContractType.UnDelegateContract);

            if (StringUtils.isBlank(password)) {
                Protocol.Transaction tx = transactionCapsule.getInstance();
                JSONObject jsonObject = JSONObject.parseObject(params.getParams());
                tx = Util.setTransactionPermissionId(jsonObject, tx);
                response.getWriter().println(Util.printCreateTransaction(tx, params.isVisible()));
                return;
            }

            AccountKeyStore accountKeyStore = GenerateWalletUtil.loadAccountKeyStore(password, StringUtil.encode58Check(build.getOwnerAddress().toByteArray()));
            if(accountKeyStore == null){
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }

            GrpcAPI.Return result = wallet.transactionSignAndBroadcastTransaction(transactionCapsule, accountKeyStore);
            String transactionID = ByteArray
                    .toHexString(transactionCapsule.getTransactionId().getBytes());

            JSONObject res = JSONObject.parseObject(JsonFormat.printToString(result, params.isVisible()));
            res.put("txid", transactionID);
            response.getWriter().println(res.toJSONString());

        } catch (Exception e) {
            Util.processError(e, response);
        }

    }
}
