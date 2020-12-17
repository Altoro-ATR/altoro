package org.altoro.tool.litefullnode.db;

import org.altoro.tool.litefullnode.iterator.DBIterator;

import java.io.Closeable;
import java.io.IOException;

public interface DBInterface extends Closeable {

  byte[] get(byte[] key);

  void put(byte[] key, byte[] value);

  void delete(byte[] key);

  DBIterator iterator();

  long size();

  void close() throws IOException;

}
