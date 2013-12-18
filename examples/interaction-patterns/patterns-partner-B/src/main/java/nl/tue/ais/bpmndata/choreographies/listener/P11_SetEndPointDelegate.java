package nl.tue.ais.bpmndata.choreographies.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public class P11_SetEndPointDelegate implements JavaDelegate {

  public void execute(DelegateExecution execution) throws Exception {
	  
	String caseObjectId = ((ExecutionEntity) execution).getCaseObjectId();

    String endPoint = dbConnectionGetEndPointFromRequest(Context.getProcessEngineConfiguration().getBpmnDataConfiguration(), 
            "SELECT endPoint FROM RequestB WHERE requestID = '" + caseObjectId + "' AND state='received';");
    
    execution.setVariable("endPoint", endPoint);
  }
  
  public String dbConnectionGetEndPointFromRequest(BpmnDataConfiguration config, String query) {
    Connection con = null;
      Statement st = null;
      ResultSet rs = null;

      String result = null;
      
      String url = config.getJdbcUrl();
      String user = config.getJdbcUsername();
      String password = config.getJdbcPassword();

      try {
        con = DriverManager.getConnection(url, user, password);
        st = con.createStatement();
        
        System.out.println(query);
        
        rs = st.executeQuery(query);
        
        if (rs.next()) {
        	result = rs.getString(1); 
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
