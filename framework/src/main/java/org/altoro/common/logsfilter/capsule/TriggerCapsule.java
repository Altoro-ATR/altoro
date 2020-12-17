package org.altoro.common.logsfilter.capsule;

import com.google.protobuf.ByteString;
import org.altoro.core.capsule.TransactionInfoCapsule;
import org.altoro.core.exception.BadItemException;
import org.altoro.core.exception.StoreException;
import org.altoro.core.store.TransactionHistoryStore;
import org.altoro.core.store.TransactionRetStore;
import org.altoro.protos.Protocol;

import java.util.Objects;

public class TriggerCapsule {

  public void processTrigger() {
    throw new UnsupportedOperationException();
  }

  public Protocol.TransactionInfo getTransactionInfoById(ByteString transactionId, TransactionHistoryStore transactionHistoryStore, TransactionRetStore transactionRetStore) {
    if (Objects.isNull(transactionId)) {
      return null;
    }
    TransactionInfoCapsule transactionInfoCapsule;
    try {
      transactionInfoCapsule = transactionHistoryStore.get(transactionId.toByteArray());
    } catch (StoreException e) {
      return null;
    }
    if (transactionInfoCapsule != null) {
      return transactionInfoCapsule.getInstance();
    }
    try {
      transactionInfoCapsule = transactionRetStore.getTransactionInfo(transactionId.toByteArray());
    } catch (BadItemException e) {
      return null;
    }
    return transactionInfoCapsule == null ? null : transactionInfoCapsule.getInstance();
  }

}
