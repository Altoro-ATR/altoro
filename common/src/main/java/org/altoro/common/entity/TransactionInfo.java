package org.altoro.common.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class TransactionInfo implements Comparable<TransactionInfo> {
    private String status;
    private String ownerAddress;
    private String toAddress;
    private long amount;
    private long timestamp;
    private long blockNumber;
    private String txId;
    private String type;


    @Override
    public int compareTo(TransactionInfo o) {
        return (int) (o.getTimestamp() - this.getTimestamp());
    }

}
