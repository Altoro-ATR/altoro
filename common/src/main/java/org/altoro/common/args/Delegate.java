package org.altoro.common.args;

import lombok.Getter;
import lombok.Setter;
import org.altoro.common.utils.StringUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

import static org.altoro.common.utils.DecodeUtil.addressValid;

public class Delegate implements Serializable {

  private static final long serialVersionUID = -7446501098542377380L;

  @Getter
  private byte[] address;

  @Getter
  private String url;

  @Getter
  @Setter
  private long delegateCount;

  @Getter
  private byte[] witnessAddress;
  /**
   * set address.
   */
  public void setAddress(final byte[] address) {
    if (!addressValid(address)) {
      throw new IllegalArgumentException(
          "The address(" + StringUtil.createReadableString(address) + ") must be a 21 bytes.");
    }
    this.address = address;
  }

  public void setWitnessAddress(byte[] witnessAddress) {
    if (!addressValid(witnessAddress)) {
      throw new IllegalArgumentException(
              "The address(" + StringUtil.createReadableString(witnessAddress) + ") must be a 21 bytes.");
    }
    this.witnessAddress = witnessAddress;
  }

  /**
   * set url.
   */
  public void setUrl(final String url) {
    if (StringUtils.isBlank(url)) {
      throw new IllegalArgumentException(
          "The url(" + url + ") format error.");
    }

    this.url = url;
  }
}