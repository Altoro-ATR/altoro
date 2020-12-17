package org.altoro.common.args;

import lombok.Getter;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class GenesisBlock implements Serializable {

  public static final String DEFAULT_NUMBER = "0";
  public static final String DEFAULT_TIMESTAMP = "0";
  public static final String DEFAULT_PARENT_HASH = "0";
  private static final long serialVersionUID = 3559533002594201715L;
  @Getter
  private List<Account> assets;

  @Getter
  private List<org.altoro.common.args.Witness> witnesses;

  @Getter
  private String timestamp;

  @Getter
  private String parentHash;

  @Getter
  private String number;

  @Getter
  private List<Delegate> delegate;

  public GenesisBlock() {
    this.number = "0";
  }

  /**
   * return default genesis block.
   */
  public static org.altoro.common.args.GenesisBlock getDefault() {
    final org.altoro.common.args.GenesisBlock genesisBlock = new org.altoro.common.args.GenesisBlock();
    List<Account> assets = Collections.emptyList();
    genesisBlock.setAssets(assets);
    List<org.altoro.common.args.Witness> witnesses = Collections.emptyList();
    genesisBlock.setWitnesses(witnesses);
    genesisBlock.setNumber(DEFAULT_NUMBER);
    genesisBlock.setTimestamp(DEFAULT_TIMESTAMP);
    genesisBlock.setParentHash(DEFAULT_PARENT_HASH);
    return genesisBlock;
  }

  /**
   * Empty assets.
   */
  public void setAssets(final List<Account> assets) {
    this.assets = assets;

    if (assets == null) {
      this.assets = Collections.emptyList();
    }
  }

  /**
   * Timestamp >= 0.
   */
  public void setTimestamp(final String timestamp) {
    this.timestamp = timestamp;

    if (this.timestamp == null) {
      this.timestamp = DEFAULT_TIMESTAMP;
    }

    try {
      long l = Long.parseLong(this.timestamp);
      if (l < 0) {
        throw new IllegalArgumentException("Timestamp(" + timestamp + ") must be a Long type.");
      }
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Timestamp(" + timestamp + ") must be a Long type.");
    }
  }

  /**
   * Set parent hash.
   */
  public void setParentHash(final String parentHash) {
    this.parentHash = parentHash;

    if (this.parentHash == null) {
      this.parentHash = DEFAULT_PARENT_HASH;
    }
  }

  public void setNumber(final String number) {
    this.number = "0";
  }

  /**
   * Empty witnesses.
   */
  public void setWitnesses(final List<org.altoro.common.args.Witness> witnesses) {
    this.witnesses = witnesses;

    if (witnesses == null) {
      this.witnesses = Collections.emptyList();
    }
  }

  public void setDelegate(List<Delegate> delegate) {
    this.delegate = delegate;

    if (delegate == null) {
      this.delegate = Collections.emptyList();
    }
  }

}
