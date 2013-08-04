package de.hpi.uni.potsdam.bpmn_to_sql.xquery;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQSequence;

import net.sf.saxon.xqj.SaxonXQDataSource;

import org.camunda.bpm.engine.impl.pvm.runtime.AtomicOperationActivityExecute;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.hpi.uni.potsdam.bpmn_to_sql.execution.QueryExecutionHandler;

public class XQueryHandler {
  
  private static Logger log = Logger.getLogger(AtomicOperationActivityExecute.class.getName());
	
	public ArrayList<String> buildObjectXML(String objectName){
		ArrayList<String> results = new ArrayList<String>();		
		QueryExecutionHandler sqlHandler = QueryExecutionHandler.getInstance();
		ArrayList<String> resultMetaData = sqlHandler.getColumnNames();
		int columnCount = sqlHandler.getColumnCount();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		
    try {
      builder = factory.newDocumentBuilder();
      ArrayList<Object> result = sqlHandler.getNextResult();
  		while (result != null){
  		  Document object = builder.newDocument();        
        Element objectElement = object.createElement(objectName.replace(" ", "_"));        
  			for (int i = 0; i < columnCount; i++){
  				Element column = object.createElement(resultMetaData.get(i));
  				if(result.get(i) == null){
  				  column.appendChild(object.createTextNode(""));  				  
  				} else
  				{
  				  column.appendChild(object.createTextNode(result.get(i).toString()));
  				}  				
  				objectElement.appendChild(column);
  			}  			
  			object.appendChild(objectElement);
  			result = sqlHandler.getNextResult();
  			object.getDocumentElement().normalize();
        String resultXML = transformDocToString(object);
        results.add(resultXML);
  		}
    } catch (ParserConfigurationException e) {
      log.log(Level.SEVERE, e.getMessage(), e);
    }
    
		return results;
	}
	
	public static String transformDocToString(Document doc){
	  TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer;
    StringWriter writer = new StringWriter();
	  String output = "";
	  
	  try {
	    transformer = tf.newTransformer();
	    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	    transformer.transform(new DOMSource(doc), new StreamResult(writer));
	    output = writer.getBuffer().toString();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
	  
    return output;
	}
	
	public static Document transformStringToDoc(String xml){
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		Document doc;
		
    try {
      builder = factory.newDocumentBuilder();
      doc = builder.parse(new InputSource(new StringReader(xml)));
      return doc;
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
	}
	
	public String runXPath(String xml, String xPathExpression) {
	  Document doc = transformStringToDoc(xml);
	  XPathFactory factory = XPathFactory.newInstance();
	  XPath xpath = factory.newXPath();
	  String result = null;
	  
	  try {
	    XPathExpression expression = xpath.compile(xPathExpression);
	    result =  expression.evaluate(doc);
	  } catch (XPathExpressionException e) {
	    throw new RuntimeException(e);
	  }
	  
	  return result;
	}
	
	public ArrayList<String> runXQuery(ArrayList<String> sourceDocs, String query){
		String tempSource = "";
		for (String sourceDoc : sourceDocs){
		  tempSource += sourceDoc;
		}
		ArrayList<String> results = runXQuery(tempSource, query);
		return results;
	}
	
	public ArrayList<String> runXQuery(String source, String query){
		ArrayList<String> results = new ArrayList<String>();
		Document doc = transformStringToDoc(source);
		if(!query.equals("")){
		  query = query.replaceAll("\\n", "");
  		XQDataSource ds = new SaxonXQDataSource();
      XQConnection conn;
      try {
        conn = ds.getConnection();    
        XQPreparedExpression exp;
        System.out.println(query);
        exp = conn.prepareExpression(query);    
        exp.bindNode(XQConstants.CONTEXT_ITEM, doc, null);    
  		  XQSequence seq;    
        seq = exp.executeQuery();    
        while (seq.next()) {
          XQItem item = seq.getItem();
          String xml = item.getItemAsString(null);
          xml = xml.replaceAll("\\n", ""); 
          results.add(xml);
        }
      } catch (XQException e) {
        throw new RuntimeException(e);
      }
		} else {
		  results.add(source);
		}
    
		return results;		
	}
	
	public HashMap<String, HashMap<String, String>> extractInformation(String xmlObject){
		HashMap<String, HashMap<String, String>> extractedInformation = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> keys = new HashMap<String, String>();
		
		Document doc = transformStringToDoc(xmlObject);
		Element currentElement = doc.getDocumentElement();
		String objectName = currentElement.getTagName();
		extractedInformation.put(objectName, keys);
		
		NodeList childNodes = currentElement.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
		  Node child = childNodes.item(i);
		  if (child.getNodeType() != Node.TEXT_NODE){
		    keys.put(child.getNodeName(), child.getFirstChild().getNodeValue());
		  }
		}
		
		return extractedInformation;
	}

}
