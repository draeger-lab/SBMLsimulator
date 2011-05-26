/*
 * $Id:  AddAnnotations.java 11:26:57 keller$
 * $URL: AddAnnotations.java $
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

package org.sbml.simulator.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;

import de.zbit.io.CSVReader;
import de.zbit.kegg.KeggInfoManagement;
import de.zbit.util.InfoManagement;
import de.zbit.util.ProgressBar;

/**
 * @author Roland Keller
 * @version $Rev$
 * @since
 */
public class AddAnnotations {
	/**
	 * @param args
	 * @throws XMLStreamException
	 * @throws FileNotFoundException
	 * @throws SBMLException
	 */
	public static void main(String[] args) throws FileNotFoundException,
		XMLStreamException, SBMLException {
		SBMLDocument doc = (new SBMLReader()).readSBML(args[0]);
		
		CSVReader reader = new CSVReader(args[2],false);
		
		Map<String,String> annotations = new HashMap<String,String>();
		String[] line=null;
		try {
			line = reader.getNextLine();
			
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		while(line != null) {
			try {
				if(line.length>1) {
					annotations.put(line[0], line[1]);
				}
				line = reader.getNextLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
		KeggInfoManagement manager=null;
		try {
			manager = 
				(KeggInfoManagement) KeggInfoManagement.loadFromFilesystem("kgim.dat");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (manager==null) manager = new KeggInfoManagement();
		
		int total = 0, matched = 0, notMatched = 0;
		ProgressBar prog = new ProgressBar(doc.getModel().getListOfSpecies().size());
		
		for (Species spec : doc.getModel().getListOfSpecies()) {
			total++;
			prog.DisplayBar();
			String symbol = spec.getName();
			
			if(spec.getAnnotation().isSetAnnotation()) { 
				matched++;
				continue;
			}
			
			String annotation = annotations.get(symbol);
			if(annotation!=null && !annotation.equalsIgnoreCase("")) {
				if(annotation.startsWith("P")) {
					if(!spec.isSetMetaId()) {
						spec.setMetaId("meta_" + spec.getId());
					}
					spec.addCVTerm(new CVTerm(CVTerm.Type.BIOLOGICAL_QUALIFIER,
						CVTerm.Qualifier.BQB_IS, "urn:miriam:uniprot:" + annotation));
				}
				else {
					Annotate.annotate(spec, annotation, manager);
				}
				matched++;
			}
			else {
				notMatched++;
				System.out.println(symbol);
			}
			
		}
		InfoManagement.saveToFilesystem("kgim.dat", manager);
		
		// report
		System.out.println("Annotation results:");
		System.out.println("Total: " + total + " Matched: " + matched
				+ " Unmatched: " + notMatched);
		
		SBMLWriter w = new SBMLWriter();
		w.write(doc, args[1]);
		
	}
}
