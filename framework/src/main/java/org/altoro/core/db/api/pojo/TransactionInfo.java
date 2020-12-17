package org.altoro.core.db.api.pojo;

import lombok.Data;

@Data
public class TransactionInfo {

  private String status;
  private String ownerAddress;
  private String toAddress;
  private String amount;
  private Long timestamp;
  private Long blockNumber;
  private String txId;
  private String type;
  private String transactionType;

}
