/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.camunda.bpm.engine.test.api.runtime;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;

/**
 * @author Thorben Lindhauer
 */
public class MessageCorrelationTest extends PluggableProcessEngineTestCase {

  @Deployment
  public void testCatchingMessageEventCorrelation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);
    
    variables = new HashMap<String, Object>();
    variables.put("aKey", "anotherValue");
    runtimeService.startProcessInstanceByKey("process", variables);
    
    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<String, Object>();
    correlationKeys.put("aKey", "aValue");
    Map<String, Object> messagePayload = new HashMap<String, Object>();
    messagePayload.put("aNewKey", "aNewVariable");
    
    runtimeService.correlateMessage(messageName, correlationKeys, messagePayload);
    
    long uncorrelatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "anotherValue").messageEventSubscriptionName("newInvoiceMessage")
        .count();
    assertEquals(1, uncorrelatedExecutions);
    
    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aKey", "aValue").processVariableValueEquals("aNewKey", "aNewVariable")
        .count();
    assertEquals(1, correlatedExecutions);
  }
  
  @Deployment(resources = "org/camunda/bpm/engine/test/api/runtime/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  public void testTwoMatchingProcessInstancesCorrelation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);
    
    variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", variables);
    
    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<String, Object>();
    correlationKeys.put("aKey", "aValue");
    
    try {
      runtimeService.correlateMessage(messageName, correlationKeys);
      fail("Expected an Exception");
    } catch (MismatchingMessageCorrelationException e) {
      assertTextPresent("2 executions match the correlation keys.", e.getMessage());
    }
  }
  
  @Deployment(resources = "org/camunda/bpm/engine/test/api/runtime/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  public void testExecutionCorrelationByBusinessKey() {
    String businessKey = "aBusinessKey";
    runtimeService.startProcessInstanceByKey("process", businessKey);
    runtimeService.correlateMessage("newInvoiceMessage", businessKey);
    
    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery().activityId("task").count();
    assertEquals(1, correlatedExecutions);
  }
  
  @Deployment(resources = "org/camunda/bpm/engine/test/api/runtime/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  public void testExecutionCorrelationByBusinessKeyWithVariables() {
    String businessKey = "aBusinessKey";
    runtimeService.startProcessInstanceByKey("process", businessKey);
    
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.correlateMessage("newInvoiceMessage", businessKey, variables);
    
    // the execution that has been correlated should have advanced
    long correlatedExecutions = runtimeService.createExecutionQuery()
        .processVariableValueEquals("aKey", "aValue").count();
    assertEquals(1, correlatedExecutions);
  }

  
  public void testNullMessageEventCorrelation() {
    try {
      runtimeService.correlateMessage(null);
      fail("Exception expected");
    } catch (ProcessEngineException e) {
      assertTextPresent("messageName cannot be null", e.getMessage());
    }
  }
  
  @Deployment
  public void testMessageStartEventCorrelation() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<String, Object>(), variables);
    
    long instances = runtimeService.createProcessInstanceQuery().processDefinitionKey("messageStartEvent")
        .variableValueEquals("aKey", "aValue").count();
    assertEquals(1, instances);
  }

  @Deployment
  public void testMatchingStartEventAndExecution() {
	// instantiate process with correlation information 
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<String, Object>(), variables);

    // check that there is 1 instance
    long instances1a = runtimeService.createProcessInstanceQuery().processDefinitionKey("process")
            .variableValueEquals("aKey", "aValue").count();
    assertEquals(1, instances1a);
    
    // send second message with same correlation information
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<String, Object>(), variables);
    
    // check that there is still 1 instance
    long instances1b = runtimeService.createProcessInstanceQuery().processDefinitionKey("process")
        .variableValueEquals("aKey", "aValue").count();
    assertEquals(1, instances1b);
    
	// instantiate process second time with new correlation information 
    Map<String, Object> variables2 = new HashMap<String, Object>();
    variables2.put("aKey", "aValue2");
    runtimeService.correlateMessage("newInvoiceMessage", new HashMap<String, Object>(), variables2);
    
    // check that there is another instance
    long instances2 = runtimeService.createProcessInstanceQuery().processDefinitionKey("process")
        .variableValueEquals("aKey", "aValue2").count();
    assertEquals(1, instances2);
  }
  
  public void testMessageStartEventCorrelationWithNonMatchingDefinition() {
    try {
      runtimeService.correlateMessage("aMessageName");
      fail("Expect an Exception");
    } catch (MismatchingMessageCorrelationException e) {
      assertTextPresent("Cannot correlate message", e.getMessage());
    }
  }
  
  @Deployment(resources = "org/camunda/bpm/engine/test/api/runtime/MessageCorrelationTest.testCatchingMessageEventCorrelation.bpmn20.xml")
  public void testCorrelationByBusinessKeyAndVariables() {
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);
    
    variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", "anotherBusinessKey", variables);
    
    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<String, Object>();
    correlationKeys.put("aKey", "aValue");
    
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("aProcessVariable", "aVariableValue");
    runtimeService.correlateMessage(messageName, "aBusinessKey", correlationKeys, processVariables);
    
    Execution correlatedExecution = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aProcessVariable", "aVariableValue")
        .singleResult();
    
    assertNotNull(correlatedExecution);
    
    ProcessInstance correlatedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId()).singleResult();
    
    assertEquals("aBusinessKey", correlatedProcessInstance.getBusinessKey());
  }
  
  @Deployment(resources = "org/camunda/bpm/engine/test/api/runtime/MessageCorrelationTest.testPartialCorrelation.bpmn20.xml")
  public void testPartialCorrelationFailsOnInitializedKeys() {

		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("aKey", "aValue");
		runtimeService.startProcessInstanceByKey("process", "aBusinessKey",
				variables);

		try {
			String messageName = "newInvoiceMessage";
			Map<String, Object> correlationKeys = new HashMap<String, Object>();
			correlationKeys.put("aKey", "aValueFalse");
			correlationKeys.put("aKey2", "aValue2");

			Map<String, Object> processVariables = new HashMap<String, Object>();
			processVariables.put("aKey2", "aValue2");
			runtimeService.correlateMessage(messageName, "aBusinessKey",
					correlationKeys, processVariables);

			fail("Expect an Exception");
		} catch (MismatchingMessageCorrelationException e) {
			assertTextPresent("Cannot correlate message", e.getMessage());
		}
  }
  
  @Deployment
  public void testPartialCorrelation() {

    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("aKey", "aValue");
    runtimeService.startProcessInstanceByKey("process", "aBusinessKey", variables);
    
    String messageName = "newInvoiceMessage";
    Map<String, Object> correlationKeys = new HashMap<String, Object>();
    correlationKeys.put("aKey", "aValue");
    correlationKeys.put("aKey2", "aValue2");
    
    Map<String, Object> processVariables = new HashMap<String, Object>();
    processVariables.put("aKey2", "aValue2");
    runtimeService.correlateMessage(messageName, "aBusinessKey", correlationKeys, processVariables);

    Map<String, Object> processVariables2 = new HashMap<String, Object>();
    processVariables2.put("aProcessVariable", "aVariableValue");
    runtimeService.correlateMessage(messageName, "aBusinessKey", correlationKeys, processVariables2);
    
    Execution correlatedExecution = runtimeService.createExecutionQuery()
        .activityId("task").processVariableValueEquals("aProcessVariable", "aVariableValue")
        .singleResult();
    
    assertNotNull(correlatedExecution);
    
    ProcessInstance correlatedProcessInstance = runtimeService.createProcessInstanceQuery()
        .processInstanceId(correlatedExecution.getProcessInstanceId()).singleResult();
    
    assertEquals("aBusinessKey", correlatedProcessInstance.getBusinessKey());
  }
  
  
}
