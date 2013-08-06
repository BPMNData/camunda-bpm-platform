package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.airline;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

public class ResolveTravelAgencyDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    execution.setVariable("endpointAddress", "http://localhost:8080/bpmn-data-endpoint/message");
  }

}
