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
	 * @param eventNumber
	 * @param id
	 * @param value
	 */
	public SpeciesReferenceAssignment(double processTime, int eventNumber, String id, Double value){
		super(processTime,eventNumber,value);
		this.id = id;	
	}
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param id
	 * @param value
	 * @param priority
	 * @param trigger
	 */
	public SpeciesReferenceAssignment(double processTime, int eventNumber,String id, Double value, Priority prio,Trigger trigger){
		super(processTime,eventNumber,value,prio,trigger);
		this.id = id;	
	}
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param id
	 */
	public SpeciesReferenceAssignment(double processTime, int eventNumber,String id){
		super(processTime,eventNumber);
		this.id = id;
				
	}
	
	/**
	 * 
	 * @param processTime
	 * @param eventNumber
	 * @param id
	 * @param priority
	 * @param trigger
	 */
	public SpeciesReferenceAssignment(double processTime, int eventNumber,String id,
			Priority priority,Trigger trigger) {
		super(processTime,eventNumber,priority,trigger);
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

