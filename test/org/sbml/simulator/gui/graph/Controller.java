/*
 * $Id$
 * $URL$ 
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
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
 * Controller for a DynamicControlPanel.
 * No check for existing core before operations on it, because related buttons only active,
 * when core existing.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class Controller implements ChangeListener, ActionListener{
	
	private DynamicCore core;

	public Controller(DynamicCore core) {
		this.core = core;
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		System.out.println("TEST STATECHANGED");
		if(core != null) {
			int timepoint = ((JSlider)e.getSource()).getValue();
			core.setCurrTimepoint(timepoint);
		}
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("Play")) {
			core.play();
		}else if(e.getActionCommand().equals("Pause")) {
			core.pausePlay();
		}else if(e.getActionCommand().equals("Stop")) {
			core.stopPlay();
		}
		
		
	}

}
