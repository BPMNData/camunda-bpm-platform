package de.hpi.uni.potsdam.test.bpmn_to_sql.pattern.sql;

import junit.framework.TestCase;

import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.AttributeSetClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.FromClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.InsertStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SetClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.UpdateStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.ValuesClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.ValuesSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.WhereSubClause;

public class SqlStatementTest extends TestCase {

  public void testSelectStatementCreation() {
    String expectedSql = "SELECT a, b, c FROM d, e WHERE f = 'a'";
    
    SelectStatement statement = new SelectStatement();
    SelectClause select = new SelectClause("a", "b", "c");
    FromClause from = new FromClause();
    from.addTableName("d");
    from.addTableName("e");
    WhereClause where = new WhereClause();
    WhereSubClause sub = new PlainValueWhereSubClause("f", "'a'");
    where.addSubClause(sub);
    
    statement.setSelectClause(select);
    statement.setFromClause(from);
    statement.setWhereClause(where);
    
    String sql = statement.toSqlString();
    
    Assert.assertEquals(expectedSql, sql);
  }
  
  public void testInsertStatementCreation() {
    String expectedSql = "INSERT INTO a(b, c) VALUES (\"d\", \"e\"), (\"f\", \"g\")";
    
    InsertStatement statement = new InsertStatement("a", "b", "c");
    ValuesClause values = new ValuesClause();
    ValuesSubClause value1 = new ValuesSubClause("\"d\"", "\"e\"");
    ValuesSubClause value2 = new ValuesSubClause("\"f\"", "\"g\"");
    values.addValuesSubClause(value1);
    values.addValuesSubClause(value2);
    statement.setValues(values);
    
    String sql = statement.toSqlString();
    
    Assert.assertEquals(expectedSql, sql);
  }
  
  public void testUpdateStatementCreation() {
    String expectedSql = "UPDATE a SET b = \"d\", c = \"e\" WHERE f = \"g\"";
    
    UpdateStatement statement = new UpdateStatement("a");
    SetClause set = new SetClause();
    AttributeSetClause attributeClause1 = new AttributeSetClause("b", "\"d\"");
    AttributeSetClause attributeClause2 = new AttributeSetClause("c", "\"e\"");
    set.addAttributeClause(attributeClause1);
    set.addAttributeClause(attributeClause2);
    statement.setSetClause(set);
    
    WhereClause where = new WhereClause();
    WhereSubClause sub = new PlainValueWhereSubClause("f", "\"g\"");
    where.addSubClause(sub);
    statement.setWhereClause(where);
    
    String sql = statement.toSqlString();
    
    Assert.assertEquals(expectedSql, sql);
  }
}
