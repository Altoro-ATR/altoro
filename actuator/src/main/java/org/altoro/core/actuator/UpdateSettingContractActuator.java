package org.altoro.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.DecodeUtil;
import org.altoro.common.utils.StringUtil;
import org.altoro.core.capsule.AccountCapsule;
import org.altoro.core.capsule.ContractCapsule;
import org.altoro.core.capsule.TransactionResultCapsule;
import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;
import org.altoro.core.store.AccountStore;
import org.altoro.core.store.ContractStore;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.Protocol.Transaction.Result.code;
import org.altoro.protos.contract.SmartContractOuterClass.UpdateSettingContract;

import java.util.Arrays;
import java.util.Objects;
import static org.altoro.core.actuator.ActuatorConstant.ACCOUNT_EXCEPTION_STR;
@Slf4j(topic = "actuator")
public class UpdateSettingContractActuator extends AbstractActuator {

  public UpdateSettingContractActuator() {
    super(ContractType.UpdateSettingContract, UpdateSettingContract.class);
  }

  @Override
  public boolean execute(Object object) throws ContractExeException {
    TransactionResultCapsule ret = (TransactionResultCapsule) object;
    if (Objects.isNull(ret)) {
      throw new RuntimeException(ActuatorConstant.TX_RESULT_NULL);
    }

    long fee = calcFee();
    ContractStore contractStore = chainBaseManager.getContractStore();
    try {
      UpdateSettingContract usContract = any.unpack(UpdateSettingContract.class);
      long newPercent = usContract.getConsumeUserResourcePercent();
      byte[] contractAddress = usContract.getContractAddress().toByteArray();
      ContractCapsule deployedContract = contractStore.get(contractAddress);

      contractStore.put(contractAddress, new ContractCapsule(
              deployedContract.getInstance().toBuilder().setConsumeUserResourcePercent(newPercent)
                      .build()));

      ret.setStatus(fee, code.SUCESS);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      ret.setStatus(fee, code.FAILED);
      throw new ContractExeException(e.getMessage());
    }
    return true;
  }

  @Override
  public boolean validate() throws ContractValidateException {
    if (this.any == null) {
      throw new ContractValidateException(ActuatorConstant.CONTRACT_NOT_EXIST);
    }
    if (chainBaseManager == null) {
      throw new ContractValidateException("No account store or contract store!");
    }
    AccountStore accountStore = chainBaseManager.getAccountStore();
    ContractStore contractStore = chainBaseManager.getContractStore();
    if (!this.any.is(UpdateSettingContract.class)) {
      throw new ContractValidateException("contract type error, expected type "
              + "[UpdateSettingContract], real type[" + any.getClass() + "]");
    }
    final UpdateSettingContract contract;
    try {
      contract = this.any.unpack(UpdateSettingContract.class);
    } catch (InvalidProtocolBufferException e) {
      logger.debug(e.getMessage(), e);
      throw new ContractValidateException(e.getMessage());
    }
    if (!DecodeUtil.addressValid(contract.getOwnerAddress().toByteArray())) {
      throw new ContractValidateException("Invalid address");
    }
    byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
    String readableOwnerAddress = StringUtil.createReadableString(ownerAddress);

    AccountCapsule accountCapsule = accountStore.get(ownerAddress);
    if (accountCapsule == null) {
      throw new ContractValidateException(
              ACCOUNT_EXCEPTION_STR + readableOwnerAddress + "] does not exist");
    }

    long newPercent = contract.getConsumeUserResourcePercent();
    if (newPercent > ActuatorConstant.ONE_HUNDRED || newPercent < 0) {
      throw new ContractValidateException(
              "percent not in [0, 100]");
    }

    byte[] contractAddress = contract.getContractAddress().toByteArray();
    ContractCapsule deployedContract = contractStore.get(contractAddress);

    if (deployedContract == null) {
      throw new ContractValidateException(
              "Contract does not exist");
    }

    byte[] deployedContractOwnerAddress = deployedContract.getInstance().getOriginAddress()
            .toByteArray();

    if (!Arrays.equals(ownerAddress, deployedContractOwnerAddress)) {
      throw new ContractValidateException(
              "Account[" + readableOwnerAddress + "] is not the owner of the contract");
    }

    return true;
  }

  @Override
  public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
    return any.unpack(UpdateSettingContract.class).getOwnerAddress();
  }

  @Override
  public long calcFee() {
    return 0;
  }

}