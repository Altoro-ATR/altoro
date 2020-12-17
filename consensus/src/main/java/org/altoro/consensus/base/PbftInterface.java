package org.altoro.consensus.base;

import org.altoro.consensus.pbft.message.PbftBaseMessage;
import org.altoro.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}