/*
 * $Id:  CSVDataReader.java 16:16:00 Meike Aichele$
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
package org.sbml.simulator.fba.controller;

import java.awt.Component;
import java.io.File;
import java.util.ArrayList;

import javax.swing.SwingWorker;

import org.sbml.jsbml.Model;

import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.csv.CSVReader;

/**
 * @author Meike Aichele
 * @version $Rev$
 * @date 04.06.2012
 * @since 1.0
 */
public class CSVDataReader extends SwingWorker<String[][], Void>{

	private File file;
	private String[][] stringData;
	
	public CSVDataReader(File file) {
		this.file = file;
	}

	/*
	 * (non-Javadoc)
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected String[][] doInBackground() throws Exception {
		Component parent;
		CSVImporterV2 converter = new CSVImporterV2(file.getAbsolutePath(), new ArrayList<ExpectedColumn>(2));
		if ((parent == null) || CSVImporterV2.showDialog(parent, converter)) {
		      CSVReader reader = converter.getCSVReader();
		      stringData = reader.getData();
		      return stringData;
		}
		return null;
	}
	
	/* 
	 * (non-Javadoc)
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected void done() {
		firePropertyChange("done", null, stringData);
	}
}
