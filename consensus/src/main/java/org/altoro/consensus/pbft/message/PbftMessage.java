package org.altoro.consensus.pbft.message;

import com.google.protobuf.ByteString;
import org.altoro.common.crypto.ECKey;
import org.altoro.common.crypto.ECKey.ECDSASignature;
import org.altoro.common.utils.Sha256Hash;
import org.altoro.consensus.base.Param.Miner;
import org.altoro.core.capsule.BlockCapsule;
import org.altoro.core.net.message.MessageTypes;
import org.altoro.protos.Protocol.PBFTMessage;
import org.altoro.protos.Protocol.PBFTMessage.DataType;
import org.altoro.protos.Protocol.PBFTMessage.MsgType;
import org.altoro.protos.Protocol.PBFTMessage.Raw;
import org.altoro.protos.Protocol.SRL;

import java.util.List;

public class PbftMessage extends org.altoro.consensus.pbft.message.PbftBaseMessage {

  public PbftMessage() {
  }

  public PbftMessage(byte[] data) throws Exception {
    super(MessageTypes.PBFT_MSG.asByte(), data);
  }

  public String getNo() {
    return pbftMessage.getRawData().getViewN() + "_" + pbftMessage.getRawData().getDataType();
  }

  public static org.altoro.consensus.pbft.message.PbftMessage prePrepareBlockMsg(BlockCapsule block, long epoch, Miner miner) {
    return buildCommon(DataType.BLOCK, block.getBlockId().getByteString(), block, epoch,
        block.getNum(), miner);
  }

  public static org.altoro.consensus.pbft.message.PbftMessage fullNodePrePrepareBlockMsg(BlockCapsule block,
                                                                                       long epoch) {
    return buildFullNodeCommon(DataType.BLOCK, block.getBlockId().getByteString(), block, epoch,
        block.getNum());
  }

  public static org.altoro.consensus.pbft.message.PbftMessage prePrepareSRLMsg(BlockCapsule block,
                                                                             List<ByteString> currentWitness, long epoch, Miner miner) {
    SRL.Builder srListBuilder = SRL.newBuilder();
    ByteString data = srListBuilder.addAllSrAddress(currentWitness).build().toByteString();
    return buildCommon(DataType.SRL, data, block, epoch, epoch, miner);
  }

  public static org.altoro.consensus.pbft.message.PbftMessage fullNodePrePrepareSRLMsg(BlockCapsule block,
                                                                                     List<ByteString> currentWitness, long epoch) {
    SRL.Builder srListBuilder = SRL.newBuilder();
    ByteString data = srListBuilder.addAllSrAddress(currentWitness).build().toByteString();
    return buildFullNodeCommon(DataType.SRL, data, block, epoch, epoch);
  }

  private static org.altoro.consensus.pbft.message.PbftMessage buildCommon(DataType dataType, ByteString data, BlockCapsule block,
                                                                         long epoch, long viewN, Miner miner) {
    org.altoro.consensus.pbft.message.PbftMessage pbftMessage = new org.altoro.consensus.pbft.message.PbftMessage();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    Raw.Builder rawBuilder = Raw.newBuilder();
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    rawBuilder.setViewN(viewN).setEpoch(epoch).setDataType(dataType)
        .setMsgType(MsgType.PREPREPARE).setData(data);
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(true, raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSignature(ByteString.copyFrom(signature.toByteArray()));
    PBFTMessage message = builder.build();
    pbftMessage.setType(MessageTypes.PBFT_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(block.isSwitch());
    return pbftMessage;
  }

  private static org.altoro.consensus.pbft.message.PbftMessage buildFullNodeCommon(DataType dataType, ByteString data,
                                                                                 BlockCapsule block, long epoch, long viewN) {
    org.altoro.consensus.pbft.message.PbftMessage pbftMessage = new org.altoro.consensus.pbft.message.PbftMessage();
    Raw.Builder rawBuilder = Raw.newBuilder();
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    rawBuilder.setViewN(viewN).setEpoch(epoch).setDataType(dataType)
        .setMsgType(MsgType.PREPREPARE).setData(data);
    Raw raw = rawBuilder.build();
    builder.setRawData(raw);
    PBFTMessage message = builder.build();
    pbftMessage.setType(MessageTypes.PBFT_MSG.asByte())
        .setPbftMessage(message).setData(message.toByteArray()).setSwitch(block.isSwitch());
    return pbftMessage;
  }

  public org.altoro.consensus.pbft.message.PbftMessage buildPrePareMessage(Miner miner) {
    return buildMessageCapsule(MsgType.PREPARE, miner);
  }

  public org.altoro.consensus.pbft.message.PbftMessage buildCommitMessage(Miner miner) {
    return buildMessageCapsule(MsgType.COMMIT, miner);
  }

  private org.altoro.consensus.pbft.message.PbftMessage buildMessageCapsule(MsgType type, Miner miner) {
    org.altoro.consensus.pbft.message.PbftMessage pbftMessage = new org.altoro.consensus.pbft.message.PbftMessage();
    ECKey ecKey = ECKey.fromPrivate(miner.getPrivateKey());
    PBFTMessage.Builder builder = PBFTMessage.newBuilder();
    Raw.Builder rawBuilder = Raw.newBuilder();
    rawBuilder.setViewN(getPbftMessage().getRawData().getViewN())
        .setDataType(getPbftMessage().getRawData().getDataType())
        .setMsgType(type).setEpoch(getPbftMessage().getRawData().getEpoch())
        .setData(getPbftMessage().getRawData().getData());
    Raw raw = rawBuilder.build();
    byte[] hash = Sha256Hash.hash(true, raw.toByteArray());
    ECDSASignature signature = ecKey.sign(hash);
    builder.setRawData(raw).setSignature(ByteString.copyFrom(signature.toByteArray()));
    PBFTMessage message = builder.build();
    pbftMessage.setType(getType().asByte())
        .setPbftMessage(message).setData(message.toByteArray());
    return pbftMessage;
  }
}