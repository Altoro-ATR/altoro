package org.altoro.core.vm.config;


import lombok.extern.slf4j.Slf4j;
import org.altoro.common.parameter.CommonParameter;
import org.altoro.core.store.DynamicPropertiesStore;
import org.altoro.core.store.StoreFactory;

import static org.altoro.core.capsule.ReceiptCapsule.checkForEnergyLimit;

@Slf4j(topic = "VMConfigLoader")
public class ConfigLoader {

  //only for unit test
  public static boolean disable = false;

  public static void load(StoreFactory storeFactory) {
    if (!disable) {
      DynamicPropertiesStore ds = storeFactory.getChainBaseManager().getDynamicPropertiesStore();
      VMConfig.setVmTrace(CommonParameter.getInstance().isVmTrace());
      if (ds != null) {
        VMConfig.initVmHardFork(checkForEnergyLimit(ds));
        VMConfig.initAllowMultiSign(ds.getAllowMultiSign());
        VMConfig.initAllowTvmTransferTrc10(ds.getAllowTvmTransferTrc10());
        VMConfig.initAllowTvmConstantinople(ds.getAllowTvmConstantinople());
        VMConfig.initAllowTvmSolidity059(ds.getAllowTvmSolidity059());
        VMConfig.initAllowShieldedTRC20Transaction(ds.getAllowShieldedTRC20Transaction());
        VMConfig.initAllowTvmIstanbul(ds.getAllowTvmIstanbul());
        VMConfig.initAllowTvmStake(ds.getAllowTvmStake());
        VMConfig.initAllowTvmAssetIssue(ds.getAllowTvmAssetIssue());
      }
    }
  }
}
