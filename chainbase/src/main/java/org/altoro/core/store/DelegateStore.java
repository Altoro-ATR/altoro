package org.altoro.core.store;

import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.capsule.DelegateCapsule;
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
public class DelegateStore extends TronStoreWithRevoking<DelegateCapsule> {

  @Autowired
  protected DelegateStore(@Value("delegate") String dbName) {
    super(dbName);
  }

  /**
   * get all witnesses.
   */
  public List<DelegateCapsule> getAllDelegate() {
    return Streams.stream(iterator())
        .map(Entry::getValue)
        .collect(Collectors.toList());
  }

  @Override
  public DelegateCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegateCapsule(value);
  }
}
