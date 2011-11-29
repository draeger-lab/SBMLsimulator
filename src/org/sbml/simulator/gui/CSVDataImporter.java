/*
 * $Id$ $URL:
 * svn://rarepos
 * /SBMLsimulator/trunk/src/org/sbml/simulator/gui/CSVDataImporter.java $
 * --------------------------------------------------------------------- This
 * file is part of SBMLsimulator, a Java-based simulator for models of
 * biochemical processes encoded in the modeling language SBML.
 * 
 * Copyright (C) 2007-2011 by the University of Tuebingen, Germany.
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation. A copy of the license agreement is provided in the file
 * named "LICENSE.txt" included with this software distribution and also
 * available online as <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui;

import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.sbml.jsbml.Model;
import org.simulator.math.odes.MultiTable;

import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.CSVReader;
import de.zbit.util.StringUtil;

/**
 * @author Andreas Dr&auml;ger
 * @author Roland Keller
 * @date 2010-09-03
 * @version $Rev$
 * @since 1.0
 */
public class CSVDataImporter {
	
  /**
   * A {@link java.util.logging.Logger} for this class.
   */
  private static final transient Logger logger = Logger.getLogger(CSVDataImporter.class.getName());
  
	/**
	 * 
	 */
	private static final String FILE_NOT_CORRECTLY_FORMATTED = "Cannot read this format, because no column identifiers are provided.";
	
	/**
	 * 
	 */
	private static final String ADDITIONAL_COLUMNS_ARE_IGNORED = "The data file contains some elements that do not have a counterpart in the given model. These elements will not occur in the plot and are ignored in the analysis.";
	
	/**
	 * 
	 */
	public CSVDataImporter() {
	  super();
	}
	
	/**
	 * @param model
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public MultiTable convert(Model model, String pathname)
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
	public MultiTable convert(Model model, String pathname, Component parent)
		throws IOException {
	  MultiTable data = new MultiTable();
    String expectedHeader[] = getExpectedTableHead(model); // According to the model: which symbols

    List<ExpectedColumn> cols = new ArrayList<ExpectedColumn>(
      expectedHeader.length + 1);
    for (String head : expectedHeader) {
      cols.add(new ExpectedColumn(head, false));
    }
    cols.add(new ExpectedColumn(data.getTimeName(), true));
    
    CSVImporterV2 converter = new CSVImporterV2(pathname, cols);
    
    int i, j, timeColumn;
    if ((parent == null) || CSVImporterV2.showDialog(parent, converter)) {
      CSVReader reader = converter.getCSVReader();
      String stringData[][] = reader.getData();
      timeColumn = reader.getColumn(data.getTimeName());
      if (timeColumn > -1) {
        double timePoints[] = new double[stringData.length];
        for (i = 0; i < stringData.length; i++) {
          timePoints[i] = Double.parseDouble(stringData[i][timeColumn]);
        }
        data.setTimePoints(timePoints);
        // exclude time column
        
        String newHead[] = new String[(int) Math.max(0,
          reader.getHeader().length - 1)];
        
        if (newHead.length > expectedHeader.length) {
          String message = ADDITIONAL_COLUMNS_ARE_IGNORED;
          JOptionPane.showMessageDialog(parent, StringUtil.toHTML(message, 40),
            "Some elements are ignored", JOptionPane.INFORMATION_MESSAGE);
        }
        
        i = 0;
        for (String head : reader.getHeader()) {
          if (!head.equalsIgnoreCase(data.getTimeName())) {
            newHead[i++] = head;
          }
        }
        data.addBlock(newHead); // alphabetically sorted
        Map<String, Integer> nameToColumn = new HashMap<String, Integer>();
        for (i = 0; i < newHead.length; i++) {
          nameToColumn.put(newHead[i], reader.getColumnSensitive(newHead[i]));
        }
        double dataBlock[][] = data.getBlock(0).getData();
        for (i = 0; i < dataBlock.length; i++) {
          j = 0; // timeCorrection(j, timeColumn)
          for (String head : newHead) {
            String s = stringData[i][nameToColumn.get(head)];
            if (s.equalsIgnoreCase("INF")) {
              dataBlock[i][j] = Double.POSITIVE_INFINITY;
            } else if (s.equalsIgnoreCase("-INF")) {
              dataBlock[i][j] = Double.NEGATIVE_INFINITY;
            } else if (s.equalsIgnoreCase("NAN")) {
              dataBlock[i][j] = Double.NaN;
            } else {
              dataBlock[i][j] = Double.parseDouble(s);
            }
            j++;
          }
        }
        return data;
      } else {
        if (parent != null) {
          JOptionPane.showMessageDialog(parent, StringUtil
              .toHTML(FILE_NOT_CORRECTLY_FORMATTED), "Unreadable file",
            JOptionPane.WARNING_MESSAGE);
        } else {
          logger.fine(FILE_NOT_CORRECTLY_FORMATTED);
        }
      }
    }
    return null;
	}
	
	/**
	 * 
	 * @param model
	 * @return
	 */
	private String[] getExpectedTableHead(Model model) {
    return getExpectedTableHead(model, null);
  }
	
	/**
	 * @param model
	 * @param timeName
	 * @return
	 */
	private String[] getExpectedTableHead(Model model, String timeName) {
		List<String> modelSymbols = gatherSymbolIds(model);
		if (timeName != null) {
		  String head[] = new String[modelSymbols.size() + 1];
      head[0] = timeName;
      for (int i = 1; i < head.length; i++) {
        head[i] = modelSymbols.get(i - 1);
      }
      return head;
    }
    return modelSymbols.toArray(new String[] {});
	}
	
	/**
	 * @param model
	 * @return
	 */
	private List<String> gatherSymbolIds(Model model) {
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
	
}
