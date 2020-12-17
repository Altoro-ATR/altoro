package org.altoro.common.logsfilter.capsule;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.logsfilter.EventPluginLoader;
import org.altoro.common.logsfilter.trigger.InternalTransactionPojo;
import org.altoro.common.logsfilter.trigger.TransactionLogTrigger;
import org.altoro.common.runtime.InternalTransaction;
import org.altoro.common.runtime.ProgramResult;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.actuator.TransactionFactory;
import org.altoro.core.capsule.BlockCapsule;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.core.db.TransactionTrace;
import org.altoro.core.services.http.JsonFormat;
import org.altoro.core.services.http.Util;
import org.altoro.core.store.AccountTransactionStore;
import org.altoro.core.store.TransactionHistoryStore;
import org.altoro.core.store.TransactionRetStore;
import org.altoro.protos.Protocol;
import org.altoro.protos.contract.*;
import org.altoro.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.altoro.protos.contract.BalanceContract.TransferContract;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.altoro.protos.Protocol.Transaction.Contract.ContractType.*;

@Slf4j
public class TransactionLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private TransactionLogTrigger transactionLogTrigger;

  public TransactionLogTriggerCapsule(TransactionCapsule trxCapsule, BlockCapsule blockCapsule
          , TransactionHistoryStore transactionHistoryStore, TransactionRetStore transactionRetStore,
                                      AccountTransactionStore accountTransactionStore
  ) {
    transactionLogTrigger = new TransactionLogTrigger();
    if (Objects.nonNull(blockCapsule)) {
      transactionLogTrigger.setBlockHash(blockCapsule.getBlockId().toString());
    }
    transactionLogTrigger.setTransactionId(trxCapsule.getTransactionId().toString());
    transactionLogTrigger.setTimeStamp(trxCapsule.getTimestamp());
    transactionLogTrigger.setBlockNumber(trxCapsule.getBlockNum());
    transactionLogTrigger.setData(Hex.toHexString(trxCapsule
            .getInstance().getRawData().getData().toByteArray()));

    TransactionTrace trxTrace = trxCapsule.getTrxTrace();

    //result
    if (Objects.nonNull(trxCapsule.getContractRet())) {
      transactionLogTrigger.setResult(trxCapsule.getContractRet().toString());
    }

    if (Objects.nonNull(trxCapsule.getInstance().getRawData())) {

      JSONArray contracts = new JSONArray();
      trxCapsule.getInstance().getRawData().getContractList().stream().forEach(contract -> {
        try {
          JSONObject contractJson = null;
          Any contractParameter = contract.getParameter();
          switch (contract.getType()) {
            case CreateSmartContract:
              SmartContractOuterClass.CreateSmartContract deployContract = contractParameter
                      .unpack(SmartContractOuterClass.CreateSmartContract.class);
              contractJson = JSONObject.parseObject(JsonFormat.printToString(deployContract));
              byte[] ownerAddress = deployContract.getOwnerAddress().toByteArray();
              byte[] contractAddress = Util.generateContractAddress(trxCapsule.getInstance(), ownerAddress);
              contractJson.put("contract_address", StringUtil.encode58Check(contractAddress));
              break;
            default:
              Class clazz = TransactionFactory.getContract(contract.getType());
              if (clazz != null) {
                contractJson = JSONObject
                        .parseObject(JsonFormat.printToString(contractParameter.unpack(clazz)));
              }
              break;
          }
          contracts.add(contractJson);
        } catch (InvalidProtocolBufferException e) {
          logger.debug("InvalidProtocolBufferException: {}", e.getMessage());
        }
      });
      transactionLogTrigger.setContracts(contracts.toJSONString());

      Protocol.TransactionInfo transactionInfo = getTransactionInfoById(trxCapsule.getTransactionId().getByteString(), transactionHistoryStore, transactionRetStore);
      if(transactionInfo != null){
        transactionLogTrigger.setTransactionInfo(JsonFormat.printToString(transactionInfo));
      }

      // feelimit
      transactionLogTrigger.setFeeLimit(trxCapsule.getInstance().getRawData().getFeeLimit());

      Protocol.Transaction.Contract contract = trxCapsule.getInstance().getRawData().getContract(0);
      Any contractParameter = null;
      // contract type
      if (Objects.nonNull(contract)) {
        Protocol.Transaction.Contract.ContractType contractType = contract.getType();
        if (Objects.nonNull(contractType)) {
          transactionLogTrigger.setContractType(contractType.toString());
        }

        contractParameter = contract.getParameter();

        transactionLogTrigger.setContractCallValue(TransactionCapsule.getCallValue(contract));
      }

      if (Objects.nonNull(contractParameter) && Objects.nonNull(contract)) {
        try {
          if (contract.getType() == TransferContract) {
            TransferContract contractTransfer = contractParameter.unpack(TransferContract.class);

            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(
                        StringUtil.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }

              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
            transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
          } else if (contract.getType() == TransferAssetContract) {
            TransferAssetContract contractTransfer = contractParameter
                    .unpack(TransferAssetContract.class);

            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getAssetName())) {
                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
              }

              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }

              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(
                        StringUtil.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }
              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
            transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
          } else if (contract.getType() == FreezeBalanceContract) {
            BalanceContract.FreezeBalanceContract contractTransfer = contractParameter
                    .unpack(BalanceContract.FreezeBalanceContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              String printToString = JsonFormat.printToString(contractTransfer, true);
              JSONObject jsonObject = JSON.parseObject(printToString);
              jsonObject.put("resource",contractTransfer.getResource().name());
              if (Objects.nonNull(contractTransfer.getReceiverAddress())) {
                jsonObject.put("receiver_address",StringUtil.encode58Check(contractTransfer.getReceiverAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(jsonObject.toJSONString());
              transactionLogTrigger.setAssetAmount(contractTransfer.getFrozenBalance());
            }
          } else if (contract.getType() == UnfreezeBalanceContract) {
            BalanceContract.UnfreezeBalanceContract contractTransfer = contractParameter.unpack(BalanceContract.UnfreezeBalanceContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              String printToString = JsonFormat.printToString(contractTransfer, true);
              JSONObject jsonObject = JSON.parseObject(printToString);
              jsonObject.put("resource",contractTransfer.getResource().name());
              if (Objects.nonNull(contractTransfer.getReceiverAddress())) {
                jsonObject.put("receiver_address",StringUtil.encode58Check(contractTransfer.getReceiverAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(jsonObject.toJSONString());
              if(transactionInfo != null){
                transactionLogTrigger.setAssetAmount(transactionInfo.getUnfreezeAmount());
              }
            }
          } else if (contract.getType() == WithdrawBalanceContract) {
            BalanceContract.WithdrawBalanceContract contractTransfer = contractParameter
                    .unpack(BalanceContract.WithdrawBalanceContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if(transactionInfo != null){
                transactionLogTrigger.setAssetAmount(transactionInfo.getWithdrawAmount());
              }
            }
          }else if (contract.getType() == AccountCreateContract) {
            AccountContract.AccountCreateContract contractTransfer = contractParameter
                    .unpack(AccountContract.AccountCreateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getAccountAddress())) {
                transactionLogTrigger.setToAddress(
                        StringUtil.encode58Check(contractTransfer.getAccountAddress().toByteArray()));
              }
            }
          }else if (contract.getType() == VoteAssetContract) {
            VoteAssetContractOuterClass.VoteAssetContract contractTransfer = contractParameter
                    .unpack(VoteAssetContractOuterClass.VoteAssetContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == VoteWitnessContract) {
            WitnessContract.VoteWitnessContract contractTransfer = contractParameter
                    .unpack(WitnessContract.VoteWitnessContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == WitnessCreateContract) {
            WitnessContract.WitnessCreateContract contractTransfer = contractParameter
                    .unpack(WitnessContract.WitnessCreateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == AssetIssueContract) {
            AssetIssueContractOuterClass.AssetIssueContract contractTransfer = contractParameter
                    .unpack(AssetIssueContractOuterClass.AssetIssueContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == WitnessUpdateContract) {
            WitnessContract.WitnessUpdateContract contractTransfer = contractParameter
                    .unpack(WitnessContract.WitnessUpdateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == ParticipateAssetIssueContract) {
            AssetIssueContractOuterClass.ParticipateAssetIssueContract contractTransfer = contractParameter
                    .unpack(AssetIssueContractOuterClass.ParticipateAssetIssueContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getToAddress())) {
                transactionLogTrigger.setToAddress(StringUtil.encode58Check(contractTransfer.getToAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getAssetName())) {
                transactionLogTrigger.setAssetName(contractTransfer.getAssetName().toStringUtf8());
              }
              transactionLogTrigger.setAssetAmount(contractTransfer.getAmount());
            }
          }else if (contract.getType() == AccountUpdateContract) {
            AccountContract.AccountUpdateContract contractTransfer = contractParameter
                    .unpack(AccountContract.AccountUpdateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getAccountName())) {
                transactionLogTrigger.setAssetName(contractTransfer.getAccountName().toStringUtf8());
              }
            }
          }else if (contract.getType() == UnfreezeAssetContract) {
            AssetIssueContractOuterClass.UnfreezeAssetContract contractTransfer = contractParameter
                    .unpack(AssetIssueContractOuterClass.UnfreezeAssetContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
            }
          }else if (contract.getType() == UpdateAssetContract) {
            AssetIssueContractOuterClass.UpdateAssetContract contractTransfer = contractParameter
                    .unpack(AssetIssueContractOuterClass.UpdateAssetContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
              }
            }
          }else if (contract.getType() == ProposalCreateContract) {
            ProposalContract.ProposalCreateContract contractTransfer = contractParameter
                    .unpack(ProposalContract.ProposalCreateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == ProposalApproveContract) {
            ProposalContract.ProposalApproveContract contractTransfer = contractParameter
                    .unpack(ProposalContract.ProposalApproveContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == ProposalDeleteContract) {
            ProposalContract.ProposalDeleteContract contractTransfer = contractParameter
                    .unpack(ProposalContract.ProposalDeleteContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == SetAccountIdContract) {
            AccountContract.SetAccountIdContract contractTransfer = contractParameter
                    .unpack(AccountContract.SetAccountIdContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == GetContract) {
            SmartContractOuterClass.UpdateSettingContract contractTransfer = contractParameter
                    .unpack(SmartContractOuterClass.UpdateSettingContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getContractAddress())) {
                transactionLogTrigger.setContractAddress(StringUtil.encode58Check(contractTransfer.getContractAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == ExchangeCreateContract) {
            ExchangeContract.ExchangeCreateContract contractTransfer = contractParameter
                    .unpack(ExchangeContract.ExchangeCreateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == ExchangeInjectContract) {
            ExchangeContract.ExchangeInjectContract contractTransfer = contractParameter
                    .unpack(ExchangeContract.ExchangeInjectContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == ExchangeWithdrawContract) {
            ExchangeContract.ExchangeWithdrawContract contractTransfer = contractParameter
                    .unpack(ExchangeContract.ExchangeWithdrawContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == ExchangeTransactionContract) {
            ExchangeContract.ExchangeTransactionContract contractTransfer = contractParameter
                    .unpack(ExchangeContract.ExchangeTransactionContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == UpdateEnergyLimitContract) {
            SmartContractOuterClass.UpdateEnergyLimitContract contractTransfer = contractParameter
                    .unpack(SmartContractOuterClass.UpdateEnergyLimitContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getContractAddress())) {
                transactionLogTrigger.setContractAddress(StringUtil.encode58Check(contractTransfer.getContractAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == AccountPermissionUpdateContract) {
            AccountContract.AccountPermissionUpdateContract contractTransfer = contractParameter
                    .unpack(AccountContract.AccountPermissionUpdateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == ClearABIContract) {
            SmartContractOuterClass.ClearABIContract contractTransfer = contractParameter
                    .unpack(SmartContractOuterClass.ClearABIContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getContractAddress())) {
                transactionLogTrigger.setContractAddress(StringUtil.encode58Check(contractTransfer.getContractAddress().toByteArray()));
              }
            }
          }else if (contract.getType() == UpdateBrokerageContract) {
            StorageContract.UpdateBrokerageContract contractTransfer = contractParameter
                    .unpack(StorageContract.UpdateBrokerageContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == ShieldedTransferContract) {
            ShieldContract.ShieldedTransferContract contractTransfer = contractParameter
                    .unpack(ShieldContract.ShieldedTransferContract.class);
            if (Objects.nonNull(contractTransfer)) {
              if (Objects.nonNull(contractTransfer.getTransparentFromAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getTransparentFromAddress().toByteArray()));
              }
              if (Objects.nonNull(contractTransfer.getTransparentToAddress())) {
                transactionLogTrigger.setToAddress(StringUtil.encode58Check(contractTransfer.getTransparentToAddress().toByteArray()));
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer, true));
            }
          }else if (contract.getType() == DelegateRewardContract) {
            BalanceContract.DelegateRewardContract contractTransfer = contractParameter.unpack(BalanceContract.DelegateRewardContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                contractTransfer.getDelegateRewardList().forEach(reward->{
                  transactionLogTrigger.setAssetAmount(transactionLogTrigger.getAssetAmount()+reward.getReward());
                });
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          }else if (contract.getType() == DelegateContract) {
            WitnessContract.DelegateContract contractTransfer = contractParameter.unpack(WitnessContract.DelegateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(
                        StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
                transactionLogTrigger.setAssetAmount(contractTransfer.getDelegateCount());
              }
              transactionLogTrigger.setContract(JsonFormat.printToString(contractTransfer,true));
            }
          } else if (contract.getType() == UnDelegateContract) {
            WitnessContract.UnDelegateContract contractTransfer = contractParameter
                    .unpack(WitnessContract.UnDelegateContract.class);
            if (Objects.nonNull(contractTransfer)) {
              transactionLogTrigger.setAssetName("ATR");
              if (Objects.nonNull(contractTransfer.getOwnerAddress())) {
                transactionLogTrigger.setFromAddress(StringUtil.encode58Check(contractTransfer.getOwnerAddress().toByteArray()));
              }
              if(transactionInfo != null){
                transactionLogTrigger.setAssetAmount(transactionInfo.getUndelegateAmount());
              }
            }
          }
        } catch (Exception e) {
          logger.error("failed to load transferAssetContract, error'{}'", e);
        }
      }
    }

    // receipt
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getReceipt())) {
      transactionLogTrigger.setEnergyFee(trxTrace.getReceipt().getEnergyFee());
      transactionLogTrigger.setOriginEnergyUsage(trxTrace.getReceipt().getOriginEnergyUsage());
      transactionLogTrigger.setEnergyUsageTotal(trxTrace.getReceipt().getEnergyUsageTotal());
      transactionLogTrigger.setNetUsage(trxTrace.getReceipt().getNetUsage());
      transactionLogTrigger.setNetFee(trxTrace.getReceipt().getNetFee());
      transactionLogTrigger.setEnergyUsage(trxTrace.getReceipt().getEnergyUsage());
    }

    // program result
    if (Objects.nonNull(trxTrace) && Objects.nonNull(trxTrace.getRuntime()) && Objects
            .nonNull(trxTrace.getRuntime().getResult())) {
      ProgramResult programResult = trxTrace.getRuntime().getResult();
      ByteString contractResult = ByteString.copyFrom(programResult.getHReturn());
      ByteString contractAddress = ByteString.copyFrom(programResult.getContractAddress());

      if (Objects.nonNull(contractResult) && contractResult.size() > 0) {
        transactionLogTrigger.setContractResult(Hex.toHexString(contractResult.toByteArray()));
      }

      if (Objects.nonNull(contractAddress) && contractAddress.size() > 0) {
        transactionLogTrigger
                .setContractAddress(StringUtil.encode58Check((contractAddress.toByteArray())));
      }

      // internal transaction
      transactionLogTrigger.setInternalTransactionList(getInternalTransactionList(programResult.getInternalTransactions()));
    }
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    transactionLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  private List<InternalTransactionPojo> getInternalTransactionList(
          List<InternalTransaction> internalTransactionList) {
    List<InternalTransactionPojo> pojoList = new ArrayList<>();

    internalTransactionList.forEach(internalTransaction -> {
      InternalTransactionPojo item = new InternalTransactionPojo();

      item.setHash(Hex.toHexString(internalTransaction.getHash()));
      item.setCallValue(internalTransaction.getValue());
      item.setTokenInfo(internalTransaction.getTokenInfo());
      item.setCaller_address(Hex.toHexString(internalTransaction.getSender()));
      item.setTransferTo_address(Hex.toHexString(internalTransaction.getTransferToAddress()));
      item.setData(Hex.toHexString(internalTransaction.getData()));
      item.setRejected(internalTransaction.isRejected());
      item.setNote(internalTransaction.getNote());

      pojoList.add(item);
    });

    return pojoList;
  }

  @Override
  public void processTrigger() {
    EventPluginLoader.getInstance().postTransactionTrigger(transactionLogTrigger);
  }
}
