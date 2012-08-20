/*
 * $Id:  FBAPanel.java 16:38:58 Meike Aichele$
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
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.simulator.fba.controller.CSVDataConverter;
import org.sbml.simulator.fba.controller.Constraints;
import org.sbml.simulator.fba.controller.FluxBalanceAnalysis;
import org.sbml.simulator.fba.controller.FluxMinimizationUtils;
import org.sbml.simulator.gui.LegendPanel;
import org.sbml.simulator.gui.SimulationPanel;

import de.zbit.util.prefs.SBPreferences;


/**
 * the FBAPanel consists of 3 components:
 * - a ChartPanel, which shows the computed data of fba in a table
 * - a SettingPanel, which shows the setting that can be made to change the result of fba
 *   and its visualization (the VODPanel)
 * - a VODPanel, which visualizes the results of fba in a diagram.
 *
 * @author Meike Aichele
 * @version $Rev$
 * @date 07.05.2012
 * @since 1.0
 */
public class FBAPanel extends JPanel implements ActionListener, TableModelListener{

	/**
	 * The tab with the chart of concentrations
	 */
	private ChartPanel chartConc;

	/**
	 * The tab with the chart of fluxes
	 */
	private ChartPanel chartFlux;

	/**
	 * The concentration file
	 */
	private File concFile;

	/**
	 * The concentration lower bounds
	 */
	private double[] ConcLowerBound;

	/**
	 * The concentration upper bounds
	 */
	private double[] ConcUpperBound;

	/**
	 * The current opened {@link SBMLDocument}
	 */
	private SBMLDocument originalDoc;

	/**
	 * Constraint compute Error is on, when this variable is set true.
	 */
	private boolean Error_constraint;

	/**
	 * The current computed {@link FluxBalanceAnalysis}
	 */
	private FluxBalanceAnalysis fba;

	/**
	 * The fluxes lower bounds
	 */
	private double[] FluxLowerBound;

	/**
	 * The fluxes upper bounds
	 */
	private double[] FluxUpperBound;

	/**
	 * the Gibbs energy file
	 */
	private File gibbsFile;

	/**
	 * Number of iterations for CPLEX
	 */
	private int iterations;

	/**
	 * Constraint J*G < 0 is on, when this variable is set true.
	 */
	private boolean JG_less_than_0;

	/**
	 * Constraint J > 0 is on, when this variable is set true.
	 */
	private boolean J_greater_0;

	/**
	 * Constraint |J| - r_max*|G| < 0 is on, when this variable is set true
	 */
	private boolean J_rmax_G_less_than_0;

	/**
	 * weighted factor lambda1 to weight the concentrations
	 */
	private double lambda1;

	/**
	 * weighted factor lambda2 to weight the errors
	 */
	private double lambda2;

	/**
	 * weighted factor lambda3 to weight the L-Matrix
	 */
	private double lambda3;

	/**
	 * weighted factor lambda4 to weight the Gibbs energies
	 */
	private double lambda4;

	/**
	 * Contains the parent simulator panel.
	 */
	private SimulationPanel simultorPanel;

	/**
	 * serial version number
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The setting panel wich contains the upper and lower bounds of fluxes and concentrations
	 */
	private FBASettingPanel settings;

	/**
	 * Contains the system boundaries file.
	 */
	private File systemBoundariesFile;

	/**
	 * The target fluxes
	 */
	private String[] targetFluxes = null;

	/**
	 * Contains if this is the first call of FBA
	 */
	private boolean theFirstCall = true;

	/**
	 * Contains the visualization of data
	 */
	private VODPanel vod;

	/**
	 * Constructor that sets all the different panels
	 * @param document
	 */
	public FBAPanel (SBMLDocument document) {
		super(new BorderLayout());
		originalDoc = document;
		
		//set the panels
		this.chartConc = new ChartPanel(document);
		this.chartFlux = new ChartPanel(document);
		this.settings = new FBASettingPanel(this);
		this.vod = new VODPanel();
		
		//init the fba panel
		init();
		setVisible(true);
	}


