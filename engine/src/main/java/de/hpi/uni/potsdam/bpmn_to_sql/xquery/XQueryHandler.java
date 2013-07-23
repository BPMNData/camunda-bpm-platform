package de.hpi.uni.potsdam.bpmn_to_sql.xquery;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
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
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQConstants;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItem;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQSequence;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.hpi.uni.potsdam.bpmn_to_sql.execution.QueryExecutionHandler;
import net.sf.saxon.xqj.SaxonXQDataSource;

public class XQueryHandler {
	
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
  			HashMap<String,Document> objects = new HashMap<String,Document>();
  			for (int i = 1; i <= columnCount; i++){
  				Document object = builder.newDocument();;				
  				Element rootElement = object.createElement(objectName);
  				object.appendChild(rootElement);
  				objects.put(objectName, object);
  				Element column = object.createElement(resultMetaData.get(i));
  				column.appendChild(object.createTextNode(result.get(i).toString()));
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
