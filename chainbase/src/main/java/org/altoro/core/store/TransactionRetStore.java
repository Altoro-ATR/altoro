package org.altoro.core.store;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.altoro.common.parameter.CommonParameter;
import org.altoro.common.utils.ByteArray;
import org.altoro.core.capsule.TransactionInfoCapsule;
import org.altoro.core.capsule.TransactionRetCapsule;
import org.altoro.core.db.TransactionStore;
import org.altoro.core.db.TronStoreWithRevoking;
import org.altoro.core.exception.BadItemException;
import org.altoro.protos.Protocol.TransactionInfo;

import java.util.Objects;

@Slf4j(topic = "DB")
@Component
public class TransactionRetStore extends TronStoreWithRevoking<TransactionRetCapsule> {

  @Autowired
  private TransactionStore transactionStore;

  @Autowired
  public TransactionRetStore(@Value("transactionRetStore") String dbName) {
    super(dbName);
  }

  @Override
  public void put(byte[] key, TransactionRetCapsule item) {
    if (BooleanUtils.toBoolean(CommonParameter.getInstance()
        .getStorage().getTransactionHistorySwitch())) {
      super.put(key, item);
    }
  }

  public TransactionInfoCapsule getTransactionInfo(byte[] key) throws BadItemException {
    long blockNumber = transactionStore.getBlockNumber(key);
    if (blockNumber == -1) {
      return null;
    }
    byte[] value = revokingDB.getUnchecked(ByteArray.fromLong(blockNumber));
    if (Objects.isNull(value)) {
      return null;
    }

    TransactionRetCapsule result = new TransactionRetCapsule(value);
    if (Objects.isNull(result.getInstance())) {
      return null;
    }

    for (TransactionInfo transactionResultInfo : result.getInstance().getTransactioninfoList()) {
      if (transactionResultInfo.getId().equals(ByteString.copyFrom(key))) {
        return new TransactionInfoCapsule(transactionResultInfo);
      }
    }
    return null;
  }

  public TransactionRetCapsule getTransactionInfoByBlockNum(byte[] key) throws BadItemException {

    byte[] value = revokingDB.getUnchecked(key);
    if (Objects.isNull(value)) {
      return null;
    }

    return new TransactionRetCapsule(value);
  }

}
