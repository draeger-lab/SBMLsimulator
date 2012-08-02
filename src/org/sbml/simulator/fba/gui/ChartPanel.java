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
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
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
	 * Default serial version number
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 2D-Array that contains the data of the computed fluxes in {@link FluxBalanceAnalysis}
	 */
	private String[][] dataFluxes;
	
	/**
	 * 2D-Array that contains the data of the computed concentrations in {@link FluxBalanceAnalysis}
	 */
	private String[][] dataConc;
	
	/**
	 * The table that shows the computed values of fluxes
	 */
	private JTable tableFluxes;
	
	/**
	 * Table that shows the computed values of concentrations
	 */
	private JTable tableConc;

	/**
	 * The current {@link SBMLDocument}
	 */
	private SBMLDocument doc;

	
	/**
	 * Constructor that creates the tables of the incoming data-arrays
	 * @param rowDataFluxes
	 * @param rowDataConc
	 * @param columnNamesFluxes
	 * @param columnNamesConc
	 */
	public ChartPanel(String[][] rowDataFluxes, String[][] rowDataConc, String[] columnNamesFluxes, String[] columnNamesConc) {
		super();
		JTabbedPane tabs = new JTabbedPane();
		tableFluxes = new JTable(rowDataFluxes, columnNamesFluxes);
		tableConc = new JTable(rowDataConc, columnNamesConc);
		tabs.add("Fluxes", tableFluxes);
		tabs.add("Concentrations", tableConc);
		this.add(tabs);
	}

	/**
	 * Constructor that gets only the {@link SBMLDocument}
	 * @param document
	 */
	public ChartPanel(SBMLDocument document) {
		super();
		doc = document;
	}

	/**
	 * Sets the concentrations and creates the table of this content
	 * @param solution_concentrations
	 * @throws Exception 
	 */
	public void setConcentrations(double[] solution_concentrations) throws Exception {
		SBMLDocument doc_new = FluxMinimizationUtils.getExpandedDocument(doc);
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

	/**
	 * Initializes the whole panel
	 */
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

	/**
	 * Sets the fluxes and creates the table of fluxes, if the incoming array is not null.
	 * @param solution_fluxVector
	 * @throws Exception 
	 */
	public void setFluxes(double[] solution_fluxVector) throws Exception {
		SBMLDocument doc_new = FluxMinimizationUtils.getExpandedDocument(doc);
		String[] reactionIds = new String[doc_new.getModel().getReactionCount()];
		for (int i = 0; i < reactionIds.length; i++) {
			reactionIds[i] = doc_new.getModel().getReaction(i).getId();
		}
		// if there is a solution
		if (solution_fluxVector != null) {
			dataFluxes = new String[solution_fluxVector.length][2];
			for (int i = 0; i < solution_fluxVector.length; i++) {
				dataFluxes[i][0] = reactionIds[i];
				dataFluxes[i][1] = Double.toString(solution_fluxVector[i]);
			}
		}
	}

	/**
	 * @return the tableFluxes
	 */
	public JTable getTableFluxes() {
		return tableFluxes;
	}

	/**
	 * @return the tableConc
	 */
	public JTable getTableConc() {
		return tableConc;
	}
}
