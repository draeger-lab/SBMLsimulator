/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.Component;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.math.odes.MultiBlockTable;

import de.zbit.gui.CSVImporter;
import de.zbit.gui.GUITools;
import de.zbit.io.CSVReader;
import de.zbit.util.StringUtil;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-03
 * @version $Rev$
 * @since 1.0
 */
public class CSVDataImporter {

    /**
	 * 
	 */
    private static final String FILE_NOT_CORRECTLY_FORMATTED = "Cannot read this format, because no column identifiers are provided.";

    /**
	 * 
	 */
    private static final String ADDITIONAL_COLUMNS_ARE_IGNORED = "The data file contains some elements that do not have a counterpart in the given model. These elements will not occur in the plot and are ignored in the analysis.";

    /**
     * For testing only.
     * 
     * @param args
     *        path to an SBML file and path to a corresponding CSV file
     * @throws XMLStreamException
     * @throws IOException
     */
	public static void main(String[] args) throws XMLStreamException,
			IOException {
		SBMLReader reader = new SBMLReader();
		SBMLDocument doc = reader.readSBML(args[0]);
		CSVDataImporter importer = new CSVDataImporter();

		JFrame frame = new JFrame("Conveter test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception exc) {
			exc.printStackTrace();
			GUITools.showErrorMessage(frame, exc);
		}

		TableModel table = importer.convert(doc.getModel(), args[1], frame);

		if (table != null) {
			frame.getContentPane().add(
					new JScrollPane(new JTable(table),
							JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
							JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		} else {
			System.exit(0);
		}
	}

    /**
	 * 
	 */
    public CSVDataImporter() {
    }

    /**
     * @param model
     * @param pathname
     * @return
     * @throws IOException
     */
    public MultiBlockTable convert(Model model, String pathname)
	throws IOException {
	return convert(model, pathname, null);
    }

    /**
     * @param model
     * @param pathname
     * @param parent
     * @return
     * @throws IOException
     */
    public MultiBlockTable convert(Model model, String pathname,
	Component parent) throws IOException {

	MultiBlockTable data = new MultiBlockTable();
	String expectedHeader[] = getExpectedTableHead(model, data
		.getTimeName()); // According to the model: which symbols

	CSVImporter converter = new CSVImporter(null, false, pathname, false,
				expectedHeader);

	CSVReader reader = converter.getCSVReader();
	String stringData[][] = reader.getData();

	int i, j, timeColumn = converter.getColumnIndex(data.getTimeName());
	if (timeColumn >= 0) {
	    double timePoints[] = new double[stringData.length];
	    for (i = 0; i < stringData.length; i++) {
		timePoints[i] = Double.parseDouble(stringData[i][timeColumn]);
	    }
	    data.setTimePoints(timePoints);
	    // exclude time column
	    String newHead[] = new String[(int) Math.max(0, converter
		    .getNewHead().length - 1)];

	    if (newHead.length > expectedHeader.length) {
		String message = ADDITIONAL_COLUMNS_ARE_IGNORED;
		JOptionPane.showMessageDialog(parent, StringUtil.toHTML(
		    message, 40), "Some elements are ignored",
		    JOptionPane.INFORMATION_MESSAGE);
	    }

	    i = 0;
	    for (String head : converter.getNewHead()) {
		if (!head.equals(data.getTimeName())) {
		    newHead[i++] = head;
		}
	    }
	    data.addBlock(newHead); // alphabetically sorted
	    Map<String, Integer> nameToColumn = new HashMap<String, Integer>();
			for (i = 0; i < newHead.length; i++) {
				nameToColumn.put(newHead[i], converter
						.getColumnIndex(newHead[i]));
			}
	    double dataBlock[][] = data.getBlock(0).getData();
			for (i = 0; i < dataBlock.length; i++) {
				j = 0; // timeCorrection(j, timeColumn)
				for (String head : newHead) {
					dataBlock[i][j] = Double
							.parseDouble(stringData[i][nameToColumn.get(head)]);
					j++;
				}
			}
	    return data;
	} else if (!converter.isCanceled()) {
	    JOptionPane.showMessageDialog(parent, StringUtil
		    .toHTML(FILE_NOT_CORRECTLY_FORMATTED), "Unreadable file",
		JOptionPane.WARNING_MESSAGE);
	}
	return null;
    }
    
    
    /**
     * @param model
     * @param pathname
     * @return
     * @throws IOException
     */
    public MultiBlockTable convertWithoutWindows(Model model, String pathname) throws IOException {

	MultiBlockTable data = new MultiBlockTable();
	String expectedHeader[] = getExpectedTableHead(model, data
		.getTimeName()); // According to the model: which symbols

	CSVImporter converter = new CSVImporter(null, true, pathname, true, true,expectedHeader);

	CSVReader reader = converter.getCSVReader();
	String stringData[][] = reader.getData();

	int i, j, timeColumn = converter.getColumnIndex(data.getTimeName());
	if (timeColumn >= 0) {
	    double timePoints[] = new double[stringData.length];
	    for (i = 0; i < stringData.length; i++) {
		timePoints[i] = Double.parseDouble(stringData[i][timeColumn]);
	    }
	    data.setTimePoints(timePoints);
	    // exclude time column
	    String newHead[] = new String[(int) Math.max(0, converter
		    .getNewHead().length - 1)];

	    i = 0;
	    for (String head : converter.getNewHead()) {
		if (!head.equals(data.getTimeName())) {
		    newHead[i++] = head;
		}
	    }
	    data.addBlock(newHead); // alphabetically sorted
	    Map<String, Integer> nameToColumn = new HashMap<String, Integer>();
			for (i = 0; i < newHead.length; i++) {
				nameToColumn.put(newHead[i], converter
						.getColumnIndex(newHead[i]));
			}
	    double dataBlock[][] = data.getBlock(0).getData();
			for (i = 0; i < dataBlock.length; i++) {
				j = 0; // timeCorrection(j, timeColumn)
				for (String head : newHead) {
					dataBlock[i][j] = Double
							.parseDouble(stringData[i][nameToColumn.get(head)]);
					j++;
				}
			}
	    return data;
	} 
	return null;
    }

    /**
     * @param model
     * @param timeName
     * @return
     */
    private String[] getExpectedTableHead(Model model, String timeName) {
	List<String> modelSymbols = getSymbolIds(model);
	String head[] = new String[modelSymbols.size() + 1];
	head[0] = timeName;
	for (int i = 1; i < head.length; i++) {
	    head[i] = modelSymbols.get(i - 1);
	}
	return head;
    }

    /**
     * @param model
     * @return
     */
    private List<String> getSymbolIds(Model model) {
	List<String> head = new LinkedList<String>();
	int i;
	for (i = 0; i < model.getNumCompartments(); i++) {
	    head.add(model.getCompartment(i).getId());
	}
	for (i = 0; i < model.getNumSpecies(); i++) {
	    head.add(model.getSpecies(i).getId());
	}
	for (i = 0; i < model.getNumParameters(); i++) {
	    head.add(model.getParameter(i).getId());
	}
	return head;
    }

    /**
     * @param currColumn
     * @param timeColumn
     * @return
     */
    private int timeCorrection(int currColumn, int timeColumn) {
	if (currColumn < timeColumn) {
	    return currColumn;
	}
	if (currColumn > timeColumn) {
	    return currColumn - 1;
	}
	return -1;
    }

}