	/*
	 * (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
	}

	/**
	 * Check if the options were changed.
	 */
	private void checkPreferences() {
		//check files
		SBPreferences sbPrefs = SBPreferences.getPreferencesFor(FBAOptions.class);
		if (sbPrefs.getFile(FBAOptions.LOAD_CONCENTRATION_FILE) != null) {
			concFile = sbPrefs.getFile(FBAOptions.LOAD_CONCENTRATION_FILE);
		}
		if (sbPrefs.getFile(FBAOptions.LOAD_GIBBS_FILE)!= null) {
			gibbsFile = sbPrefs.getFile(FBAOptions.LOAD_GIBBS_FILE);
		}
		if (sbPrefs.getFile(FBAOptions.LOAD_SYSTEM_BOUNDARIES_FILE) != null) {
			systemBoundariesFile = sbPrefs.getFile(FBAOptions.LOAD_SYSTEM_BOUNDARIES_FILE);
		}
		//check constraints and interations
		Error_constraint = sbPrefs.getBoolean(FBAOptions.ACTIVATE_CONSTRAINT_ERROR);
		JG_less_than_0 = sbPrefs.getBoolean(FBAOptions.ACTIVATE_CONSTRAINT_JG_LESS_THAN_0);
		J_rmax_G_less_than_0 = sbPrefs.getBoolean(FBAOptions.ACTIVATE_CONSTRAINT_J_R_MAX_G_LESS_THAN_0);
		J_greater_0 = sbPrefs.getBoolean(FBAOptions.ACTIVATE_CONSTRAINT_J_GREATER_THAN_0);
		iterations = sbPrefs.getInt(FBAOptions.SET_ITERATIONS);
	
		//check lambdas
		lambda1 = sbPrefs.getDouble(FBAOptions.LAMBDA1);
		lambda2 = sbPrefs.getDouble(FBAOptions.LAMBDA2);
		lambda3 = sbPrefs.getDouble(FBAOptions.LAMBDA3);
		lambda4 = sbPrefs.getDouble(FBAOptions.LAMBDA4);
	
		//check upper and lower bounds
		ConcLowerBound = settings.getConcLowerBoundValues();
		ConcUpperBound = settings.getConcUpperBoundValues();
		FluxLowerBound = settings.getFluxLowerBoundValues();
		FluxUpperBound = settings.getFluxUpperBoundValues();
	}


	/**
	 * @return the concFile
	 */
	public File getConcFile() {
		return concFile;
	}


	/**
	 * @return the currentDoc
	 */
	public SBMLDocument getCurrentDoc() {
		return originalDoc;
	}


	/**
	 * @return the fba
	 */
	public FluxBalanceAnalysis getFba() {
		return fba;
	}


	/**
	 * @return the settings
	 */
	public FBASettingPanel getFBASettingPanel() {
		return settings;
	}


	/**
	 * @return the gibbsFile
	 */
	public File getGibbsFile() {
		return gibbsFile;
	}


	/**
	 * @return the vod
	 */
	public VODPanel getVODPanel() {
		return vod;
	}

	/**
	 * Initialize the whole FBA frame.
	 */
	private void init() {
		JSplitPane jsp = new JSplitPane();
		vod.setBorder(BorderFactory.createLoweredBevelBorder());
	
		// legend panel
		LegendPanel legendPanel = new LegendPanel(originalDoc.getModel(), true);
		legendPanel.addTableModelListener(this);
		legendPanel.setBorder(BorderFactory.createLoweredBevelBorder());
	
		// split left components
		JSplitPane topDown = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				legendPanel, settings);
		topDown.setDividerLocation(topDown.getDividerLocation() + 100);
	
		JTabbedPane tabs = new JTabbedPane();
		tabs.add("Visualization", vod);
		tabs.add("Table Concentrations", chartConc);
		tabs.add("Table Fluxes", chartFlux);
	
		// split all
		jsp.setLeftComponent(topDown);
		jsp.setRightComponent(tabs);
		jsp.setDividerLocation(topDown.getDividerLocation() + 200);
	
