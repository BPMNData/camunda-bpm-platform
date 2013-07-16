package de.hpi.uni.potsdam.bpmn_to_sql;

public class BpmnDataConfiguration {

  protected String jdbcDriver = "com.mysql.jdbc.Driver";
  protected String jdbcUrl = "jdbc:mysql://localhost:3306/testdb";
  protected String jdbcUsername = "testuser";
  protected String jdbcPassword = "test623";
  
  public String getJdbcDriver() {
    return jdbcDriver;
  }
  public void setJdbcDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
  }
  public String getJdbcUrl() {
    return jdbcUrl;
  }
  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }
  public String getJdbcUsername() {
    return jdbcUsername;
  }
  public void setJdbcUsername(String jdbcUsername) {
    this.jdbcUsername = jdbcUsername;
  }
  public String getJdbcPassword() {
    return jdbcPassword;
  }
  public void setJdbcPassword(String jdbcPassword) {
    this.jdbcPassword = jdbcPassword;
  }
}
