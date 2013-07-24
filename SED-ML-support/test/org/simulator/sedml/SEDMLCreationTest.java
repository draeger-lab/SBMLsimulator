/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2013 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.simulator.sedml;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math.ode.DerivativeException;
import org.jdom.Element;
import org.jlibsedml.Algorithm;
import org.jlibsedml.ArchiveComponents;
import org.jlibsedml.Curve;
import org.jlibsedml.DataGenerator;
import org.jlibsedml.FileModelContent;
import org.jlibsedml.IModelContent;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Notes;
import org.jlibsedml.Plot2D;
import org.jlibsedml.SEDMLDocument;
import org.jlibsedml.SedML;
import org.jlibsedml.Task;
import org.jlibsedml.UniformTimeCourse;
import org.jlibsedml.Variable;
import org.jlibsedml.VariableSymbol;
import org.jlibsedml.XMLException;
import org.jlibsedml.modelsupport.SBMLSupport;
import org.jlibsedml.modelsupport.SUPPORTED_LANGUAGE;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DormandPrince54Solver;
import org.simulator.sbml.SBMLinterpreter;

/**
 * Test cases here show how to create SEDML from a JSBML model object 
 *  and a solver object. These SEDML files describe a simulation that will produce
 *   output for all species in a model.
 * @author radams
 * @version $Rev$
 */
public class SEDMLCreationTest {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	//demonstrates how to create a SEDML file, describing an output to plot all species from a simulation.
	//SED_ML_CREATION_TEST must be set to the address of the file to use for testing the sed-ml creation.
	@Test
	public final void testSEDMLCreate() throws IOException, XMLStreamException, SBMLException, ModelOverdeterminedException, DerivativeException, XMLException {
		File abc1test = new File(System.getenv("SED_ML_CREATION_TEST"));
		// Read the model and initialize solver
		double start=0;
		double end=10;
		double stepsize=0.1;
	    Model model = (new SBMLReader()).readSBML(abc1test).getModel();
	    AbstractDESSolver solver = new DormandPrince54Solver();
	    solver.setStepSize(0.1);
	    SBMLinterpreter interpreter = new SBMLinterpreter(model);
	
	    if (solver instanceof AbstractDESSolver) {
	      ((AbstractDESSolver) solver).setIncludeIntermediates(false);
	    }
	    
	    SEDMLDocument doc = saveExperimentToSEDML(start,end,stepsize,solver,model);
	    System.err.println(doc.writeDocumentToString());
	    
	    //write the SED-ML file
	    doc.writeDocument(new File("GenSedML.xml"));
	    
	    // alternatively  we can generate an archive format - a self-contained binary archive which 
	    // can be executed in SBSI or in
	    // SEDML Web tools - http://sysbioapps.dyndns.org/SED-ML%20Web%20Tools.
	    // This is likely to become a 'Combine archive' in near future so API and format may alter.
	    
	    // replace absolute filepath with relative path for a SEDX archive
	    doc.getSedMLModel().getModels().get(0).setSource(abc1test.getName()); // use relative path for this.
	    FileModelContent fmc = new FileModelContent(abc1test);
	    ArchiveComponents ac = new ArchiveComponents(Arrays.asList(new IModelContent[]{fmc}), doc);
	    File sedxOut = new File("sedxout.sedx");
	    byte [] zipped = Libsedml.writeSEDMLArchive(ac, "sedxout");
	    FileUtils.writeByteArrayToFile(sedxOut,zipped);
	}

	// creates a SEDML file from SBMLSimulator config. This hardcodes the model file path for now, which
	// illustrates the process but is not very exchangeable. Alternatives are to use a URL or Miriam URN, 
	// or to generate a SED-ML archive.
	private SEDMLDocument saveExperimentToSEDML(double start, double end,
			double stepsize, AbstractDESSolver solver, Model model) {
		File abc1test = new File(System.getenv("SED_ML_CREATION_TEST"));
		SEDMLDocument doc = Libsedml.createDocument();
		SedML sedml = doc.getSedMLModel();
		
		// notes and annotations are as in SBML.
		addNote("An example SED-ML generated from SBML Simulator", sedml);
	
		// model details
		org.jlibsedml.Model m = new org.jlibsedml.Model("abc1", "Example model",SUPPORTED_LANGUAGE.SBML_GENERIC.getURN(),
				abc1test.getAbsolutePath());
		// time course info
		UniformTimeCourse utc = new UniformTimeCourse("sim", "sim", 0d, start, end, 
				(int)((end-start)/stepsize),new Algorithm("KISAO:0000087")); // hardcoded kisao ID for now
		// link time course to model
		Task t1 = new Task("t1","TASK",m.getId(),utc.getId());
		sedml.addModel(m);
		sedml.addSimulation(utc);
		sedml.addTask(t1);
		ListOf<Species> los =  model.getListOfSpecies();
		// create fields for model variables
		for (Species s:los) {
			DataGenerator dg = new DataGenerator(s.getId()+"dg", s.getId(), Libsedml.parseFormulaString(s.getId()));
			SBMLSupport support= new SBMLSupport();
			Variable v = new Variable(s.getId(), s.getId(), t1.getId(),support.getXPathForSpecies(s.getId()));
			dg.addVariable(v);
			sedml.addDataGenerator(dg);
		}
		// create time datagenerator
		DataGenerator time = new DataGenerator("timedg", "Time",  Libsedml.parseFormulaString("Time"));
		Variable timeVar = new Variable("Time", "Time",t1.getId(),VariableSymbol.TIME);
		time.addVariable(timeVar);
		sedml.addDataGenerator(time);
		
		// now create outputs - e.g., a series of curves of time vs species
		Plot2D plot2d = new Plot2D("plot","Basic plot");
		sedml.addOutput(plot2d);
		int indx=0;
		for (DataGenerator dg: sedml.getDataGenerators()) {
			// we don't want to plot time vs time. Equality checks are based on ID
			if(!dg.equals(time)) {
				Curve curve = new Curve("curve"+ indx++ +"", null,false,false,time.getId(),dg.getId());
				plot2d.addCurve(curve);
			}
			
		}
		return doc;
	}

	private void addNote(String text, SedML sedml) {
		Element el = new Element("p");
		el.setText(text);
		Notes n = new Notes(el);
		sedml.addNote(n);
		
	}

}
