package org.altoro.core.db;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.altoro.common.parameter.CommonParameter;
import org.altoro.common.storage.leveldb.LevelDbDataSourceImpl;
import org.altoro.common.storage.rocksdb.RocksDbDataSourceImpl;
import org.altoro.common.utils.StorageUtils;
import org.altoro.core.db.common.DbSourceInter;
import org.altoro.core.db2.core.ITronChainBase;
import org.altoro.core.exception.BadItemException;
import org.altoro.core.exception.ItemNotFoundException;
import org.iq80.leveldb.WriteOptions;

import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map.Entry;

@Slf4j(topic = "DB")
public abstract class TronDatabase<T> implements ITronChainBase<T> {

  protected DbSourceInter<byte[]> dbSource;
  @Getter
  private String dbName;

  protected TronDatabase(String dbName) {
    this.dbName = dbName;

    if ("LEVELDB".equals(CommonParameter.getInstance().getStorage()
            .getDbEngine().toUpperCase())) {
      dbSource =
              new LevelDbDataSourceImpl(StorageUtils.getOutputDirectoryByDbName(dbName),
                      dbName,
                      StorageUtils.getOptionsByDbName(dbName),
                      new WriteOptions().sync(CommonParameter.getInstance()
                              .getStorage().isDbSync()));
    } else if ("ROCKSDB".equals(CommonParameter.getInstance()
            .getStorage().getDbEngine().toUpperCase())) {
      String parentName = Paths.get(StorageUtils.getOutputDirectoryByDbName(dbName),
              CommonParameter.getInstance().getStorage().getDbDirectory()).toString();
      dbSource =
              new RocksDbDataSourceImpl(parentName, dbName, CommonParameter.getInstance()
                      .getRocksDBCustomSettings());
    }

    dbSource.initDB();
  }

  protected TronDatabase() {
  }

  public DbSourceInter<byte[]> getDbSource() {
    return dbSource;
  }

  /**
   * reset the database.
   */
  public void reset() {
    dbSource.resetDb();
  }

  /**
   * close the database.
   */
  @Override
  public void close() {
    dbSource.closeDB();
  }

  public abstract void put(byte[] key, T item);

  public abstract void delete(byte[] key);

  public abstract T get(byte[] key)
          throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  public T getUnchecked(byte[] key) {
    return null;
  }

  public abstract boolean has(byte[] key);

  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Iterator<Entry<byte[], T>> iterator() {
    throw new UnsupportedOperationException();
  }
}
