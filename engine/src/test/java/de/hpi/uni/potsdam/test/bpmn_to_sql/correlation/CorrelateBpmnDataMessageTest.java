package de.hpi.uni.potsdam.test.bpmn_to_sql.correlation;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.test.AbstractProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.correlation.BpmnDataCorrelationHandler;

public class CorrelateBpmnDataMessageTest extends AbstractProcessEngineTestCase {

  private static final String BPMN_DATA_MESSAGE = 
      "<message name=\"aMessageName\">" +
      " <correlation>" +
      "   <key name=\"some key\">" +
      "     <property name=\"some-prop\">8</property>" +
      "   </key>" +
      " </correlation>" +
      "</message>";
  
  protected void initializeProcessEngine() {
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("activiti.cfg.xml");
    
    config.setCorrelationHandler(new BpmnDataCorrelationHandler());
    processEngine = config.buildProcessEngine();
  }
  
  @Deployment
  public void testMessageNameExtraction() {
    runtimeService.startProcessInstanceByKey("process");
    
    Execution waitingExecution = runtimeService
        .createExecutionQuery().activityId("messageReceive").singleResult();
    
    Assert.assertNotNull(waitingExecution);
    
    // manually set the expected matching key
    runtimeService.setVariable(waitingExecution.getId(), "correlation-property", "8");
    
    runtimeService.correlateBpmnDataMessage(BPMN_DATA_MESSAGE);
    
    Assert.assertEquals(0, runtimeService.createExecutionQuery().activityId("messageReceive").count());
    
    waitingExecution = runtimeService.createExecutionQuery().variableValueEquals("correlation-property", "8")
        .activityId("task").singleResult();
    
    Assert.assertNotNull(waitingExecution);
  }
}