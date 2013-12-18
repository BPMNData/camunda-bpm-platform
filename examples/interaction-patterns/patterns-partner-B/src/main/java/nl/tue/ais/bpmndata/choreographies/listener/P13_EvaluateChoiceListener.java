package nl.tue.ais.bpmndata.choreographies.listener;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

/**
 * Listener to populate a data object from form fields.
 * 
 * @author dfahland
 */
public class P13_EvaluateChoiceListener implements org.camunda.bpm.engine.delegate.ExecutionListener  {
	
  public void notify(DelegateExecution execution) throws Exception {
    
		String caseObjectId = ((ExecutionEntity) execution).getCaseObjectId();

	    String requestText = dbConnectionGetRequestText(Context.getProcessEngineConfiguration().getBpmnDataConfiguration(), 
	            "SELECT requestText FROM RequestB WHERE RequestB.requestID = '" + caseObjectId + "';");

	    execution.setVariable("RequestB_requestText", requestText);
  }

  public String dbConnectionGetRequestText(BpmnDataConfiguration config, String query) {
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
