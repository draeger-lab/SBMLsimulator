package org.sbml.simulator.math.odes;

import org.sbml.jsbml.Priority;
import org.sbml.jsbml.Trigger;

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
	 * @param value
	 * @param priority
	 * @param trigger
	 */
	public DESAssignment(double processTime, int index, Double value, Priority prio, Trigger trigger){
		super(processTime,value, prio, trigger);
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
	 * @param processTime
	 * @param index
	 * @param priority
	 * @param trigger
	 */
	public DESAssignment(double processTime, int index,
			Priority priority, Trigger trigger) {
		super(processTime,priority,trigger);
		this.index=index;
	}

	/**
	 * 
	 * @return
	 */
	public int getIndex() {
		return index;
	}
	
	
}
