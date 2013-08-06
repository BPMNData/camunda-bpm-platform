package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.agency;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ResolveAirlineDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariable("endpointAddress", "http://localhost:8082/bpmn-data-endpoint/message");
  }

}
