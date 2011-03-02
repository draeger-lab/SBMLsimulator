package org.sbml.simulator.math.odes;

import org.sbml.jsbml.Priority;
import org.sbml.jsbml.Trigger;

/**
 * 
 * @author Alexander D&ouml;rr, Roland Keller
 *
 */

public class EventAssignment {
	protected double processTime;
	protected Double value;
	protected Priority prio;
	protected Trigger trigger;
	protected int eventNumber;
	
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param value
	 */
	public EventAssignment(double processTime, int eventNumber, Double value){
		this.processTime = processTime;
		this.value = value;			
		this.eventNumber=eventNumber;
	}
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param value
	 * @param trigger
	 */
	public EventAssignment(double processTime, int eventNumber, Double value, Priority prio,Trigger trigger){
		this.processTime = processTime;
		this.eventNumber=eventNumber;
		this.value = value;			
		this.prio=prio;
		this.trigger=trigger;
	}
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 */
	public EventAssignment(double processTime, int eventNumber){
		this.processTime = processTime;
		this.eventNumber=eventNumber;
		this.value = null;			
	}
	
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param prio
	 * @param trigger
	 */
	public EventAssignment(double processTime, int eventNumber, Priority prio, Trigger trigger) {
		this.processTime=processTime;
		this.eventNumber=eventNumber;
		this.prio=prio;
		this.trigger=trigger;
	}

	public EventAssignment(double processTime, double value) {
		this.processTime=processTime;
		this.value=value;
	}

	/**
	 * 
	 * @return
	 */
	public double getProcessTime() {
		return processTime;
	}
	

	/**
	 * 
	 * @return
	 */
	public Double getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return
	 */
	public Priority getPriority() {
		return prio;
	}
	
	/**
	 * 
	 * @return
	 */
	public Trigger getTrigger() {
		return trigger;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getEventNumber() {
		return eventNumber;
	}
	
	/**
	 * 
	 * @param value
	 */
	public void setValue(Double value) {
		this.value = value;
	}
	
}


