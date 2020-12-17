package org.altoro.core.net;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.backup.BackupServer;
import org.altoro.common.overlay.message.Message;
import org.altoro.common.overlay.server.ChannelManager;
import org.altoro.common.overlay.server.SyncPool;
import org.altoro.common.utils.Sha256Hash;
import org.altoro.core.ChainBaseManager;
import org.altoro.core.capsule.BlockCapsule;
import org.altoro.core.capsule.BlockCapsule.BlockId;
import org.altoro.core.capsule.PbftSignCapsule;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.core.db.Manager;
import org.altoro.core.exception.*;
import org.altoro.core.exception.P2pException.TypeEnum;
import org.altoro.core.metrics.MetricsService;
import org.altoro.core.net.message.BlockMessage;
import org.altoro.core.net.message.MessageTypes;
import org.altoro.core.net.message.TransactionMessage;
import org.altoro.core.net.peer.PeerConnection;
import org.altoro.core.store.WitnessScheduleStore;
import org.altoro.protos.Protocol.Inventory.InventoryType;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.altoro.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

@Slf4j(topic = "net")
@Component
public class TronNetDelegate {

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private WitnessScheduleStore witnessScheduleStore;

  @Getter
  private Object blockLock = new Object();

  @Autowired
  private BackupServer backupServer;

  @Autowired
  private MetricsService metricsService;

  private volatile boolean backupServerStartFlag;

  private int blockIdCacheSize = 100;

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > blockIdCacheSize) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  public void trustNode(PeerConnection peer) {
    channelManager.getTrustNodes().put(peer.getInetAddress(), peer.getNode());
  }

  public Collection<PeerConnection> getActivePeer() {
    return syncPool.getActivePeers();
  }

  public long getSyncBeginNumber() {
    return dbManager.getSyncBeginNumber();
  }

  public long getBlockTime(BlockId id) throws P2pException {
    try {
      return chainBaseManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException | ItemNotFoundException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, id.getString());
    }
  }

  public BlockId getHeadBlockId() {
    return chainBaseManager.getHeadBlockId();
  }

  public BlockId getSolidBlockId() {
    return chainBaseManager.getSolidBlockId();
  }

  public BlockId getGenesisBlockId() {
    return chainBaseManager.getGenesisBlockId();
  }

  public BlockId getBlockIdByNum(long num) throws P2pException {
    try {
      return chainBaseManager.getBlockIdByNum(num);
    } catch (ItemNotFoundException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, "num: " + num);
    }
  }

  public BlockCapsule getGenesisBlock() {
    return chainBaseManager.getGenesisBlock();
  }

  public long getHeadBlockTimeStamp() {
    return chainBaseManager.getHeadBlockTimeStamp();
  }

  public boolean containBlock(BlockId id) {
    return chainBaseManager.containBlock(id);
  }

  public boolean containBlockInMainChain(BlockId id) {
    return chainBaseManager.containBlockInMainChain(id);
  }

  public List<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) throws P2pException {
    try {
      return dbManager.getBlockChainHashesOnFork(forkBlockHash);
    } catch (NonCommonBlockException e) {
      throw new P2pException(TypeEnum.HARD_FORKED, forkBlockHash.getString());
    }
  }

  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }

  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return chainBaseManager.containBlock(hash);
    } else if (type.equals(MessageTypes.TRX)) {
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  public Message getData(Sha256Hash hash, InventoryType type) throws P2pException {
    try {
      switch (type) {
        case BLOCK:
          return new BlockMessage(chainBaseManager.getBlockById(hash));
        case TRX:
          TransactionCapsule tx = chainBaseManager.getTransactionStore().get(hash.getBytes());
          if (tx != null) {
            return new TransactionMessage(tx.getInstance());
          }
          throw new StoreException();
        default:
          throw new StoreException();
      }
    } catch (StoreException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND,
              "type: " + type + ", hash: " + hash.getByteString());
    }
  }

  public void processBlock(BlockCapsule block, boolean isSync) throws P2pException {
    BlockId blockId = block.getBlockId();
    synchronized (blockLock) {
      try {
        if (!freshBlockId.contains(blockId)) {
          if (block.getNum() <= getHeadBlockId().getNum()) {
            logger.warn("Receive a fork block {} witness {}, head {}",
                    block.getBlockId().getString(),
                    Hex.toHexString(block.getWitnessAddress().toByteArray()),
                    getHeadBlockId().getString());
          }
          if (!isSync) {
            //record metrics
            metricsService.applyBlock(block);
          }
          dbManager.pushBlock(block);
          freshBlockId.add(blockId);
          logger.info("Success process block {}.", blockId.getString());
          if (!backupServerStartFlag
                  && System.currentTimeMillis() - block.getTimeStamp() < BLOCK_PRODUCED_INTERVAL) {
            backupServerStartFlag = true;
            backupServer.initServer();
          }
        }
      } catch (ValidateSignatureException
              | ContractValidateException
              | ContractExeException
              | UnLinkedBlockException
              | ValidateScheduleException
              | AccountResourceInsufficientException
              | TaposException
              | TooBigTransactionException
              | TooBigTransactionResultException
              | DupTransactionException
              | TransactionExpirationException
              | BadNumberBlockException
              | BadBlockException
              | NonCommonBlockException
              | ReceiptCheckErrException
              | VMIllegalException
              | ZksnarkException e) {
        metricsService.failProcessBlock(block.getNum(), e.getMessage());
        logger.error("Process block failed, {}, reason: {}.", blockId.getString(), e.getMessage());
        throw new P2pException(TypeEnum.BAD_BLOCK, e);
      }
    }
  }

  public void pushTransaction(TransactionCapsule trx) throws P2pException {
    try {
      trx.setTime(System.currentTimeMillis());
      dbManager.pushTransaction(trx);
    } catch (ContractSizeNotEqualToOneException
            | VMIllegalException e) {
      throw new P2pException(TypeEnum.BAD_TRX, e);
    } catch (ContractValidateException
            | ValidateSignatureException
            | ContractExeException
            | DupTransactionException
            | TaposException
            | TooBigTransactionException
            | TransactionExpirationException
            | ReceiptCheckErrException
            | TooBigTransactionResultException
            | AccountResourceInsufficientException e) {
      throw new P2pException(TypeEnum.TRX_EXE_FAILED, e);
    }
  }

  public boolean validBlock(BlockCapsule block) throws P2pException {
    try {
      return witnessScheduleStore.getActiveWitnesses().contains(block.getWitnessAddress())
              && block
              .validateSignature(dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());
    } catch (ValidateSignatureException e) {
      throw new P2pException(TypeEnum.BAD_BLOCK, e);
    }
  }

  public PbftSignCapsule getBlockPbftCommitData(long blockNum) {
    return chainBaseManager.getPbftSignDataStore().getBlockSignData(blockNum);
  }

  public PbftSignCapsule getSRLPbftCommitData(long epoch) {
    return chainBaseManager.getPbftSignDataStore().getSrSignData(epoch);
  }

  public boolean allowPBFT() {
    return chainBaseManager.getDynamicPropertiesStore().allowPBFT();
  }
}
