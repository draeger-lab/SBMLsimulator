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
	 * @param eventNumber
	 * @param value
	 */
	public DESAssignment(double processTime, int index, int eventNumber, Double value){
		super(processTime,eventNumber,value);
		this.index = index;		
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param eventNumber
	 * @param value
	 * @param priority
	 * @param trigger
	 */
	public DESAssignment(double processTime, int index, int eventNumber, Double value, Priority prio, Trigger trigger){
		super(processTime,eventNumber,value, prio, trigger);
		this.index = index;		
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param eventNumber
	 */
	public DESAssignment(double processTime, int index, int eventNumber){
		super(processTime,eventNumber);
		this.index = index;			
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param eventNumber
	 * @param priority
	 * @param trigger
	 */
	public DESAssignment(double processTime, int index, int eventNumber,
			Priority priority, Trigger trigger) {
		super(processTime,eventNumber, priority,trigger);
		this.index=index;
	}

	public DESAssignment(double processTime, int index,
			double value) {
		super(processTime,value);
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
