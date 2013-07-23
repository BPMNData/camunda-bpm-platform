package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.runtime.AtomicOperationActivityExecute;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;

public class QueryExecutionHandler {
  
  private static QueryExecutionHandler instance = new QueryExecutionHandler(Context.getProcessEngineConfiguration().getBpmnDataConfiguration());
  
  private static Logger log = Logger.getLogger(AtomicOperationActivityExecute.class.getName());
  
  private static Connection con;
  private static Statement st;
  private static ResultSet rs;
  private static ResultSetMetaData rsMetadata;
  
  protected BpmnDataConfiguration configuration;
  
  private QueryExecutionHandler(BpmnDataConfiguration configuration) {
    this.configuration = configuration;
    setupConnection();
  }
  
  protected void finalize( ){
    try {
      if (st != null) {
        st.close();
      }
      if (con != null) {
        con.close();
      }
    } catch (SQLException ex) {
      log.log(Level.WARNING, ex.getMessage(), ex);
    }
    
  }
  
  public static QueryExecutionHandler getInstance() {
    return instance;
  }

  private void setupConnection() {

    String url = configuration.getJdbcUrl();
    String user = configuration.getJdbcUsername();
    String password = configuration.getJdbcPassword();

    try {
      con = DriverManager.getConnection(url, user, password);
      st = con.createStatement();
    } catch (SQLException ex) {
      log.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }
  
  public void runQuery(String query){
    try {
      rs = st.executeQuery(query);
      rsMetadata = rs.getMetaData();
    } catch (SQLException ex) {
      log.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }

  public void runUpdate(String query){
    try {
      st.executeUpdate(query);
    } catch (SQLException ex) {
      log.log(Level.SEVERE, ex.getMessage(), ex);
    }
  }
  
  public ArrayList<Object> getNextResult(){
    ArrayList<Object> result = new ArrayList<Object>();
    try {
      if(rs.next()){
        for(int i = 1; i <= rsMetadata.getColumnCount(); i++){
          result.add(rs.getObject(i)); 
        }
      }
      else {
        result = null;
      }
      return result;
    } catch (SQLException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
    return null;
  }
  
  public ArrayList<String> getColumnNames(){
    ArrayList<String> columnNames = new ArrayList<String>();
    try {
      for(int i = 1; i <= rsMetadata.getColumnCount(); i++){
        columnNames.add(rsMetadata.getColumnName(i));
      }
      return columnNames;
    } catch (SQLException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
    return null;
  }
  
  public int getColumnCount(){
    int count = 0;
    try {
      count = rsMetadata.getColumnCount();
    } catch (SQLException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
    return count;
  }
  

}