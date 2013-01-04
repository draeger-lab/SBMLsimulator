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

import org.apache.commons.io.FileUtils;
import org.jlibsedml.Output;
import org.jlibsedml.SedML;
import org.jlibsedml.Simulation;
import org.jlibsedml.UniformTimeCourse;
import org.jlibsedml.execution.AbstractSedmlExecutor;
import org.jlibsedml.execution.ExecutionStatusElement;
import org.jlibsedml.execution.ExecutionStatusElement.ExecutionStatusType;
import org.jlibsedml.execution.IModel2DataMappings;
import org.jlibsedml.execution.IRawSedmlSimulationResults;
import org.jlibsedml.modelsupport.BioModelsModelsRetriever;
import org.jlibsedml.modelsupport.KisaoOntology;
import org.jlibsedml.modelsupport.KisaoTerm;
import org.jlibsedml.modelsupport.URLResourceRetriever;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.DormandPrince54Solver;
import org.simulator.math.odes.EulerMethod;
import org.simulator.math.odes.MultiTable;
import org.simulator.math.odes.MultiTable.Block.Column;
import org.simulator.math.odes.RosenbrockSolver;
import org.simulator.sbml.SBMLinterpreter;

/**
 * This class extends an abstract class from jlibsedml, which provides various support functions 
 *  such as retrieving models, applying changes to models, working out what tasks need to be executed to achieve 
 *  an Output, and post-processing of results.
 * @author radams
 *
 */
public class SedMLSBMLSimulatorExecutor extends AbstractSedmlExecutor {
  /*
   * A list of KISAO Ids corresponding to supported algorithm types in SBMLSimulator.
   *  These are used to determine if we are able to perform the simulation.
   */
  final static String [] SupportedIDs = new String []{"KISAO:0000033","KISAO:0000030","KISAO:0000087"};
	public SedMLSBMLSimulatorExecutor(SedML arg0, Output arg1) {
		super(arg0, arg1);
		// add extra model resolvers - only FileModelResolver is included by default.
		addModelResolver(new BioModelsModelsRetriever());
		addModelResolver(new URLResourceRetriever());
	
		
		// TODO Auto-generated constructor stub
	}

	/*
	 * test based on kisaoIDs that are available for solvers
	 * @see org.jlibsedml.execution.AbstractSedmlExecutor#canExecuteSimulation(org.jlibsedml.Simulation)
	 */
	@Override
	protected boolean canExecuteSimulation(Simulation sim) {
		String kisaoID = sim.getAlgorithm().getKisaoID();
		KisaoTerm wanted = KisaoOntology.getInstance().getTermById(kisaoID);
		for (String supported: SupportedIDs){
			
			KisaoTerm offered = KisaoOntology.getInstance().getTermById(
					supported);
			// If the available type is, or is a subtype of the desired algorithm,
			//we can simulate.
			if (wanted != null & offered != null && offered.is_a(wanted)) {
				return true;
			}
		}
		return false;
	}

	/** This method performs the actual simulation, using the model and simulation configuration
	 that are passed in as arguments.
	 @return An {@link IRawSedmlSimulationResults} object that is used for post-processing by the framework.
	 */
	@Override
	protected IRawSedmlSimulationResults executeSimulation(String modelStr,
			UniformTimeCourse sim) {
		AbstractDESSolver solver = getSolverForKisaoID(sim.getAlgorithm().getKisaoID());
		File tmp = null;
		try {
			// get a JSBML object from the model string.
			 tmp = File.createTempFile("Sim", "sbml");
			FileUtils.writeStringToFile(tmp, modelStr,"UTF-8");
			Model model = (new SBMLReader()).readSBML(tmp).getModel();
			// now run simulation
			SBMLinterpreter interpreter = new SBMLinterpreter(model);
			solver.setIncludeIntermediates(false);
			solver.setStepSize((sim.getOutputEndTime() -sim.getOutputStartTime() )/ sim.getNumberOfPoints());
			MultiTable mts = solver.solve(interpreter, interpreter.getInitialValues(),
					 sim.getOutputStartTime(),sim.getOutputEndTime());
			
			// adapt the MultiTable to jlibsedml interface.
			return new MultTableSEDMLWrapper(mts);
			
			
		} catch (Exception e) {
			addStatus(new ExecutionStatusElement(e, "Simulation failed", ExecutionStatusType.ERROR));
		}
		return null;
		
	}
	
	/*
	 * This class adapts the native results to an interface that the SEDML processor can use
	 *  to post-process results.
	 */
	class MultTableSEDMLWrapper implements IRawSedmlSimulationResults {
		public MultTableSEDMLWrapper(MultiTable mTable) {
			super();
			this.mTable = mTable;
		}

		private MultiTable mTable;
		public String[] getColumnHeaders() {
			String [] hdrs = new String [mTable.getColumnCount()];
			for (int i=0; i < hdrs.length;i++){
				hdrs[i]=mTable.getColumnIdentifier(i);
			}
			return hdrs;
		}

		public double[][] getData() {
			double [][] data = new double [mTable.getRowCount()][mTable.getColumnCount()];
			for (int i = 0; i < mTable.getRowCount();i++){
				for (int j =0; j< mTable.getColumnCount();j++){
					data[i][j] = mTable.getValueAt(i, j);
				
				}
				
			}
			return data;
		}

		public Double[] getDataByColumnId(String id) {
			
			Double [] rc = new Double[mTable.getRowCount()];
			Column col = mTable.getColumn(id);
			for (int i =0; i< mTable.getRowCount();i++){
				rc[i]=col.getValue(i);
			}
			return rc;
		}

		public Double[] getDataByColumnIndex(int indx) {
			Double [] rc = new Double[mTable.getRowCount()];
			Column col = mTable.getColumn(indx);
			for (int i =0; i< mTable.getRowCount();i++){
				rc[i]=col.getValue(i);
			}
			return rc;
		}

		public int getIndexByColumnID(String colID) {
			return mTable.getColumnIndex(colID);
		}

		public int getNumColumns() {
			return mTable.getColumnCount();
		}

		public int getNumDataRows() {
			return mTable.getRowCount();
		}

		/*
		 * This class maps variable IDs to data column headers.
		 * In this class, data column headers are usually IDs, so no mapping is required
		 * (non-Javadoc)
		 * @see org.jlibsedml.execution.IRawSedmlSimulationResults#getMappings()
		 */
		public IModel2DataMappings getMappings() {
			return new IModel2DataMappings() {
				
				public boolean hasMappingFor(String id) {
					return mTable.getColumn(id)!=null;
				}
				
				public String getColumnTitleFor(String modelID) {
					return modelID;
				}
				
				public int getColumnIndexFor(String colID) {
					return mTable.getColumnIndex(colID);
				}
			};
		}
		
	}

	/* SBMLSimulator can simulate SBML....
	 * (non-Javadoc)
	 * @see org.jlibsedml.execution.AbstractSedmlExecutor#supportsLanguage(java.lang.String)
	 */
	@Override
	protected boolean supportsLanguage(String language) {
		return language.contains("sbml");
	}
	
	/*
	 * Simple factory to return a solver based on the KISAO ID.
	 */
	AbstractDESSolver getSolverForKisaoID(String id){
		if(SupportedIDs[0].equals(id)){
			return new RosenbrockSolver();
		}else if (SupportedIDs[1].equals(id)){
			return new EulerMethod();
		}else if (SupportedIDs[2].equals(id)){
			return new DormandPrince54Solver();
		}else {
			return new RosenbrockSolver(); // default
		}
	}

}
