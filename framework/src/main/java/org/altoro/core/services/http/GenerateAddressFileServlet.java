package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;


/**
 * @author  little liu
 */
@Component
@Slf4j(topic = "API")
public class GenerateAddressFileServlet extends RateLimiterServlet {


    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String contract = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            JSONObject input = JSONObject.parseObject(contract);
            String password = input.getString("password");

            if (StringUtils.isEmpty(password)) {
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }

            AccountKeyStore accountKeyStore = GenerateWalletUtil.generateWallet(password);
            JSONObject jsonAddress = new JSONObject();
            jsonAddress.put("address", accountKeyStore.getAddress());
            response.getWriter().println(jsonAddress.toJSONString());
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}