/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.simulator.SimulationOptions;

import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @date 07.12.2011
 */
public class ToolPanel extends JPanel implements ChangeListener, ItemListener, KeyListener {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 732384672242621355L;
	
	public ToolPanel() {
		super();
		SBPreferences prefs = SBPreferences.getPreferencesFor(SimulationOptions.class);
		add(PreferencesPanel.createJComponentForOption(SimulationOptions.SIM_START_TIME, prefs, this));
		add(PreferencesPanel.createJComponentForOption(SimulationOptions.SIM_END_TIME, prefs, this));
		
		setBorder(BorderFactory.createLineBorder(Color.BLACK));
	}
	
	/**
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		JOptionPane.showMessageDialog(null, new ToolPanel());
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent)
	 */
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent)
	 */
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent)
	 */
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
	 */
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	public void stateChanged(ChangeEvent e) {
		// TODO Auto-generated method stub
		
	}
}
