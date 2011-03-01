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
	
	
	/**
	 * 
	 * @param processTime
	 * @param value
	 */
	public EventAssignment(double processTime, Double value){
		this.processTime = processTime;
		this.value = value;			
	}
	
	/**
	 * 
	 * @param processTime
	 * @param value
	 * @param trigger
	 */
	public EventAssignment(double processTime, Double value, Priority prio,Trigger trigger){
		this.processTime = processTime;
		this.value = value;			
		this.prio=prio;
		this.trigger=trigger;
	}
	
	/**
	 * 
	 * @param processTime
	 */
	public EventAssignment(double processTime){
		this.processTime = processTime;
		this.value = null;			
	}
	
	
	/**
	 * 
	 * @param processTime
	 * @param prio
	 * @param trigger
	 */
	public EventAssignment(double processTime, Priority prio, Trigger trigger) {
		this.processTime=processTime;
		this.prio=prio;
		this.trigger=trigger;
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
	 * @param value
	 */
	public void setValue(Double value) {
		this.value = value;
	}
	
}


