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
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class DynamicView extends JSplitPane implements DynamicGraph, PropertyChangeListener{
	private static final long serialVersionUID = 4111494340467647183L;
	
	private TranslatorSBMLgraphPanel graphPanel;
	private DynamicControlPanel controlPanel;
	
	private DynamicCore core;
	
	public DynamicView(SBMLDocument document){
		super(JSplitPane.VERTICAL_SPLIT, false);
		graphPanel = new TranslatorSBMLgraphPanel(document, false);
		controlPanel = new DynamicControlPanel(); //TODO: set core
		
		add(graphPanel);
		add(controlPanel);
		
		setDividerLocation(300); 
		//TODO: adjust location fullscreen/windowed
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double, org.simulator.math.odes.MultiTable)
	 */
	@Override
	public void updateGraph(double timepoint, MultiTable updateThem) {
		//TODO
		System.out.println("corechange");
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		// TODO set core of controlPanel
		
		core = new DynamicCore(this);
		//core.setData(TODO);
		controlPanel.setCore(core);
		//System.out.println("done");
		
	}

}
