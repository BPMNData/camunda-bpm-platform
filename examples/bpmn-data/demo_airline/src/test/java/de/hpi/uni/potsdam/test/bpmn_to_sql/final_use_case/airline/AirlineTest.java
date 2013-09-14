package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.airline;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static de.hpi.uni.potsdam.test.bpmn_to_sql.util.PersistentObjectAssertionSpecification.dataObjects;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.junit.Assert;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import de.hpi.uni.potsdam.test.bpmn_to_sql.util.AbstractBpmnDataTestCase;
import de.hpi.uni.potsdam.test.bpmn_to_sql.util.DatabaseSetup;

public class AirlineTest extends AbstractBpmnDataTestCase {


  private static final String DEMO_DATA_MAPPING_FILE = "de/hpi/uni/potsdam/test/bpmn_to_sql/db/demo_mappings.xml";
  
  private static final String REQUEST_MESSAGE = 
      "<message name=\"Message_2\">" +
      " <correlation>" +
      "   <key name=\"Flight_Request\">" +
      "     <property name=\"request_id\">42</property>" +
      "   </key>" +
      " </correlation>" +
      " <payload>"+
      "  <Flight_Request>" +
      "   <request_id>42</request_id>" +
      "   <departure>Berlin</departure>" +
      "   <destination>London</destination>" +
      "   <start_date>1.1.2000</start_date>" +
      "   <return_date>7.1.2000</return_date>" +
      "  </Flight_Request>" +
      " </payload>" +
      "</message>";
  
  protected WireMockServer mockEndpoint = new WireMockServer(WireMockConfiguration.wireMockConfig());
  
  protected String getCustomDbMappingsFilePath() {
    return DEMO_DATA_MAPPING_FILE;
  }
  
  protected void setUp() throws Exception {
    super.setUp();
    mockEndpoint.start();
    
    // TODO assert message that is sent
    stubFor(post(urlEqualTo("/bpmn-data-endpoint/message"))
        .withHeader("Content-Type", equalTo("application/xml"))
        .willReturn(aResponse().withStatus(204)));
  }
  
  @DatabaseSetup(resources = "setup_airline_db.sql")
  @Deployment(resources = "airline.compiled.bpmn")
  public void testCustomer() {
    runtimeService.correlateBpmnDataMessage(REQUEST_MESSAGE);
    
    dataObjects("Request", 1)
      .where("messageRequestID", "42")
      .where("departure", "Berlin")
      .where("destination", "London")
      .where("start_date", "1.1.2000")
      .where("return_date", "7.1.2000")
	  .shouldHave("state", "created")
    .doAssert();
    
    assertAndRunDataInputJobForActivity("UserTask_1", 1, 1);
    
    Task createOfferTask = taskService.createTaskQuery().singleResult();
    Map<String, Object> variables = new HashMap<String, Object>();
    variables.put("inboundFlightNumber", "1234");
    variables.put("outboundFlightNumber", "12345");
    variables.put("price", 1000.0d);
    taskService.complete(createOfferTask.getId(), variables);
    
    dataObjects("Offer", 1)
  	  .shouldHave("requestID", "5")
  	  .shouldHave("state", "received")
  	  .shouldHave("inboundFlightNumber", "1234")
  	  .shouldHave("outboundFlightNumber", "12345")
  	  .shouldHave("price", 1000.0d)
    .doAssert();
    
    assertAndRunDataInputJobForActivity("SendTask_1", 1, 1);
    
    // instance should have finished
    Assert.assertNull(runtimeService.createProcessInstanceQuery().singleResult());
    
  }
}
