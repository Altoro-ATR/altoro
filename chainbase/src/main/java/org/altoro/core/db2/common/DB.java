package org.altoro.core.db2.common;

import java.util.Map;
import java.util.Iterator;

public interface DB<K, V> extends Iterable<Map.Entry<K, V>>, Instance<DB<K, V>> {

  V get(K k);

  void put(K k, V v);

  long size();

  boolean isEmpty();

  void remove(K k);

  Iterator iterator();

  void close();

  String getDbName();
}
