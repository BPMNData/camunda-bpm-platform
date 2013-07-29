package de.hpi.uni.potsdam.test.bpmn_to_sql.correlation;

import java.util.HashMap;
import java.util.Map;

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
  
  private static final String BPMN_DATA_TWO_PROPERTIES_MESSAGE = 
      "<message name=\"aMessageName\">" +
      " <correlation>" +
      "   <key name=\"some key\">" +
      "     <property name=\"some-prop\">8</property>" +
      "   </key>" +
      "   <key name=\"another key\">" +
      "     <property name=\"another-prop\">42</property>" +
      "   </key>" +
      " </correlation>" +
      "</message>";
  
  protected void initializeProcessEngine() {
    ProcessEngineConfigurationImpl config = (ProcessEngineConfigurationImpl) ProcessEngineConfiguration
        .createProcessEngineConfigurationFromResource("activiti.cfg.xml");
    
    config.setCorrelationHandler(new BpmnDataCorrelationHandler());
    config.setBpmnDataAware(true);
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
  
  @Deployment
  public void testCorrelationPropertyPopulationOnMessageStart() {
    runtimeService.correlateBpmnDataMessage(BPMN_DATA_MESSAGE);
    
    Execution processInstance = runtimeService.createExecutionQuery().singleResult();
    
    Assert.assertNotNull("a process instance should have been started", processInstance);
    
    Object variable = runtimeService.getVariable(processInstance.getId(), "correlation-property");
    Assert.assertNotNull("the correlation property should have been set", variable);
    Assert.assertEquals("the correlation property should have been populated correctly", "8", variable);
    
  }
  
  @Deployment
  public void testCorrelationPropertyPopulationOnReceiveTask() {
    Map<String, Object> initialCorrelationProperties = new HashMap<String, Object>();
    initialCorrelationProperties.put("correlation-property", "8");
    
    runtimeService.startProcessInstanceByKey("process", initialCorrelationProperties);
    
    runtimeService.correlateBpmnDataMessage(BPMN_DATA_TWO_PROPERTIES_MESSAGE);
    
    Execution processInstance = runtimeService.createExecutionQuery().singleResult();
    
    Assert.assertNotNull("a process instance should have been started", processInstance);
    
    Object variable = runtimeService.getVariable(processInstance.getId(), "correlation-property");
    Assert.assertEquals("the initial correlation property should not have changed", "8", variable);
    
    variable = runtimeService.getVariable(processInstance.getId(), "another-correlation-property");
    Assert.assertNotNull("the new correlation property should have been set", variable);
    Assert.assertEquals("the new correlation property should have been populated correctly", "42", variable);
  }
}
