package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.util.ClassNameUtil;
import org.camunda.bpm.engine.impl.util.IoUtil;

public class SqlTestHelper {
  
  private static Logger log = Logger.getLogger(SqlTestHelper.class.getName());
  
  private static final String SQL_FILE_SUFFIX = "sql";
  
  // TODO make this configurable
  private static final String DB_USER = "testuser";
  private static final String DB_PASSWORD = "test623";
  private static final String DB_URL = "jdbc:mysql://localhost:3306/testdb";

  public static String sqlScriptDatabaseSetUp(Class<?> testClass, String methodName) {
    String deploymentId = null;
    Method method = null;
    try {
      method = testClass.getDeclaredMethod(methodName, (Class<?>[])null);
    } catch (Exception e) {
      throw new ProcessEngineException("can't get method by reflection", e);
    }
    
    DatabaseSetup databaseSetupAnnotation = method.getAnnotation(DatabaseSetup.class);
    if (databaseSetupAnnotation != null) {
      log.fine("annotation @DatabaseSetupScript sets up the database for "+ClassNameUtil.getClassNameWithoutPackage(testClass)+"."+methodName);
      String[] resources = databaseSetupAnnotation.resources();
      if (resources.length == 0) {
        String name = method.getName();
        String resource = getSqlResource(testClass, name);
        resources = new String[]{resource};
      }
      
      for (String resource : resources) {
        executeSqlScript(resource);
      }
    }
    
    return deploymentId;
  }
  
  private static void executeSqlScript(String resource) {
    String script = IoUtil.readFileAsString(resource);
    Connection con = null;
    try {
      con = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
      ScriptRunner runner = new ScriptRunner(con);
      runner.runScript(new StringReader(script));
    } catch (SQLException e) {
      throw new ProcessEngineException("Cannot connect database", e);
    } finally {
      try {
        con.close();
      } catch (SQLException e) {
        throw new ProcessEngineException("Cannot close db connection", e);
      }
    }
    
  }

  public static String getSqlResource(Class< ? > type, String name) {
    return type.getName().replace('.', '/') + "." + name + "." + SQL_FILE_SUFFIX;
  }
}
