package nl.tue.ais.bpmndata.choreographies.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class P9_CreateSubRequestsListener implements TaskListener {

  private String XML_ATTRIBUTE_TEMPLATE = "<%s>%s</%s>";

  public void notify(DelegateTask delegateTask) {


    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<SubRequestA>");
    addAttribute(xmlBuilder, delegateTask, "requestText");
    xmlBuilder.append("</SubRequestA>");
    
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
