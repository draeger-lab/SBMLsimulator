package org.sbml.simulator.math.odes;

/**
 * 
 * @author Alexander D&ouml;rr
 *
 */
public class DESAssignment extends EventAssignment{
	private int index;
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 */
	public DESAssignment(double processTime, int index, Double value){
		super(processTime,value);
		this.index = index;		
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 */
	public DESAssignment(double processTime, int index){
		super(processTime);
		this.index = index;			
	}
	
	/**
	 * 
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	
}
