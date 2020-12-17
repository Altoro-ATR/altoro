package org.altoro.core.metrics.net;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.altoro.core.metrics.MetricsKey;
import org.altoro.core.metrics.MetricsUtil;
import org.altoro.core.net.TronNetDelegate;
import org.altoro.core.net.peer.PeerConnection;
import org.altoro.protos.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


@Component
public class NetMetricManager {

  @Autowired
  private TronNetDelegate tronNetDelegate;

  /**
   * get net info.
   *
   * @return NetInfo
   */
  public org.altoro.core.metrics.net.NetInfo getNetInfo() {
    org.altoro.core.metrics.net.NetInfo netInfo = new org.altoro.core.metrics.net.NetInfo();
    setNetInfo(netInfo);
    return netInfo;
  }

  private void setNetInfo(org.altoro.core.metrics.net.NetInfo netInfo) {
    //set connection info
    netInfo.setConnectionCount(tronNetDelegate.getActivePeer().size());
    int validConnectionCount = 0;
    for (PeerConnection peerConnection : tronNetDelegate.getActivePeer()) {
      if (!(peerConnection.isNeedSyncFromUs() || peerConnection.isNeedSyncFromPeer())) {
        validConnectionCount++;
      }
    }
    netInfo.setValidConnectionCount(validConnectionCount);

    long errorProtoCount = MetricsUtil.getCounter(MetricsKey.NET_ERROR_PROTO_COUNT)
        .getCount();
    netInfo.setErrorProtoCount((int) errorProtoCount);

    org.altoro.core.metrics.net.RateInfo tcpInTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_TCP_IN_TRAFFIC);
    netInfo.setTcpInTraffic(tcpInTraffic);

