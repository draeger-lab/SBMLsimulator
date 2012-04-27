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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Controller class for DynamicControlPanel.
 * It represents the controller in MVC-pattern.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicController implements ChangeListener, ActionListener{

	/**
	 * Pointer to associated core
	 */
	private DynamicCore core;
	
	/**
	 * Pointer to associated control panel
	 */
	private DynamicControlPanel controlPanel;
	
	
	/**
	 * Constructs a new controller.
	 * @param core
	 */
	public DynamicController(DynamicCore core, DynamicControlPanel controlPanel){
		this.core = core;
		this.controlPanel = controlPanel;
	}
	

	/**
	 * Sets the core of this controller
	 * @param core
	 */
	public void setCore(DynamicCore core) {
		this.core = core;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(core != null){
			System.out.println("CONTROLLER: " + e.getActionCommand());
			if(e.getActionCommand().equals("PLAY")){
				core.play();
				controlPanel.enablePlay(false);
				controlPanel.enableSearchBar(false);
				controlPanel.enablePause(true);
				controlPanel.enableStop(true);
				//TODO: enable button when play finished
				//TODO: maybe disable toVideo button
			}else if(e.getActionCommand().equals("PAUSE")){
				core.pausePlay();
				controlPanel.enablePlay(true);
				controlPanel.enableSearchBar(true);
				controlPanel.enablePause(false);
			}else if(e.getActionCommand().equals("STOP")){
				core.stopPlay();
				controlPanel.enablePlay(true);
				controlPanel.enableSearchBar(true);
				controlPanel.enablePause(false);
				controlPanel.enableStop(false);
			}else if(e.getActionCommand().equals("TOVIDEO")){
				//TODO save as video with maximum sim-speed
			}		
		}
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		if(core != null){
			int timepoint = ((JSlider)e.getSource()).getValue();
			core.setCurrTimepoint(timepoint);
		}
		
	}
	
}
