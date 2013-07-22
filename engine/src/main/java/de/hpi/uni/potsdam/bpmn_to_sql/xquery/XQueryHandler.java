package de.hpi.uni.potsdam.bpmn_to_sql.xquery;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XQueryHandler {
	
	public ArrayList<String> buildObjectXML(ResultSet result, String objectName) throws SQLException{
		ArrayList<String> results = new ArrayList<String>();
		ResultSetMetaData resultMetaData = result.getMetaData();
		int columnCount = resultMetaData.getColumnCount();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		
    try {
      builder = factory.newDocumentBuilder();      
  		while (result.next()){
  			HashMap<String,Document> objects = new HashMap<String,Document>();
  			for (int i = 1; i <= columnCount; i++){
  				String tableName = resultMetaData.getTableName(i).replace(" ", "_");
  				Document object;				
  				if(objects.containsKey(tableName)){
  					object = objects.get(tableName);
  				} else{
  					object = builder.newDocument();
  					Element rootElement = object.createElement(tableName);
  					object.appendChild(rootElement);
  					objects.put(tableName, object);
  				}
  				Element column = object.createElement(resultMetaData.getColumnName(i));
  				column.appendChild(object.createTextNode(result.getString(i)));
  				object.getFirstChild().appendChild(column);
  				object.getDocumentElement().normalize();
  			}
  			String resultXML = "";
  			for(Document resultObject : objects.values()){
  				resultXML += transformDocToString(resultObject);				
  			}
  			results.add(resultXML);
  		}
    } catch (ParserConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (TransformerException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SAXException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
		return null;		
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
	    e.printStackTrace();
	  }
	  
	  return result;
	}
	
	public ArrayList<String> runXQuery(ArrayList<String> sourceDocs, String query){
		ArrayList<String> results = new ArrayList<String>();
		for (String sourceDoc : sourceDocs){
			ArrayList<String> tempResults = runXQuery(sourceDoc, query);
			results.addAll(tempResults);
		}
		return results;
	}
	
	protected ArrayList<String> runXQuery(String source, String query){
		ArrayList<String> results = new ArrayList<String>();
		Document doc = transformStringToDoc(source);
		XQDataSource ds = new SaxonXQDataSource();
    XQConnection conn;
    try {
      conn = ds.getConnection();    
      XQPreparedExpression exp;
      exp = conn.prepareExpression(query);    
      exp.bindNode(XQConstants.CONTEXT_ITEM, doc, null);    
		  XQSequence seq;    
      seq = exp.executeQuery();    
      while (seq.next()) {
        XQItem item = seq.getItem();
        String xml = item.getItemAsString(null);
        xml = xml.replaceAll("\\n[\\s]+", ""); 
        results.add(xml);
      }
    } catch (XQException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
		return results;		
	}
	
	public HashMap<String, HashMap<String, String>> getCorrelationInformation(String message){
		String query = "for $d in ./message/correlation/key return $d";
		HashMap<String, HashMap<String, String>> correlationKeys = extractInformation(message, query);
		
		return correlationKeys;
	}

	public HashMap<String, HashMap<String, String>> getPayloadInformation(String message){
		String query = "for $d in ./message/payload/object return $d";
		HashMap<String, HashMap<String, String>> payload = extractInformation(message, query);
		
		return payload;
	}
	
	public HashMap<String, HashMap<String, String>> extractInformation(String message, String query){
		HashMap<String, HashMap<String, String>> extractedInformation = new HashMap<String, HashMap<String, String>>();
		ArrayList<String> tempInformations = runXQuery(message, query);
		
		for (String tempInformation : tempInformations){
			Document doc = transformStringToDoc(tempInformation);
			Element currentElement = doc.getDocumentElement();
			String objectName = currentElement.getAttribute("name");
			HashMap<String, String> keys;
			if(extractedInformation.containsKey(objectName)){
				keys = extractedInformation.get(objectName);				
			} else {
			  keys = new HashMap<String, String>();
				extractedInformation.put(objectName, keys);
			}
			NodeList childNodes = currentElement.getChildNodes();
			for (int i = 0; i < childNodes.getLength(); i++) {
			  Node child = childNodes.item(i);
			  if (child.getNodeType() != Node.TEXT_NODE){
			    keys.put(child.getAttributes().item(0).getNodeValue(), child.getFirstChild().getNodeValue());
			  }
			}		
		}
		return extractedInformation;
	}

}
