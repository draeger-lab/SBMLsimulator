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
package org.sbml.simulator.math;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.ode.DerivativeException;
import org.junit.Assert;
import org.junit.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.validator.ModelOverdeterminedException;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.SBMLsimulator;
import org.sbml.simulator.io.CSVDataImporter;
import org.simulator.math.EuclideanDistance;
import org.simulator.math.QualityMeasure;
import org.simulator.math.RelativeEuclideanDistance;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.RosenbrockSolver;
import org.simulator.sbml.SBMLinterpreter;

/**
 * @author Andreas Dr&auml;ger
 * @version $Rev$
 * @since 1.0
 */
public class SimulationTestAutomatic {
	private static final Logger logger = Logger.getLogger(SimulationTestAutomatic.class.getName());
	static {
//		try {
//			System.loadLibrary("sbmlj");
//			// Extra check to be sure we have access to libSBML:
//			Class.forName("org.sbml.libsbml.libsbml");
//		} catch (Exception e) {
//			System.err.println("Error: could not load the libSBML library");
//			e.printStackTrace();
//			System.exit(1);
//		}
	}

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		boolean onlyRosenbrock=true;
		boolean testBiomodels=false;
		if((args.length>=2)&&(args[1].equals("all"))) {
			onlyRosenbrock=false;
		}
		if((args.length>=2)&&(args[1].equals("biomodels"))) {
			onlyRosenbrock=false;
			testBiomodels=true;
		}
		
