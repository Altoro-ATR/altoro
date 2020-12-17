package org.altoro.core.store;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.capsule.AccountTransactionCapsule;
import org.altoro.core.db.TronStoreWithRevoking;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j(topic = "DB")
@Component
public class AccountTransactionStore extends TronStoreWithRevoking<AccountTransactionCapsule> {

  @Autowired
  protected AccountTransactionStore(@Value("accountTransaction") String dbName) {
    super(dbName);
  }

  /**
   * get all witnesses.
   */
  public List<AccountTransactionCapsule> getAllAccountTransaction() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public AccountTransactionCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new AccountTransactionCapsule(value);
  }
}
