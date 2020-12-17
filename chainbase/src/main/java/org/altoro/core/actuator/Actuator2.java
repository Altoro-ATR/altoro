package org.altoro.core.actuator;

import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;

public interface Actuator2 {

  void execute(Object object) throws ContractExeException;

  void validate(Object object) throws ContractValidateException;
}