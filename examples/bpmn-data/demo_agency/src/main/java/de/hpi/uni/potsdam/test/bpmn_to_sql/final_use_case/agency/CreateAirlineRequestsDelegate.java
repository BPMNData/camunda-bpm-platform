package de.hpi.uni.potsdam.test.bpmn_to_sql.final_use_case.agency;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public class CreateAirlineRequestsDelegate implements JavaDelegate {

  private String XML_ATTRIBUTE_TEMPLATE = "<%s>%s</%s>";
  private static final String[] TRAVEL_DETAILS_ATTRIBUTES = new String[]{"departure", "destination", "start_date", "return_date"};
  
  @Override
  public void execute(DelegateExecution execution) throws Exception {

    String caseObjectId = ((ExecutionEntity) execution).getCaseObjectId();
    
    Map<String, String> travelDetails = dbConnectionSelectTravelDetails(Context.getProcessEngineConfiguration().getBpmnDataConfiguration(), 
        "SELECT * FROM TravelDetails WHERE travelID = '" + caseObjectId + "'");
    String dataOutput = null;
    if (!travelDetails.isEmpty()) {
      StringBuilder xmlBuilder = new StringBuilder();
      xmlBuilder.append("<AirlineRequest>");
      for (String attribute : TRAVEL_DETAILS_ATTRIBUTES) {
        addAttribute(xmlBuilder, attribute, travelDetails.get(attribute));
      }
      xmlBuilder.append("</AirlineRequest>");
      dataOutput = xmlBuilder.toString();
    } else {
      throw new RuntimeException("No travel details with id " + caseObjectId + " found");
    }
    
    execution.setVariable("numberOfRequests", "2");
    execution.setVariableLocal("dataOutput", dataOutput);
  }
  
  private void addAttribute(StringBuilder xmlBuilder, String attributeName, String attributeValue) {
    if (attributeValue != null) {
      xmlBuilder.append(String.format(XML_ATTRIBUTE_TEMPLATE, attributeName, attributeValue, attributeName));
    }
  }
  

  public Map<String, String> dbConnectionSelectTravelDetails(BpmnDataConfiguration config, String query) {
    Connection con = null;
      Statement st = null;
      ResultSet rs = null;

      Map<String, String> result = new HashMap<String, String>();
      
      String url = config.getJdbcUrl();
      String user = config.getJdbcUsername();
      String password = config.getJdbcPassword();

      try {
        con = DriverManager.getConnection(url, user, password);
        st = con.createStatement();
        rs = st.executeQuery(query);
        
        if (rs.next()) {
          for (String attribute : TRAVEL_DETAILS_ATTRIBUTES) {
            result.put(attribute, rs.getString(attribute));
          }
        }
          
      } catch (SQLException ex) {
          System.out.println(ex.getMessage());

      } finally {
          try {
              if (st != null) {
                  st.close();
              }
              if (con != null) {
                  con.close();
              }
          } catch (SQLException ex) {
            System.out.println(ex.getMessage());
          }
      }
      return result;
  }

}
