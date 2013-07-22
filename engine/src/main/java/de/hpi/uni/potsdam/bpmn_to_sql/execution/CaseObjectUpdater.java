package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;

import de.hpi.uni.potsdam.bpmn_to_sql.BpmnDataConfiguration;
import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;
import de.hpi.uni.potsdam.bpmn_to_sql.job.AsyncDataInputJobHandler;

public class CaseObjectUpdater {
  
  private static Logger log = Logger.getLogger(AsyncDataInputJobHandler.class.getName());

  public void updateExecution(ActivityExecution execution) {
    String dataObjectID = execution.getEffectiveCaseObjectID();
    ActivityImpl activity = (ActivityImpl) execution.getActivity();

    // if process is an MI-sub-process, the activiti:collection variable is set
    // with the collection of PKs of the given scope object
    // and set sub process key once the sub process is initialized with PK of
    // case object
    if (execution.isScope() && activity.getProperty("type").equals("subProcess")) {
      DataObject dataObj = new DataObject();
      ArrayList<DataObject> dataObjectList = BpmnParse.getInputData().get(activity.getId());
      if (dataObjectList == null) {
        return;
      }
      
      for (DataObject dataObject : dataObjectList) {
        if (dataObject.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(activity.getId()))) {
          dataObj = dataObject; // the dataObj is selected which has the same
                                // name as the caseObj
          break;
        }
      }
      HashMap<String, Object> m = (HashMap<String, Object>) execution.getVariables();
      // if(!execution.hasVariables()) ////Sub-Process Instance has usually no
      // variables besides it is a started multi-sub-process instance by a scope
      // execution
      if (!execution.getVariableNames().contains("loopCounter")) { // loopCounter
                                                                   // only
                                                                   // exists in
                                                                   // the second
                                                                   // and later
                                                                   // subprocess
                                                                   // instance
        String query = new String();
        // create query to select the list of PKs of the scopeObject for that
        // process instance
        query = "SELECT `" + dataObj.getPkey() + "` FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` = \"" + dataObjectID + "\"";
        System.out.println(query);
        ArrayList<String> miList = dbConnection3(query);

        if (activity.getProperties().containsKey("multiInstance")) { // check
                                                                     // whether
                                                                     // the
                                                                     // current
                                                                     // sub-process
                                                                     // is
                                                                     // MI-instance
          // if MI-instance, the activiti:collection variable is set with the
          // list of PKs
          execution.setVariable(dataObj.getName(), miList);
          execution.setCaseObjectID(miList.get(0)); // current sub-process
                                                    // instance gets the first
                                                    // primary key of the return
                                                    // ArrayList
        } else {
          execution.setCaseObjectID(miList.get(miList.size() - 1));
        }

        execution.setCaseObjectID(miList.get(0)); // current sub-process
                                                  // instance gets the first
                                                  // primary key of the return
                                                  // ArrayList (in case of
                                                  // simple sub-process, is it
                                                  // only one)
      } else {
        execution.setCaseObjectID((String) execution.getVariable(dataObj.getPkey()));
      }
    }
  }
  
  public void updateMICaseObjectCollection(ActivityExecution execution) {
    String dataObjectID = execution.getEffectiveCaseObjectID();
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    
    DataObject dataObj = new DataObject();
    ArrayList<DataObject> dataObjectList = BpmnParse.getInputData().get(activity.getId());
    if (dataObjectList == null) {
      return;
    }
    
    for (DataObject dataObject : dataObjectList) {
      if (dataObject.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(activity.getId()))) {
        dataObj = dataObject; // the dataObj is selected which has the same
                              // name as the caseObj
        break;
      }
    }
    
    String query = new String();
    // create query to select the list of PKs of the scopeObject for that
    // process instance
    query = "SELECT `" + dataObj.getPkey() + "` FROM `" + dataObj.getName() + "` WHERE `" + dataObj.getFkeys().get(0) + "` = \"" + dataObjectID + "\"";
    System.out.println(query);
    ArrayList<String> miList = dbConnection3(query);
    
    execution.setVariable(dataObj.getName(), miList);
    execution.setCaseObjectID(miList.get(0));
  }
  

  // TODO: BPMN_SQL added
  public ArrayList<String> dbConnection3(String query) {
    Connection con = null;
    Statement st = null;
    ResultSet rs = null;
    ArrayList<String> result = new ArrayList<String>();

    BpmnDataConfiguration config = Context.getProcessEngineConfiguration().getBpmnDataConfiguration();
    String url = config.getJdbcUrl();
    String user = config.getJdbcUsername();
    String password = config.getJdbcPassword();

    try {
      con = DriverManager.getConnection(url, user, password);
      st = con.createStatement();
      rs = st.executeQuery(query);

      while (rs.next()) {
        result.add(rs.getString(1));
      }

    } catch (SQLException ex) {
      log.log(Level.SEVERE, ex.getMessage(), ex);

    } finally {
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
    return result;
  }
}
