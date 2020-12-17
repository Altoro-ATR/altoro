package org.altoro.core.services.http;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.Wallet;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.util.stream.Collectors;


/**
 * @author little liu
 */
@Component
@Slf4j(topic = "API")
public class DeleteAddressFileServlet extends RateLimiterServlet {
    @Autowired
    private Wallet wallet;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {

            String contract = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Util.checkBodySize(contract);
            JSONObject input = JSONObject.parseObject(contract);
            String owner_address = input.getString("owner_address");
            if (StringUtils.isEmpty(owner_address)) {
                response.getWriter().println("Invalid please input again.");
                return;
            }

            FileUtils.deleteQuietly(new File("Wallet/" + owner_address + ".json"));

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("result", true);
            response.getWriter().println(jsonObject.toJSONString());

        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}