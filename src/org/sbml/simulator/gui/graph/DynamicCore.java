/*
 * $Id$
 * $URL$ 
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2012 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui.graph;

import java.util.ArrayList;

import org.simulator.math.odes.MultiTable;

/**
 * Represents the core of the dynamic visualization. 
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicCore {
	
	
	/**
	 * Own thread to cycle through timepoints
	 * 	
	 * @author Fabian Schwarzkopf
	 * @version $Rev$
	 */
	private class Play extends Thread{
		public void run(){
			for(int i = getIndexOfTimePoint(currTimePoint); i < timePoints.length; i++){
				setCurrTimePoint(timePoints[i]);				
				try {
					sleep(playspeed);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Play-thread object
	 */
	private Play play;

	/**
	 * Saves the currently displayed timestep.
	 */
	private double currTimePoint;
	
	/**
	 * Saves the maximum simulated time.
	 */
	private double maxTime;
	
	/**
	 * Saves the minimum simulated time. By default zero.
	 */
	private double minTime = 0;
	
	/**
	 * Saves the stepsize of the simulated data.
	 */
	private double stepSize;
	
	/**
	 * Dynamic displayed data.
	 */
	private MultiTable data;
	
	/**
	 * List of all simulated timepoints
	 */
	private double[] timePoints;
	
	/**
	 * List of all listeners, which will be notified, when the current timestep changes.
	 */
	private ArrayList<DynamicGraph> observers = new ArrayList<DynamicGraph>();
	
	/**
	 * Determines the speed of the the play method
	 */
	private int playspeed = 400;
	
	/**
	 * Constructs the core with an observer.
	 * @param observer
	 */
	public DynamicCore(DynamicGraph observer){
		observers.add(observer);
	}
	
	/**
	 * Set simulated data
	 * @param data
	 */
	public void setData(MultiTable data){
		this.data = data;
		timePoints = data.getTimePoints();
		currTimePoint = data.getTimePoint(0);
		//TODO: set simulation settings (min,max,stepsize)
	}
	
	/**
	 * Set the current time displayed by the graph.
	 * @param time
	 */
	public void setCurrTimePoint(double time){
		//TODO: check if time exists and if it's within borders
		this.currTimePoint = time;
		currTimePointChanged(time);
	}
	
	/**
	 * Set the current time displayed by the graph to the given rowIndex of the MultiTable.
	 * @param time
	 */
	public void setCurrTimePoint(int rowIndex){
		//TODO: check if time exists and if it's within borders
		this.currTimePoint = data.getTimePoint(rowIndex);
		currTimePointChanged(data.getTimePoint(rowIndex));
	}
	
	/**
	 * Add an observer
	 * @param observer
	 */
	public void addObserver(DynamicGraph observer){
		observers.add(observer);
	}
	
	/**
	 * Notifies all observers about the change and delivers changed Species & Reactions
	 */
	//TODO: deliver changed values with respect to the chosen graphelements
	private void currTimePointChanged(double changedTo){
		double[] currTimePoints = {currTimePoint};
		for(DynamicGraph dg : observers){
			dg.updateGraph(currTimePoint, data.filter(currTimePoints));
		}
	}
	
	/**
	 * Cycles through all saved timepoints (ongoing from the current timepoint)
	 * and additionally updates the graph
	 */
	public void play(){
		play = new Play();
		play.start();
	}	
	
	/**
	 * Pauses the play thread
	 */
	public void pausePlay(){
		System.out.println("pause");
		play.stop();
	}
	
	/**
	 * Searches the index of a given timepoint.
	 * @param timepoint
	 * @return index of the timepoint, if not found, then -1
	 */
	public int getIndexOfTimePoint(double timepoint) {
		int searchedIndex = -1;
		for(int i = 0; i < timePoints.length; i++){
			if(timepoint == timePoints[i]){
				searchedIndex = i;
			}
		}
		return searchedIndex;
	}
}
