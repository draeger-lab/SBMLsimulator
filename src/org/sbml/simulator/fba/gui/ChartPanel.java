/*
 * $Id:  ChartPanel.java 16:42:12 Meike Aichele$
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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;


/**
 * this class visualizes the table/chart in which the computed data
 * of fba is shown.
 * 
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class ChartPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String[][] dataFluxes;
	private String[][] dataConc;
	private JTable tableFluxes;
	private JTable tableConc;

	private SBMLDocument doc;

	public ChartPanel(String[][] rowDataFluxes, String[][] rowDataConc, String[] columnNamesFluxes, String[] columnNamesConc) {
		super();
		JTabbedPane tabs = new JTabbedPane();
		tableFluxes = new JTable(rowDataFluxes, columnNamesFluxes);
		tableConc = new JTable(rowDataConc, columnNamesConc);
		tabs.add("Fluxes", tableFluxes);
		tabs.add("Concentrations", tableConc);
		this.add(tabs);
	}

	public ChartPanel(SBMLDocument document) {
		super();
		doc = document;
	}

	public void setConcentrations(double[] solution_concentrations) {
		SBMLDocument doc_new = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(doc);
		if (solution_concentrations != null) {
			String[] speciesId = new String[doc_new.getModel().getSpeciesCount()];
			for (int i = 0; i < doc_new.getModel().getSpeciesCount(); i++) {
				speciesId[i] = doc_new.getModel().getSpecies(i).getId();
			}
			
			dataConc = new String[solution_concentrations.length][2];
			for (int i = 0; i < solution_concentrations.length; i++) {
				dataConc[i][0] = speciesId[i];
				dataConc[i][1] = Double.toString(solution_concentrations[i]);
			}
		}
	}

	public void init() {
		if (dataFluxes != null) {
			String[] columns = {"Id", "Value"};
			tableFluxes = new JTable(dataFluxes, columns);
		} 
		if (dataConc != null) {
			String[] columns = {"Id", "Value"};
			tableConc = new JTable(dataConc, columns);
		}
		JPanel panel = new JPanel();

		if (dataFluxes != null) {
			JScrollPane scrollPane = new JScrollPane(tableFluxes);
			tableFluxes.setFillsViewportHeight(true);
			panel.add(scrollPane);
		}

		if (dataConc != null) {
			JScrollPane scrollPane = new JScrollPane(tableConc);
			tableConc.setFillsViewportHeight(true);			
			panel.add(scrollPane);
		}

		this.add(panel);
	}

	public void setFluxes(double[] solution_fluxVector) {
		SBMLDocument doc_new = FluxMinimizationUtils.eliminateTransportsAndSplitReversibleReactions(doc);
		String[] reactionIds = new String[doc_new.getModel().getReactionCount()];
		for (int i = 0; i < reactionIds.length; i++) {
			reactionIds[i] = doc_new.getModel().getReaction(i).getId();
		}
		
		if (solution_fluxVector != null) {
			dataFluxes = new String[solution_fluxVector.length][2];
			for (int i = 0; i < solution_fluxVector.length; i++) {
				dataFluxes[i][0] = reactionIds[i];
				dataFluxes[i][1] = Double.toString(solution_fluxVector[i]);
			}
		}
	}
}
