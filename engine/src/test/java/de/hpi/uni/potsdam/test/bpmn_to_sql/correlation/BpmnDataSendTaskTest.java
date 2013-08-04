package de.hpi.uni.potsdam.test.bpmn_to_sql.correlation;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

public class BpmnDataSendTaskTest extends PluggableProcessEngineTestCase {

  private static final String CORRELATION_PROPERTY_VALUE = "8";
  private static final String BPMN_DATA_MESSAGE = 
      "<message name=\"AuctionRequest\">" +
      " <correlation>" +
      "   <key name=\"some key\">" +
      "     <property name=\"some-prop\">" + CORRELATION_PROPERTY_VALUE + "</property>" +
      "   </key>" +
      " </correlation>" +
      "</message>";
  
  protected WireMockServer mockEndpoint = new WireMockServer(WireMockConfiguration.wireMockConfig());
  
  protected void setUp() throws Exception {
    super.setUp();
    mockEndpoint.start();
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  protected void tearDown() throws Exception {
    super.tearDown();
    mockEndpoint.stop();
  }
  
  @Deployment(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/correlation/simpleBpmnDataSendTask.bpmn20.xml")
  public void testSendTaskBehavior() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("dataInput", "<message></message>");
    runtimeService.startProcessInstanceByKey("sellerProcess", variables);
    
    Execution waitingExecution = runtimeService.createExecutionQuery().activityId("task").singleResult();
    
    Assert.assertNotNull("The process should have successfully processed the send task", waitingExecution);
  }
  
  @Deployment(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/correlation/simpleBpmnDataSendTask.bpmn20.xml")
  public void testSendAttemptWithEmptyMessage() {
    try {
      runtimeService.startProcessInstanceByKey("sellerProcess");
      Assert.fail();
    } catch (ProcessEngineException e) {
      // happy path
      assertTextPresent("message content is empty", e.getMessage());
    }
  }
  
  @Deployment(resources = "de/hpi/uni/potsdam/test/bpmn_to_sql/correlation/simpleBpmnDataSendTask.bpmn20.xml")
  public void testCorrelationPropertyPopulation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("dataInput", BPMN_DATA_MESSAGE);
    runtimeService.startProcessInstanceByKey("sellerProcess", variables);
    
    Execution waitingExecution = runtimeService.createExecutionQuery().singleResult();
    
    if (waitingExecution == null) {
      Assert.fail("The process should have successfully processed the send task");
    }
    
    // note: here, the execution is also the process instance, so we can directly check on that
    Object variable = runtimeService.getVariable(waitingExecution.getId(), "correlation-property");
    Assert.assertNotNull("the correlation property should be set on the execution", variable);
    
    Assert.assertEquals("the correlation property should be populated correctly", 
        CORRELATION_PROPERTY_VALUE, variable);
  }
  
  // ensure, that variable is set locally on sub process execution
  @Deployment
  public void testSubprocessCorrelationPropertyPopulation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("dataInput", BPMN_DATA_MESSAGE);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("sellerWithSubprocess", variables);
    
    // in the single instance subprocess case, the subprocess execution should be the one on the user task
    Execution subprocessScopeExecution = runtimeService.createExecutionQuery().activityId("task").singleResult();
    
    if (subprocessScopeExecution == null) {
      Assert.fail("The subprocess should still be active");
    }
    
    // note: here, the execution is also the process instance, so we can directly check on that
    Object variable = runtimeService.getVariableLocal(subprocessScopeExecution.getId(), "correlation-property");
    Assert.assertNotNull("the correlation property should be set locally on the subprocess execution", variable);
    
    Assert.assertEquals("the correlation property should be populated correctly", 
        CORRELATION_PROPERTY_VALUE, variable);
    
    Object instanceVariable = runtimeService.getVariable(instance.getId(), "correlation-property");
    Assert.assertNull("The correlation property should be not accessible in " +
    		"the outer process instance scope", instanceVariable);
  }
  
  @Deployment
  public void testMultiInstanceSubprocessCorrelationPropertyPopulation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("dataInput", BPMN_DATA_MESSAGE);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("sellerWithSubprocess", variables);
    
    List<Execution> subprocessScopeExecutions = runtimeService.createExecutionQuery().activityId("task").list();
    
    Assert.assertEquals(2, subprocessScopeExecutions.size());
    
    for (Execution subprocessExecution : subprocessScopeExecutions) {
      Object variable = runtimeService.getVariableLocal(subprocessExecution.getId(), "correlation-property");
      Assert.assertNotNull("the correlation property should be set locally on the subprocess execution", variable);
      
      Assert.assertEquals("the correlation property should be populated correctly", 
          CORRELATION_PROPERTY_VALUE, variable);
      
      Object instanceVariable = runtimeService.getVariable(instance.getId(), "correlation-property");
      Assert.assertNull("The correlation property should be not accessible in " +
          "the outer process instance scope", instanceVariable);
    }
  }
  
  @Deployment
  public void testParallelSubprocessCorrelationPropertyPopulation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("dataInput", BPMN_DATA_MESSAGE);
    ProcessInstance instance = runtimeService.startProcessInstanceByKey("sellerWithSubprocess", variables);
    
    Execution subprocessScopeExecution = runtimeService.createExecutionQuery().activityId("subProcessFork").singleResult();
    
    if (subprocessScopeExecution == null) {
      Assert.fail("The subprocess should still be active");
    }
    
    // note: here, the execution is also the process instance, so we can directly check on that
    Object variable = runtimeService.getVariableLocal(subprocessScopeExecution.getId(), "correlation-property");
    Assert.assertNotNull("the correlation property should be set locally on the subprocess execution", variable);
    
    Assert.assertEquals("the correlation property should be populated correctly", 
        CORRELATION_PROPERTY_VALUE, variable);
    
    Object instanceVariable = runtimeService.getVariable(instance.getId(), "correlation-property");
    Assert.assertNull("The correlation property should be not accessible in " +
        "the outer process instance scope", instanceVariable);
  }
}
