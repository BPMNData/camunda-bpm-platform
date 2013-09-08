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
package org.camunda.bpm.engine.impl.pvm.runtime;

import java.util.logging.Logger;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.execution.CaseObjectUpdater;


/**
 * @author Tom Baeyens
 */
public class AtomicOperationTransitionCreateScope implements AtomicOperation {
  
  private static Logger log = Logger.getLogger(AtomicOperationTransitionCreateScope.class.getName());
  
  public boolean isAsync(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    return activity.isAsync();
  }

  public void execute(InterpretableExecution execution) {
    
    // we are continuing execution along this sequence flow:
    // reset activity instance id before creating the scope
    execution.setActivityInstanceId(execution.getParentActivityInstanceId());
    
    InterpretableExecution propagatingExecution = null;
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if (activity.isScope()) {
      propagatingExecution = (InterpretableExecution) execution.createExecution();
      propagatingExecution.setActivity(activity);
      propagatingExecution.setTransition(execution.getTransition());
      execution.setTransition(null);
      execution.setActive(false);
      execution.setActivity(null);
      log.fine("create scope: parent "+execution+" continues as execution "+propagatingExecution);
      propagatingExecution.initialize();

    } else {
      propagatingExecution = execution;
    }
    
//    CaseObjectUpdater coUpdater = new CaseObjectUpdater();
//    coUpdater.updateExecution(propagatingExecution);
    
    propagatingExecution.performOperation(AtomicOperation.TRANSITION_NOTIFY_LISTENER_START);
  }

  @Override
  public String getCanonicalName() {
    return "transition-create-scope";
  }
}
