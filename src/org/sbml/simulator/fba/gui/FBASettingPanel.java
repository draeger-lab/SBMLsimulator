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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


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
public class FBASettingPanel extends JPanel implements ActionListener, ChangeListener{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The corresponding FluxBalanceAnalysis-object
	 */
	private FluxBalanceAnalysis fba;

	/**
	 * Backup of the original values
	 */
	private double originalConcValues[][];

	/**
	 * Backup of the original values
	 */
	private double originalFluxValues[][];

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
	 * parent FBAPanel
	 */
	private FBAPanel parent;

	/**
	 * User changed the bounds of fluxes or concentrations
	 */
	private boolean boundsAreChanged;


	/**
	 * Default constructor
	 */
	public FBASettingPanel() {
		super(new BorderLayout());
		tab = new JTabbedPane();
		init();
	}

	/**
	 * Constructor that gets the corresponding {@link FBAPanel}
	 * @param parent
	 */
	public FBASettingPanel(FBAPanel parent) {
		super(new BorderLayout());
		this.parent = parent;
		fba = parent.getFba();
		tab = new JTabbedPane();
		document = parent.getCurrentDoc();
		init();
	}

	/**
	 * Initialize the whole panel with the different tabs
	 */
	private void init() {
		initTabs(document);

		JPanel foot = new JPanel();
		buttonReset = GUITools.createJButton(this, Command.RESET);
		buttonReset.setEnabled(false);
		foot.add(buttonReset);

		tab.setSize(500, 800);
		JScrollPane sp = new JScrollPane(tab);
		add(sp, BorderLayout.CENTER);
		add(foot, BorderLayout.SOUTH);

		this.setVisible(true);
	}

