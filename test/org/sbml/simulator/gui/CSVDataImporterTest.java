/*
 * $Id:  CSVDataImporterTest.java 18:15:31 draeger$
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

import java.io.IOException;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableModel;
import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.io.CSVDataImporter;

import de.zbit.gui.GUITools;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class CSVDataImporterTest {

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

}
