package nl.tue.ais.bpmndata.choreographies.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

/**
 * Listener to populate a data object from form fields.
 * 
 * @author dfahland
 */
public class CompleteEnterResponseListener implements TaskListener {

  private String XML_ATTRIBUTE_TEMPLATE = "<%s>%s</%s>";
  
  public void notify(DelegateTask delegateTask) {
    StringBuilder xmlBuilder = new StringBuilder();
    // put here the name of the data object to populate
    // requirement: the name matches the name of a table
    xmlBuilder.append("<ResponseB>");
    // add here all attributes set in the form of the user task
    // requirement: the attributes in the user task form are named
    // exactly like columns in the corresponding table 
    addAttribute(xmlBuilder, delegateTask, "responseText");
    xmlBuilder.append("</ResponseB>");
    
    System.out.println(delegateTask.getVariables()); // debugging on the console
    
    String dataOutput = xmlBuilder.toString();
    delegateTask.getExecution().setVariable("dataOutput", dataOutput);
  }

  private void addAttribute(StringBuilder xmlBuilder, DelegateTask task, String attributeName) {
    Object value = task.getVariable(attributeName);
    if (value != null) {
      xmlBuilder.append(String.format(XML_ATTRIBUTE_TEMPLATE, attributeName, value, attributeName));
    }
  }
}