    org.altoro.core.metrics.net.RateInfo tcpOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_TCP_OUT_TRAFFIC);
    netInfo.setTcpOutTraffic(tcpOutTraffic);

    org.altoro.core.metrics.net.RateInfo udpInTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_UDP_IN_TRAFFIC);
    netInfo.setUdpInTraffic(udpInTraffic);

    org.altoro.core.metrics.net.RateInfo udpOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_UDP_OUT_TRAFFIC);
    netInfo.setUdpOutTraffic(udpOutTraffic);

    // set api request info
    org.altoro.core.metrics.net.ApiInfo apiInfo = new org.altoro.core.metrics.net.ApiInfo();
    org.altoro.core.metrics.net.RateInfo APIQPS = MetricsUtil.getRateInfo(MetricsKey.NET_API_QPS);
    apiInfo.setQps(APIQPS);

    org.altoro.core.metrics.net.RateInfo FailQPS = MetricsUtil.getRateInfo(MetricsKey.NET_API_FAIL_QPS);
    apiInfo.setFailQps(FailQPS);

    org.altoro.core.metrics.net.RateInfo totalOutTraffic = MetricsUtil.getRateInfo(MetricsKey.NET_API_OUT_TRAFFIC);
    apiInfo.setOutTraffic(totalOutTraffic);

    List<org.altoro.core.metrics.net.ApiDetailInfo> apiDetails = new ArrayList<>();
    SortedMap<String, Meter> endpointQPSMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_QPS);
    SortedMap<String, Meter> endpointFailQPSMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_FAIL_QPS);
    SortedMap<String, Meter> endpointOutTrafficMap
        = MetricsUtil.getMeters(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC);
    for (Map.Entry<String, Meter> entry : endpointQPSMap.entrySet()) {
      org.altoro.core.metrics.net.ApiDetailInfo apiDetail = new org.altoro.core.metrics.net.ApiDetailInfo();
      String endpointName = entry.getKey().substring(MetricsKey.NET_API_DETAIL_QPS.length());
      apiDetail.setName(endpointName);
      org.altoro.core.metrics.net.RateInfo APIDetailQPS = MetricsUtil.getRateInfo(entry.getKey());
      apiDetail.setQps(APIDetailQPS);
      if (endpointOutTrafficMap.containsKey(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpointName)) {
        org.altoro.core.metrics.net.RateInfo APIDetailOutTraffic = MetricsUtil
            .getRateInfo(MetricsKey.NET_API_DETAIL_OUT_TRAFFIC + endpointName);
        apiDetail.setOutTraffic(APIDetailOutTraffic);
      }
      if (endpointFailQPSMap.containsKey(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpointName)) {
        org.altoro.core.metrics.net.RateInfo APIDetailFailQps = MetricsUtil
            .getRateInfo(MetricsKey.NET_API_DETAIL_FAIL_QPS + endpointName);
        apiDetail.setFailQps(APIDetailFailQps);
      }
      apiDetails.add(apiDetail);
    }

    apiInfo.setDetail(apiDetails);
    netInfo.setApi(apiInfo);

    long disconnectionCount
        = MetricsUtil.getCounter(MetricsKey.NET_DISCONNECTION_COUNT).getCount();
    netInfo.setDisconnectionCount((int) disconnectionCount);
    List<org.altoro.core.metrics.net.DisconnectionDetailInfo> disconnectionDetails =
        new ArrayList<>();
    SortedMap<String, Counter> disconnectionReason
        = MetricsUtil.getCounters(MetricsKey.NET_DISCONNECTION_DETAIL);
    for (Map.Entry<String, Counter> entry : disconnectionReason.entrySet()) {
      org.altoro.core.metrics.net.DisconnectionDetailInfo detail = new org.altoro.core.metrics.net.DisconnectionDetailInfo();
      String reason = entry.getKey().substring(MetricsKey.NET_DISCONNECTION_DETAIL.length());
      detail.setReason(reason);
      detail.setCount((int) entry.getValue().getCount());
      disconnectionDetails.add(detail);
    }
    netInfo.setDisconnectionDetail(disconnectionDetails);

    org.altoro.core.metrics.net.LatencyInfo latencyInfo = getBlockLatencyInfo();
    netInfo.setLatency(latencyInfo);
  }

  public Protocol.MetricsInfo.NetInfo getNetProtoInfo() {
    Protocol.MetricsInfo.NetInfo.Builder netInfo =
        Protocol.MetricsInfo.NetInfo.newBuilder();
    org.altoro.core.metrics.net.NetInfo net = getNetInfo();
    netInfo.setErrorProtoCount(net.getErrorProtoCount());
    Protocol.MetricsInfo.NetInfo.ApiInfo.Builder apiInfo =
        Protocol.MetricsInfo.NetInfo.ApiInfo.newBuilder();
    // api
    org.altoro.core.metrics.net.RateInfo qps = net.getApi().getQps();
    Protocol.MetricsInfo.RateInfo qpsInfo = qps.toProtoEntity();
    apiInfo.setQps(qpsInfo);
    org.altoro.core.metrics.net.RateInfo failQps = net.getApi().getFailQps();
    Protocol.MetricsInfo.RateInfo failQpsInfo = failQps.toProtoEntity();
    apiInfo.setFailQps(failQpsInfo);
    org.altoro.core.metrics.net.RateInfo outTraffic = net.getApi().getOutTraffic();
    Protocol.MetricsInfo.RateInfo outTrafficInfo = outTraffic.toProtoEntity();
    apiInfo.setOutTraffic(outTrafficInfo);

    for (org.altoro.core.metrics.net.ApiDetailInfo apiDetail : net.getApi().getDetail()) {
      Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.Builder detailInfo =
          Protocol.MetricsInfo.NetInfo.ApiInfo.ApiDetailInfo.newBuilder();
      detailInfo.setName(apiDetail.getName());
      org.altoro.core.metrics.net.RateInfo detailQps = apiDetail.getQps();
      Protocol.MetricsInfo.RateInfo detailQpsInfo = detailQps.toProtoEntity();
      detailInfo.setQps(detailQpsInfo);
      org.altoro.core.metrics.net.RateInfo detailFailQps = apiDetail.getFailQps();
      if (detailFailQps != null) {
        Protocol.MetricsInfo.RateInfo detailFailQpsInfo = detailFailQps.toProtoEntity();
        detailInfo.setFailQps(detailFailQpsInfo);
      }
      org.altoro.core.metrics.net.RateInfo DetailOutTraffic = apiDetail.getOutTraffic();
      if (DetailOutTraffic != null) {
        Protocol.MetricsInfo.RateInfo DetailOutTrafficInfo =
            DetailOutTraffic.toProtoEntity();
        detailInfo.setOutTraffic(DetailOutTrafficInfo);
      }
      apiInfo.addDetail(detailInfo);
    }
    netInfo.setApi(apiInfo.build());

    // connection
    netInfo.setConnectionCount(net.getConnectionCount());
    netInfo.setValidConnectionCount(net.getValidConnectionCount());
    netInfo.setDisconnectionCount(net.getDisconnectionCount());
    for (org.altoro.core.metrics.net.DisconnectionDetailInfo disconnectionDetail : net.getDisconnectionDetail()) {
      Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.Builder disconnectionDetailInfo =
          Protocol.MetricsInfo.NetInfo.DisconnectionDetailInfo.newBuilder();
      disconnectionDetailInfo.setCount(disconnectionDetail.getCount());
      disconnectionDetailInfo.setReason(disconnectionDetail.getReason());
      netInfo.addDisconnectionDetail(disconnectionDetailInfo.build());
    }
    // tcp
    org.altoro.core.metrics.net.RateInfo tcpInTraffic = net.getTcpInTraffic();
    Protocol.MetricsInfo.RateInfo tcpInTrafficInfo = tcpInTraffic.toProtoEntity();
    netInfo.setTcpInTraffic(tcpInTrafficInfo);
    org.altoro.core.metrics.net.RateInfo tcpOutTraffic = net.getTcpOutTraffic();
    Protocol.MetricsInfo.RateInfo tcpOUTrafficInfo = tcpOutTraffic.toProtoEntity();
    netInfo.setTcpOutTraffic(tcpOUTrafficInfo);
    // udp
    org.altoro.core.metrics.net.RateInfo udpInTraffic = net.getUdpInTraffic();
    Protocol.MetricsInfo.RateInfo udpInTrafficInfo = udpInTraffic.toProtoEntity();
    netInfo.setTcpOutTraffic(udpInTrafficInfo);
    org.altoro.core.metrics.net.RateInfo udpOutTraffic = net.getUdpOutTraffic();
    Protocol.MetricsInfo.RateInfo udpOutTrafficInfo = udpOutTraffic.toProtoEntity();
    netInfo.setUdpOutTraffic(udpOutTrafficInfo);

    // latency
    Protocol.MetricsInfo.NetInfo.LatencyInfo.Builder latencyInfo =
        Protocol.MetricsInfo.NetInfo.LatencyInfo.newBuilder();
    latencyInfo.setTop99(net.getLatency().getTop99());
    latencyInfo.setTop95(net.getLatency().getTop95());
    latencyInfo.setTop75(net.getLatency().getTop75());
    latencyInfo.setTotalCount(net.getLatency().getTotalCount());
    latencyInfo.setDelay1S(net.getLatency().getDelay1S());
    latencyInfo.setDelay2S(net.getLatency().getDelay2S());
    latencyInfo.setDelay3S(net.getLatency().getDelay3S());
    for (org.altoro.core.metrics.net.LatencyDetailInfo detail : net.getLatency().getDetail()) {
      Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.Builder detailInfo =
          Protocol.MetricsInfo.NetInfo.LatencyInfo.LatencyDetailInfo.newBuilder();
      detailInfo.setTop99(detail.getTop99());
      detailInfo.setTop95(detail.getTop95());
      detailInfo.setTop75(detail.getTop75());
      detailInfo.setCount(detail.getCount());
      detailInfo.setDelay1S(detail.getDelay1S());
      detailInfo.setDelay2S(detail.getDelay2S());
      detailInfo.setDelay3S(detail.getDelay3S());
      latencyInfo.addDetail(detailInfo.build());
    }

    netInfo.setLatency(latencyInfo.build());

    return netInfo.build();
  }

  private org.altoro.core.metrics.net.LatencyInfo getBlockLatencyInfo() {
    org.altoro.core.metrics.net.LatencyInfo latencyInfo = new org.altoro.core.metrics.net.LatencyInfo();
    long delay1SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".1S")
        .getCount();
    latencyInfo.setDelay1S((int) delay1SCount);
    long delay2SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".2S")
        .getCount();
    latencyInfo.setDelay2S((int) delay2SCount);
    long delay3SCount = MetricsUtil.getCounter(MetricsKey.NET_LATENCY + ".3S")
        .getCount();
    latencyInfo.setDelay3S((int) delay3SCount);
    Histogram blockLatency = MetricsUtil.getHistogram(MetricsKey.NET_LATENCY);
    latencyInfo.setTop99((int) blockLatency.getSnapshot().get99thPercentile());
    latencyInfo.setTop95((int) blockLatency.getSnapshot().get95thPercentile());
    latencyInfo.setTop75((int) blockLatency.getSnapshot().get75thPercentile());
    latencyInfo.setTotalCount((int) blockLatency.getCount());

    List<org.altoro.core.metrics.net.LatencyDetailInfo> latencyDetailInfos = new ArrayList<>();
    SortedMap<String, Histogram> witnessLatencyMap
        = MetricsUtil.getHistograms(MetricsKey.NET_LATENCY_WITNESS);
    for (Map.Entry<String, Histogram> entry : witnessLatencyMap.entrySet()) {
      org.altoro.core.metrics.net.LatencyDetailInfo latencyDetailTemp = new org.altoro.core.metrics.net.LatencyDetailInfo();
      String address = entry.getKey().substring(MetricsKey.NET_LATENCY_WITNESS.length());
      latencyDetailTemp.setCount((int) entry.getValue().getCount());
      latencyDetailTemp.setWitness(address);
      latencyDetailTemp.setTop99((int) entry.getValue().getSnapshot().get99thPercentile());
      latencyDetailTemp.setTop95((int) entry.getValue().getSnapshot().get95thPercentile());
      latencyDetailTemp.setTop75((int) entry.getValue().getSnapshot().get75thPercentile());
      long witnessDelay1S = MetricsUtil.getCounter(
          MetricsKey.NET_LATENCY_WITNESS + address + ".1S").getCount();
      latencyDetailTemp.setDelay1S((int) witnessDelay1S);
      long witnessDelay2S = MetricsUtil.getCounter(
          MetricsKey.NET_LATENCY_WITNESS + address + ".2S").getCount();
      latencyDetailTemp.setDelay2S((int) witnessDelay2S);
      long witnessDelay3S = MetricsUtil.getCounter(
          MetricsKey.NET_LATENCY_WITNESS + address + ".3S").getCount();
      latencyDetailTemp.setDelay3S((int) witnessDelay3S);
      latencyDetailInfos.add(latencyDetailTemp);
    }
    latencyInfo.setDetail(latencyDetailInfos);

    return latencyInfo;
  }
}
