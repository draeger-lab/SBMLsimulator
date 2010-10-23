package org.sbml.simulator.math.odes;

/**
 * 
 * @author Alexander D&ouml;rr
 *
 */
public class DESAssignment{
	private double processTime;
	private int index;
	private Double value;
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 */
	public DESAssignment(double processTime, int index, Double value){
		this.processTime = processTime;
		this.index = index;
		this.value = value;			
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 */
	public DESAssignment(double processTime, int index){
		this.processTime = processTime;
		this.index = index;
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
	public int getIndex() {
		return index;
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
