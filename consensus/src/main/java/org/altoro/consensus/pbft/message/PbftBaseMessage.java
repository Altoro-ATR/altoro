package org.altoro.consensus.pbft.message;

import com.google.protobuf.InvalidProtocolBufferException;
import org.spongycastle.util.encoders.Hex;
import org.altoro.common.crypto.ECKey;
import org.altoro.common.overlay.message.Message;
import org.altoro.common.utils.ByteUtil;
import org.altoro.common.utils.Sha256Hash;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.core.exception.P2pException;
import org.altoro.protos.Protocol.PBFTMessage;
import org.altoro.protos.Protocol.PBFTMessage.DataType;
import org.altoro.protos.Protocol.SRL;

import java.io.IOException;
import java.security.SignatureException;
import java.util.stream.Collectors;

public abstract class PbftBaseMessage extends Message {

  protected PBFTMessage pbftMessage;

  private boolean isSwitch;

  private byte[] publicKey;

  public PbftBaseMessage() {
  }

  public PbftBaseMessage(byte type, byte[] data) throws IOException, P2pException {
    super(type, data);
    this.pbftMessage = PBFTMessage.parseFrom(getCodedInputStream(data));
    if (isFilter()) {
      compareBytes(data, pbftMessage.toByteArray());
    }
  }

  @Override
  public Class<?> getAnswerMessage() {
    return null;
  }

  public PBFTMessage getPbftMessage() {
    return pbftMessage;
  }

  public org.altoro.consensus.pbft.message.PbftBaseMessage setPbftMessage(PBFTMessage pbftMessage) {
    this.pbftMessage = pbftMessage;
    return this;
  }

  public boolean isSwitch() {
    return isSwitch;
  }

  public org.altoro.consensus.pbft.message.PbftBaseMessage setSwitch(boolean aSwitch) {
    isSwitch = aSwitch;
    return this;
  }

  public org.altoro.consensus.pbft.message.PbftBaseMessage setData(byte[] data) {
    this.data = data;
    return this;
  }

  public org.altoro.consensus.pbft.message.PbftBaseMessage setType(byte type) {
    this.type = type;
    return this;
  }

  public byte[] getPublicKey() {
    return publicKey;
  }

  public String getKey() {
    return getNo() + "_" + Hex.toHexString(publicKey);
  }

  public String getDataKey() {
    return getNo() + "_" + Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  public long getNumber() {
    return pbftMessage.getRawData().getViewN();
  }

  public long getEpoch() {
    return pbftMessage.getRawData().getEpoch();
  }

  public DataType getDataType() {
    return pbftMessage.getRawData().getDataType();
  }

  public abstract String getNo();

  public void analyzeSignature() throws SignatureException {
    byte[] hash = Sha256Hash.hash(true, getPbftMessage().getRawData().toByteArray());
    publicKey = ECKey.signatureToAddress(hash, TransactionCapsule
        .getBase64FromByteString(getPbftMessage().getSignature()));
  }

  @Override
  public String toString() {
    return "DataType:" + getDataType() + ", MsgType:" + pbftMessage.getRawData().getMsgType()
        + ", node address:" + (ByteUtil.isNullOrZeroArray(publicKey) ? null
        : Hex.toHexString(publicKey))
        + ", viewN:" + pbftMessage.getRawData().getViewN()
        + ", epoch:" + pbftMessage.getRawData().getEpoch()
        + ", data:" + getDataString()
        + ", " + super.toString();
  }

  public String getDataString() {
    return getDataType() == DataType.SRL ? decode()
        : Hex.toHexString(pbftMessage.getRawData().getData().toByteArray());
  }

  private String decode() {
    try {
      SRL srList = SRL.parseFrom(pbftMessage.getRawData().getData().toByteArray());
      return "sr list = " + srList.getSrAddressList().stream().map(
          bytes -> StringUtil.encode58Check(bytes.toByteArray())).collect(Collectors.toList());
    } catch (InvalidProtocolBufferException e) {
    }
    return "decode error";
  }
}