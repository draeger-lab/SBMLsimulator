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

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;

import org.simulator.math.odes.MultiTable;

/**
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class JSliderTest extends JFrame implements DynamicGraph{
	private static final long serialVersionUID = -7669702948757945505L;
	
	JFrame window = new JFrame("SliderTest");
	JSlider searchBar = new JSlider();
	JLabel timepoint = new JLabel("0");
	JLabel dataStrings = new JLabel("data");
	JButton play = new JButton("Play");
	JButton pause = new JButton("Pause");
	
	
	private DynamicCore core = new DynamicCore(this);
	
	private Controller controller = new Controller(core, this);
	
	private MultiTable testTable = new MultiTable();
	
	private void generateTestTable(){
		double[] timepoints = {0.1,0.2,0.3,0.4,0.5};
		testTable.setTimePoints(timepoints);
		
		String[] IDs = {"eins", "zwei", "drei"};
		testTable.addBlock(IDs);
		
		double[][] data = {{1,2,3},{4,5,6},{7,8,9},{10,11,12},{13,14,15}};
		testTable.getBlock(0).setData(data);
	}
	
	public void init(){
		generateTestTable();
		core.setData(testTable);
		core.addObserver(this);
		
		searchBar.setMaximum(testTable.getRowCount()-1);
		searchBar.setMinimum(0);
		searchBar.setValue(0);
		searchBar.setMajorTickSpacing(1);
		searchBar.setPaintTicks(true);
		searchBar.addChangeListener(controller);
		
		play.addActionListener(controller);
		pause.addActionListener(controller);
		
		window.setSize(260, 200);
		window.setDefaultCloseOperation(EXIT_ON_CLOSE);
		window.setLayout(new FlowLayout());
		window.add(timepoint);
		window.add(dataStrings);
		window.add(searchBar);
		window.add(play);
		window.add(pause);
		window.setVisible(true);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.DynamicGraph#updateGraph(double, org.simulator.math.odes.MultiTable)
	 */
	@Override
	public void updateGraph(double timePoint, MultiTable updateThem) {
		timepoint.setText("Timepoint: " +timePoint);
		dataStrings.setText("Data: " + updateThem.getValueAt(0, 1));
		searchBar.setValue(core.getIndexOfTimePoint(timePoint));
	}

}
