package de.hpi.uni.potsdam.bpmn_to_sql.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class CompleteTravelDetailsTaskListener implements TaskListener {

  private String XML_ATTRIBUTE_TEMPLATE = "<%s>%s</%s>";
  
  public void notify(DelegateTask delegateTask) {
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<TravelDetails>");
    addAttribute(xmlBuilder, delegateTask, "departure");
    addAttribute(xmlBuilder, delegateTask, "destination");
    addAttribute(xmlBuilder, delegateTask, "start_date");
    addAttribute(xmlBuilder, delegateTask, "return_date");
    addAttribute(xmlBuilder, delegateTask, "hotel");
    addAttribute(xmlBuilder, delegateTask, "price");
    xmlBuilder.append("</TravelDetails>");
    
    System.out.println(delegateTask.getVariables());
    
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
