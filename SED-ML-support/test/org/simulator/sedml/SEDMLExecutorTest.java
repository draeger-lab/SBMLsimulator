package org.simulator.sedml;

import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.jlibsedml.DataGenerator;
import org.jlibsedml.Libsedml;
import org.jlibsedml.Output;
import org.jlibsedml.SEDMLDocument;
import org.jlibsedml.SedML;
import org.jlibsedml.Task;
import org.jlibsedml.Variable;
import org.jlibsedml.VariableSymbol;
import org.jlibsedml.XMLException;
import org.jlibsedml.execution.IProcessedSedMLSimulationResults;
import org.jlibsedml.execution.IRawSedmlSimulationResults;
import org.jlibsedml.execution.SedMLResultsProcesser2;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.simulator.math.odes.MultiTable;

/**
 * This test class shows how a SED-ML file can be interpreted and executed using 
 *  SBML Simulator Core solvers. <br/>
 *  It makes extensive use of jlibsedml's Execution framework which performs boiler-plate
 *   code for operations such as post-processing of results, etc., 
 * @author radams
 *
 */
public class SEDMLExecutorTest {
	SedML sedml = null;
	@Before
	public void setUp() throws Exception {
		
	}

	@After
	public void tearDown() throws Exception {
	}

//SED_ML_EXECUTOR_TEST must be set to the address of the file to use for testing the sed-ml execution.
	@Test
	public final void testBasicSEDMLExecutorForLocalFile() throws XMLException {
		/*
	 * This file describes a straightforward simulation of a basic model a->b->c
	 */
		File abc1test = new File(System.getenv("SED_ML_EXECUTOR_TEST"));
	
	// get the SED-ML object model from file. The model referred to in this 
		//SEDML file is defined by a relative path and is in the top-level folder.
		SEDMLDocument doc = Libsedml.readDocument(abc1test);
		// check no errors in SEDML file, else simulation will not work so well.
		assertFalse(doc.hasErrors());
		sedml=doc.getSedMLModel();
		// in this sedml file there's just one output. If there were several,
		// we could either iterate or get user to  decide which output to generate.
		Output wanted = sedml.getOutputs().get(0);
		SedMLSBMLSimulatorExecutor exe = new SedMLSBMLSimulatorExecutor(sedml,wanted);
		
		// Here we run all the simulations needed to create an output, and get the 
		// raw results.
		Map<Task, IRawSedmlSimulationResults>res = exe.runSimulations();
		if(res==null ||res.isEmpty() || !exe.isExecuted()){
			 fail ("Simulatation failed: " + exe.getFailureMessages().get(0));
		}
			
			 MultiTable mt = processSimulationResults(wanted, res);
			assertTrue( 5== mt.getColumnCount());
			assertTrue ("Was"  + mt.getRowCount(), 101 == mt.getRowCount());
			assertEquals("Time", mt.getTimeName());
			assertEquals(1,mt.getBlock(0).getColumn(0).getValue(0),0.001);
			assertEquals("A_dg",mt.getBlock(0).getColumn(0).getColumnName());
		
	}
	
  //SED_ML_MIRIAM_TEST must be set to the address of the file to test with.
	// retrieves model from Miriam DB - needs internet connection
	@Test
	public final void testBasicSEDMLExecutorForMiriamURNDefinedModel() throws XMLException {
		File miriamtest = new File(System.getenv("SED_ML_MIRIAM_TEST"));
		
		sedml=Libsedml.readDocument(miriamtest).getSedMLModel();
		// in this SED-ML file there's just one output. If there were several,
		// we could either iterate or get user to  decide what they want to run.
		Output wanted = sedml.getOutputs().get(0);
		SedMLSBMLSimulatorExecutor exe = new SedMLSBMLSimulatorExecutor(sedml,wanted);
		// This gets the raw simulation results - one for each Task that was run.
		Map<Task, IRawSedmlSimulationResults>res = exe.runSimulations();
		if(res==null ||res.isEmpty() || !exe.isExecuted()){
			 fail ("Simulatation failed: " + exe.getFailureMessages().get(0));
		}
		      // now process.In this case, there's no processing performed - we're displaying the
		     // raw results.
			 MultiTable mt = processSimulationResults(wanted, res);
			assertTrue( 3== mt.getColumnCount());
			assertTrue ("Was"  + mt.getRowCount(), 1001 == mt.getRowCount());
			assertEquals("Time", mt.getTimeName());
	}

