package de.hpi.uni.potsdam.bpmn_to_sql.execution;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;

import de.hpi.uni.potsdam.bpmn_to_sql.bpmn.DataObject;

public class DataObjectClassification {
	
	public static Boolean hasMainDataObjectAnnotation(String scopeName) {
		return 		BpmnParse.getScopeInformation().containsKey(scopeName)
				&& 	BpmnParse.getScopeInformation().get(scopeName) != null
				&& !BpmnParse.getScopeInformation().get(scopeName).isEmpty();
	}

	public static Boolean isMainDataObject(DataObject dataObj, String scopeName) {
		if (dataObj.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(scopeName))) {
			return true;
		} else {
			return false;
		}
	}
	
	public static Boolean isDependentDataObject(DataObject dataObj, String scopeName) {
		//annahme: FkList ist  f�r jedes dep. do nicht leer
		if(!dataObj.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(scopeName)) && dataObj.getIsCollection()!=true){
//			boolean fkCheck = true;
//			for (String fk : dataObj.getFkeys()) {
//				if (fk == null) {
//					fkCheck = false;
//				}
//			}
//			return fkCheck;
			return true;
		} else {
			return false;
		}
	}
	
	public static Boolean isDependentDataObjectWithUnspecifiedFK(DataObject dataObj, String scopeName) {
		if(!dataObj.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(scopeName))){
			boolean fkNull = true;
			for (String fk : dataObj.getFkeys()) {
				if (fk != null) {
					fkNull = false;
				}
			}
			return fkNull;
		} else {
			return false;
		}
	}
	
	public static Boolean isMIDependentDataObject(DataObject dataObj, String scopeName) {
		if(!dataObj.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(scopeName)) && dataObj.getIsCollection()==true){
//			boolean fkCheck = true;
//			for (String fk : dataObj.getFkeys()) {
//				if (fk == null) {
//					fkCheck = false;
//				}
//					
//			}
//			return fkCheck;
			return true;
		} else {
			return false;
		}
	}
	
	
//	public static Boolean isMIDependentDataObjectWithUnspecifiedFK(DataObject dataObj, String scopeName) {
//		if(!dataObj.getName().equalsIgnoreCase(BpmnParse.getScopeInformation().get(scopeName)) && dataObj.getIsCollection()==true){
//			boolean fkNull = false;
//			for (String fk : dataObj.getFkeys()) {
//				if (fk == null) {
//					fkNull = true;
//				}
//			}
//			return fkNull;
//		} else {
//			return false;
//		}
//	}
	
	public static Boolean isNumberingInformationDataObject(DataObject dataObj) {
		if (dataObj.getPkey().isEmpty()) {
			return true;
		} else {
			return false;
		}
	}
}
