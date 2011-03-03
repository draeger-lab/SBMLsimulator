package org.sbml.simulator.math.odes;

/**
 * 
 * @author Alexander D&ouml;rr
 * 
 */
public class DESAssignment {
	private int index;
	private double processTime;
	private Double value;
	private int eventNumber;

	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param eventNumber
	 * @param value
	 */
	public DESAssignment(double processTime, int index, int eventNumber,
			Double value) {
		this.processTime = processTime;
		this.value = value;
		this.eventNumber = eventNumber;
		this.index = index;
	}

	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param eventNumber
	 */
	public DESAssignment(double processTime, int index, int eventNumber) {
		this.processTime = processTime;
		this.eventNumber = eventNumber;
		this.index = index;
	}

	/**
	 * 
	 * @param processTime
	 * @param index
	 * @param value
	 */
	public DESAssignment(double processTime, int index, double value) {
		this.processTime = processTime;
		this.value = value;
		this.index = index;
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
