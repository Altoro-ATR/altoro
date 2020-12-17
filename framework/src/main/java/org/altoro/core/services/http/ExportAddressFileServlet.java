package org.altoro.core.services.http;

import lombok.extern.slf4j.Slf4j;
import org.altoro.keystore.AccountKeyStore;
import org.altoro.keystore.GenerateWalletUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;


/**
 * @author little liu
 */
@Component
@Slf4j(topic = "API")
public class ExportAddressFileServlet extends RateLimiterServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        doGet(request,response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String password = request.getParameter("password");
            String owner_address = request.getParameter("owner_address");
            if (StringUtils.isEmpty(password) || StringUtils.isEmpty(owner_address)) {
                response.getWriter().println(Util.printErrorMsgString("Invalid please input again."));

                return;
            }

            AccountKeyStore accountKeyStore = GenerateWalletUtil.loadAccountKeyStore(password, owner_address);
            if(accountKeyStore == null){
                response.getWriter().println(Util.printErrorMsgString("Invalid password, please input again."));
                return;
            }
            accountKeyStore.setPriKey("");

            FileInputStream fileInputStream = FileUtils.openInputStream(new File(GenerateWalletUtil.KEYSTORE + accountKeyStore.getAddress() + GenerateWalletUtil.SUFFIX));
            String mimeType = getServletContext().getMimeType(GenerateWalletUtil.KEYSTORE + accountKeyStore.getAddress() + GenerateWalletUtil.SUFFIX);
            response.setContentType(mimeType);
            response.setHeader("content-Disposition", "attachment;filename=" + accountKeyStore.getAddress() + GenerateWalletUtil.SUFFIX);
            ServletOutputStream outputStream = response.getOutputStream();
            IOUtils.copy(fileInputStream, outputStream);
            outputStream.println();
        } catch (Exception e) {
            Util.processError(e, response);
        }
    }
}