	MultiTable processSimulationResults(Output wanted,
			Map<Task, IRawSedmlSimulationResults> res) {
		// here we post-process the results
		 SedMLResultsProcesser2 pcsr2 =  new SedMLResultsProcesser2(sedml, wanted);
		 pcsr2.process(res);
		 
		 // this does not necessarily have time as x-axis - another variable could be  the 
		 // independent variable.
		 IProcessedSedMLSimulationResults prRes = pcsr2.getProcessedResult();
		 
		 // now we restore a MultiTable from the processed results. This basic example assumes a typical 
		 // simulation where time = xaxis - otherwise, if output is a Plot, we would need to analyse the x-axis
		 // datagenerators
		MultiTable mt = createMultiTableFromProcessedResults(wanted, prRes);
		return mt;
	}

	// Here we need to check which of the results are the independent axis to create a MultiTable
	MultiTable createMultiTableFromProcessedResults(Output wanted,
			IProcessedSedMLSimulationResults prRes) {
		String timeColName = findTimeColumn(prRes, wanted, sedml);
		
		// most of the rest of this code is concerned with adapting a processed result set
		// back to a multitable.
		
		double [] time = getTimeData(prRes, timeColName);
		// we need to get a new datset that does not contain the time-series dataset.
		double [][] data = getNonTimeData(prRes, timeColName);
		// now we ignore the time dataset
		String []hdrs = getNonTimeHeaders(prRes, timeColName);
		
		 MultiTable mt = new MultiTable(time, data, hdrs);
		return mt;
	}

	private String[] getNonTimeHeaders(IProcessedSedMLSimulationResults prRes,
			String timeColName) {
		String []rc = new String [prRes.getNumColumns()-1];
		int rcIndx =0;
		for (String col:prRes.getColumnHeaders()){
			if(!col.equals(timeColName)){
				rc[rcIndx++]=col;
			}
		}
		return rc;
		
	}

	// gets the variable ( or non-time data )
	private double[][] getNonTimeData(IProcessedSedMLSimulationResults prRes,
			String timeColName) {
		double [][] data = prRes.getData();
		int indx = prRes.getIndexByColumnID(timeColName);
		double [][] rc = new double [prRes.getNumDataRows() ][prRes.getNumColumns()-1];
		for (int r = 0; r< data.length;r++){
			int colIndx=0;
			for ( int c = 0; c< data[r].length;c++){
				if (c!=indx) {
					rc[r][colIndx++]=data[r][c];
				}
			}
		}
		return rc;
		
		
	}

	//gets the time data from the processed result array.
	private double[] getTimeData(IProcessedSedMLSimulationResults prRes,
			String timeColName) {
		Double [] tim = prRes.getDataByColumnId(timeColName);
	
		
		double [] rc = new double[tim.length];
		int indx=0;
		for (Double d: tim){
			rc[indx++]=d.doubleValue();
		}
		return rc;
	}

	// Identifies the time column's title. Raw results have column headers equal to the DataGenerator
	// id in the SEDML file. 
	private String findTimeColumn(IProcessedSedMLSimulationResults prRes,
			Output wanted, SedML sedml2) {
		// TODO Auto-generated method stub
		List<String>dgIds = wanted.getAllDataGeneratorReferences();
		for (String dgID:dgIds){
			DataGenerator dg = sedml.getDataGeneratorWithId(dgID);
			if(dg != null){
				List<Variable> vars = dg.getListOfVariables();
				for (Variable v: vars){
					if (v.isSymbol() && VariableSymbol.TIME.equals(v.getSymbol())){
						return dgID;
					}
				}
			}
		}
		return null;
	}

}
