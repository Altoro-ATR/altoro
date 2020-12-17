package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.ByteArray;
import org.altoro.core.Wallet;
import org.altoro.core.capsule.AccountCapsule;
import org.altoro.core.capsule.DelegateCapsule;
import org.altoro.core.db.Manager;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Component
@Slf4j(topic = "API")
public class GetAccountServlet extends RateLimiterServlet {

    @Autowired
    private Wallet wallet;

    @Autowired
    private Manager dbManager;

    private ArrayList<Protocol.DelegateAccount> sortDelegateAccountList(List<Protocol.DelegateAccount> delegateAccountList) {
        ArrayList<Protocol.DelegateAccount> arrayList = new ArrayList<>(delegateAccountList);
        arrayList.sort((m1, m2) -> {
            long diff = m1.getDelegateCount() - m2.getDelegateCount();
            if (diff > 0) {
                return -1;
            } else if (diff < 0) {
                return 1;
            }
            return 0; //相等为0
        });
        return arrayList;
    }

    private String convertOutput(Account account) {
        // convert asset id
        if (account.getAssetIssuedID().isEmpty()) {
            return JsonFormat.printToString(account, false);
        } else {
            JSONObject accountJson = JSONObject.parseObject(JsonFormat.printToString(account, false));
            String assetId = accountJson.get("asset_issued_ID").toString();
            accountJson.put(
                    "asset_issued_ID", ByteString.copyFrom(ByteArray.fromHexString(assetId)).toStringUtf8());
            return accountJson.toJSONString();
        }
    }

    private Account setAccountRank(Account account) {
        List<Protocol.DelegateAccount> delegateList = account.getDelegateList();
        AccountCapsule accountCapsule = dbManager.getAccountStore().get(account.getAddress().toByteArray());
        accountCapsule.clearDelegate();
        for (Protocol.DelegateAccount delegateAccount : delegateList) {
            DelegateCapsule delegateCapsule = dbManager.getDelegateStore().get(delegateAccount.getDelegateAddress().toByteArray());
            ArrayList<Protocol.DelegateAccount> delegateAccountList = sortDelegateAccountList(delegateCapsule.getDelegateAccountList());
            for (int i = 0; i < delegateAccountList.size(); i++) {
                Protocol.DelegateAccount delegateAccount1 = delegateAccountList.get(i);
                if (delegateAccount1.getOwnerAddress().equals(delegateAccount.getOwnerAddress())) {
                    Protocol.DelegateAccount build = delegateAccount.toBuilder().setRank(i + 1).build();
                    accountCapsule.addDelegateAccount(build);
                    break;
                }
            }
        }
        return accountCapsule.getInstance();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        doPost(request,response);
        /*try {
            boolean visible = Util.getVisible(request);
            String address = request.getParameter("address");
            Account.Builder build = Account.newBuilder();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("address", address);
            JsonFormat.merge(jsonObject.toJSONString(), build, visible);

            Account reply = wallet.getAccount(build.build());
            if (reply != null) {
                setAccountRank(reply);
                if (visible) {
                    response.getWriter().println(JsonFormat.printToString(reply, true));
                } else {
                    response.getWriter().println(convertOutput(reply));
                }
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }*/
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String account = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(account);
            boolean visible = Util.getVisiblePost(account);
            Account.Builder build = Account.newBuilder();
            JsonFormat.merge(account, build, visible);

            Account reply = wallet.getAccount(build.build());
            if (reply != null) {
                reply = setAccountRank(reply);
                if (visible) {
                    response.getWriter().println(JsonFormat.printToString(reply, true));
                } else {
                    response.getWriter().println(convertOutput(reply));
                }
            } else {
                response.getWriter().println("{}");
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}
