package org.altoro.core.capsule;


import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.ByteArray;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.AccountTransaction;

import java.util.List;

@Slf4j(topic = "capsule")
public class AccountTransactionCapsule implements ProtoCapsule<AccountTransaction>{

  private AccountTransaction accountTransaction;

  public AccountTransactionCapsule(final AccountTransaction accountTransaction) {
    this.accountTransaction = accountTransaction;
  }


  public AccountTransactionCapsule(final byte[] data) {
    try {
      this.accountTransaction = AccountTransaction.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public AccountTransactionCapsule(final byte[] address,final Protocol.AccountTransaction.AccountTx accountTx) {
    this.accountTransaction = AccountTransaction.newBuilder()
            .setAddress(ByteString.copyFrom(address))
            .addTxid(accountTx).build();
  }

  public void addTransaction(final Protocol.AccountTransaction.AccountTx accountTx) {
    this.accountTransaction = this.accountTransaction.toBuilder()
            .addTxid(accountTx)
            .build();
  }

  public List<Protocol.AccountTransaction.AccountTx> getAccountTransactionList() {
    if (this.accountTransaction.getTxidList() != null) {
      return this.accountTransaction.getTxidList();
    } else {
      return Lists.newArrayList();
    }
  }

  public void clearAccountTransaction() {
    this.accountTransaction = this.accountTransaction.toBuilder()
            .clearTxid()
            .build();
  }

  public ByteString getAddress() {
    return this.accountTransaction.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.accountTransaction.toByteArray();
  }

  @Override
  public AccountTransaction getInstance() {
    return this.accountTransaction;
  }


}
