package org.altoro.core.services.http;


import lombok.extern.slf4j.Slf4j;
import org.altoro.common.parameter.RateLimiterInitialization;
import org.altoro.core.config.args.Args;
import org.altoro.core.services.ratelimiter.RateLimiterContainer;
import org.altoro.core.services.ratelimiter.RuntimeData;
import org.altoro.core.services.ratelimiter.adapter.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Constructor;


@Slf4j
public abstract class RateLimiterServlet extends HttpServlet {

  private static final String KEY_PREFIX_HTTP = "http_";
  private static final String ADAPTER_PREFIX = "org.altoro.core.services.ratelimiter.adapter.";

  protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods", "*");
    response.setHeader("Access-Control-Max-Age", "3600");
    response.setHeader("Access-Control-Allow-Headers", "Authorization,Origin,X-Requested-With,Content-Type,Accept,"
            + "content-Type,origin,x-requested-with,content-type,accept,authorization,token,id,X-Custom-Header,X-Cookie,Connection,User-Agent,Cookie,*");
    response.setHeader("Access-Control-Request-Headers", "Authorization,Origin, X-Requested-With,content-Type,Accept");
    response.setHeader("Access-Control-Expose-Headers", "*");
  }

  @Autowired
  private RateLimiterContainer container;

  @PostConstruct
  private void addRateContainer() {

    RateLimiterInitialization.HttpRateLimiterItem item = Args.getInstance()
        .getRateLimiterInitialization().getHttpMap().get(getClass().getSimpleName());

    boolean success = false;

    if (item != null) {
      String cName = "";
      String params = "";
      Object obj;
      try {
        cName = item.getStrategy();
        params = item.getParams();

        // add the specific rate limiter strategy of servlet.
        Class<?> c = Class.forName(ADAPTER_PREFIX + cName);
        Constructor constructor;
        if (c == GlobalPreemptibleAdapter.class || c == QpsRateLimiterAdapter.class
            || c == IPQPSRateLimiterAdapter.class) {
          constructor = c.getConstructor(String.class);
          obj = constructor.newInstance(params);
          container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), (IRateLimiter) obj);

        } else {
          constructor = c.getConstructor();
          obj = constructor.newInstance();
          container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), (IRateLimiter) obj);
        }
        success = true;
      } catch (Exception e) {
        logger.warn(
            "failure to add the rate limiter strategy. servlet = {}, strategy name = {}, params = \"{}\".",
            getClass().getSimpleName(), cName, params);
      }
    }

    if (!success) {
      // if the specific rate limiter strategy of servlet is not defined or fail to add,
      // then add a default Strategy.
      try {
        IRateLimiter rateLimiter = new DefaultBaseQqsAdapter("qps=1000");
        container.add(KEY_PREFIX_HTTP, getClass().getSimpleName(), rateLimiter);
      } catch (Exception e) {
        logger.warn(
            "failure to add the default rate limiter strategy. servlet = {}.",
            getClass().getSimpleName());
      }
    }

  }

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    IRateLimiter rateLimiter = container.get(KEY_PREFIX_HTTP, getClass().getSimpleName());

    boolean acquireResource = true;

    if (rateLimiter != null) {
      acquireResource = rateLimiter.acquire(new RuntimeData(req));
    }

    try {
      if (acquireResource) {
        super.service(req, resp);
      } else {
        resp.getWriter()
            .println(Util.printErrorMsg(new IllegalAccessException("lack of computing resources")));
      }
    } catch (ServletException | IOException e) {
      throw e;
    } catch (Exception unexpected) {
      logger.error("Http Api Error: {}", unexpected.getMessage());
    } finally {
      if (rateLimiter instanceof IPreemptibleRateLimiter && acquireResource) {
        ((IPreemptibleRateLimiter) rateLimiter).release();
      }
    }
  }
}