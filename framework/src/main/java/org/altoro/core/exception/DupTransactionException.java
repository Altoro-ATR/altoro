package org.altoro.core.exception;

public class DupTransactionException extends TronException {

  public DupTransactionException() {
    super();
  }

  public DupTransactionException(String message) {
    super(message);
  }
}
