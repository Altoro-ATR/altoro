package org.altoro.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.capsule.AccountCapsule;
import org.altoro.core.capsule.TransactionResultCapsule;
import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;
import org.altoro.core.store.AccountStore;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.Protocol.Transaction.Result.code;
import org.altoro.protos.contract.BalanceContract.DelegateRewardContract;

import java.util.Objects;

@Slf4j(topic = "actuator")
public class DelegateRewardActuator extends AbstractActuator {

    public DelegateRewardActuator() {
        super(ContractType.DelegateRewardContract, DelegateRewardContract.class);
    }

    @Override
    public boolean execute(Object result) throws ContractExeException {
        TransactionResultCapsule ret = (TransactionResultCapsule) result;
        if (Objects.isNull(ret)) {
            throw new RuntimeException("TransactionResultCapsule is null");
        }
        long fee = calcFee();
        final DelegateRewardContract delegateRewardContract;
        AccountStore accountStore = chainBaseManager.getAccountStore();
        try {
            delegateRewardContract = any.unpack(DelegateRewardContract.class);

            for (DelegateRewardContract.DelegateReward delegateReward : delegateRewardContract.getDelegateRewardList()) {
                long reward = delegateReward.getReward();
                AccountCapsule accountCapsule = accountStore.get(delegateReward.getToAddress().toByteArray());
                long oldBalance = accountCapsule.getBalance();
                accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                        .setBalance(oldBalance + reward)
                        .build());
                accountStore.put(accountCapsule.createDbKey(), accountCapsule);
            }

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
            throw new ContractValidateException("No contract!");
        }
        if (chainBaseManager == null) {
            throw new ContractValidateException("No account store or dynamic store!");
        }
        return true;
    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return any.unpack(DelegateRewardContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
