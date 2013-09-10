package de.hpi.uni.potsdam.bpmn_to_sql.correlation;

import org.camunda.bpm.engine.impl.bpmn.webservice.MessageDefinition;

public class MessageInstance {

  protected MessageDefinition messageDefinition;
  
  protected String content;

  public MessageDefinition getMessageDefinition() {
    return messageDefinition;
  }

  public void setMessageDefinition(MessageDefinition messageDefinition) {
    this.messageDefinition = messageDefinition;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
