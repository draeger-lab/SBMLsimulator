package org.sbml.simulator.math.odes;

/**
 * 
 * @author Alexander D&ouml;rr, Roland Keller
 *
 */

public class EventAssignment {
	protected double processTime;
	protected Double value;
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 */
	public EventAssignment(double processTime, Double value){
		this.processTime = processTime;
		this.value = value;			
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 */
	public EventAssignment(double processTime){
		this.processTime = processTime;
		this.value = null;			
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
	 * @param value
	 */
	public void setValue(Double value) {
		this.value = value;
	}
	
}


