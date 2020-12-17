package org.altoro.common.entity;

public class PeerInfo {

  private String lastSyncBlock;
  private long remainNum;
  private long lastBlockUpdateTime;
  private boolean syncFlag = true;
  private long headBlockTimeWeBothHave;
  private boolean needSyncFromPeer;
  private boolean needSyncFromUs;
  private String host;
  private int port;
  private String nodeId;
  private long connectTime;
  private double avgLatency;
  private int syncToFetchSize;
  private long syncToFetchSizePeekNum;
  private int syncBlockRequestedSize;
  private long unFetchSynNum;
  private int blockInPorcSize;
  private String headBlockWeBothHave;

  private boolean isActive;
  private int score;
  private int nodeCount;
  private long inFlow;
  private int disconnectTimes;
  private String localDisconnectReason;
  private String remoteDisconnectReason;

  public String getLastSyncBlock() {
    return lastSyncBlock;
  }

  public org.altoro.common.entity.PeerInfo setLastSyncBlock(String lastSyncBlock) {
    this.lastSyncBlock = lastSyncBlock;
    return this;
  }

  public long getRemainNum() {
    return remainNum;
  }

  public org.altoro.common.entity.PeerInfo setRemainNum(long remainNum) {
    this.remainNum = remainNum;
    return this;
  }

  public long getLastBlockUpdateTime() {
    return lastBlockUpdateTime;
  }

  public org.altoro.common.entity.PeerInfo setLastBlockUpdateTime(long lastBlockUpdateTime) {
    this.lastBlockUpdateTime = lastBlockUpdateTime;
    return this;
  }

  public boolean isSyncFlag() {
    return syncFlag;
  }

  public org.altoro.common.entity.PeerInfo setSyncFlag(boolean syncFlag) {
    this.syncFlag = syncFlag;
    return this;
  }

  public long getHeadBlockTimeWeBothHave() {
    return headBlockTimeWeBothHave;
  }

  public org.altoro.common.entity.PeerInfo setHeadBlockTimeWeBothHave(long headBlockTimeWeBothHave) {
    this.headBlockTimeWeBothHave = headBlockTimeWeBothHave;
    return this;
  }

  public boolean isNeedSyncFromPeer() {
    return needSyncFromPeer;
  }

  public org.altoro.common.entity.PeerInfo setNeedSyncFromPeer(boolean needSyncFromPeer) {
    this.needSyncFromPeer = needSyncFromPeer;
    return this;
  }

  public boolean isNeedSyncFromUs() {
    return needSyncFromUs;
  }

  public org.altoro.common.entity.PeerInfo setNeedSyncFromUs(boolean needSyncFromUs) {
    this.needSyncFromUs = needSyncFromUs;
    return this;
  }

  public String getHost() {
    return host;
  }

  public org.altoro.common.entity.PeerInfo setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public org.altoro.common.entity.PeerInfo setPort(int port) {
    this.port = port;
    return this;
  }

  public String getNodeId() {
    return nodeId;
  }

  public org.altoro.common.entity.PeerInfo setNodeId(String nodeId) {
    this.nodeId = nodeId;
    return this;
  }

  public long getConnectTime() {
    return connectTime;
  }

  public org.altoro.common.entity.PeerInfo setConnectTime(long connectTime) {
    this.connectTime = connectTime;
    return this;
  }

  public double getAvgLatency() {
    return avgLatency;
  }

  public org.altoro.common.entity.PeerInfo setAvgLatency(double avgLatency) {
    this.avgLatency = avgLatency;
    return this;
  }

  public int getSyncToFetchSize() {
    return syncToFetchSize;
  }

  public org.altoro.common.entity.PeerInfo setSyncToFetchSize(int syncToFetchSize) {
    this.syncToFetchSize = syncToFetchSize;
    return this;
  }

  public long getSyncToFetchSizePeekNum() {
    return syncToFetchSizePeekNum;
  }

  public org.altoro.common.entity.PeerInfo setSyncToFetchSizePeekNum(long syncToFetchSizePeekNum) {
    this.syncToFetchSizePeekNum = syncToFetchSizePeekNum;
    return this;
  }

  public int getSyncBlockRequestedSize() {
    return syncBlockRequestedSize;
  }

  public org.altoro.common.entity.PeerInfo setSyncBlockRequestedSize(int syncBlockRequestedSize) {
    this.syncBlockRequestedSize = syncBlockRequestedSize;
    return this;
  }

  public long getUnFetchSynNum() {
    return unFetchSynNum;
  }

  public org.altoro.common.entity.PeerInfo setUnFetchSynNum(long unFetchSynNum) {
    this.unFetchSynNum = unFetchSynNum;
    return this;
  }

  public int getBlockInPorcSize() {
    return blockInPorcSize;
  }

  public org.altoro.common.entity.PeerInfo setBlockInPorcSize(int blockInPorcSize) {
    this.blockInPorcSize = blockInPorcSize;
    return this;
  }

  public String getHeadBlockWeBothHave() {
    return headBlockWeBothHave;
  }

  public org.altoro.common.entity.PeerInfo setHeadBlockWeBothHave(String headBlockWeBothHave) {
    this.headBlockWeBothHave = headBlockWeBothHave;
    return this;
  }

  public boolean isActive() {
    return isActive;
  }

  public org.altoro.common.entity.PeerInfo setActive(boolean active) {
    isActive = active;
    return this;
  }

  public int getScore() {
    return score;
  }

  public org.altoro.common.entity.PeerInfo setScore(int score) {
    this.score = score;
    return this;
  }

  public int getNodeCount() {
    return nodeCount;
  }

  public org.altoro.common.entity.PeerInfo setNodeCount(int nodeCount) {
    this.nodeCount = nodeCount;
    return this;
  }

  public long getInFlow() {
    return inFlow;
  }

  public org.altoro.common.entity.PeerInfo setInFlow(long inFlow) {
    this.inFlow = inFlow;
    return this;
  }

  public int getDisconnectTimes() {
    return disconnectTimes;
  }

  public org.altoro.common.entity.PeerInfo setDisconnectTimes(int disconnectTimes) {
    this.disconnectTimes = disconnectTimes;
    return this;
  }

  public String getLocalDisconnectReason() {
    return localDisconnectReason;
  }

  public org.altoro.common.entity.PeerInfo setLocalDisconnectReason(String localDisconnectReason) {
    this.localDisconnectReason = localDisconnectReason;
    return this;
  }

  public String getRemoteDisconnectReason() {
    return remoteDisconnectReason;
  }

  public org.altoro.common.entity.PeerInfo setRemoteDisconnectReason(String remoteDisconnectReason) {
    this.remoteDisconnectReason = remoteDisconnectReason;
    return this;
  }
}
