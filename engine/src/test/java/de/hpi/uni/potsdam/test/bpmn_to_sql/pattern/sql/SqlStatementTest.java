package de.hpi.uni.potsdam.test.bpmn_to_sql.pattern.sql;

import junit.framework.TestCase;

import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.FromClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.PlainValueWhereSubClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectClause;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
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
}
