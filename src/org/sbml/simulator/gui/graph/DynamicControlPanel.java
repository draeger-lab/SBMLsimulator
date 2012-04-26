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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;

/**
 * JPanel to control the timepoints of a dynamic simulation
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicControlPanel extends JPanel{
	private static final long serialVersionUID = 6692563909762370732L;
	
	/**
	 * Pointer to related core
	 */
	private DynamicCore core;
	
	/**
	 * Registered controller
	 */
	private DynamicController controller;
	
	/*
	 * GUI elements
	 * TODO: GUITools Buttons
	 */
	private JLabel timelbl;
	private JSlider searchBar;
	private JButton play;
	private JButton pause;
	private JButton stop;
	private JButton video;
	private JLabel simVelolbl;
	private JComboBox simVeloCombo;
	private JSpinner simVeloSpin;
	
	
	/**
	 * Constructs a new control panel without an associated core
	 */
	public DynamicControlPanel() {
		init();
	}

	/**
	 * Constructs an new control panel with associated core.
	 * @param core
	 */
	public DynamicControlPanel(DynamicCore core){
		setCore(core);
		init();
	}

	/**
	 * Sets the related core to this control panel.
	 * A DynamicController is constructed in addition, hence all control actions 
	 * (buttons, searchbar) are associated with a controller.
	 * 
	 * @param core
	 */
	public void setCore(DynamicCore core){
		this.core = core;
		double[] timepointsOfSimulation = core.getTimepoints();
		searchBar.setMinimum(0);
		searchBar.setMaximum(timepointsOfSimulation.length-1);
		
		/*
		 * Controller needs to be assigned after setting the boundries of JSlider.
		 * Otherwise the current JSlider-value will change, thus invoking a change of the 
		 * current saved timepoint of the core.
		 * (Does not cause inconsistency but leads to a change of the first setted curr core value).
		 */
		controller.setCore(core);
		setTimepoint(core.getCurrTimepoint());
		//TODO: enable Buttons when core is set
	}
	
	/**
	 * Initialize this panel
	 */
	private void init(){
		GridBagLayout gbl = new GridBagLayout();
		setLayout(gbl);
		
		searchBar = new JSlider();
		searchBar.setMajorTickSpacing(1);
		searchBar.setPaintTicks(true);
		searchBar.setValue(0);
		
		//TODO: GUITools JButtons
		timelbl = new JLabel("Zeitpunkt: N/A");
		play = new JButton("Play");
		pause = new JButton("Pause");
		stop = new JButton("Stop");
		video = new JButton("Vid");
		simVelolbl = new JLabel("Simulationsgeschwindigkeit");
		simVeloCombo = new JComboBox();
		simVeloSpin = new JSpinner();
		
		
		controller = new DynamicController(null);
		searchBar.addChangeListener(controller);
		play.addActionListener(controller);
		pause.addActionListener(controller);
		stop.addActionListener(controller);
		
		//TODO: Layout -.-
		addComponent(gbl, searchBar, 	0, 0, 6, 1, GridBagConstraints.CENTER, 	GridBagConstraints.HORIZONTAL, 	1, 0, new Insets(0,0,0,0));
		addComponent(gbl, play, 		0, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, pause, 		1, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, stop, 		2, 1, 1, 1, GridBagConstraints.CENTER, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, video, 		3, 1, 1, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, timelbl, 		5, 1, 1, 1, GridBagConstraints.EAST, 	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVelolbl,	0, 2, 4, 1, GridBagConstraints.WEST,	GridBagConstraints.NONE, 		0, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloCombo,	3, 2, 1, 1, GridBagConstraints.CENTER,  GridBagConstraints.HORIZONTAL,	1, 0, new Insets(2,2,2,2));
		addComponent(gbl, simVeloSpin,	5, 2, 1, 1, GridBagConstraints.EAST, 	GridBagConstraints.BOTH, 		0, 0, new Insets(2,2,2,2));
		
		setMinimumSize(new Dimension(0, 50)); //TODO: Adjust
	}
		
	/**
	 * Sets the JSlider to the given timepoint and updates the time label
	 * @param timepoint
	 */
	public void setTimepoint(double timepoint){
		searchBar.setValue(core.getIndexOfTimepoint(timepoint));
		timelbl.setText("Zeitpunkt: " + timepoint);
		//TODO: locale
	}
	
	/**
	 * Checks if the given timepoint is already shown by the slider
	 * @param value
	 * @return
	 */
	public boolean isValueOfSlider(double timepoint){
		return searchBar.getValue() == core.getIndexOfTimepoint(timepoint);
	}
	
	/**
	 * Helper to layout components
	 * @param gbl
	 * @param c
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param weightx
	 * @param weighty
	 * @param insets
	 * @param anchor
	 */
	private void addComponent(GridBagLayout gbl, Component c, int x, int y,
            int width, int height, int anchor, int fill, double weightx, double weighty, Insets insets) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = fill;
		gbc.gridx = x; gbc.gridy = y;
		gbc.gridwidth = width; 
		gbc.gridheight = height;
		gbc.weightx = weightx; 
		gbc.weighty = weighty;
		gbc.insets = insets;
		gbc.anchor = anchor;
		gbl.setConstraints( c, gbc );
		add( c );
	}
}
