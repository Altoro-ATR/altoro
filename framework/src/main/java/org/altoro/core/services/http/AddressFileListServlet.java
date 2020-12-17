package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.GenerateWalletUtil;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author  little liu
 */
@Component
@Slf4j(topic = "API")
public class AddressFileListServlet extends RateLimiterServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {

            JSONObject jsonAddress = new JSONObject();
            jsonAddress.put("addressList", GenerateWalletUtil.getWalletFileList());
            response.getWriter().println(jsonAddress.toJSONString());

        } catch (Exception e) {
            Util.processError(e, response);
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }
}