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
package org.sbml.simulator.io;

import java.awt.Component;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.UniqueNamedSBase;
import org.simulator.math.odes.MultiTable;

import de.zbit.gui.csv.CSVImporterV2;
import de.zbit.gui.csv.ExpectedColumn;
import de.zbit.io.csv.CSVReader;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;

/**
 * This class facilitates reading comma-separated files and translates the
 * result into a {@link MultiTable} for further processing.
 * 
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
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");
	
	/**
	 * 
	 */
	public CSVDataImporter() {
	  super();
	}
	
	/**
	 * 
	 * @param pathname
	 * @return
	 * @throws IOException
	 */
	public MultiTable convert(String pathname) throws IOException {
		return convert(null, pathname);
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
	  List<ExpectedColumn> cols;
	  String expectedHeader[];
	  
		if (model != null) {
			expectedHeader = expectedTableHead(model); // According to the model: which symbols
			cols = new ArrayList<ExpectedColumn>(expectedHeader.length + 1);
			for (String head : expectedHeader) {
				cols.add(new ExpectedColumn(head, false));
			}
		} else {
			expectedHeader = new String[0];
			cols = new ArrayList<ExpectedColumn>(1);
		}
		cols.add(new ExpectedColumn(data.getTimeName(), true));
    
    CSVImporterV2 converter = new CSVImporterV2(pathname, cols);
    
    int i, j, timeColumn;
    if ((parent == null) || CSVImporterV2.showDialog(parent, converter)) {
      CSVReader reader = converter.getApprovedCSVReader();
      String stringData[][] = reader.getData();
      data.setTimeName(reader.getHeader()[0]);
      timeColumn = reader.getColumn(data.getTimeName());
      if (timeColumn > -1) {
        double timePoints[] = new double[stringData.length];
        for (i = 0; i < stringData.length; i++) {
          timePoints[i] = Double.parseDouble(stringData[i][timeColumn]);
        }
        data.setTimePoints(timePoints);
        // exclude time column
        
        if ((model != null) && (reader.getHeader().length > expectedHeader.length) && (parent != null)) {
          JOptionPane.showMessageDialog(parent, StringUtil.toHTML(bundle
              .getString("ADDITIONAL_COLUMNS_ARE_IGNORED_TOOLTIP"), 40), bundle
              .getString("ADDITIONAL_COLUMNS_ARE_IGNORED"),
            JOptionPane.INFORMATION_MESSAGE);
        }
        Map<String, Integer> nameToColumn = new HashMap<String, Integer>();
        
        List<String> newHeadList = new ArrayList<String>();
        i = 0;
        for (ExpectedColumn col: cols) {
          String colName = ((String)col.getName());
          if (!(colName.equalsIgnoreCase(data.getTimeName()))) {
        	  if((parent != null) && (col.getAssignedColumns().size() > 0)) {
        		  newHeadList.add(colName);
        		  nameToColumn.put(colName, col.getAssignedColumns().get(0));
        	  }
        	  else if(parent == null) {
        		 int colNumber = reader.getColumnSensitive(colName);
        		 if(colNumber != -1) {
        			 nameToColumn.put(colName, colNumber);
        			 newHeadList.add(colName);
        		 }
        		  
        	  }
          }
        }
        
        
        String newHead[] = newHeadList.toArray(new String[newHeadList.size()]);
        data.addBlock(newHead); // alphabetically sorted
        
        double dataBlock[][] = data.getBlock(0).getData();
      	
        for (i = 0; i < dataBlock.length; i++) {
          j = 0; // timeCorrection(j, timeColumn)
          for (String head : newHead) {
            String s = stringData[i][nameToColumn.get(head)];
            if ((s != null) && (s.length() > 0)) {
            	if (s.equalsIgnoreCase("INF")) {
            		dataBlock[i][j] = Double.POSITIVE_INFINITY;
            	} else if (s.equalsIgnoreCase("-INF")) {
            		dataBlock[i][j] = Double.NEGATIVE_INFINITY;
            	} else if (s.equalsIgnoreCase("NAN")) {
            		dataBlock[i][j] = Double.NaN;
            	} else {
            		dataBlock[i][j] = Double.parseDouble(s);
            	}
            }
            j++;
          }
        }
        
				if (model != null) {
					String colNames[] = new String[newHead.length];
					UniqueNamedSBase sbase;
					j = 0;
					for (String head : newHead) {
						sbase = model.findUniqueNamedSBase(head);
						colNames[j++] = (sbase != null) && (sbase.isSetName()) ? sbase
								.getName() : null;
					}
					data.getBlock(0).setColumnNames(colNames);
				}
        data.setTimeName(bundle.getString("TIME"));
        
        return data;
        
      } else {
        if (parent != null) {
          JOptionPane.showMessageDialog(parent, StringUtil.toHTML(bundle
              .getString("FILE_NOT_CORRECTLY_FORMATTED_TOOLTIP")), bundle
              .getString("FILE_NOT_CORRECTLY_FORMATTED"),
            JOptionPane.WARNING_MESSAGE);
        } else {
          logger.fine(bundle.getString("FILE_NOT_CORRECTLY_FORMATTED_TOOLTIP"));
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
	private String[] expectedTableHead(Model model) {
    return expectedTableHead(model, null);
  }
	
	/**
	 * @param model
	 * @param timeName
	 * @return
	 */
	private String[] expectedTableHead(Model model, String timeName) {
		List<String> modelSymbols = gatherSymbolIds(model);
		if (timeName != null) {
		  String head[] = new String[modelSymbols.size() + 1];
      head[0] = timeName;
      for (int i = 1; i < head.length; i++) {
        head[i] = modelSymbols.get(i - 1);
      }
      return head;
    }
    return modelSymbols.toArray(new String[0]);
	}
	
	/**
	 * @param model
	 * @return
	 */
	private List<String> gatherSymbolIds(final Model model) {
		return new AbstractList<String>() {

			/* (non-Javadoc)
			 * @see java.util.AbstractList#get(int)
			 */
			public String get(int index) {
				if (index < model.getCompartmentCount()) {
					return model.getCompartment(index).getId();
				}
				index -= model.getCompartmentCount();
				if (index < model.getSpeciesCount()) {
					return model.getSpecies(index).getId();
				}
				index -= model.getSpeciesCount();
				if (index < model.getParameterCount()) {
					return model.getParameter(index).getId();
				}
				
				index -= model.getParameterCount();
				return model.getReaction(index).getId();
			}

			/* (non-Javadoc)
			 * @see java.util.AbstractCollection#size()
			 */
			public int size() {
				return model.getCompartmentCount() + model.getSpeciesCount()
						+ model.getParameterCount() + model.getReactionCount();
			}
		};
	}
	
}
