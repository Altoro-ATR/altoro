package org.altoro.common.logsfilter.capsule;

import lombok.Getter;
import lombok.Setter;
import org.altoro.common.logsfilter.EventPluginLoader;
import org.altoro.common.logsfilter.trigger.ContractLogTrigger;

import static org.altoro.common.logsfilter.EventPluginLoader.matchFilter;
public class ContractLogTriggerCapsule extends TriggerCapsule {

  @Getter
  @Setter
  private ContractLogTrigger contractLogTrigger;

  public ContractLogTriggerCapsule(ContractLogTrigger contractLogTrigger) {
    this.contractLogTrigger = contractLogTrigger;
  }

  public void setLatestSolidifiedBlockNumber(long latestSolidifiedBlockNumber) {
    contractLogTrigger.setLatestSolidifiedBlockNumber(latestSolidifiedBlockNumber);
  }

  @Override
  public void processTrigger() {
    if (matchFilter(contractLogTrigger)) {
      EventPluginLoader.getInstance().postContractLogTrigger(contractLogTrigger);
    }
  }
}
