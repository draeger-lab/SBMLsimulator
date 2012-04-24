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

import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import de.zbit.gui.GUITools;

/**
 * JPanel to control the timepoints of a dynamic simulation
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicControlPanel extends JPanel{
	private static final long serialVersionUID = 6692563909762370732L;
	
	
	private DynamicCore core;
	private double[] timepointsOfSimulation;
	private DynamicController controller;
	
	private JLabel timelbl;
	private JSlider searchBar;
	private JButton play;
	private JButton pause;
	private JButton stop;
	
	public DynamicControlPanel(){
		init();
		//TODO: construct controller
	}
	
	public DynamicControlPanel(DynamicCore core){
		this.core = core;
		init();
		//TODO: set JSlider values with respect to core 
	}
	
	public void setCore(DynamicCore core){
		this.core = core;
		timepointsOfSimulation = core.getTimepoints();
		searchBar.setMinimum(0);
		searchBar.setMaximum(timepointsOfSimulation.length-1);
		timelbl.setText(timepointsOfSimulation[0] + "");
		
		//TODO: enable Buttons when core is set
	}
	
	private void init(){
		searchBar = new JSlider();
		
		//TODO: GUITools JButtons
		//TODO: Add savetoimagesbutton
		timelbl = new JLabel();
		play = new JButton("Play");
		pause = new JButton("Pause");
		stop = new JButton("Stop");
		
		add(timelbl);
		add(searchBar);
		add(play);
		add(pause);
		add(stop);
		
		setMinimumSize(new Dimension(0, 50)); //TODO: Adjust
	}
	
	//TODO: accessmethods
	public void setMinValue(int minValue){
		searchBar.setMinimum(minValue);
	}
	
	public void setMaxValue(int maxValue){
		searchBar.setMaximum(maxValue);
	}
	
	public void setValue(int value){
		searchBar.setValue(value);
	}
}
