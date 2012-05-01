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

import javax.swing.SwingWorker;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.simulator.math.odes.MultiTable;

/**
 * Represents the core of the dynamic visualization and therefore
 * the model in MVC-pattern.
 * Holds all necessary data and logic to run the simulation.
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
	private class PlayWorker extends SwingWorker<Void, Void>{

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected Void doInBackground() throws Exception {
			for(int i = getIndexOfTimepoint(currTimepoint)+1; i < timePoints.length; i++){
				graphIsDrawn = false;
				setCurrTimepoint(timePoints[i]);
				Thread.sleep(playspeed);
				
				/*
				 * Wait till graph drawing is finished 
				 */
				while(!graphIsDrawn){
					Thread.sleep(AWAITING);
				}
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#done()
		 */
		@Override
		protected void done() {
			super.done();
			/*
			 * Ensure that only one playWorker is active
			 */
			playWorker = null;
		}
	}
	
	/**
	 * Play thread worker
	 */
	private PlayWorker playWorker;
	
	/**
	 * Notification if graph is updated.
	 * Gets changed by to threads, therefore volatile.
	 */
	private volatile boolean graphIsDrawn;
	
	/**
	 * Sleeptime while waiting for notification
	 */
	private final long AWAITING = 20;
	
	/**
     * Saves the currently displayed timestep.
     */
	private double currTimepoint;
	
	/**
	 * Saves the maximum simulated time.
	 */
	private double maxTime;
	
	/**
	 * Saves the minimum simulated time. By default zero.
	 */
	private double minTime = 0;
	
	/**
	 * Dynamic displayed data.
	 */
	private MultiTable data;
	
	/**
	 * List of all simulated timepoints
	 */
	private double[] timePoints;
	
	/**
	 * Lower limit of species data in Multitable
	 */
	private double minDataSpecies;
	
	/**
	 * Upper limit of species data in Multitable
	 */
	private double maxDataSpecies;
	
	/**
     * Current status of limits whether computed or not. Not computed by default
     * to save constructing time.
     */
	private boolean limitsComputed = false;
	
	/**
	 * List of all listeners, which will be notified, when the current timestep changes.
	 */
	private ArrayList<DynamicGraph> observers = new ArrayList<DynamicGraph>();
	
	/**
	 * Determines the speed of the the play method
	 */
	private int playspeed = 700;
	
	
	/**
	 * Construstructs the core with an observer and simulation data.
	 * @param observer
	 * @param data
	 */
	public DynamicCore(DynamicGraph observer, MultiTable data){
		observers.add(observer);
		setData(data);
	}
	
	/**
	 * Set simulated data and the current 
	 * timepoint to the first entry of the MultiTable
	 * @param data
	 */
	public void setData(MultiTable data){
		this.data = data;
		timePoints = data.getTimePoints();
		minTime = data.getTimePoint(0);
		maxTime = data.getTimePoint(data.getRowCount()-1);
		setCurrTimepoint(data.getTimePoint(0));
	}
	
	/**
	 * Get timepoints of the simulation
	 * @return
	 */
	public double[] getTimepoints(){
		return timePoints;
	}
	
	
	/**
	 * Set the current time displayed by the graph.
	 * @param time
	 */
	public void setCurrTimepoint(double time){
		if(currTimepoint != time && time >= minTime && time <= maxTime){
			this.currTimepoint = time;
			currTimepointChanged(time);
		}
	}
	
	/**
	 * Set the current time displayed by the graph to the given rowIndex of the MultiTable.
	 * @param time
	 */
	public void setCurrTimepoint(int rowIndex){
		double incomingTimepoint = data.getTimePoint(rowIndex);
        if (currTimepoint != incomingTimepoint && incomingTimepoint >= minTime
                && incomingTimepoint <= maxTime){
            this.currTimepoint = incomingTimepoint;
            currTimepointChanged(incomingTimepoint);
        }
	}
	
	/**
	 * Get the current timepoint of the core
	 * @return
	 */
	public double getCurrTimepoint(){
		return currTimepoint;
	}
	
	/**
	 * Get the maximum timepoint of the core
	 * @return
	 */
	public double getMaxTime(){
	    return maxTime;
	}
	
	/**
	 * Get the lower limit of the data in simulation data
	 * @return lower limit if already computed by {@link computeLimits} otherwise null
	 */
	public double getMinDataSpecies(){
	    return limitsComputed ? minDataSpecies : null;
	}
	
	/**
	 * Get the upper limit of the data in simulation data
     * @return upper limit if already computed by {@link computeLimits} otherwise null
	 */
	public double getMaxDataSpecies(){
	    return limitsComputed ? maxDataSpecies : null;
	}
	
	/**
	 * Get the speed of cycling through all timepoints with the play method
	 * @return
	 */
	public int getPlayspeed(){
	    return playspeed;
	}
	
	/**
	 * Set the speed of cycling through all timepoints with the play method
	 * @param speed
	 */
	public void setPlayspeed(int speed){
	    playspeed = speed;
	}
	
	/**
	 * Add an observer
	 * @param observer
	 */
	public void addObserver(DynamicGraph observer){
		observers.add(observer);
	}
	
	/**
	 * Notifies the play worker, that the graph is ready for the next
	 * timepoint. 
	 * (Graph drawing completed)
	 */
	public void graphUpdateFinished(){
		graphIsDrawn = true;
	}
	
	/**
	 * Notifies all observers about the change and delivers changed Species & Reactions
	 */
	private void currTimepointChanged(double changedTo){
		double[] currTimePoints = {currTimepoint};
		for(DynamicGraph dg : observers){
			dg.updateGraph(currTimepoint, data.filter(currTimePoints));
		}
	}
	
	/**
	 * Cycles through all saved timepoints (ongoing from the current timepoint)
	 * and additionally updates the graph
	 */
	public void play(){
		if(playWorker == null){
			playWorker = new PlayWorker();
			playWorker.execute();
		}
	}	
	
	/**
	 * Pauses the play worker
	 */
	public void pausePlay(){
		if(playWorker != null){
			playWorker.cancel(true);
			playWorker = null;
		}
	}
	
	/**
	 * Stops the play worker
	 */
	public void stopPlay(){
		if(playWorker != null){
			playWorker.cancel(true);
			playWorker = null;
		}
		currTimepoint = data.getTimePoint(0);
		currTimepointChanged(data.getTimePoint(0));
		
	}
	
	/**
	 * Searches the index of a given timepoint.
	 * @param timepoint
	 * @return index of the timepoint, if not found, then -1
	 */
	public int getIndexOfTimepoint(double timepoint) {
		int searchedIndex = -1;
		for(int i = 0; i < timePoints.length; i++){
			if(timepoint == timePoints[i]){
				searchedIndex = i;
			}
		}
		return searchedIndex;
	}
	
	/**
	 * Computation of min values in and max values in the simulation data. O(n^2).
	 * @param document needed to distinguish between reactions and species.
	 */
	public void computeLimits(SBMLDocument document){
	    //init
	    ListOf<Species> speciesIDs = document.getModel().getListOfSpecies();
	    maxDataSpecies = Double.MIN_VALUE;
	    minDataSpecies = Double.MAX_VALUE;
	    
	    //compute
	    for (int i = 1; i < data.getColumnCount(); i++){
            if (speciesIDs.get(data.getColumnIdentifier(i)) != null){
                for (int j = 0; j < data.getRowCount(); j++){
                    if (speciesIDs.get(data.getColumnIdentifier(i)) != null){
                        double tmpValue = data.getValueAt(j, i);
                        if (tmpValue < minDataSpecies){
                            minDataSpecies = tmpValue;
                        }else if (tmpValue > maxDataSpecies){
                            maxDataSpecies = tmpValue;
                        }
                    }
                }
            }
        }
	    
	    limitsComputed = true;
	}
}
