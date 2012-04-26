/*
 * $Id$
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
package org.sbml.simulator.gui.graph;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JSplitPane;

import org.sbml.jsbml.SBMLDocument;
import org.simulator.math.odes.MultiTable;

import de.zbit.graph.gui.TranslatorSBMLgraphPanel;

/**
 * This class brings all elements concerning the dynamic visualization together
 * and makes sure that every part is consistent with each other, i.e. search bar with 
 * visualized values.
 * (This is ensured by the implementation of DynamicGraph. Every change of the slider
 * wents through this class).
 * It represents the view in MVC-Pattern and therefore awaits change notifies of the
 * DynamicCore.
 * 
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements DynamicGraph, PropertyChangeListener{
	private static final long serialVersionUID = 4111494340467647183L;
	
	/**
	 * Panel for the graph representation
	 */
	private TranslatorSBMLgraphPanel graphPanel;
	
	/**
	 * Panel for the controls of the dynamic visualization
	 */
	private DynamicControlPanel controlPanel;
	
	//TODO implementation of a panel to choose which elements should be visualized by matter of dynamics
	
	/**
	 * Pointer to the used core
	 */
	private DynamicCore core;

	
	/**
	 * Constructs all necessary elements
	 * @param document
	 */
	public DynamicView(SBMLDocument document){
		super(JSplitPane.VERTICAL_SPLIT, false);
		graphPanel = new TranslatorSBMLgraphPanel(document, false);
		controlPanel = new DynamicControlPanel();
		
		add(graphPanel);
		add(controlPanel);
		
		setDividerLocation(250); 
		//TODO: adjust location fullscreen/windowed
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double, org.simulator.math.odes.MultiTable)
	 */
	@Override
	public void updateGraph(double timepoint, MultiTable updateThem) {
		//TODO implement changes of the graphelements with respect to the given MultiTable and chosen elements
		controlPanel.setTimepoint(timepoint);
		
		//TODO update graph
		
		/*
		 * notify core that graph is updated
		 */
		System.out.println("DEBUG: graph finished");
		core.graphUpdateFinished();		
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent e) {
		if(e.getPropertyName().equals("done")){
			//simulation done
			MultiTable data = (MultiTable) e.getNewValue();
			if (data != null) {
				/*
				 * As a new core is constructed every time the simulation is finished,
				 * the control panel is consistent with the simulated data.
				 */
				core = new DynamicCore(this, data);
				controlPanel.setCore(core);
			}
		}
	}

}
