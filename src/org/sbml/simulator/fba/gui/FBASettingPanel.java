/*
 * $Id:  FBASettingPanel.java 16:54:53 Meike Aichele$
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
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;


import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import org.sbml.jsbml.Species;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
import org.sbml.simulator.gui.InteractiveScanPanel.Command;

import de.zbit.gui.GUITools;
import de.zbit.gui.layout.LayoutHelper;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FBASettingPanel extends JPanel implements ActionListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private FluxBalanceAnalysis fba;

	/**
	 * Backup of the original values
	 */
	private double originalValues[];

	/**
	 * button to reset the changes
	 */
	private JButton buttonReset;

	/**
	 * containing the tabs fluxes, species and general properties.
	 */
	private JTabbedPane tab;
	
	/**
	 * contains the current {@link SBMLDocument}.
	 */
	private SBMLDocument document;

	/**
	 * array of JSpinners for concentrations upper bounds
	 */
	private JSpinner[] concUpperBounds;
	
	/**
	 * array of JSpinners for concentrations lower bounds
	 */
	private JSpinner[] concLowerBounds;
	
	/**
	 * array of JSpinners for fluxes upper bounds
	 */
	private JSpinner[] fluxUpperBounds;
	
	/**
	 * array of JSpinners for fluxes lower bounds
	 */
	private JSpinner[] fluxLowerBounds;
	
	
	

	/**
	 * default constructor
	 */
	public FBASettingPanel() {
		super(new BorderLayout());
		tab = new JTabbedPane();
		init();
		// TODO Auto-generated constructor stub
	}

	/**
	 * constructor that gets the corresponding {@link FBAPanel}
	 * @param parent
	 */
	public FBASettingPanel(FBAPanel parent) {
		super(new BorderLayout());
		fba = parent.getFba();
		tab = new JTabbedPane();
		document = parent.getCurrentDoc();
		init();
	}

	private void init() {
		initTabs(document);
		
		JPanel foot = new JPanel();
	    buttonReset = GUITools.createJButton(this, Command.RESET);
	    buttonReset.setEnabled(false);
	    foot.add(buttonReset);
	    
	    tab.setSize(500, 800);
	    add(tab, BorderLayout.CENTER);
	    add(foot, BorderLayout.SOUTH);
		
		this.setVisible(true);
	}

	private void initTabs(SBMLDocument document2) {
		if (document2 != null) {
			// create the panel for the concentrations of species
			JPanel speciesPanel = new JPanel();
			LayoutHelper lh = new LayoutHelper(speciesPanel);
			initSpecificTab(speciesPanel, document2.getModel().getListOfSpecies().toArray());
			tab.add("Concentrations", speciesPanel);
			
			// create the panel for general properties
			JPanel propertiesPanel = new JPanel();
			initPropertyTab(propertiesPanel);
			propertiesPanel.setVisible(true);
			tab.add("Properties", propertiesPanel);
			
			// create the panel for the flux values
			JPanel fluxPanel = new JPanel();
			try {
				initSpecificTab(fluxPanel, document2.getModel().getListOfReactions().toArray());
			} catch (Exception e) {
				LinkedList<Integer> nullList = new LinkedList<Integer>();
				for (int i = 0; i < document2.getModel().getListOfReactions().size(); i++) {
					nullList.add(0);
				}
				initSpecificTab(fluxPanel, null);
				e.printStackTrace();
			}
			tab.add("Fluxes", fluxPanel);
		}
	}

	private void initPropertyTab(JPanel propertiesPanel) {
		// TODO Auto-generated method stub
		
	}

	private void initSpecificTab(JPanel panel,
			Object[] objects) {
		if (objects != null){
			if(objects[0] instanceof Species) {
				concUpperBounds = new JSpinner[objects.length];
				concLowerBounds = new JSpinner[objects.length];
				for(int i = 0; i < objects.length; i++) {
					
					//create components of this row 
					JPanel row_i = new JPanel(new BorderLayout());
					concUpperBounds[i] = new JSpinner();
					concUpperBounds[i].setValue(0.1);
					concLowerBounds[i] = new JSpinner();
					concLowerBounds[i].setValue(0);
					Species currentSpec = (Species) objects[i];
					JLabel specLabel = new JLabel(currentSpec.isSetName() ? currentSpec.getName() : currentSpec.getId());
					
					//add Components
					row_i.add(specLabel);
					row_i.add(concLowerBounds[i]);
					row_i.add(concUpperBounds[i]);
				}
			} else if (objects[0] instanceof Reaction) {
				fluxUpperBounds = new JSpinner[objects.length];
				fluxLowerBounds = new JSpinner[objects.length];
				for(int i = 0; i < objects.length; i++) {
					
					//create components of this row 
					JPanel row_i = new JPanel(new BorderLayout());
					fluxUpperBounds[i] = new JSpinner();
					fluxUpperBounds[i].setValue(10000);
					fluxLowerBounds[i] = new JSpinner();
					fluxLowerBounds[i].setValue(-10000);
					Reaction currentReac = (Reaction) objects[i];
					JLabel reacLabel = new JLabel(currentReac.isSetName() ? currentReac.getName() : currentReac.getId());
					
					//add Components
					row_i.add(reacLabel, new FlowLayout(FlowLayout.LEFT));
					JPanel bounds = new JPanel();
					bounds.add(fluxLowerBounds[i]);
					bounds.add(fluxUpperBounds[i]);
					row_i.add(bounds, new FlowLayout(FlowLayout.CENTER));
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub

	}

}
