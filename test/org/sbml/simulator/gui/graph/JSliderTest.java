/*
 * $Id$
 * $URL$ 
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2016 by the University of Tuebingen, Germany.
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
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;

import org.simulator.math.odes.MultiTable;

/**
 * @author Fabian Schwarzkopf
 * @version $Rev$
 */
public class JSliderTest extends JFrame implements IDynamicGraph{
	private static final long serialVersionUID = -7669702948757945505L;
	
	JFrame window = new JFrame("SliderTest");
	JSlider searchBar = new JSlider();
	JLabel timepoint = new JLabel("Timepoint: 0");
	JLabel dataStrings = new JLabel("<data>");
	JButton play = new JButton("Play");
	JButton pause = new JButton("Pause");
	JButton stop = new JButton("Stop");
	
	
	private DynamicCore core;
	
	private Controller controller;
	
	private MultiTable testTable = new MultiTable();
	
	private void generateTestTable() {
		double[] timepoints = {0.1,0.2,0.3,0.4,0.5};
		testTable.setTimePoints(timepoints);
		
		String[] IDs = {"ID1", "ID2", "ID3"};
		testTable.addBlock(IDs);
		
		double[][] data = {{1,2,3},{4,5,6},{7,8,9},{10,11,12},{13,14,15}};
		testTable.getBlock(0).setData(data);
	}
	
	public void init() {
		generateTestTable();
		core = new DynamicCore(this, testTable);
		controller = new Controller(core);
		
		searchBar.setMaximum(testTable.getRowCount()-1);
		searchBar.setMinimum(0);
		searchBar.setValue(0);
		searchBar.setMajorTickSpacing(1);
		searchBar.setPaintTicks(true);
		searchBar.addChangeListener(controller);
		
		play.addActionListener(controller);
		pause.addActionListener(controller);
		stop.addActionListener(controller);
		
		window.setSize(260, 200);
		window.setDefaultCloseOperation(EXIT_ON_CLOSE);
		window.setLayout(new FlowLayout());
		window.add(timepoint);
		window.add(dataStrings);
		window.add(searchBar);
		window.add(play);
		window.add(pause);
		window.add(stop);
		window.setVisible(true);
	}

	/* (non-Javadoc)
	 * @see org.sbml.simulator.gui.graph.IDynamicGraph#updateGraph(double, org.simulator.math.odes.MultiTable)
	 */
	@Override
	public void updateGraph(double timePoint, MultiTable updateThem) {
		timepoint.setText("Timepoint: " +timePoint); 
		String dataString = "";
		for(int i = 1; i <= updateThem.getColumnCount(); i++) {
			dataString += updateThem.getColumnIdentifier(i) + ": " + updateThem.getValueAt(0, i) + " || " ; //there's just one row (timepoint to be updated)
		}
		dataStrings.setText(dataString);
		searchBar.setValue(core.getIndexOfTimepoint(timePoint));
	}

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IDynamicGraph#donePlay()
     */
    @Override
    public void donePlay() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see org.sbml.simulator.gui.graph.IDynamicGraph#takeGraphshot(int, int)
     */
    @Override
    public BufferedImage takeGraphshot(int width, int height) {
        // TODO Auto-generated method stub
        return null;
    }

}
