package org.sbml.simulator.math;

import java.util.Properties;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.resources.Resource;
import org.sbml.jsbml.xml.stax.SBMLReader;
import org.sbml.simulator.SimulatorCfgKeys;
import org.sbml.simulator.gui.SimulatorUI;

import de.zbit.util.SBProperties;

/**
 * @author draeger
 * 
 */
public class SimulationModeTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// try {
		// System.loadLibrary("sbmlj");
		// // Extra check to be sure we have access to libSBML:
		// Class.forName("org.sbml.libsbml.libsbml");
		// } catch (Exception e) {
		// System.err.println("Error: could not load the libSBML library");
		// e.printStackTrace();
		// System.exit(1);
		// }
		new SimulationModeTest(args[0]);
	}

	public SimulationModeTest(String testCasesDir) {

		String sbmlfile, csvfile, configfile;
		if (!testCasesDir.endsWith("/")) {
			testCasesDir += "/";
		}

		SBProperties settings = new SBProperties();
		// SBMLsqueezer.getProperties();
		// settings.put(CfgKeys.CHECK_FOR_UPDATES, Boolean.valueOf(false));

		// SBMLio sbmlIo = new SBMLio(new LibSBMLReader(), new LibSBMLWriter());
		// SBMLsqueezerUI gui = new SBMLsqueezerUI(sbmlIo, settings);

		// 919
		for (int modelnr = 1; modelnr < 2; modelnr++)
			try {
				StringBuilder modelFile = new StringBuilder();
				modelFile.append(modelnr);
				while (modelFile.length() < 5) {
					modelFile.insert(0, '0');
				}
				String path = modelFile.toString();
				modelFile.append('/');
				modelFile.append(path);
				modelFile.insert(0, testCasesDir);
				path = modelFile.toString();
				sbmlfile = path + "-sbml-l2v4.xml";
				csvfile = path + "-results.csv";
				configfile = path + "-settings.txt";

				settings.put(SimulatorCfgKeys.SBML_FILE, sbmlfile);
				settings.put(SimulatorCfgKeys.CSV_FILE, csvfile);

				Properties cfg = Resource.readProperties(configfile);
				double start = Double.parseDouble(cfg.get("start").toString());
				double end = start
						+ Double.parseDouble(cfg.get("duration").toString());
				double stepsize = (end - start)
						/ Double.parseDouble(cfg.get("steps").toString());

				settings.put(SimulatorCfgKeys.SIM_START_TIME, Double
						.valueOf(start));
				settings
						.put(SimulatorCfgKeys.SIM_END_TIME, Double.valueOf(end));
				settings.put(SimulatorCfgKeys.SIM_STEP_SIZE, Double
						.valueOf(stepsize));

				// sbmlIo.convert2Model(sbmlfile);

				Model model = SBMLReader.readSBML(sbmlfile).getModel();
				if (model != null) {
					SimulatorUI d = new SimulatorUI(model, settings);
					d.setSelectedQuantities(cfg.get("variables").toString()
							.trim().split(", "));
					if (csvfile != null) {
						d.openExperimentalData(csvfile);
					}
					d.simulate();
					d.setVisible(true);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		// gui.dispose();
		System.exit(0);
	}

}
