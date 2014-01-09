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
package org.sbml.simulator.gui;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.simulator.SimulationOptions;

import de.zbit.gui.prefs.PreferencesPanel;
import de.zbit.gui.prefs.PreferencesPanelForKeyProvider;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @date 07.12.2011
 */
public class ToolPanel extends PreferencesPanelForKeyProvider {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = 732384672242621355L;
	
	public ToolPanel() throws IOException {
		super(SimulationOptions.class);
	}
	
	/**
	 * 
	 * @param args
	 * @throws IOException 
	 * @throws HeadlessException 
	 */
	public static void main(String args[]) throws HeadlessException, IOException {
		JOptionPane.showMessageDialog(null, new ToolPanel());
	}

}
