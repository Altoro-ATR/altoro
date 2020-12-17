package org.altoro.core.actuator;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.utils.DecodeUtil;
import org.altoro.core.capsule.AccountCapsule;
import org.altoro.core.capsule.DelegateCapsule;
import org.altoro.core.capsule.TransactionResultCapsule;
import org.altoro.core.config.Parameter;
import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;
import org.altoro.core.store.AccountStore;
import org.altoro.core.store.DelegateStore;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Transaction.Contract.ContractType;
import org.altoro.protos.Protocol.Transaction.Result.code;
import org.altoro.protos.contract.WitnessContract.DelegateContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j(topic = "actuator")
public class DelegateActuator extends AbstractActuator {


    public DelegateActuator() {
        super(ContractType.DelegateContract, DelegateContract.class);
    }

    @Override
    public boolean execute(Object object) throws ContractExeException {
        TransactionResultCapsule ret = (TransactionResultCapsule) object;
        if (Objects.isNull(ret)) {
            throw new RuntimeException("TransactionResultCapsule is null");
        }
        long fee = calcFee();
        try {
            DelegateContract delegateContract = any.unpack(DelegateContract.class);
            executeDelegate(delegateContract);
            ret.setStatus(fee, code.SUCESS);
        } catch (InvalidProtocolBufferException | ContractValidateException e) {
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

        if (!this.any.is(DelegateContract.class)) {
            throw new ContractValidateException(
                    "contract type error, expected type [DelegateContract], real type[" + any
                            .getClass() + "]");
        }
        final DelegateContract contract;
        try {
            contract = this.any.unpack(DelegateContract.class);
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

        if (contract.getDelegateCount() < Parameter.ChainConstant.DELEGATE_MIN_COUNT) {
            throw new ContractValidateException(
                    "DelegateCount must more than 10000");
        }

        DelegateStore delegateStore = chainBaseManager.getDelegateStore();

        AccountStore accountStore = chainBaseManager.getAccountStore();

        if (!delegateStore.has(contract.getDelegateAddress().toByteArray())) {
            throw new ContractValidateException("Invalid DelegateAddress");
        }

        if (!accountStore.has(contract.getOwnerAddress().toByteArray())) {
            throw new ContractValidateException("Invalid OwnerAddress");
        }

        try {

            byte[] ownerAddress = contract.getOwnerAddress().toByteArray();
            byte[] delegateAddress = contract.getDelegateAddress().toByteArray();
            long delegateCount = contract.getDelegateCount();
            DelegateCapsule delegateCapsule = delegateStore.get(delegateAddress);
            AccountCapsule accountCapsule = accountStore.get(ownerAddress);

            Protocol.DelegateAccount delegate = null;
            for (Protocol.DelegateAccount delegateAccount : accountCapsule.getDelegateList()) {
                if (delegateAccount.getDelegateAddress().equals(contract.getDelegateAddress())) {
                    delegate = delegateAccount;
                    break;
                }
            }

            long balance = accountCapsule.getBalance();
            if (delegate != null && balance + delegate.getDelegateCount() < delegateCount) {
                throw new ContractValidateException("balance not enough");
            }

            List<Protocol.DelegateAccount> delegateAccountList = sortDelegateAccountList(delegateCapsule.getDelegateAccountList());
            if (delegateAccountList.size() >= Parameter.ChainConstant.DELEGATE_MAX_SIZE) {
                if (delegateAccountList.get(delegateAccountList.size() - 1).getDelegateCount() >= delegateCount) {
                    throw new ContractValidateException("Too few delegate count");
                }
            }

        } catch (ArithmeticException e) {
            logger.debug(e.getMessage(), e);
            throw new ContractValidateException(e.getMessage());
        }

        return true;
    }

    private void removeDelegate(Protocol.DelegateAccount delegateAccount ,
                                DelegateStore delegateStore, AccountStore accountStore

    ) {

        DelegateCapsule delegateCapsule =  delegateStore.get(delegateAccount.getDelegateAddress().toByteArray());
        AccountCapsule accountCapsule  = accountStore.get(delegateAccount.getOwnerAddress().toByteArray());

        List<Protocol.DelegateAccount> delegateAccountList = delegateCapsule.getDelegateAccountList();
        delegateCapsule.clearDelegateAccount();
        for (Protocol.DelegateAccount account : delegateAccountList) {
            if(!account.getOwnerAddress().equals(delegateAccount.getOwnerAddress())){
                delegateCapsule.addDelegateAccount(account);
            }
        }
        delegateCapsule.setDelegateCount(delegateCapsule.getDelegateCount() - delegateAccount.getDelegateCount());

        List<Protocol.DelegateAccount> delegateList = accountCapsule.getDelegateList();
        accountCapsule.clearDelegate();
        for (Protocol.DelegateAccount account : delegateList) {
            if(!account.getDelegateAddress().equals(delegateAccount.getDelegateAddress())){
                accountCapsule.addDelegateAccount(account);
            }
        }

        accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                .setBalance(accountCapsule.getBalance() + delegateAccount.getDelegateCount())
                .setDelegateFrozen(accountCapsule.getDelegateFrozen() - delegateAccount.getDelegateCount())
                .build());

        delegateStore.put(delegateCapsule.createDbKey(), delegateCapsule);
        accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    }


    private  ArrayList<Protocol.DelegateAccount> sortDelegateAccountList(List<Protocol.DelegateAccount> delegateAccountList) {
        ArrayList<Protocol.DelegateAccount> arrayList = new ArrayList<>(delegateAccountList);
        arrayList.sort((m1, m2) -> {
            long diff = m1.getDelegateCount() - m2.getDelegateCount();
            if (diff > 0) {
                return -1;
            } else if (diff < 0) {
                return 1;
            }
            return 0; //相等为0
        });
        return arrayList;
    }

    private void executeDelegate(DelegateContract delegateContract) throws ContractValidateException {

        AccountStore accountStore = chainBaseManager.getAccountStore();
        DelegateStore delegateStore = chainBaseManager.getDelegateStore();

        byte[] ownerAddress = delegateContract.getOwnerAddress().toByteArray();
        byte[] delegateAddress = delegateContract.getDelegateAddress().toByteArray();
        AccountCapsule accountCapsule = accountStore.get(ownerAddress);
        DelegateCapsule delegateCapsule = delegateStore.get(delegateAddress);

        List<Protocol.DelegateAccount> delegateAccountList = sortDelegateAccountList(delegateCapsule.getDelegateAccountList());
        if (delegateAccountList.size() >= Parameter.ChainConstant.DELEGATE_MAX_SIZE) {
            if (delegateAccountList.get(delegateAccountList.size() - 1).getDelegateCount() >= delegateContract.getDelegateCount()) {
                throw new ContractValidateException("Too few delegate count");
            }
            removeDelegate(delegateAccountList.get(delegateAccountList.size() - 1),delegateStore,accountStore);
        }

         delegateCapsule = delegateStore.get(delegateAddress);

        List<Protocol.DelegateAccount> accountDelegateList = accountCapsule.getDelegateList();
        boolean isDelegate = false;
        Protocol.DelegateAccount delegate = null;
        for (Protocol.DelegateAccount delegateAccount : accountDelegateList) {
            if (delegateAccount.getDelegateAddress().equals(delegateContract.getDelegateAddress())) {
                isDelegate = true;
                delegate = delegateAccount;
                break;
            }
        }

        if (!isDelegate) {

            Protocol.DelegateAccount.Builder builder = Protocol.DelegateAccount.newBuilder();
            builder.setDelegateAddress(delegateContract.getDelegateAddress())
                    .setOwnerAddress( delegateContract.getOwnerAddress())
                    .setDelegateCount(delegateContract.getDelegateCount())
                    .setTimestamp(System.currentTimeMillis());

            delegateCapsule.addDelegateAccount(builder.build());
            delegateCapsule.setDelegateCount(delegateCapsule.getDelegateCount() + delegateContract.getDelegateCount());

            accountCapsule.addDelegateAccount(builder.build());
            accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                    .setBalance(accountCapsule.getBalance() - delegateContract.getDelegateCount())
                    .setDelegateFrozen(accountCapsule.getDelegateFrozen() + delegateContract.getDelegateCount())
                    .build());

        } else {

            long delegateCount = delegate.getDelegateCount();

            Protocol.DelegateAccount.Builder builder = Protocol.DelegateAccount.newBuilder();
            builder.setDelegateAddress(delegateContract.getDelegateAddress())
                    .setOwnerAddress( delegateContract.getOwnerAddress())
                    .setDelegateCount(delegateContract.getDelegateCount())
                    .setTimestamp(System.currentTimeMillis());

            List<Protocol.DelegateAccount> delegateAccountList1 = delegateCapsule.getDelegateAccountList();
            delegateCapsule.clearDelegateAccount();
            for (Protocol.DelegateAccount delegateAccount : delegateAccountList1) {
                if (delegateAccount.getOwnerAddress().equals(delegateContract.getOwnerAddress())) {
                    delegateCapsule.addDelegateAccount(builder.build());
                } else {
                    delegateCapsule.addDelegateAccount(delegateAccount);
                }
            }
            delegateCapsule.setDelegateCount(delegateCapsule.getDelegateCount() + delegateContract.getDelegateCount() - delegateCount);


            accountCapsule.clearDelegate();
            for (Protocol.DelegateAccount delegateAccount : accountDelegateList) {
                if (delegateAccount.getDelegateAddress().equals(delegateContract.getDelegateAddress())) {
                    accountCapsule.addDelegateAccount(builder.build());
                } else {
                    accountCapsule.addDelegateAccount(delegateAccount);
                }
            }
            accountCapsule.setInstance(accountCapsule.getInstance().toBuilder()
                    .setBalance(accountCapsule.getBalance() - delegateContract.getDelegateCount() + delegateCount)
                    .setDelegateFrozen(accountCapsule.getDelegateFrozen() + delegateContract.getDelegateCount() - delegateCount)
                    .build());


        }

        delegateStore.put(delegateCapsule.createDbKey(), delegateCapsule);
        accountStore.put(accountCapsule.createDbKey(), accountCapsule);

    }

    @Override
    public ByteString getOwnerAddress() throws InvalidProtocolBufferException {
        return any.unpack(DelegateContract.class).getOwnerAddress();
    }

    @Override
    public long calcFee() {
        return 0;
    }

}
