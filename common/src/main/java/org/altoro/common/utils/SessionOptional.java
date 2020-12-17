package org.altoro.common.utils;

import org.altoro.core.db2.ISession;

import java.util.Optional;

public final class SessionOptional {

  private static final org.altoro.common.utils.SessionOptional INSTANCE = OptionalEnum.INSTANCE.getInstance();

  private Optional<ISession> value;

  private SessionOptional() {
    this.value = Optional.empty();
  }

  public static org.altoro.common.utils.SessionOptional instance() {
    return INSTANCE;
  }

  public synchronized org.altoro.common.utils.SessionOptional setValue(ISession value) {
    if (!this.value.isPresent()) {
      this.value = Optional.of(value);
    }
    return this;
  }

  public synchronized boolean valid() {
    return value.isPresent();
  }

  public synchronized void reset() {
    value.ifPresent(ISession::destroy);
    value = Optional.empty();
  }

  private enum OptionalEnum {
    INSTANCE;

    private org.altoro.common.utils.SessionOptional instance;

    OptionalEnum() {
      instance = new org.altoro.common.utils.SessionOptional();
    }

    private org.altoro.common.utils.SessionOptional getInstance() {
      return instance;
    }
  }

}
