package org.altoro.core.store;

import org.altoro.common.utils.ByteArray;
import org.altoro.core.capsule.BytesCapsule;
import org.altoro.core.db.TronStoreWithRevoking;
import org.apache.commons.lang3.ArrayUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DelegateRewardStore extends TronStoreWithRevoking<BytesCapsule> {

    @Autowired
    public DelegateRewardStore(@Value("delegateReward") String dbName) {
        super(dbName);
    }

    @Override
    public BytesCapsule get(byte[] key) {
        byte[] value = revokingDB.getUnchecked(key);
        return ArrayUtils.isEmpty(value) ? null : new BytesCapsule(value);
    }

    public void addReward(byte[] address, long value) {
        BytesCapsule bytesCapsule = get(address);
        if (bytesCapsule == null) {
            put(address, new BytesCapsule(ByteArray.fromLong(value)));
        } else {
            put(address, new BytesCapsule(ByteArray
                    .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
        }
    }

    public long getReward(byte[] key) {
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            return 0L;
        } else {
            return ByteArray.toLong(bytesCapsule.getData());
        }
    }

    public void addReward(byte[] witnessAddress, byte[] delegateAddress, long value) {
        byte[] key = buildRewardKey(witnessAddress,delegateAddress);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            put(key, new BytesCapsule(ByteArray.fromLong(value)));
        } else {
            put(key, new BytesCapsule(ByteArray
                    .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
        }
    }

    public long getReward(byte[] witnessAddress, byte[] delegateAddress) {
        byte[] key = buildRewardKey(witnessAddress,delegateAddress);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            return 0L;
        } else {
            return ByteArray.toLong(bytesCapsule.getData());
        }
    }

    public void addReward(byte[] witnessAddress, byte[] delegateAddress, byte[] address, long value) {
        byte[] key = buildRewardKey(witnessAddress,delegateAddress,address);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            put(key, new BytesCapsule(ByteArray.fromLong(value)));
        } else {
            put(key, new BytesCapsule(ByteArray
                    .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
        }
    }

    public long getReward(byte[] witnessAddress, byte[] delegateAddress, byte[] address) {
        byte[] key = buildRewardKey(witnessAddress,delegateAddress,address);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            return 0L;
        } else {
            return ByteArray.toLong(bytesCapsule.getData());
        }
    }

    public void addDelegateReward(byte[] witnessAddress, byte[] delegateAddress, long value) {
        byte[] key = buildDelegateRewardKey(witnessAddress,delegateAddress);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            put(key, new BytesCapsule(ByteArray.fromLong(value)));
        } else {
            put(key, new BytesCapsule(ByteArray
                    .fromLong(ByteArray.toLong(bytesCapsule.getData()) + value)));
        }
    }

    public long getDelegateReward(byte[] witnessAddress, byte[] delegateAddress) {
        byte[] key = buildDelegateRewardKey(witnessAddress,delegateAddress);
        BytesCapsule bytesCapsule = get(key);
        if (bytesCapsule == null) {
            return 0L;
        } else {
            return ByteArray.toLong(bytesCapsule.getData());
        }
    }

    private byte[] buildRewardKey(byte[] witnessAddress,byte[] delegateAddress) {
        return (Hex.toHexString(witnessAddress) + "-" + Hex.toHexString(delegateAddress)).getBytes();
    }

    private byte[] buildRewardKey(byte[] witnessAddress,byte[] delegateAddress, byte[] address) {
        return (Hex.toHexString(witnessAddress) + "-" + Hex.toHexString(delegateAddress)+ "-" + Hex.toHexString(address)).getBytes();
    }

    private byte[] buildDelegateRewardKey(byte[] witnessAddress, byte[] delegateAddress) {
        return (Hex.toHexString(witnessAddress) + "-" + Hex.toHexString(delegateAddress)+ "-delegateReward").getBytes();
    }

}