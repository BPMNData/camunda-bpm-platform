package de.hpi.uni.potsdam.test.bpmn_to_sql.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSessionManager;

public class PersistentObjectManager {

  protected SqlSessionManager sqlSessionManager;
  protected static final String MAPPER_NAMESPACE = "de.hpi.uni.potsdam.test.bpmn_to_sql.db";
  
  public PersistentObjectManager(SqlSessionManager sqlSessionManager) {
    this.sqlSessionManager = sqlSessionManager;
  }
  
  
  public Map<String, Object> getPersistentObject(String type, String id) {
    String selectStatement = getSelectStatementForType(type);
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("id", id);
    return (Map<String, Object>) sqlSessionManager.selectOne(selectStatement, params);
  }
  
  private String getSelectStatementForType(String type) {
    return MAPPER_NAMESPACE + ".selectSingle" + type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase();
  }
  
  public List<Map<String, Object>> getPersistentObjects(String type, Map<String, Object> selections) {
    String selectStatement = getSelectStatementForType(type);
    return sqlSessionManager.selectList(selectStatement, selections);
  }
}
