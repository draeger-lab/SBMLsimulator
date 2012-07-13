/*
 * $Id:  VODPanel.java 16:43:13 Meike Aichele$
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
package org.sbml.simulator.fba.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class represents a panel for the Visualization Of Datasets (VOD)
 * of the computed datasets of fba.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */

public class VODPanel extends JPanel implements ActionListener{

	/**
	 * default serial version
	 */
	private static final long serialVersionUID = 1L;
	
	private double[] fluxes;
	private double[] concentrations;
	private double[] gibbs_values;
	
	public VODPanel() {
		JPanel plot = new JPanel(new BorderLayout());
		JLabel label = new JLabel("here will be the panel to visualize the computed values of flux balance analysis");
		plot.setSize(200, 200);
		plot.add(label, BorderLayout.CENTER);
		
		JPanel changePlotPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton switchButton = new JButton("Change to dynamic View");
		switchButton.addActionListener(this);
		changePlotPanel.add(switchButton, BorderLayout.EAST);
		
		add(plot, BorderLayout.CENTER);
		add(changePlotPanel, BorderLayout.SOUTH);
		setVisible(true);
	}

	
	
	/**
	 * @return the fluxes
	 */
	public double[] getFluxes() {
		return fluxes;
	}

	/**
	 * @param fluxes the fluxes to set
	 */
	public void setFluxes(double[] fluxes) {
		this.fluxes = fluxes;
	}

	/**
	 * @return the concentrations
	 */
	public double[] getConcentrations() {
		return concentrations;
	}

	/**
	 * @param concentrations the concentrations to set
	 */
	public void setConcentrations(double[] concentrations) {
		this.concentrations = concentrations;
	}

	/**
	 * @return the gibbs_values
	 */
	public double[] getGibbs_values() {
		return gibbs_values;
	}

	/**
	 * @param gibbs_values the gibbs_values to set
	 */
	public void setGibbs_values(double[] gibbs_values) {
		this.gibbs_values = gibbs_values;
	}





	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

	
}
