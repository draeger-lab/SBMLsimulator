package org.sbml.simulator.math.odes;

import org.sbml.jsbml.Priority;
import org.sbml.jsbml.Trigger;


/**
 * 
 * @author Roland Keller
 *
 */

public class SpeciesReferenceAssignment extends EventAssignment{
	private String id;
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 */
	public SpeciesReferenceAssignment(double processTime, String id, Double value){
		super(processTime,value);
		this.id = id;	
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 * @param priority
	 * @param trigger
	 */
	public SpeciesReferenceAssignment(double processTime, String id, Double value, Priority prio,Trigger trigger){
		super(processTime,value,prio,trigger);
		this.id = id;	
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 */
	public SpeciesReferenceAssignment(double processTime, String id){
		super(processTime);
		this.id = id;
				
	}
	
	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param priority
	 * @param trigger
	 */
	public SpeciesReferenceAssignment(double processTime, String id,
			Priority priority,Trigger trigger) {
		super(processTime,priority,trigger);
		this.id = id;	
	}

	/**
	 * 
	 * @return
	 */
	public String getSpeciesReferenceID() {
		return id;
	}
	
}

