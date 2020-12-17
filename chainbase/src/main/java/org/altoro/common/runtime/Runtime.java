package org.altoro.common.runtime;

import org.altoro.core.db.TransactionContext;
import org.altoro.core.exception.ContractExeException;
import org.altoro.core.exception.ContractValidateException;


public interface Runtime {

  void execute(TransactionContext context)
      throws ContractValidateException, ContractExeException;

  ProgramResult getResult();

  String getRuntimeError();

}
