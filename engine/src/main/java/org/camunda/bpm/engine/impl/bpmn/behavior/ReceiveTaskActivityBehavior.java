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

package org.camunda.bpm.engine.impl.bpmn.behavior;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.MessageFlow;
import de.hpi.uni.potsdam.bpmn_to_sql.correlation.CorrelationHelper;


/**
 * A receive task is a wait state that waits for the receival of some message.
 * 
 * Currently, the only message that is supported is the external trigger,
 * given by calling the {@link RuntimeService#signal(String)} operation.
 * 
 * @author Joram Barrez
 */
public class ReceiveTaskActivityBehavior extends TaskActivityBehavior {

  public void execute(ActivityExecution execution) throws Exception {
    // Do nothing: waitstate behavior
    
    
    // if this is an instantiating receive task, do not enter wait state
    if ("true".equals(execution.getActivity().getProperty("instantiate"))) {
      leave(execution);
    }
  }
  
  public void signal(ActivityExecution execution, String signalName, Object data) throws Exception {
    if (Context.getProcessEngineConfiguration().isBpmnDataAware()) {
      String message = (String) execution.getVariable("message");
      updateCorrelationKeys(execution, message);
      execution.setVariableLocal("dataOutput", message);
    }
    
    leave(execution);
  }
  
  protected void updateCorrelationKeys(ActivityExecution execution, String message) {
    MessageFlow messageFlow = ((ActivityImpl) execution.getActivity()).getIncomingMessageFlow();
    
    if (messageFlow == null) {
      throw new ProcessEngineException("Start event " + execution.getActivity().getId() + " has no incoming message flow");
    }
    
    // the keys have to be set on the parent scope, as a receive task/event always creates a new child scope
    CorrelationHelper.populateCorrelationKeysInScope(execution.getParent(), message, messageFlow.getCorrelationKeys());
  }
  
}
