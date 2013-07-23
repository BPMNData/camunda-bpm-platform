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

package org.camunda.bpm.engine.impl.cmd;

import java.io.Serializable;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.db.DbSqlSession;
import org.camunda.bpm.engine.impl.form.StartFormHandler;
import org.camunda.bpm.engine.impl.interceptor.Command;
import org.camunda.bpm.engine.impl.interceptor.CommandContext;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.HistoricFormPropertyEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.runtime.ProcessInstance;


/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class SubmitStartFormCmd implements Command<ProcessInstance>, Serializable {

  private static final long serialVersionUID = 1L;
  
  protected final String processDefinitionId;
  protected final String businessKey;
  protected Map<String, String> properties;
  
  public SubmitStartFormCmd(String processDefinitionId, String businessKey, Map<String, String> properties) {
    this.processDefinitionId = processDefinitionId;
    this.businessKey = businessKey;
    this.properties = properties;
  }

  @Override
  public ProcessInstance execute(CommandContext commandContext) {
    ProcessDefinitionEntity processDefinition = Context
      .getProcessEngineConfiguration()
      .getDeploymentCache()
      .findDeployedProcessDefinitionById(processDefinitionId);

    if (processDefinition == null) {
      throw new ProcessEngineException("No process definition found for id = '" + processDefinitionId + "'");
    }
    
    ExecutionEntity processInstance = null;
    if (businessKey != null) {
      processInstance = processDefinition.createProcessInstance(businessKey);
    } else {
      processInstance = processDefinition.createProcessInstance();
    }

    int historyLevel = Context.getProcessEngineConfiguration().getHistoryLevel();
    if (historyLevel>=ProcessEngineConfigurationImpl.HISTORYLEVEL_ACTIVITY) {
      DbSqlSession dbSqlSession = commandContext.getSession(DbSqlSession.class);

      if (historyLevel>=ProcessEngineConfigurationImpl.HISTORYLEVEL_AUDIT) {
        for (String propertyId: properties.keySet()) {
          String propertyValue = properties.get(propertyId);
          HistoricFormPropertyEntity historicFormProperty = new HistoricFormPropertyEntity(processInstance, propertyId, propertyValue);
          dbSqlSession.insert(historicFormProperty);
        }
      }
    }
    
    StartFormHandler startFormHandler = processDefinition.getStartFormHandler();
    startFormHandler.submitFormProperties(properties, processInstance);

    processInstance.start();
    
    return processInstance;
  }
}
