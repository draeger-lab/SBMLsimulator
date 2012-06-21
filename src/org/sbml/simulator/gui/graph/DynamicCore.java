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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.sbml.jsbml.SBMLDocument;
import org.simulator.math.odes.MultiTable;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;

import de.zbit.util.ResourceManager;

/**
 * Represents the core of the dynamic visualization and therefore
 * the model in MVC-pattern.
 * Holds all necessary data and logic to run the dynamic visualization.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicCore {
    
    /**
     * Localization support.
     */
    private static final transient ResourceBundle bundle = ResourceManager
            .getBundle("org.sbml.simulator.gui.graph.DynamicGraph");
    
    /**
	 * Own thread to cycle through timepoints.
	 * 
	 * @author Fabian Schwarzkopf
	 * @version $Rev$
	 */
	private class PlayWorker extends SwingWorker<Void, Double>{
	    
	    /**
	     * Sleeptime while waiting for notification.
	     */
	    private final long AWAITING_GRAPH_DRAWING = 20;
	    
	    /**
	     * Enable/Disable video encoding.
	     */
	    private boolean generateVideo = false;
	    
	    /**
	     * Video encoder.
	     */
	    private IMediaWriter encoder;
	    
	    /**
	     * Timestamp and step size to capture images.
	     */
	    private int timestamp, captureStepSize;
	    
	    /**
	     * Some control elements for video encoding.
	     */
	    private int width, height, frame, image, totalimages;
	    
	    /**
	     * Construct {@link PlayWorker} without video encoding.
	     */
	    public PlayWorker() {
	    }
	    
	    /**
	     * Construct {@link PlayWorker} with implicit video encoding.
	     * @param generateVideo
	     * @param width
	     * @param height
	     * @param captureStepSize
	     * @param destinationFile
	     */
        public PlayWorker(boolean generateVideo, int width, int height,
                int timestamp, int captureEveryXStep, String destinationFile) {
            this.generateVideo = generateVideo;
            captureStepSize = captureEveryXStep;
            this.timestamp = timestamp;
            this.width = width;
            this.height = height;
            frame = 0;
            image = 1;
            totalimages = data.getRowCount()-1/captureEveryXStep;
            encoder = ToolFactory.makeWriter(destinationFile);
            logger.fine("Added " + width +"x" + height + " videostream");
            encoder.addVideoStream(0, 0, width, height);
        }
	    
		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected Void doInBackground() throws Exception {
			for(int i = getIndexOfTimepoint(currTimepoint)+1; i < timePoints.length; i++){
				publish(timePoints[i]);
				Thread.sleep(playspeed);
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#done()
		 */
		@Override
		protected void done() {
			super.done();
			if (encoder != null) {
			    encoder.close();
			    logger.fine("Videoencoding successful");
			}
			
			/*
			 * Notify observer.
			 */
            observer.donePlay();
			
			/*
			 * Ensure that only one playWorker is active
			 */
			playWorker = null;
		}

		/* (non-Javadoc)
		 * @see javax.swing.SwingWorker#process(java.util.List)
		 */
		@Override
		protected void process(List<Double> chunks) {
            /*
             * Ensures that there are not set any more timepoints after calling
             * stopPlay() such that the timepoint remains on the first given
             * timepoint like stopPlay() is intented.
             */
            if (!isCancelled()) { 
                for (Double timePoint : chunks) {
                    operationsDone = false;
                    setCurrTimepoint(timePoint);
                    
                    /*
                     * Wait till graph drawing is finished in case of large data.
                     * (Observer has to invoke operationsDone() after finished graph drawing).
                     */
                    while (!operationsDone) {
                        try {
                            logger.fine("Waiting for graph drawing to be completed.");
                            Thread.sleep(AWAITING_GRAPH_DRAWING);
                        } catch (InterruptedException e) {
                            logger.fine("Could not wait for graph drawing to be completed.");
                            e.printStackTrace();
                        }
                    }
                    
                    if (generateVideo) {
                        frame += timestamp; //timestamp for video encoding
                        if (getIndexOfTimepoint(timePoint) % captureStepSize == 0) {
                            // take picture now
                            logger.info(MessageFormat.format(
                                    bundle.getString("PROCESSING_IMAGE"),
                                    new Object[] { image, totalimages }));
                            encoder.encodeVideo(0, observer.takeGraphshot(width, height),
                                    frame, TimeUnit.MILLISECONDS);
                            image++;
                        }
                    }
                }
		    }
		}
	}
    
	/**
     * A {@link Logger} for this class.
     */
    private static final transient Logger logger = Logger.getLogger(DynamicCore.class.getName());
	
	/**
	 * Play thread worker.
	 */
	private PlayWorker playWorker;
	
	/**
	 * Notification if graph is updated.
	 * Gets changed by to threads, therefore volatile.
	 */
	private volatile boolean operationsDone;
	
	/**
     * Saves the currently displayed timestep. To ensure that the first
     * timepoint setted by construction is send to the observers, this field is
     * initialized with -1.
     */
	private double currTimepoint = -1;
	
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
     * After computation of Limits this HashMap saves for each species or
     * reaction min and max data value.
     */
	private Map<String, double[]> id2MinMaxData = new HashMap<String, double[]>();
	
	/**
     * Current status of limits whether computed or not. Not computed by default
     * to save constructing time if not needed.
     */
	private boolean limitsComputed = false;
	
	/**
	 * Listener, which will be notified, when the current timestep changes.
	 */
	private IDynamicGraph observer;
	
	/**
	 * Determines the speed of the the play method.
	 * By default 700.
	 */
	private int playspeed = 700;
	
	/**
	 * Constructs the core with an observer and simulation data.
	 * Does not provide any data limits directly after construction.
	 * 
	 * @param observer
	 * @param data
	 */
	public DynamicCore(IDynamicGraph observer, MultiTable data){
	    this.observer = observer;
		setData(data);
	}
	
	/**
     * Constructs the core with an observer and simulation data. Does provide
     * data limits, which will be implicit computed, therefore this constructor
     * is in O(n^2).
     * 
     * @param observer
     * @param data
     * @param document
     */
	public DynamicCore(IDynamicGraph observer, MultiTable data, SBMLDocument document){
	    this(observer, data);
	    computeSpecificLimits(document);
	}
	
    /**
	 * Computation of a Hashmap which saves for all species and references max/min values. O(n^2).
	 * @param document document needed to distinguish between reactions and species.
	 */
    public void computeSpecificLimits(SBMLDocument document) {
        for (int i = 1; i < data.getColumnCount(); i++) {
            double maxData = Double.MIN_VALUE;
            double minData = Double.MAX_VALUE;

            /*
             * only look through species/reactions columns in particular
             */
            if (document.getModel().getSpecies(data.getColumnIdentifier(i)) != null) {
                for (int j = 0; j < data.getRowCount(); j++) {
                    double tmpValue = data.getValueAt(j, i);
                    if (tmpValue < minData) {
                        minData = tmpValue;
                    } 
                    if (tmpValue > maxData) {
                        maxData = tmpValue;
                    }
                    
                    /*
                     * avoid min=max in case of only 1 timepoint given (e.g.
                     * flux balance)
                     */
                    if (data.getRowCount() == 1) {
                        minData = 0;
                    }
                }
                // min/max saved for this specie
                id2MinMaxData.put(data.getColumnIdentifier(i), new double[] {
                        minData, maxData });
            } else if (document.getModel().getReaction(
                    data.getColumnIdentifier(i)) != null) {
                for (int j = 0; j < data.getRowCount(); j++) {
                    double tmpValue = data.getValueAt(j, i);
                    if (tmpValue < minData) {
                        minData = tmpValue;
                    } 
                    if (tmpValue > maxData) {
                        maxData = tmpValue;
                    }
                    
                    /*
                     * avoid min=max in case of only 1 timepoint given (e.g.
                     * flux balance)
                     */
                    if (data.getRowCount() == 1) {
                        minData = 0;
                    }
                }
                // min/max saved for this reaction
                id2MinMaxData.put(data.getColumnIdentifier(i), new double[] {
                        minData, maxData });
            }
        }
        limitsComputed = true;
    }
	
	/**
     * Notifies observer about the change and delivers changed species &
     * reactions to all registered {@link DynamicView}s.
     */
	public void fireTimepointChanged() {
		double[] currTimePoints = {currTimepoint};
		observer.updateGraph(currTimepoint, data.filter(currTimePoints));
	}
	
	
	/**
     * Generates a video by cycling through every timestep and taking graphshots
     * at given capturepoints.
     * 
     * @param width
     * @param height
     * @param framerate
     * @param captureEveryXstep
     * @param destinationFile
     */
    public void generateVideo(int width, int height, int framerate,
            int captureEveryXstep, String destinationFile) {
	    //start off by zero time
	    currTimepoint = data.getTimePoint(0);
        fireTimepointChanged();
        playspeed = 1; //as fast as possible
        
        if(playWorker == null){
            //generate video
            playWorker = new PlayWorker(true, width, height, framerate,
                    captureEveryXstep,
                    destinationFile);
            playWorker.execute();
        }
	}
	
	/**
	 * Get the current timepoint of the core.
	 * @return
	 */
	public double getCurrTimepoint(){
		return currTimepoint;
	}
	
    /**
     * Returns mapping from every species or reactions id to their specific
     * minimum value and maximum value, if computed.
     * 
     * @return
     */
	public Map<String, double[]> getId2minMaxData(){
	    return limitsComputed ? id2MinMaxData : null;
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
	 * Get the maximum timepoint of the core.
	 * @return
	 */
	public double getMaxTime(){
	    return maxTime;
	}
	
	/**
     * If limits computed it returns a double array whose first element is the
     * minimum data of the given ids, and second element is the maximum data of
     * given ids. Otherwise null.
     * 
     * @param ids
     * @return
     */
    public double[] getMinMaxOfIDs(String... ids) {
        if (limitsComputed) {
            double minData = Double.MAX_VALUE;
            double maxData = Double.MIN_VALUE;
            for (String id : ids) {
                if (id2MinMaxData.containsKey(id)) {
                    double[] dataOfID = id2MinMaxData.get(id);
                    if (dataOfID[0] < minData) {
                        minData = dataOfID[0];
                    }
                    if (dataOfID[1] > maxData) {
                        maxData = dataOfID[1];
                    }
                }
            }
            return new double[] { minData, maxData };
        } else {
            return null;
        }
	}
	
	/**
	 * Get the speed of cycling through all timepoints with the play method.
	 * @return
	 */
	public int getPlayspeed(){
	    return playspeed;
	}
	
    /**
	 * Get timepoints of the simulation.
	 * @return
	 */
	public double[] getTimepoints(){
		return timePoints;
	}
	
	/**
     * Notifies the play worker, that the graph is ready for the next timepoint.
     * (Graph drawing completed).
     */
	public void operationsDone(){
		operationsDone = true;
	}
	
	/**
	 * Pauses the play worker.
	 */
	public void pausePlay(){
		if(playWorker != null){
			playWorker.cancel(true);
			playWorker = null;
		}
	}
	
	/**
	 * Cycles through all saved timepoints (ongoing from the current timepoint)
	 * and additionally updates the graph.
	 */
	public void play(){
		if(playWorker == null){
		    
		    //start from the beginning, if currently the last timepoint is set.
		    if (currTimepoint == timePoints[timePoints.length - 1]) {
		        setCurrTimepoint(0);
		    }
			playWorker = new PlayWorker();
			playWorker.execute();
		}
	}	
	
	/**
	 * Set the current time displayed by the graph.
	 * @param time
	 */
	public void setCurrTimepoint(double time){
		if (currTimepoint != time && time >= minTime && time <= maxTime){
			this.currTimepoint = time;
			fireTimepointChanged();
		}
	}
	
	/**
	 * Set the current time displayed by the graph to the given rowIndex of the data.
	 * @param time
	 */
	public void setCurrTimepoint(int rowIndex){
		double incomingTimepoint = data.getTimePoint(rowIndex);
        if (currTimepoint != incomingTimepoint && incomingTimepoint >= minTime
                && incomingTimepoint <= maxTime){
            this.currTimepoint = incomingTimepoint;
            fireTimepointChanged();
        }
	}
	
	/**
     * Set simulated data and the current timepoint to the first entry of the
     * given data.
     * @param data
     */
	public void setData(MultiTable data){
		this.data = data;
		timePoints = data.getTimePoints();
		minTime = data.getTimePoint(0);
		maxTime = data.getTimePoint(data.getRowCount()-1);
		currTimepoint = data.getTimePoint(0);
	}
	
	/**
	 * Set the speed of cycling through all timepoints with the play method.
	 * @param speed
	 */
	public void setPlayspeed(int speed){
	    playspeed = speed;
	}
	
    /**
     * Stops the play worker if working, and/or sets timepoint to first
     * timepoint in data
     */
	public void stopPlay(){
		if(playWorker != null){
			playWorker.cancel(true);
			playWorker = null;
		}
		currTimepoint = data.getTimePoint(0);
		fireTimepointChanged();
	}
}