		add(jsp);
	}


	/**
	 * @return true if all bounds are set, else false.
	 */
	private boolean isSetBounds() {
		return FluxLowerBound != null && FluxUpperBound!= null && ConcLowerBound != null && ConcUpperBound != null;
	}


	/**
	 * @return the first call boolean
	 */
	public boolean isTheFirstCall() {
		return theFirstCall;
	}

	/**
	 * @param concFile the concFile to set
	 */
	public void setConcFile(File concFile) {
		this.concFile = concFile;
	}


	/**
	 * @param currentDoc the currentDoc to set
	 */
	public void setCurrentDoc(SBMLDocument currentDoc) {
		this.originalDoc = currentDoc;
	}


	/**
	 * @param settings the settings to set
	 */
	public void setFBASettingPanel(FBASettingPanel settings) {
		this.settings = settings;
	}


	/**
	 * @param gibbsFile the gibbsFile to set
	 * @throws Exception 
	 */
	public void setGibbsFile(File gibbsFile) throws Exception {
		this.gibbsFile = gibbsFile;
		startFBA();
	}


	/**
	 * @param simPanel
	 */
	public void setSimulatorPanel(SimulationPanel simPanel) {
		simultorPanel = simPanel;
		init();
	}


	/**
	 * @param firstCall set the boolean that contains true if
	 * this is the first call
	 */
	public void setTheFirstCall(boolean firstCall) {
		theFirstCall = firstCall;
	}


	/**
	 * @param vod the vod to set
	 */
	public void setVODPanel(VODPanel vod) {
		this.vod = vod;
	}


	/**
	 * start the flux balance analysis
	 * @throws Exception 
	 */
	public void startFBA() throws Exception {
		checkPreferences();
		try {
			CSVDataConverter csvConverterSysBounds;
			
			if (systemBoundariesFile != null) {
				csvConverterSysBounds = new CSVDataConverter(originalDoc, true);
				csvConverterSysBounds.readSystemBoundariesFromFile(systemBoundariesFile);
			}
			else {
				csvConverterSysBounds = new CSVDataConverter(originalDoc);
			}

			while(csvConverterSysBounds.getSystemBoundariesArray() == null) {
				//wait while reading
			}

			double[] sysBounds = FluxMinimizationUtils.getCorrectedSystemBoundaries(originalDoc, csvConverterSysBounds.getSystemBoundariesArray());
			CSVDataConverter csvConverterConc = new CSVDataConverter(originalDoc, sysBounds);
			if (concFile != null) {
				csvConverterConc.readConcentrationsFromFile(concFile);
				while(csvConverterConc.getConcentrationsArray() == null) {
					//wait while reading
				}
			} 
			
			CSVDataConverter csvConverterGibbs = new CSVDataConverter(originalDoc, sysBounds);
			if (gibbsFile != null) {
				csvConverterGibbs.readGibbsFromFile(gibbsFile);
				while(csvConverterGibbs.getGibbsArray() == null) {
					//wait while reading
				}
			}

			Constraints c = new Constraints(originalDoc, csvConverterGibbs.getGibbsArray(), csvConverterConc.getConcentrationsArray(), sysBounds, true);
			fba = new FluxBalanceAnalysis(originalDoc, c, targetFluxes);
	
			//set constraints and iterations
			fba.setConctraintError(Error_constraint);
			fba.setConstraintJG(JG_less_than_0);
			fba.setConstraintJ_rmaxG(J_rmax_G_less_than_0);
			fba.setCplexIterations(iterations);
			fba.setConstraintJ0(J_greater_0);
	
			//set lambdas
			fba.setLambda1(lambda1);
			fba.setLambda2(lambda2);
			fba.setLambda3(lambda3);
			fba.setLambda4(lambda4);
	
			//set bounds
			if (isSetBounds() && settings.isBoundsAreChanged()) {
				fba.setLbOfConcentrations(ConcLowerBound);
				fba.setLbOfReactions(FluxLowerBound);
				fba.setUbOfConcentrations(ConcUpperBound);
				fba.setUbOfReactions(FluxUpperBound);
			}
	
			//solve
			try{
			fba.solve();
			} catch (ilog.cplex.CpxException cplexExc){
				cplexExc.printStackTrace();
				JOptionPane.showMessageDialog(this,
						new String("Only the null vector solution found."),
						"available solutions",
						JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this,
					e,
					"Running FBA failed",
					JOptionPane.ERROR_MESSAGE);
		}
		
		//visualize fba
		simultorPanel.getDynamicGraphView().addFluxbalance(fba.fluxesForVisualization);
		
		if (theFirstCall) {
			vod.setDocument(FluxMinimizationUtils.getExpandedDocument(originalDoc));
			vod.setFluxes(fba.solutionFluxVector);
			vod.setConcentrations(fba.solutionConcentrations);
			vod.init();
			chartFlux.setFluxes(fba.solutionFluxVector);
			chartConc.setConcentrations(fba.solutionConcentrations);
			chartFlux.init();
			chartConc.init();
			settings.setFBA(fba);
			this.repaint();
		} else {
			vod.setDocument(FluxMinimizationUtils.getExpandedDocument(originalDoc));
			vod.setFluxes(fba.solutionFluxVector);
			vod.setConcentrations(fba.solutionConcentrations);
			vod.updateUI();
			chartFlux.setFluxes(fba.solutionFluxVector);
			chartConc.setConcentrations(fba.solutionConcentrations);
			chartFlux.getTableFluxes().revalidate();
			chartFlux.getTableFluxes().repaint();
			chartFlux.getTableFluxes().updateUI();
			chartConc.getTableConc().repaint();
			settings.setFBA(fba);
			this.repaint();
		}
	}


	/*
	 * (non-Javadoc)
	 * @see javax.swing.event.TableModelListener#tableChanged(javax.swing.event.TableModelEvent)
	 */
	@Override
	public void tableChanged(TableModelEvent arg0) {
		// TODO Auto-generated method stub
	}

}
