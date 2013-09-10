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
package org.camunda.bpm.engine.impl.bpmn.webservice;

import org.camunda.bpm.engine.impl.bpmn.data.ItemDefinition;
import org.camunda.bpm.engine.impl.bpmn.data.StructureDefinition;

/**
 * Implementation of the BPMN 2.0 'message'.
 * While the parser adds the namespace to the id, the field originalId contains the id as specified in the BPMN diagram.
 * 
 * @author Esteban Robles Luna
 * @author BPMNData
 */
public class MessageDefinition {

  protected String id;
  
  protected String originalId;
  
  protected ItemDefinition itemDefinition;

  protected String name;
  
  public MessageDefinition(String id, String originalId, String name) {
    this.id = id;
    this.originalId = originalId;
    this.name = name;
  }
  
  public MessageInstance createInstance() {
    return new MessageInstance(this, this.itemDefinition.createInstance());
  }
  
  public ItemDefinition getItemDefinition() {
    return this.itemDefinition;
  }

  public StructureDefinition getStructureDefinition() {
    return this.itemDefinition.getStructureDefinition();
  }
  
  public void setItemDefinition(ItemDefinition itemDefinition) {
    this.itemDefinition = itemDefinition;
  }

  public String getId() {
    return this.id;
  }
  
  public String getOriginalId() {
    return this.originalId;
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
}
