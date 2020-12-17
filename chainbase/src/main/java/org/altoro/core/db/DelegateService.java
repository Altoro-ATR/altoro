package org.altoro.core.db;

import com.google.protobuf.ByteString;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.capsule.DelegateCapsule;
import org.altoro.core.config.Parameter;
import org.altoro.core.store.AccountStore;
import org.altoro.core.store.DelegateRewardStore;
import org.altoro.core.store.DelegateStore;
import org.altoro.protos.Protocol;
import org.altoro.protos.contract.BalanceContract;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j(topic = "delegateReward")
@Component
public class DelegateService {

    @Setter
    private DelegateStore delegateStore;

    @Setter
    private DelegateRewardStore delegateRewardStore;

    @Setter
    private AccountStore accountStore;

    private BalanceContract.DelegateRewardContract.Builder builder;

    public void initStore(DelegateStore delegateStore, DelegateRewardStore delegateRewardStore, AccountStore accountStore) {
        this.delegateRewardStore = delegateRewardStore;
        this.accountStore = accountStore;
        this.delegateStore = delegateStore;
    }

    public BalanceContract.DelegateRewardContract payBlockDelegateReward(ByteString witnessAddress, long delegateReward) {
        builder = BalanceContract.DelegateRewardContract.newBuilder();
        builder.setOwnerAddress(witnessAddress);

        long witnessReward = (long) (delegateReward * Parameter.ChainConstant.REWARD_PRODUCTION_RATIO);
        long brokerageReward = delegateReward - witnessReward;

        builderDelegateAssetContract(witnessReward, witnessAddress, null, witnessAddress, witnessAddress);

        delegateRewardStore.addReward(witnessAddress.toByteArray(), witnessReward);

        List<ByteString> delegateAddressList = new ArrayList<>();

        for (DelegateCapsule delegateCapsule : delegateStore.getAllDelegate()) {
            if (witnessAddress.equals(delegateCapsule.getInstance().getWitnessAddress())) {
                delegateAddressList.add(delegateCapsule.getAddress());
            }
        }

        long delegateSum = 0;
        for (ByteString b : delegateAddressList) {
            delegateSum += getDelegateByAddress(b).getDelegateCount();
        }

        if (delegateSum > 0) {

            double brokerageDelegatePay = (double) brokerageReward / delegateSum;

            for (ByteString delegateAddress : delegateAddressList) {
                long delegateCount = getDelegateByAddress(delegateAddress).getDelegateCount();

                if (delegateCount == 0) {
                    continue;
                }

                double delegateAccountPay = (double) delegateCount * brokerageDelegatePay;
                long delegateAccountReward = (long) (delegateAccountPay * Parameter.ChainConstant.REWARD_PRODUCTION_RATIO);

                builderDelegateAssetContract(delegateAccountReward, witnessAddress, delegateAddress, witnessAddress, delegateAddress);
                delegateRewardStore.addReward(delegateAddress.toByteArray(), delegateAccountReward);
                delegateRewardStore.addDelegateReward(witnessAddress.toByteArray(), delegateAddress.toByteArray(), delegateAccountReward);
                delegateRewardStore.addReward(witnessAddress.toByteArray(), delegateAddress.toByteArray(), delegateAccountReward);

                double accountDelegatePayRate = (delegateAccountPay - delegateAccountReward) / delegateCount;
                accountDelegateReward(witnessAddress, delegateAddress, accountDelegatePayRate);

            }
        }

        return builder.build();
    }


    private void accountDelegateReward(ByteString witnessAddress, ByteString delegateAddress, double accountDelegatePayRate) {
        try {
            DelegateCapsule delegateCapsule = delegateStore.get(delegateAddress.toByteArray());
            List<Protocol.DelegateAccount> delegateAccountList = delegateCapsule.getDelegateAccountList();
            for (Protocol.DelegateAccount delegateAccount : delegateAccountList) {
                long accountDelegatePay = (long) (delegateAccount.getDelegateCount() * accountDelegatePayRate);
                builderDelegateAssetContract(accountDelegatePay, witnessAddress, delegateAddress, delegateAddress, delegateAccount.getOwnerAddress());
                delegateRewardStore.addReward(witnessAddress.toByteArray(),
                        delegateAddress.toByteArray(),
                        delegateAccount.getOwnerAddress().toByteArray(),
                        accountDelegatePay);
                delegateRewardStore.addReward(delegateAccount.getOwnerAddress().toByteArray(), accountDelegatePay);
                delegateRewardStore.addReward(witnessAddress.toByteArray(), delegateAddress.toByteArray(), accountDelegatePay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public DelegateCapsule getDelegateByAddress(ByteString address) {
        return delegateStore.get(address.toByteArray());
    }

    private void builderDelegateAssetContract(long amount,
                                            ByteString witnessAddress, ByteString delegateAddress,
                                            ByteString fromAddress, ByteString toAddress
    ) {
        if (amount > 0) {

            BalanceContract.DelegateRewardContract.DelegateReward.Builder delegateReward = BalanceContract.DelegateRewardContract.DelegateReward.newBuilder();
            delegateReward.setReward(amount);
            if (delegateAddress != null) {
                delegateReward.setDelegateAddress(delegateAddress);
            }
            delegateReward.setFormAddress(fromAddress);
            delegateReward.setToAddress(toAddress);
            builder.addDelegateReward(delegateReward);

        }
    }


}