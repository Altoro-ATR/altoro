package org.altoro.core.metrics.net;

import java.util.ArrayList;
import java.util.List;

public class ApiInfo {

  private org.altoro.core.metrics.net.RateInfo qps;
  private org.altoro.core.metrics.net.RateInfo failQps;
  private org.altoro.core.metrics.net.RateInfo outTraffic;
  private List<org.altoro.core.metrics.net.ApiDetailInfo> detail = new ArrayList<>();

  public org.altoro.core.metrics.net.RateInfo getQps() {
    return qps;
  }

  public void setQps(org.altoro.core.metrics.net.RateInfo qps) {
    this.qps = qps;
  }

  public org.altoro.core.metrics.net.RateInfo getFailQps() {
    return failQps;
  }

  public void setFailQps(org.altoro.core.metrics.net.RateInfo failQps) {
    this.failQps = failQps;
  }

  public org.altoro.core.metrics.net.RateInfo getOutTraffic() {
    return outTraffic;
  }

  public void setOutTraffic(org.altoro.core.metrics.net.RateInfo outTraffic) {
    this.outTraffic = outTraffic;
  }

  public List<org.altoro.core.metrics.net.ApiDetailInfo> getDetail() {
    return detail;
  }

  public void setDetail(List<org.altoro.core.metrics.net.ApiDetailInfo> detail) {
    this.detail = detail;
  }
}