	/**
	 * Initialize the tabs
	 * @param document2
	 */
	private void initTabs(SBMLDocument document2) {
		if (document2 != null) {
			// create the panel for the concentrations of species
			JPanel speciesPanel = new JPanel();
			initSpecificTab(speciesPanel, document2.getModel().getListOfSpecies().toArray());
			tab.add("Concentrations", speciesPanel);

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

	/**
	 * initialize the components of the fluxes-/species-tab
	 * @param panel
	 * @param objects
	 */
	private void initSpecificTab(JPanel panel, Object[] objects) {
		if (objects != null){
			if(objects[0] instanceof Species) {
				concUpperBounds = new JSpinner[objects.length];
				concLowerBounds = new JSpinner[objects.length];
				originalConcValues = new double[objects.length][2];
				for(int i = 0; i < objects.length; i++) {

					//create components of this row 
					JPanel row_i = new JPanel(new BorderLayout());
					// upper bounds
					concUpperBounds[i] = new JSpinner();
					concUpperBounds[i].setValue(0.1);
					concUpperBounds[i].addChangeListener(this);

					// lower bounds
					concLowerBounds[i] = new JSpinner();
					concLowerBounds[i].setValue(0.0);
					concLowerBounds[i].addChangeListener(this);

					// original values
					originalConcValues[i][0] = (Double) concLowerBounds[i].getValue();
					originalConcValues[i][1] = (Double) concUpperBounds[i].getValue();

					Species currentSpec = (Species) objects[i];
					JLabel specLabel = new JLabel(currentSpec.isSetName() ? currentSpec.getName() : currentSpec.getId());

					//add Components
					LayoutHelper lh = new LayoutHelper(row_i);
					lh.add(specLabel);
					JPanel bounds = new JPanel();
					LayoutHelper lh2 = new LayoutHelper(bounds);
					lh2.add(concLowerBounds[i], 2, i, 1, 1, 0, 0);
					lh2.add(concUpperBounds[i], 4, i, 1, 1, 0, 0);
					lh.add(bounds);
					panel.add(row_i);
				}
			} else if (objects[0] instanceof Reaction) {
				fluxUpperBounds = new JSpinner[objects.length];
				fluxLowerBounds = new JSpinner[objects.length];
				originalFluxValues = new double[objects.length][2];
				for(int i = 0; i < objects.length; i++) {

					//create components of this row 
					JPanel row_i = new JPanel(new BorderLayout());

					//upper bounds
					fluxUpperBounds[i] = new JSpinner();
					fluxUpperBounds[i].setValue(10000.0);
					fluxUpperBounds[i].addChangeListener(this);

					//lower bounds
					fluxLowerBounds[i] = new JSpinner();
					fluxLowerBounds[i].setValue(0.0);
					fluxLowerBounds[i].addChangeListener(this);

					//original values
					originalFluxValues[i][0] = (Double) fluxLowerBounds[i].getValue();
					originalFluxValues[i][1] = (Double) fluxUpperBounds[i].getValue();

					Reaction currentReac = (Reaction) objects[i];
					JLabel reacLabel = new JLabel(currentReac.isSetName() ? currentReac.getName() : currentReac.getId());

					//add Components
					LayoutHelper lh = new LayoutHelper(row_i);
					lh.add(reacLabel);
					JPanel bounds = new JPanel();
					LayoutHelper lh2 = new LayoutHelper(bounds);
					lh2.add(fluxLowerBounds[i], 2, i, 1, 1, 0, 0);
					lh2.add(fluxUpperBounds[i], 4, i, 1, 1, 0, 0);
					lh.add(bounds);
					panel.add(row_i);
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
		Object source = e.getSource();
		if (source.equals(buttonReset) && tab.getSelectedIndex() == 1) {
			// its the flux tab
			for (int i = 0; i < fluxLowerBounds.length; i++) {
				fluxLowerBounds[i].setValue(originalFluxValues[i][0]);
				fluxUpperBounds[i].setValue(originalFluxValues[i][1]);
				fba.setLbOfReactionJ(originalFluxValues[i][0], i);
				fba.setUbOfReactionJ(originalFluxValues[i][1], i);
			}
		} else if (source.equals(buttonReset) && tab.getSelectedIndex() == 0) {
			// its the conc tab
			for (int j = 0; j < concLowerBounds.length; j++) {
				concLowerBounds[j].setValue(originalConcValues[j][0]);
				concUpperBounds[j].setValue(originalConcValues[j][1]);
				fba.setLbOfConcentrationI(originalConcValues[j][0], j);
				fba.setUbOfConcentrationI(originalConcValues[j][1], j);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent evt) {
		JSpinner source = (JSpinner) evt.getSource();
		for (int i = 0; i < fluxLowerBounds.length; i++) {
			if (source.equals(fluxLowerBounds[i])){
				// the source is a lb source of fluxes
				if (source.getValue() instanceof Integer) {
					fba.setLbOfReactionJ((Integer) source.getValue(), i);
				} else if (source.getValue() instanceof Double) {
					fba.setLbOfReactionJ((Double) source.getValue(), i);
				}
			} else if (source.equals(fluxUpperBounds[i])) {
				// the source is a ub source of fluxes
				if (source.getValue() instanceof Integer) {
					fba.setUbOfReactionJ((Integer) source.getValue(), i);
				} else if (source.getValue() instanceof Double) {
					fba.setUbOfReactionJ((Double) source.getValue(), i);
				}
			}
		}
		for (int s = 0; s < concLowerBounds.length; s++) {
			if (source.equals(concLowerBounds[s])) {
				// the source is a lb source of concentrations
				if (source.getValue() instanceof Integer) {
					fba.setLbOfConcentrationI((Integer) source.getValue(), s);
				} else if (source.getValue() instanceof Double) {
					fba.setLbOfConcentrationI((Double) source.getValue(), s);
				}
			} else if (source.equals(concUpperBounds[s])) {
				// the source is a ub source of concentrations
				if (source.getValue() instanceof Integer) {
					fba.setUbOfConcentrationI((Integer) source.getValue(), s);
				} else if (source.getValue() instanceof Double) {
					fba.setUbOfConcentrationI((Double) source.getValue(), s);
				}
			}
		}
		buttonReset.setEnabled(true);
		boundsAreChanged = true;
	}

	/**
	 * sets the fba object
	 * @param fba2
	 */
	public void setFBA(FluxBalanceAnalysis fba2) {
		this.fba = fba2;
	}

	/**
	 * @return the concUpperBounds
	 */
	public JSpinner[] getConcUpperBounds() {
		return concUpperBounds;
	}

	/**
	 * @param concUpperBounds the concUpperBounds to set
	 */
	public void setConcUpperBounds(JSpinner[] concUpperBounds) {
		this.concUpperBounds = concUpperBounds;
	}

	/**
	 * @return the concLowerBounds
	 */
	public JSpinner[] getConcLowerBounds() {
		return concLowerBounds;
	}

	/**
	 * @param concLowerBounds the concLowerBounds to set
	 */
	public void setConcLowerBounds(JSpinner[] concLowerBounds) {
		this.concLowerBounds = concLowerBounds;
	}

	/**
	 * @return the fluxUpperBounds
	 */
	public JSpinner[] getFluxUpperBounds() {
		return fluxUpperBounds;
	}

	/**
	 * @param fluxUpperBounds the fluxUpperBounds to set
	 */
	public void setFluxUpperBounds(JSpinner[] fluxUpperBounds) {
		this.fluxUpperBounds = fluxUpperBounds;
	}

	/**
	 * @return the fluxLowerBounds
	 */
	public JSpinner[] getFluxLowerBounds() {
		return fluxLowerBounds;
	}

	/**
	 * @param fluxLowerBounds the fluxLowerBounds to set
	 */
	public void setFluxLowerBounds(JSpinner[] fluxLowerBounds) {
		this.fluxLowerBounds = fluxLowerBounds;
	}

	/**
	 * @return the fluxLowerBoundValues
	 */
	public double[] getFluxLowerBoundValues() {
		double[] flb = new double[fluxLowerBounds.length];
		for (int i = 0; i < flb.length; i++) {
			if (fluxLowerBounds[i].getValue() instanceof Double) {
				flb[i] = (Double) fluxLowerBounds[i].getValue();
			} else if (fluxLowerBounds[i].getValue() instanceof Integer) {
				flb[i] = (Integer) fluxLowerBounds[i].getValue();
			}
		}
		return flb;
	}

	/**
	 * @return the fluxUpperBoundValues
	 */
	public double[] getFluxUpperBoundValues() {
		double[] fub = new double[fluxUpperBounds.length];
		for (int i = 0; i < fub.length; i++) {
			if (fluxUpperBounds[i].getValue() instanceof Double) {
				fub[i] = (Double) fluxUpperBounds[i].getValue();
			} else if (fluxUpperBounds[i].getValue() instanceof Integer) {
				fub[i] = (Integer) fluxUpperBounds[i].getValue();
			}
		}
		return fub;
	}


	/**
	 * @return the concLowerBoundValues
	 */
	public double[] getConcLowerBoundValues() {
		double[] clb = new double[concLowerBounds.length];
		for (int i = 0; i < clb.length; i++) {
			if (concLowerBounds[i].getValue() instanceof Double) {
				clb[i] = (Double) concLowerBounds[i].getValue();
			} else if (concLowerBounds[i].getValue() instanceof Integer) {
				clb[i] = (Integer) concLowerBounds[i].getValue();
			}
		}
		return clb;
	}

	/**
	 * @return the concUpperBoundValues
	 */
	public double[] getConcUpperBoundValues() {
		double[] cub = new double[concUpperBounds.length];
		for (int i = 0; i < cub.length; i++) {
			if (concUpperBounds[i].getValue() instanceof Double) {
				cub[i] = (Double) concUpperBounds[i].getValue();
			} else if (concUpperBounds[i].getValue() instanceof Integer) {
				cub[i] = (Integer) concUpperBounds[i].getValue();
			}
		}
		return cub;
	}


	/**
	 * @return the boundsAreChanged
	 */
	public boolean isBoundsAreChanged() {
		return boundsAreChanged;
	}

}
