package de.hpi.uni.potsdam.test.bpmn_to_sql.pattern;

import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.anyDataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification.dataObject;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.InsertObjectSpecification.insert;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.nullValue;
import static de.hpi.uni.potsdam.bpmn_to_sql.pattern.PlainAttributeValueExpression.values;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import de.hpi.uni.potsdam.bpmn_to_sql.pattern.AttributeUpdate;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectReference;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.DataObjectSpecification;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.InsertStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.SelectStatement;
import de.hpi.uni.potsdam.bpmn_to_sql.pattern.sql.UpdateStatement;

public class DataObjectSpecificationTest extends TestCase {

  /**
   * Case object read single state
   */
  public void testCR1Pattern() {
    String expectedStatement = "SELECT COUNT(`d1`.`d1_id`) FROM `d1` WHERE `d1`.`d1_id` = \"someId\" AND `d1`.`state` = \"s\"";

    SelectStatement selectCountStatement = dataObject("d1", "d1_id", "someId").attribute("state", "s").getSelectCountStatement();

    String actualStatement = selectCountStatement.toSqlString();

    Assert.assertEquals(expectedStatement, actualStatement);
  }

  /**
   * Dependent 1:1, read single state
   */
  public void testD11R1Pattern() {
    String expectedStatement = "SELECT COUNT(`d2`.`d2_id`) FROM `d2`, `d1` WHERE `d2`.`d1_id` = `d1`.`d1_id` " +
    		"AND `d1`.`d1_id` = \"someId\" AND `d2`.`state` = \"s\"";

    SelectStatement selectCountStatement = anyDataObject("d2", "d2_id").attribute("state", "s").references("d1_id", dataObject("d1", "d1_id", "someId"))
        .getSelectCountStatement();

    String actualStatement = selectCountStatement.toSqlString();

    Assert.assertEquals(expectedStatement, actualStatement);
  }

  /**
   * Dependent 1:1, read multiple states
   */
  public void testD11R2Pattern() {
    String expectedStatement = "SELECT COUNT(`d2`.`d2_id`) FROM `d2`, `d1` WHERE `d2`.`d1_id` = `d1`.`d1_id` " +
    		"AND `d1`.`d1_id` = \"someId\" AND `d2`.`state` IN (\"s\", \"t\")";

    SelectStatement selectCountStatement = anyDataObject("d2", "d2_id").attribute("state", values("s", "t"))
        .references("d1_id", dataObject("d1", "d1_id", "someId")).getSelectCountStatement();

    String actualStatement = selectCountStatement.toSqlString();

    Assert.assertEquals(expectedStatement, actualStatement);
  }

  /**
   * Dependent 1:n, read single state
   */
  public void testD1nR1Pattern() {
    // statement is very similar to 1:1 read single
  }

  /**
   * Dependent m:n, read single state
   */
  public void testDmnR1Pattern() {
    // also similar?!
  }

  /**
   * Dependent 1:1, read without foreign key
   */
  public void testD11R3Pattern() {
    String expectedStatement = "SELECT COUNT(`d2`.`d2_id`) FROM `d2` WHERE `d2`.`d3_id` IS NULL AND `d2`.`state` = \"t\"";

    SelectStatement selectCountStatement = anyDataObject("d2", "d2_id").attribute("state", values("t")).attribute("d3_id", nullValue())
        .getSelectCountStatement();

    String actualStatement = selectCountStatement.toSqlString();

    Assert.assertEquals(expectedStatement, actualStatement);
  }

