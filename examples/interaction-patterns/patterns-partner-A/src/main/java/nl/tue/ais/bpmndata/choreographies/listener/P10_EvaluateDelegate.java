package nl.tue.ais.bpmndata.choreographies.listener;

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

public class P10_EvaluateDelegate implements JavaDelegate {

  public void execute(DelegateExecution execution) throws Exception {
	  
	String caseObjectId = ((ExecutionEntity) execution).getCaseObjectId();

    int receivedResponses = dbConnectionGetReceivedSubRequests(Context.getProcessEngineConfiguration().getBpmnDataConfiguration(), 
            "SELECT * FROM `SubRequestA` WHERE SubRequestA.requestID = '" + caseObjectId + "' AND SubRequestA.state='received';");
    
    boolean inRange = false;
    if (receivedResponses >= 2 & receivedResponses <= 3) {
    	inRange = true;
    }
    
    if (!inRange && receivedResponses > 0) {
    	execution.setVariable("sendCancel", new Boolean(true));
    } else {
    	execution.setVariable("sendCancel", new Boolean(false));
    }
  }
  
  public int dbConnectionGetReceivedSubRequests(BpmnDataConfiguration config, String query) {
    Connection con = null;
      Statement st = null;
      ResultSet rs = null;

      int result = 0;
      
      String url = config.getJdbcUrl();
      String user = config.getJdbcUsername();
      String password = config.getJdbcPassword();

      try {
        con = DriverManager.getConnection(url, user, password);
        st = con.createStatement();
        
        System.out.println(query);
        
        rs = st.executeQuery(query);
        
        while (rs.next()) {
        	System.out.println(rs.toString());
        	result++;
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
