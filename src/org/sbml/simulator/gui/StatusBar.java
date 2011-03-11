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
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 * @version $Rev$
 * @since 1.0
 */
public class StatusBar extends JPanel {

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -719932485499514514L;

	private JLabel label;

	/**
	 * 
	 */
	public StatusBar() {
		super(new BorderLayout());

		setPreferredSize(new Dimension(100, 23));

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.setOpaque(false);

		add(rightPanel, BorderLayout.EAST);

		label = new JLabel();
		add(label, BorderLayout.CENTER);

		setMessage("Ready");
		setBorder(BorderFactory.createLoweredBevelBorder());
	}

	/**
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		label.setText(" " + message);
	}
	
	/**
	 * @param text
	 */
	public StatusBar(String text) {
		this();
		setMessage(text);
	}

}
