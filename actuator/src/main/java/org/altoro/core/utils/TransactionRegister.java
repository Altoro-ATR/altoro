package org.altoro.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.altoro.core.actuator.AbstractActuator;
import org.reflections.Reflections;

import java.util.Set;

@Slf4j(topic = "TransactionRegister")
public class TransactionRegister {

  public static void registerActuator() {
    Reflections reflections = new Reflections("org.altoro");
    Set<Class<? extends AbstractActuator>> subTypes = reflections
            .getSubTypesOf(AbstractActuator.class);
    for (Class _class : subTypes) {
      try {
        _class.newInstance();
      } catch (Exception e) {
        logger.error("{} contract actuator register fail!", _class, e);
      }
    }
  }

}

