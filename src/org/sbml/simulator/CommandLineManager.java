package org.sbml.simulator;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.simulator.math.odes.AbstractDESSolver;
import org.simulator.math.odes.MultiTable;

import de.zbit.AppConf;
import de.zbit.util.prefs.SBPreferences;
import de.zbit.util.prefs.SBProperties;

/**
 * This class handels
 * 
 * @author Alexander D&ouml;rr
 * @version $Rev: 281 $
 * @since 1.0
 */

public class CommandLineManager implements PropertyChangeListener {

	/**
	 * The simulation manager for the current simulation.
	 */
	private SimulationManager simulationManager;
	String openFile;
	String timeSeriesFile;
	AppConf appConf;

	public CommandLineManager(String openFile, String timeSeriesFile,
			AppConf appConf) {
		this.openFile = openFile;
		this.appConf = appConf;
		this.timeSeriesFile = timeSeriesFile;
		loadPreferences();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if ("progress".equals(evt.getPropertyName())) {
		} else if ("done".equals(evt.getPropertyName())) {
			MultiTable data = (MultiTable) evt.getNewValue();
			if (data != null) {
				processResult(data);
			} else {

			}
		}

	}

	private void processResult(MultiTable data) {
		SBProperties props = appConf.getCmdArgs();
		FileWriter filewriter;
		BufferedWriter bufferedWriter;
		String outCSVFile;
		if (props.containsKey(SimulatorOptions.SIMULATION_OUTPUT_FILE)) {
			outCSVFile = props.get(SimulatorOptions.SIMULATION_OUTPUT_FILE)
					.toString();
		} else {
			outCSVFile = openFile.substring(0, openFile.lastIndexOf('.'))
					+ "_simulated.csv";
		}

		try {
			filewriter = new FileWriter(outCSVFile);
			bufferedWriter = new BufferedWriter(filewriter);

			for (int i = 0; i < data.getColumnCount(); i++) {
				
				if (i>0) {
					bufferedWriter.write("\t");
				}
				bufferedWriter.write(data.getColumnName(i));		
				
			}
			
			bufferedWriter.write(System.getProperty("line.separator"));

			for (int i = 0; i < data.getRowCount(); i++) {
				
				bufferedWriter.write(String.valueOf(data.getTimePoint(i)));
				
				for (int j = 0; j < data.getColumnCount(); j++) {

					bufferedWriter.write("\t");
					bufferedWriter.write(String.valueOf(data.getValueAt(j, i)));
					
				}
				bufferedWriter.write(System.getProperty("line.separator"));
			}

			bufferedWriter.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void loadPreferences() {
		AbstractDESSolver solver;
		SBPreferences prefs = SBPreferences
				.getPreferencesFor(SimulationOptions.class);
		QualityMeasurement qualityMeasurement;
		SimulationConfiguration simulationConfiguration;
		SimulationManager simulationManager;
		double simEndTime, simStepSize;
		SBProperties props = appConf.getCmdArgs();

		if (props.containsKey(SimulationOptions.SIM_END_TIME)) {
			simEndTime = Double.valueOf(props
					.get(SimulationOptions.SIM_END_TIME));
		} else {
			simEndTime = prefs.getDouble(SimulationOptions.SIM_END_TIME);
		}

		if (props.containsKey(SimulationOptions.SIM_STEP_SIZE)) {
			simStepSize = Double.valueOf(props
					.get(SimulationOptions.SIM_STEP_SIZE));
		} else {
			simStepSize = prefs.getDouble(SimulationOptions.SIM_STEP_SIZE);
		}

		try {
			qualityMeasurement = (QualityMeasurement) Class.forName(
					prefs.getString(SimulationOptions.QUALITY_MEASURE))
					.newInstance();

			Model model = (new SBMLReader()).readSBML(openFile).getModel();

			solver = (AbstractDESSolver) Class.forName(
					prefs.getString(SimulationOptions.ODE_SOLVER))
					.newInstance();

			simulationConfiguration = new SimulationConfiguration(model,
					solver, 0, simEndTime, simStepSize, false);

			simulationManager = new SimulationManager(qualityMeasurement,
					simulationConfiguration);
			simulationManager.addPropertyChangeListener(this);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Conducts the simulation.
	 * 
	 * @throws Exception
	 */
	private void simulate() throws Exception {
		simulationManager.simulate();
	}

	public void perform() throws Exception {
		simulate();
	}

}
