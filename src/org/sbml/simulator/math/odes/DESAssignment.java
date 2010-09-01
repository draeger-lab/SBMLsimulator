package org.sbml.simulator.math.odes;

public class DESAssignment{
	private double processTime;
	private int index;
	private Double value;
	
	public DESAssignment(double processTime, int index, Double value){
		this.processTime = processTime;
		this.index = index;
		this.value = value;			
	}
	
	public DESAssignment(double processTime, int index){
		this.processTime = processTime;
		this.index = index;
		this.value = null;			
	}
	
	public double getProcessTime() {
		return processTime;
	}
	
	public int getIndex() {
		return index;
	}

	public Double getValue() {
		return value;
	}
	
	public void setValue(Double value) {
		this.value = value;
	}
	
}
