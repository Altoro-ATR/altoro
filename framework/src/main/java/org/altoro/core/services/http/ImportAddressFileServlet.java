package org.altoro.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.springframework.stereotype.Component;

import javax.servlet.MultipartConfigElement;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.util.List;


/**
 * @author little liu
 */
@Component
@Slf4j(topic = "API")
@MultipartConfig
public class ImportAddressFileServlet extends RateLimiterServlet {

    private static final MultipartConfigElement MULTI_PART_CONFIG = new MultipartConfigElement("/temp");

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            String contentType = request.getContentType();
            if (contentType != null && contentType.startsWith("multipart/")) {
                request.setAttribute(Request.__MULTIPART_CONFIG_ELEMENT, MULTI_PART_CONFIG);
                Part part = request.getPart("addressFile");
                List<String> walletFileList = GenerateWalletUtil.getWalletFileList();
                String fileStr = IOUtils.toString(part.getInputStream());
                AccountKeyStore accountKeyStore = JSON.parseObject(fileStr, AccountKeyStore.class);
                if (!accountKeyStore.getAddress().startsWith("ATR") || accountKeyStore.getAddress().length() != 36) {
                    response.getWriter().println("{}");
                    return;
                }
                JSONObject jsonAddress = new JSONObject();
                if (walletFileList.contains(accountKeyStore.getAddress())) {
                    jsonAddress.put("address", accountKeyStore.getAddress());
                    response.getWriter().println(jsonAddress.toJSONString());
                    return;
                }
                GenerateWalletUtil.generateWalletFile(accountKeyStore);
                jsonAddress.put("address", accountKeyStore.getAddress());
                response.getWriter().println(jsonAddress.toJSONString());
            }
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    }

}