package org.sbml.simulator.math;

import java.util.LinkedList;
import java.util.Queue;

public class EventInProcess {

	private boolean fired;
	private double priority;
	private Queue<Double> execTimes;
	private Queue<Double[]> values;
	
	
	EventInProcess(boolean fired){
		this.fired = fired;
		this.priority = Double.NEGATIVE_INFINITY;
		this.execTimes = new LinkedList<Double>(); 
		this.values = new LinkedList<Double[]>(); 
				
	}	
	
	public void addValues(Double[] values, double time){		
		this.values.add(values);
		this.execTimes.add(time);
	}
	
	public Double getPriority(){
		return priority;
	}
	
	public void changePriority(double priority){
		this.priority = priority;
	}
	
	public Double[] getValues(){		
		return values.peek();
	}
	
	public double getTime(){		
		return execTimes.peek();
	}
	
	public void executed(){
		values.poll();
		execTimes.poll();
	}
	
	public void aborted(){
		executed();
	}
	
	public boolean getFireStatus(){
		return fired;
	}
	
	public void recoverd(){
		fired = false;
	}
	
	public void fired(){
		fired = true;
	}
	
}
