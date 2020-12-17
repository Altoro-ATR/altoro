package org.altoro.consensus.base;

import org.altoro.consensus.base.Param.Miner;
import org.altoro.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}