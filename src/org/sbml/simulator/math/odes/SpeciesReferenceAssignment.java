package org.sbml.simulator.math.odes;


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
	 */
	public SpeciesReferenceAssignment(double processTime, String id){
		super(processTime);
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

