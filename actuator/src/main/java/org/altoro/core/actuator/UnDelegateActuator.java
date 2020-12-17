package org.altoro.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.DecodeUtil;
import org.altoro.core.capsule.AccountCapsule;
import org.altoro.core.capsule.DelegateCapsule;
import org.altoro.core.capsule.TransactionResultCapsule;
import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;
import org.altoro.core.store.AccountStore;
import org.altoro.core.store.DelegateStore;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.Protocol.Transaction.Result.code;
import org.altoro.protos.contract.WitnessContract.UnDelegateContract;

import java.util.List;
import java.util.Objects;

@Slf4j(topic = "actuator")
public class UnDelegateActuator extends AbstractActuator {


    public UnDelegateActuator() {
        super(ContractType.UnDelegateContract, UnDelegateContract.class);
    }

    @Override
    public boolean execute(Object object) throws ContractExeException {
        TransactionResultCapsule ret = (TransactionResultCapsule) object;
        if (Objects.isNull(ret)) {
            throw new RuntimeException("TransactionResultCapsule is null");
        }
        long fee = calcFee();
        try {

            UnDelegateContract delegateContract = any.unpack(UnDelegateContract.class);

            long delegateAmount = removeDelegate(delegateContract);

            ret.setUnDelegateAmount(delegateAmount);
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

        if (!this.any.is(UnDelegateContract.class)) {
            throw new ContractValidateException(
                    "contract type error, expected type [DelegateContract], real type[" + any
                            .getClass() + "]");
        }
        final UnDelegateContract contract;
        try {
            contract = this.any.unpack(UnDelegateContract.class);
        } catch (InvalidProtocolBufferException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }
        if (!DecodeUtil.addressValid(contract.getOwnerAddress().toByteArray())) {
            throw new ContractValidateException("Invalid OwnerAddress");
        }

        if (!DecodeUtil.addressValid(contract.getDelegateAddress().toByteArray())) {
            throw new ContractValidateException("Invalid DelegateAddress");
        }

        if (!chainBaseManager.getDelegateStore().has(contract.getDelegateAddress().toByteArray())) {
            throw new ContractValidateException("Invalid DelegateAddress");
        }

        if (!chainBaseManager.getAccountStore().has(contract.getOwnerAddress().toByteArray())) {
            throw new ContractValidateException("Invalid OwnerAddress");
        }

        return true;
    }

    private long removeDelegate(UnDelegateContract contract) {
        long delegateCount = 0L;
        AccountStore accountStore = chainBaseManager.getAccountStore();
        DelegateStore delegateStore = chainBaseManager.getDelegateStore();

        byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
        byte[] delegateAddress = contract.getDelegateAddress().toByteArray();

        DelegateCapsule delegateCapsule = delegateStore.get(delegateAddress);
        AccountCapsule accountCapsule = accountStore.get(ownerAddress);

        List<Protocol.DelegateAccount> delegateAccountList = delegateCapsule.getDelegateAccountList();
        delegateCapsule.clearDelegateAccount();
        for (Protocol.DelegateAccount delegateAccount : delegateAccountList) {
            if (delegateAccount.getDelegateAddress().equals(contract.getDelegateAddress())
                    && delegateAccount.getOwnerAddress().equals(contract.getOwnerAddress())
            ) {
                delegateCapsule.setDelegateCount(delegateCapsule.getDelegateCount() - delegateAccount.getDelegateCount());
                delegateCount = delegateAccount.getDelegateCount();
            } else {
                delegateCapsule.addDelegateAccount(delegateAccount);
            }
        }

        List<Protocol.DelegateAccount> delegateList = accountCapsule.getDelegateList();
        accountCapsule.clearDelegate();
        for (Protocol.DelegateAccount delegateAccount : delegateList) {
            if (delegateAccount.getDelegateAddress().equals(contract.getDelegateAddress())
                    && delegateAccount.getOwnerAddress().equals(contract.getOwnerAddress())
            ) {
                accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                        .setBalance(accountCapsule.getBalance() + delegateAccount.getDelegateCount())
                        .setDelegateFrozen(accountCapsule.getDelegateFrozen() - delegateAccount.getDelegateCount())
                        .build());
                delegateCount = delegateAccount.getDelegateCount();
            } else {
                accountCapsule.addDelegateAccount(delegateAccount);
            }
        }

        delegateStore.put(delegateCapsule.createDbKey(), delegateCapsule);
        accountStore.put(accountCapsule.createDbKey(), accountCapsule);

        return delegateCount;
    }


    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return any.unpack(UnDelegateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