  public void testCC1Pattern() {
    String expectedStatement = "INSERT INTO `d1`(`d1_id`, `state`) VALUES (\"some id\", \"some state\")";

    InsertStatement insertStatement = 
        insert().object(dataObject("d1", "d1_id", "some id").attribute("state", "some state")).getStatement();
    
    String actualStatement = insertStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
  
  public void testD11C1Pattern() {
    String subSelect = "(SELECT `d3`.`d3_id` FROM `d3`, `d1` WHERE `d3`.`d1_id` = `d1`.`d1_id` AND `d1`.`d1_id` = \"case object id\")";
    String expectedStatement = "INSERT INTO `d2`(`d2_id`, `d3_id`, `state`) VALUES (\"new id\", " + subSelect + ", \"some state\")";

    DataObjectSpecification caseObject = dataObject("d1", "d1_id", "case object id");
    DataObjectSpecification referencedObject = anyDataObject("d3", "d3_id").references("d1_id", caseObject);
    
    InsertStatement insertStatement = 
        insert().object(
            dataObject("d2", "d2_id", "new id")
              .attribute("state", "some state")
              .references("d3_id", referencedObject))
        .getStatement();
    
    String actualStatement = insertStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
  
  public void testD1nC1Pattern() {
    String subSelect = "(SELECT `d3`.`d3_id` FROM `d3`, `d1` WHERE `d3`.`d1_id` = `d1`.`d1_id` AND `d1`.`d1_id` = \"case object id\")";
    String expectedStatement = "INSERT INTO `d2`(`d2_id`, `d3_id`, `state`) VALUES " +
    		"(\"new id\", " + subSelect + ", \"some state\"), " +
        "(\"another id\", " + subSelect + ", \"some state\")";
    
    DataObjectSpecification caseObject = dataObject("d1", "d1_id", "case object id");
    DataObjectSpecification referencedObject = anyDataObject("d3", "d3_id").references("d1_id", caseObject);
    
    InsertStatement insertStatement = 
        insert()
          .object(
            dataObject("d2", "d2_id", "new id")
              .attribute("state", "some state")
              .references("d3_id", referencedObject))
          .object(
            dataObject("d2", "d2_id", "another id")
              .attribute("state", "some state")
              .references("d3_id", referencedObject))
        .getStatement();
    
    String actualStatement = insertStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
  
  /**
   * same as D1nU1
   */
  public void testD11U1Pattern() {
    String subSelect = "(SELECT `d3`.`d3_id` FROM `d3`, `d1` WHERE `d3`.`d1_id` = `d1`.`d1_id` AND `d1`.`d1_id` = \"case object id\")";
    String expectedStatement = "UPDATE `d2` SET `state` = \"t\" WHERE `d2`.`d3_id` = " + subSelect;
    
    DataObjectSpecification caseObject = dataObject("d1", "d1_id", "case object id");
    
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
    AttributeUpdate update = new AttributeUpdate("state", "t");
    updates.add(update);
    
    UpdateStatement updateStatement = 
        anyDataObject("d2", "d2_id").references("d3_id", anyDataObject("d3", "d3_id").references("d1_id", caseObject))
        .getUpdateStatement(updates);
    
    String actualStatement = updateStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
  
  /**
   * same as D1nU2
   */
  public void testD11U2Pattern() {
    String subSelect = "(SELECT `d3`.`d3_id` FROM `d3`, `d1` WHERE `d3`.`d1_id` = `d1`.`d1_id` AND `d1`.`d1_id` = \"case object id\")";
    String expectedStatement = "UPDATE `d2` SET `state` = \"t\" WHERE `d2`.`d3_id` = " + subSelect + " AND `d2`.`state` = \"t1\"";
  
    DataObjectSpecification caseObject = dataObject("d1", "d1_id", "case object id");
    
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
    AttributeUpdate update = new AttributeUpdate("state", "t");
    updates.add(update);
    
    UpdateStatement updateStatement = 
        anyDataObject("d2", "d2_id").attribute("state", "t1")
          .references("d3_id", anyDataObject("d3", "d3_id").references("d1_id", caseObject))
        .getUpdateStatement(updates);
    
    String actualStatement = updateStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
  
  /**
   * same as D1nU3
   */
  public void testD11U3Pattern() {
    String subSelect = "(SELECT `d3`.`d3_id` FROM `d3`, `d1` WHERE `d3`.`d1_id` = `d1`.`d1_id` AND `d1`.`d1_id` = \"case object id\")";
    String expectedStatement = "UPDATE `d2` SET `state` = \"t\", `d3_id` = " + subSelect + " WHERE `d2`.`d3_id` IS NULL AND `d2`.`state` = \"t1\"";
  
    DataObjectSpecification caseObject = dataObject("d1", "d1_id", "case object id");
    
    List<AttributeUpdate> updates = new ArrayList<AttributeUpdate>();
    AttributeUpdate update1 = new AttributeUpdate("state", "t");
    AttributeUpdate update2 = new AttributeUpdate("d3_id", new DataObjectReference(anyDataObject("d3", "d3_id").references("d1_id", caseObject)));
    updates.add(update1);
    updates.add(update2);
    
    UpdateStatement updateStatement = 
        anyDataObject("d2", "d2_id").attribute("state", "t1")
          .attribute("d3_id", nullValue())
          .getUpdateStatement(updates);
    
    String actualStatement = updateStatement.toSqlString();
    
    Assert.assertEquals(expectedStatement, actualStatement);
  }
}
