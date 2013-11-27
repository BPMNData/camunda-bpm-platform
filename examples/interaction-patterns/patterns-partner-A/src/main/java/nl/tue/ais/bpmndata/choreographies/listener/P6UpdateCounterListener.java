package nl.tue.ais.bpmndata.choreographies.listener;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;

/**
 * Listener to populate a data object from form fields.
 * 
 * @author dfahland
 */
public class P6UpdateCounterListener implements ExecutionListener {

	public void notify(DelegateExecution execution) throws Exception {
		
		Long received = new Long(0);
		
		if (execution.hasVariable("received"))
			received = (Long)execution.getVariable("received");
		
		execution.setVariable("received", received+1);
	}

}
