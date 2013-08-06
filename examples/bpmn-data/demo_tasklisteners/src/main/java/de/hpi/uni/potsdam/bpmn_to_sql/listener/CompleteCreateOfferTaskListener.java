package de.hpi.uni.potsdam.bpmn_to_sql.listener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

public class CompleteCreateOfferTaskListener implements TaskListener {

  private String XML_ATTRIBUTE_TEMPLATE = "<%s>%s</%s>";
  
  public void notify(DelegateTask delegateTask) {
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<Offer>");
    addAttribute(xmlBuilder, delegateTask, "inboundFlightNumber");
    addAttribute(xmlBuilder, delegateTask, "outboundFlightNumber");
    addAttribute(xmlBuilder, delegateTask, "price");
    xmlBuilder.append("</Offer>");
    
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