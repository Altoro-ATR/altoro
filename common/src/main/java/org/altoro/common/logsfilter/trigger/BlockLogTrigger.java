package org.altoro.common.logsfilter.trigger;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class BlockLogTrigger extends Trigger {

  @Getter
  @Setter
  private long blockNumber;

  @Getter
  @Setter
  private String blockHash;

  @Getter
  @Setter
  private String parentBlockHash;

  @Getter
  @Setter
  private long size;

  @Getter
  @Setter
  private long transactionSize;

  @Getter
  @Setter
  private long latestSolidifiedBlockNumber;

  @Getter
  @Setter
  private String witnessesAddress;

  @Getter
  @Setter
  private long totalAmount = 0;

  @Getter
  @Setter
  private List<String> transactionList = new ArrayList<>();

  public BlockLogTrigger() {
    setTriggerName(Trigger.BLOCK_TRIGGER_NAME);
  }

  @Override
  public String toString() {
    return new StringBuilder().append("triggerName: ").append(getTriggerName())
        .append("timestamp: ")
        .append(timeStamp)
        .append(", blockNumber: ")
        .append(blockNumber)
        .append(", blockhash: ")
        .append(blockHash)
        .append(", parentBlockHash: ")
        .append(parentBlockHash)
        .append(", size: ")
        .append(size)
        .append(", transactionSize: ")
        .append(transactionSize)
        .append(", latestSolidifiedBlockNumber: ")
        .append(latestSolidifiedBlockNumber)
        .append(", witnessesAddress: ")
        .append(witnessesAddress)
        .append(", totalAmount: ")
        .append(totalAmount)
        .append(", status: ")
        .append(status)
        .append(", transactionList: ")
        .append(transactionList).toString();
  }
}
