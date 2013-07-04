package de.hpi.uni.potsdam.bpmnToSql;

import org.camunda.bpm.engine.ProcessEngineException;

public class DataInputUnavailableException extends ProcessEngineException {

  private static final long serialVersionUID = 1L;

  public DataInputUnavailableException(String message) {
    super(message);
  }

}
