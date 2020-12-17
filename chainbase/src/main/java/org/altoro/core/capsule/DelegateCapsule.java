package org.altoro.core.capsule;


import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.ByteArray;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Delegate;

import java.util.List;

@Slf4j(topic = "capsule")
public class DelegateCapsule implements ProtoCapsule<Delegate>, Comparable<DelegateCapsule> {

  private Delegate delegate;

  public DelegateCapsule(final Delegate delegate) {
    this.delegate = delegate;
  }

  /**
   * DelegateCapsule constructor with address and voteminingCount.
   */
  public DelegateCapsule(final ByteString address,final ByteString witnessAddress, final long delegateCount, final String url) {
    final Delegate.Builder delegateBuilder = Delegate.newBuilder();
    this.delegate = delegateBuilder
        .setAddress(address)
            .setWitnessAddress(witnessAddress)
        .setDelegateCount(delegateCount).setUrl(url).build();
  }

  public DelegateCapsule(final byte[] data) {
    try {
      this.delegate = Delegate.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
    }
  }

  public void addDelegateAccount(final ByteString delegateAddress,
                                 final ByteString ownerAddress,
                                 final long delegateCount,
                                 final long timestamp) {
    this.delegate = this.delegate.toBuilder()
            .addDelegateAccount(Protocol.DelegateAccount.newBuilder()
                    .setDelegateAddress(delegateAddress)
                    .setOwnerAddress(ownerAddress)
                    .setDelegateCount(delegateCount)
                    .setTimestamp(timestamp)
                    .build())
            .build();
  }


  public void addDelegateAccount(final Protocol.DelegateAccount delegateAccount) {
    this.delegate = this.delegate.toBuilder().addDelegateAccount(delegateAccount).build();
  }



  public List<Protocol.DelegateAccount> getDelegateAccountList() {
    if (this.delegate.getDelegateAccountList() != null) {
      return this.delegate.getDelegateAccountList();
    } else {
      return Lists.newArrayList();
    }
  }

  public void clearDelegateAccount() {
    this.delegate = this.delegate.toBuilder()
            .clearDelegateAccount()
            .build();
  }


  @Override
  public int compareTo(DelegateCapsule otherObject) {
    return Long.compare(otherObject.getDelegateCount(), this.getDelegateCount());
  }

  public ByteString getAddress() {
    return this.delegate.getAddress();
  }

  public byte[] createDbKey() {
    return getAddress().toByteArray();
  }

  public String createReadableString() {
    return ByteArray.toHexString(getAddress().toByteArray());
  }

  @Override
  public byte[] getData() {
    return this.delegate.toByteArray();
  }

  @Override
  public Delegate getInstance() {
    return this.delegate;
  }

  public long getDelegateCount() {
    return this.delegate.getDelegateCount();
  }

  public void setDelegateCount(final long delegateCount) {
    this.delegate = this.delegate.toBuilder().setDelegateCount(delegateCount).build();
  }

  public String getUrl() {
    return this.delegate.getUrl();
  }

  public void setUrl(final String url) {
    this.delegate = this.delegate.toBuilder().setUrl(url).build();
  }
}
