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

package org.camunda.bpm.engine.impl.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.MismatchingMessageCorrelationException;
import org.camunda.bpm.engine.impl.ExecutionQueryImpl;
import org.camunda.bpm.engine.impl.ProcessDefinitionQueryImpl;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.engine.runtime.Execution;

/**
 * @author Thorben Lindhauer
 * @author Dirk Fahland
 */
public class DefaultCorrelationHandler implements CorrelationHandler {

  public Execution correlateMessageToExecution(CommandContext commandContext, String messageName,
      CorrelationSet correlationSet) {
    ExecutionQueryImpl query = new ExecutionQueryImpl();
    
    Map<String, Object> correlationKeys = correlationSet.getCorrelationKeys();
    if (correlationKeys != null) {
      for (Map.Entry<String, Object> correlationKey : correlationKeys.entrySet()) {
        query.processVariableValueEquals(correlationKey.getKey(), correlationKey.getValue());
      }
    }
    
    String businessKey = correlationSet.getBusinessKey();
    if(businessKey != null) {
      query.processInstanceBusinessKey(businessKey);
    }
    
    query.messageEventSubscriptionName(messageName);
    List<Execution> matchingExecutions = query.executeList(commandContext, null);
    
    if (matchingExecutions.size() > 1) {
      throw new MismatchingMessageCorrelationException(messageName, businessKey, correlationKeys, 
          String.valueOf(matchingExecutions.size()) + " executions match the correlation keys. Should be one or zero.");
    }
    
    Execution matchingExecution = null;
    if (!matchingExecutions.isEmpty()) {
      matchingExecution = matchingExecutions.get(0);
    } else {
    	// try matching via partial correlation
    	matchingExecution = correlateMessageToExecution_PartialUnitialized(commandContext, messageName, correlationSet);
    }

    return matchingExecution;
  }
  
  	/**
  	 * This method implements the incremental correlation key binding behavior
  	 * specified in the BPMN2.0 standard, Section 8.3.2 to allow initializing
  	 * a CorrelationKey that has not been initialized earlier, but requires
  	 * matching on all initialized correlation keys.
  	 * 
	 * @param commandContext
	 * @param messageName
	 * @param correlationSet
	 * @return the execution that partially matches the correlation set where a
	 *         non-match is allowed if the correlation property is not yet
	 *         initialized. The returned execution is the one with the highest
	 *         fit to the correlation set (largest number of matching
	 *         correlation properties), if it exists.
	 */
  public Execution correlateMessageToExecution_PartialUnitialized(
		  CommandContext commandContext, 
		  String messageName,
	      CorrelationSet correlationSet)
  {
	  	// get all executions matching the business key and waiting for the particular message
	    ExecutionQueryImpl query = new ExecutionQueryImpl();
	    
	    String businessKey = correlationSet.getBusinessKey();
	    if(businessKey != null) {
	      query.processInstanceBusinessKey(businessKey);
	    }
	    query.messageEventSubscriptionName(messageName);
	    List<Execution> matchingExecutions = query.executeList(commandContext, null);
	    
	    Map<String, Object> correlationKeys = correlationSet.getCorrelationKeys();
	    
	    // then iterate over all executions and sort them by the number of best key matches
	    Map<Integer, List<Execution>> executionsPerKeyMatches = new HashMap<Integer, List<Execution>>();
	    int bestKeyMatch = -1;
	    for (Execution execution : matchingExecutions){ 
	    	
	    	if (!(execution instanceof ExecutionEntity)) continue;
	    	ExecutionEntity executionEntity = (ExecutionEntity)execution;
	
	    	boolean isCompatible = true;
	    	int matchingKeys = 0;
	    	if (correlationKeys != null) {
		    	for (Map.Entry<String, Object> correlation : correlationKeys.entrySet()) {
		    		if (executionEntity.getVariables().containsKey(correlation.getKey())) {
		    			if (!executionEntity.getVariables().get(correlation.getKey()).equals(correlation.getValue())) {
		    				isCompatible = false;
		    				break;
		    			} {
		    				matchingKeys++;
		    			}
		    		}
		    	}
	    	}
	    	if (isCompatible) {
	    		if (matchingKeys > bestKeyMatch) bestKeyMatch = matchingKeys;
	    		if (!executionsPerKeyMatches.containsKey(matchingKeys)) executionsPerKeyMatches.put(matchingKeys, new ArrayList<Execution>());
	    		executionsPerKeyMatches.get(matchingKeys).add(execution);
	    	}
	    }

	    Execution matchingExecution = null;
	    if (bestKeyMatch >= 0) {
	    	if (executionsPerKeyMatches.get(bestKeyMatch).size() > 1) {
	    		throw new MismatchingMessageCorrelationException(messageName, businessKey, correlationKeys, 
	    				String.valueOf(executionsPerKeyMatches.get(bestKeyMatch).size()) + " executions partially match the correlation keys with "+bestKeyMatch+" matches. Should be one or zero.");
	    	}
		    if (!executionsPerKeyMatches.get(bestKeyMatch).isEmpty()) {
		      matchingExecution = executionsPerKeyMatches.get(bestKeyMatch).get(0);
		    }
	    }

	    return matchingExecution;
	  }

  public ProcessDefinition correlateMessageToProcessDefinition(CommandContext commandContext, String messageName) {
    ProcessDefinitionQueryImpl query = (ProcessDefinitionQueryImpl) new ProcessDefinitionQueryImpl().messageEventSubscriptionName(messageName);
    List<ProcessDefinition> matchingDefinitions = query.executeList(commandContext, null);
    
    if (matchingDefinitions.size() > 1) {
      throw new MismatchingMessageCorrelationException(messageName, 
          String.valueOf(matchingDefinitions.size()) + " process definitions match the message " + 
          messageName + ". Should be one or zero.");
    }
    
    ProcessDefinition matchingDefinition = null;
    if (!matchingDefinitions.isEmpty()) {
      matchingDefinition = matchingDefinitions.get(0);
    }

    return matchingDefinition;
  }

}
