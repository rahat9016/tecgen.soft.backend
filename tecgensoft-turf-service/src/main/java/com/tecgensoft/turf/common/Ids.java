package com.tecgensoft.turf.common;

public final class Ids {
  private static final com.fasterxml.uuid.impl.TimeBasedEpochRandomGenerator GENERATOR =
      com.fasterxml.uuid.Generators.timeBasedEpochRandomGenerator();

  private Ids() {}

  public static java.util.UUID next() {
    return GENERATOR.generate();
  }
}