		if(onlyRosenbrock) {
			testRosenbrockSolver(args[0]);
		}
		else if(testBiomodels) {
			testBiomodels(args[0]);
		}
		else {
			statisticForSolvers(args[0]);
		}
	}	
		
	private static void statisticForSolvers(String file)
		throws FileNotFoundException, IOException {
		String sbmlfile, csvfile, configfile;
		
		//initialize solvers
		List<AbstractDESSolver> solvers = new LinkedList<AbstractDESSolver>();
		for (Class<AbstractDESSolver> solverClass : SBMLsimulator
				.getAvailableSolvers()) {
			try {
				// instantiate solver
				AbstractDESSolver solver = solverClass.newInstance();
				if (solver != null) {
					solvers.add(solver);
				}
			} catch (Exception e) {
				
			}
		}
		
		int[] highDistances = new int[solvers.size()];
		int[] errors = new int[solvers.size()];
		int[] correctSimulations = new int[solvers.size()];
		int nModels = 0;
		for (int i = 0; i != solvers.size(); i++) {
			highDistances[i] = 0;
			errors[i] = 0;
			correctSimulations[i] = 0;
		}
		
		for (int modelnr = 1; modelnr <= 980; modelnr++) {
			System.out.println("model " + modelnr);
			nModels++;
			
			StringBuilder modelFile = new StringBuilder();
			modelFile.append(modelnr);
			while (modelFile.length() < 5)
				modelFile.insert(0, '0');
			String path = modelFile.toString();
			modelFile.append('/');
			modelFile.append(path);
			modelFile.insert(0, file);
			path = modelFile.toString();
			csvfile = path + "-results.csv";
			configfile = path + "-settings.txt";
			
			Properties props = new Properties();
			props.load(new BufferedReader(new FileReader(configfile)));
			// int start = Integer.valueOf(props.getProperty("start"));
			double duration = Double.valueOf(props.getProperty("duration"));
			double steps = Double.valueOf(props.getProperty("steps"));
			Map <String,Boolean> amountHash = new HashMap<String,Boolean>();
			String[] amounts = String.valueOf(props.getProperty("amount")).trim().split(",");
			String[] concentrations = String.valueOf(props.getProperty("concentration")).split(",");
			// double absolute = Double.valueOf(props.getProperty("absolute"));
			// double relative = Double.valueOf(props.getProperty("relative"));
			
			for(String s: amounts) {
				s=s.trim();
				if(!s.equals("")) {
					amountHash.put(s, true);
				}
			}
			
			for(String s: concentrations) {
				s=s.trim();
				if(!s.equals("")) {
					amountHash.put(s, false);
				}
			}
			// String[] sbmlFileTypes = { "-sbml-l1v2.xml", "-sbml-l2v1.xml",
			// "-sbml-l2v2.xml", "-sbml-l2v3.xml", "-sbml-l2v4.xml",
			// "-sbml-l3v1.xml" };
			
			String[] sbmlFileTypes = {"-sbml-l1v2.xml", "-sbml-l2v1.xml", "-sbml-l2v2.xml",
					"-sbml-l2v3.xml", "-sbml-l2v4.xml", "-sbml-l3v1.xml" };
			
			boolean[] highDistance = new boolean[solvers.size()];
			boolean[] errorInSimulation = new boolean[solvers.size()];
			for (int i = 0; i != solvers.size(); i++) {
				highDistance[i] = false;
				errorInSimulation[i] = false;
			}
			
			for (String sbmlFileType : sbmlFileTypes) {
				sbmlfile = path + sbmlFileType;
				Model model = null;
				try {
					model = (new SBMLReader()).readSBML(sbmlfile).getModel();
				} catch (Exception e) {
				}
				if (model != null) {
					// get timepoints
					CSVDataImporter csvimporter = new CSVDataImporter();
					MultiTable inputData = csvimporter.convert(model, csvfile);
					double[] timepoints = inputData.getTimePoints();
					duration = timepoints[timepoints.length - 1] - timepoints[0];
					for (int i = 0; i != solvers.size(); i++) {
						AbstractDESSolver solver = solvers.get(i);
						solver.reset();
						try {
							double dist = testModel(solver, model, inputData, duration
									/ steps, amountHash);
							if (dist > 0.1) {
								logger.log(Level.INFO, sbmlFileType + ": "
										+ "relative distance for model-" + modelnr
										+ " with solver " + solver.getName());
								logger.log(Level.INFO, String.valueOf(dist));
								highDistance[i] = true;
							} else if (Double.isNaN(dist)) {
								errorInSimulation[i] = true;
							}
						} catch (DerivativeException e) {
							logger.warning("Exception in model " + modelnr);
							errorInSimulation[i] = true;
						} catch (ModelOverdeterminedException e) {
							logger.warning("OverdeterminationException in model " + modelnr);
							errorInSimulation[i] = true;
						}
					}
					
				}
			}
			for (int i = 0; i != solvers.size(); i++) {
				if (highDistance[i]) {
					highDistances[i]++;
				}
				if (errorInSimulation[i]) {
					errors[i]++;
				}
				if ((!highDistance[i]) && (!errorInSimulation[i])) {
					correctSimulations[i]++;
				}
			}
		}
		for (int i = 0; i != solvers.size(); i++) {
			System.out.println(solvers.get(i).getName());
			System.out.println("Models: " + nModels);
			System.out.println("Models with too high distance to experimental data: "
					+ highDistances[i]);
			System.out.println("Models with errors in simulation: " + errors[i]);
			System.out.println("Models with correct simulation: "
					+ correctSimulations[i]);
			System.out.println();
		}
		
	}
	
	/**
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void testRosenbrockSolver(String file)
			throws FileNotFoundException, IOException {
		String sbmlfile, csvfile, configfile;
		int highDistances=0;
		int errors=0;
		int nModels=0;
		int correctSimulations=0;
		AbstractDESSolver solver = new RosenbrockSolver();
		for (int modelnr = 1; modelnr <= 1123; modelnr++) {
			System.out.println("model " + modelnr);

			StringBuilder modelFile = new StringBuilder();
			modelFile.append(modelnr);
			while (modelFile.length() < 5)
				modelFile.insert(0, '0');
			String path = modelFile.toString();
			modelFile.append('/');
			modelFile.append(path);
			modelFile.insert(0, file);
			path = modelFile.toString();
			csvfile = path + "-results.csv";
			configfile = path + "-settings.txt";

			Properties props = new Properties();
			props.load(new BufferedReader(new FileReader(configfile)));
			// int start = Integer.valueOf(props.getProperty("start"));
			double duration = Double.valueOf(props.getProperty("duration"));
			double steps = Double.valueOf(props.getProperty("steps"));
			Map <String,Boolean> amountHash = new HashMap<String,Boolean>();
			String[] amounts = String.valueOf(props.getProperty("amount")).trim().split(",");
			String[] concentrations = String.valueOf(props.getProperty("concentration")).split(",");
			// double absolute = Double.valueOf(props.getProperty("absolute"));
			// double relative = Double.valueOf(props.getProperty("relative"));
			
			for(String s: amounts) {
				s=s.trim();
				if(!s.equals("")) {
					amountHash.put(s, true);
				}
			}
			
			for(String s: concentrations) {
				s=s.trim();
				if(!s.equals("")) {
					amountHash.put(s, false);
				}
			}
			
			
			String[] sbmlFileTypes = {"-sbml-l1v2.xml", "-sbml-l2v1.xml", "-sbml-l2v2.xml",
					"-sbml-l2v3.xml", "-sbml-l2v4.xml", "-sbml-l3v1.xml" };
			boolean highDistance=false, errorInSimulation=false;
			for (String sbmlFileType : sbmlFileTypes) {
				sbmlfile = path + sbmlFileType;
				Model model = null;
				try {
					model = (new SBMLReader()).readSBML(sbmlfile).getModel();
				} catch (Exception e) {
				}
				if (model != null) {
					// get timepoints
					CSVDataImporter csvimporter = new CSVDataImporter();
					MultiTable inputData = csvimporter.convert(model, csvfile);
					double[] timepoints = inputData.getTimePoints();
					duration = timepoints[timepoints.length - 1]
							- timepoints[0];
					solver.reset();
					try {
						double dist=testModel(solver, model, inputData, duration / steps, amountHash);
						if (dist > 0.1) {
							logger.log(Level.INFO, sbmlFileType + ": "
							+ "relative distance for model-" + modelnr
							+ " with solver " + solver.getName());
							logger.log(Level.INFO, String.valueOf(dist));
							highDistance=true;
						}
						else if(Double.isNaN(dist)) {
							errorInSimulation=true;
						}
					}
					catch(DerivativeException e) {
						logger.warning("Exception in model " + modelnr);
						errorInSimulation=true;
					}
					catch(ModelOverdeterminedException e) {
						logger.warning("OverdeterminationException in model " + modelnr);
						errorInSimulation=true;
					}
				}
			}
			nModels++;
			if(highDistance) {
				highDistances++;
			}
			if(errorInSimulation) {
				errors++;
			}
			if((!highDistance) && (!errorInSimulation)) {
				correctSimulations++;
			}
		}
		System.out.println("Models: "+nModels);
		System.out.println("Models with too high distance to experimental data: "+highDistances);
		System.out.println("Models with errors in simulation: "+errors);
		System.out.println("Models with correct simulation: "+correctSimulations);
	}
	
	/**
	 * 
	 * @param file
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private static void testBiomodels(String file) throws FileNotFoundException,
		IOException {
		int errors = 0;
		int nModels = 0;
		AbstractDESSolver solver = new RosenbrockSolver();
		
		for (int modelnr = 408; modelnr <= 423; modelnr++) {
			System.out.println("Biomodel " + modelnr);
			Model model = null;
			try {
				String modelFile="";
				if(modelnr<10) {
					modelFile = file + "BIOMD000000000" + modelnr + ".xml";
				}
				else if(modelnr<100) {
					modelFile = file + "BIOMD00000000" + modelnr + ".xml";
				}
				else {
					modelFile = file + "BIOMD0000000" + modelnr + ".xml";
				}
				model = (new SBMLReader()).readSBML(modelFile).getModel();
			} catch (Exception e) {
				model = null;
				logger.warning("Exception while reading Biomodel " + modelnr);
				errors++;
			}
			if (model != null) {
				solver.reset();
				try {
					SBMLinterpreter interpreter = new SBMLinterpreter(model);
					
					if ((solver != null) && (interpreter != null)) {
						solver.setStepSize(0.1);
						
						// solve
						solver.solve(interpreter, interpreter.getInitialValues(), 0, 10);
						
						if (solver.isUnstable()) {
							logger.warning("unstable!");
							errors++;
						}
					}
				} catch (DerivativeException e) {
					logger.warning("Exception in Biomodel " + modelnr);
					errors++;
				} catch (ModelOverdeterminedException e) {
					logger.warning("OverdeterminationException in Biomodel " + modelnr);
					errors++;
				}
			}
			nModels++;
		}
		System.out.println("Models: " + nModels);
		System.out.println("Models with errors in simulation: " + errors);
		System.out.println("Models with correct simulation: " + (nModels - errors));
	}
	
	/**
	 * 
	 * @param model
	 * @param inputData
	 * @param timepoints
	 * @param sbmlFileType
	 * @param modelnr
	 * @param stepSize
	 * @throws ModelOverdeterminedException 
	 * @throws SBMLException 
	 * @throws DerivativeException 
	 */
	private static double testModel(AbstractDESSolver solver, Model model,
			MultiTable inputData, double stepSize, Map<String,Boolean> amountHash) throws SBMLException,
			ModelOverdeterminedException, DerivativeException {
		// initialize interpreter
		SBMLinterpreter interpreter = new SBMLinterpreter(model, 0, 0, 1, amountHash);

		if ((solver != null) && (interpreter != null)) {
			solver.setStepSize(stepSize);

			// solve
			MultiTable solution = solver.solve(interpreter,
					interpreter.getInitialValues(), inputData.getTimePoints());

			// compute distance
			QualityMeasure distance = new RelativeEuclideanDistance();
			double dist = distance.distance(solution, inputData);
			if (solver.isUnstable()) {
				logger.warning("unstable!");
				return Double.NaN;
			}
			return dist;
		} else {
			return Double.NaN;
		}

	}
	
	
	/**
	 * TEST_CASES must be set to the address of the folder "semantic" in the SBML test suite.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	@Test
	public void testModels() throws FileNotFoundException, IOException {
		String file=System.getenv("TEST_CASES");
		String sbmlfile, csvfile, configfile;
		for (int modelnr = 1; modelnr <= 980; modelnr++) {
		  System.out.println("model " + modelnr);

			StringBuilder modelFile = new StringBuilder();
			modelFile.append(modelnr);
			while (modelFile.length() < 5)
				modelFile.insert(0, '0');
			String path = modelFile.toString();
			modelFile.append('/');
			modelFile.append(path);
			modelFile.insert(0, file);
			path = modelFile.toString();
			
			csvfile = path + "-results.csv";
			configfile = path + "-settings.txt";

			Properties props = new Properties();
			props.load(new BufferedReader(new FileReader(configfile)));
//			int start = Integer.valueOf(props.getProperty("start"));
			double duration = Double.valueOf(props.getProperty("duration"));
			double steps = Double.valueOf(props.getProperty("steps"));
//			double absolute = Double.valueOf(props.getProperty("absolute"));
//			double relative = Double.valueOf(props.getProperty("relative"));
			/*
			 * Other variables:
			 * variables: S1, S2
			 * amount:
			 * concentration:
			 */
			
			String[] sbmlFileTypes = {"-sbml-l1v2.xml","-sbml-l2v1.xml","-sbml-l2v2.xml","-sbml-l2v3.xml","-sbml-l2v4.xml","-sbml-l3v1.xml"};
				
			for (String sbmlFileType : sbmlFileTypes) {
				sbmlfile = path + sbmlFileType;
				File sbmlFile = new File(sbmlfile);

				if ((sbmlFile != null) && (sbmlFile.exists())) {
					// read model
					Model model = null;
					boolean errorInModelReading = false;
					try {
						model = (new SBMLReader()).readSBML(sbmlFile)
								.getModel();
					} catch (Exception e) {
						errorInModelReading = true;
					}
					Assert.assertNotNull(model);
					Assert.assertFalse(errorInModelReading);

					AbstractDESSolver solver = new RosenbrockSolver();
					// initialize interpreter
					SBMLinterpreter interpreter = null;
					boolean exceptionInInterpreter = false;
					try {
						interpreter = new SBMLinterpreter(model);
					} catch (SBMLException e) {
						exceptionInInterpreter = true;
					} catch (ModelOverdeterminedException e) {
						exceptionInInterpreter = true;
					}
					Assert.assertNotNull(interpreter);
					Assert.assertFalse(exceptionInInterpreter);

					// get timepoints
					CSVDataImporter csvimporter = new CSVDataImporter();
					MultiTable inputData = csvimporter.convert(model, csvfile);
					double[] timepoints = inputData.getTimePoints();
					duration = timepoints[timepoints.length - 1]
							- timepoints[0];

					if ((solver != null) && (interpreter != null)) {
						System.out.println(sbmlFileType + " "
								+ solver.getName());
						solver.setStepSize(duration / steps);

						// solve
						MultiTable solution = null;
						boolean errorInSolve = false;
						try {
							solution = solver.solve(interpreter,
									interpreter.getInitialValues(), timepoints);
						} catch (DerivativeException e) {
							errorInSolve = true;
						}
						Assert.assertNotNull(solution);
						Assert.assertFalse(errorInSolve);
						Assert.assertFalse(solver.isUnstable());

						// compute distance
						QualityMeasure distance = new EuclideanDistance();
						double dist = distance.distance(solution, inputData);
						Assert.assertTrue(dist <= 0.2);

					}
				}

			}
		}
	}
	
}
