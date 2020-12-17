package org.altoro.common.logsfilter.capsule;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import org.altoro.common.logsfilter.EventPluginLoader;
import org.altoro.common.logsfilter.trigger.BlockLogTrigger;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.capsule.BlockCapsule;
import org.altoro.core.store.TransactionHistoryStore;
import org.altoro.core.store.TransactionRetStore;
import org.altoro.protos.Protocol;
import org.altoro.protos.contract.BalanceContract;
import org.altoro.protos.contract.WitnessContract;

public class BlockLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private BlockLogTrigger blockLogTrigger;

  public BlockLogTriggerCapsule(BlockCapsule block, TransactionHistoryStore transactionHistoryStore, TransactionRetStore transactionRetStore) {
    blockLogTrigger = new BlockLogTrigger();
    blockLogTrigger.setParentBlockHash(block.getParentBlockId().toString());
    blockLogTrigger.setBlockHash(block.getBlockId().toString());
    blockLogTrigger.setTimeStamp(block.getTimeStamp());
    blockLogTrigger.setSize(block.getData().length);
    blockLogTrigger.setBlockNumber(block.getNum());
    blockLogTrigger.setTransactionSize(block.getTransactions().size());
    blockLogTrigger.setWitnessesAddress(StringUtil.encode58Check((block.getWitnessAddress().toByteArray())));
    block.getTransactions().forEach(trx ->{
      Protocol.Transaction transaction = trx.getTrxTrace().getTrx().getInstance();
      if (Protocol.Transaction.Result.contractResult.SUCCESS.name().equals(transaction.getRet(0).getContractRet().name())) {
        Protocol.Transaction.Contract.ContractType type = transaction.getRawData().getContract(0).getType();
        Any contractParameter = transaction.getRawData().getContract(0).getParameter();
        if (Protocol.Transaction.Contract.ContractType.TransferContract.name().equals(type.name())){
          try {
            BalanceContract.TransferContract  deployContract = contractParameter.unpack(BalanceContract.TransferContract.class);
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + deployContract.getAmount());
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }else if (Protocol.Transaction.Contract.ContractType.FreezeBalanceContract.name().equals(type.name())){
          try {
            BalanceContract.FreezeBalanceContract deployContract = contractParameter.unpack(BalanceContract.FreezeBalanceContract.class);
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + deployContract.getFrozenBalance());
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }else if (Protocol.Transaction.Contract.ContractType.UnfreezeBalanceContract.name().equals(type.name())){
          Protocol.TransactionInfo transactionInfoById = getTransactionInfoById(trx.getTransactionId().getByteString()
                  ,transactionHistoryStore,transactionRetStore);
          if(transactionInfoById!=null){
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + transactionInfoById.getUnfreezeAmount());
          }
        }else if (Protocol.Transaction.Contract.ContractType.WithdrawBalanceContract.name().equals(type.name())){
          Protocol.TransactionInfo transactionInfoById = getTransactionInfoById(trx.getTransactionId().getByteString()
                  ,transactionHistoryStore,transactionRetStore);
          if(transactionInfoById!=null){
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + transactionInfoById.getWithdrawAmount());
          }
        }else if (Protocol.Transaction.Contract.ContractType.DelegateRewardContract.name().equals(type.name())){
          try {
            BalanceContract.DelegateRewardContract deployContract = contractParameter.unpack(BalanceContract.DelegateRewardContract.class);
            deployContract.getDelegateRewardList().forEach(reward->{
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + reward.getReward());
            });
          } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
          }
        }else if (Protocol.Transaction.Contract.ContractType.DelegateContract.name().equals(type.name())){
            try {
                WitnessContract.DelegateContract deployContract = contractParameter.unpack(WitnessContract.DelegateContract.class);
                blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + deployContract.getDelegateCount());
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }else if (Protocol.Transaction.Contract.ContractType.UnDelegateContract.name().equals(type.name())){
          Protocol.TransactionInfo transactionInfoById = getTransactionInfoById(trx.getTransactionId().getByteString()
                  ,transactionHistoryStore,transactionRetStore);
          if(transactionInfoById!=null){
            blockLogTrigger.setTotalAmount(blockLogTrigger.getTotalAmount() + transactionInfoById.getUndelegateAmount());
          }
        }
      }
      blockLogTrigger.getTransactionList().add(trx.getTransactionId().toString());
    }
    );
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    blockLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postBlockTrigger(blockLogTrigger);
  }
}
