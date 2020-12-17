package org.altoro.core.services.http;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.Wallet;
import org.altoro.core.config.args.Args;
import org.altoro.program.Version;
import org.altoro.protos.Protocol.Block;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;


@Component
@Slf4j(topic = "API")
public class GetNowBlockNumServlet extends RateLimiterServlet {

  @Autowired
  private Wallet wallet;

  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      Block reply = wallet.getNowBlock();
      if (reply != null) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("blockNum",reply.getBlockHeader().getRawData().getNumber());
        jsonObject.put("nowBlockNum",getNowBlock());
        jsonObject.put("nodeVersion", Version.getVersion());
        response.getWriter().println(jsonObject.toJSONString());
      } else {
        response.getWriter().println("{}");
      }
    } catch (Exception e) {
      Util.processError(e, response);
    }
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response) {
    doGet(request, response);
  }


  private long getNowBlock() {
    try {
      List<String> ipList = Args.getInstance().getSeedNode().getIpList();
      if (ipList.size() > 0) {
        String url = "http://"+ipList.get(0).split(":")[0] + ":9991/wallet/getnowblock";
        String nowBlock = get(url);
        JSONObject jsonObject = JSON.parseObject(nowBlock);
        return jsonObject.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return 0L;
  }

  private static String get(String url) {
    String result = "";
    HttpGet get = new HttpGet(url);
    try {
      CloseableHttpClient httpClient = HttpClients.createDefault();

      HttpResponse response = httpClient.execute(get);
      result = getHttpEntityContent(response);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      get.abort();
    }
    return result;
  }

  private static String getHttpEntityContent(HttpResponse response) throws UnsupportedOperationException, IOException {
    String result = "";
    HttpEntity entity = response.getEntity();
    if (entity != null) {
      InputStream in = entity.getContent();
      BufferedReader br = new BufferedReader(new InputStreamReader(in, "utf-8"));
      StringBuilder strber = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
        strber.append(line + '\n');
      }
      br.close();
      in.close();
      result = strber.toString();
    }
    return result;

  }


}