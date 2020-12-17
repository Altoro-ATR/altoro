package org.altoro.core.actuator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.altoro.core.ChainBaseManager;
import org.altoro.core.capsule.TransactionCapsule;
import org.altoro.protos.Protocol;
import org.altoro.protos.Protocol.Transaction.Contract;

import java.util.List;

@Slf4j(topic = "actuator")
public class ActuatorFactory {

  public static final org.altoro.core.actuator.ActuatorFactory INSTANCE = new org.altoro.core.actuator.ActuatorFactory();

  private ActuatorFactory() {
  }

  public static org.altoro.core.actuator.ActuatorFactory getInstance() {
    return INSTANCE;
  }

  /**
   * create actuator.
   */
  public static List<Actuator> createActuator(TransactionCapsule transactionCapsule,
      ChainBaseManager chainBaseManager) {
    List<Actuator> actuatorList = Lists.newArrayList();
    if (null == transactionCapsule || null == transactionCapsule.getInstance()) {
      logger.info("TransactionCapsule or Transaction is null");
      return actuatorList;
    }

    Preconditions.checkNotNull(chainBaseManager, "manager is null");
    Protocol.Transaction.raw rawData = transactionCapsule.getInstance().getRawData();
    rawData.getContractList()
        .forEach(contract -> {
          try {
            actuatorList
                .add(getActuatorByContract(contract, chainBaseManager, transactionCapsule));
          } catch (IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
          }
        });
    return actuatorList;
  }

  private static Actuator getActuatorByContract(Contract contract, ChainBaseManager manager,
      TransactionCapsule tx) throws IllegalAccessException, InstantiationException {
    Class<? extends Actuator> clazz = TransactionFactory.getActuator(contract.getType());
    AbstractActuator abstractActuator = (AbstractActuator) clazz.newInstance();
    abstractActuator.setChainBaseManager(manager).setContract(contract)
        .setForkUtils(manager.getForkController()).setTx(tx);
    return abstractActuator;
  }

}
