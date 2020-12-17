package org.altoro.common.overlay.discover.node;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class DBNode {

  @Getter
  @Setter
  private List<DBNodeStats> nodes = new ArrayList<>();

}
