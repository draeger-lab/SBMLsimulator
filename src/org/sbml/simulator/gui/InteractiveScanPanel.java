/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.QuantityWithDefinedUnit;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.squeezer.CfgKeys;
import org.sbml.squeezer.util.HTMLFormula;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;

/**
 * An {@link InteractiveScanPanel} is an element that allows the user to change
 * the values of all instances of {@link Compartment}, {@link Species},
 * {@link Parameter}, and {@link LocalParameter} within a {@link Model}. To this
 * end, it contains a {@link JTabbedPane} for to structure these four groups of
 * {@link QuantityWithDefinedUnit}, each containing a {@link JScrollPane} with
 * one {@link JSpinner} for each element. On the bottom there is a reset button
 * to restore the original values.
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-09-17
 */
public class InteractiveScanPanel extends JPanel implements ActionListener,
		ChangeListener {

	/**
	 * @author Andreas Dr&auml;ger
	 * @date 2010-09-17
	 */
	public enum Command {
		/**
		 * Resets all values to those defined in the model.
		 */
		RESET
	}

	/**
	 * Generated serial version identifier.
	 */
	private static final long serialVersionUID = -8244482994475574724L;
	/**
	 * Pointer to the model.
	 */
	private Model model;
	/**
	 * 
	 */
	private SpinnerNumberModel[] spinModQuantity;
	/**
	 * 
	 */
	private Set<ChangeListener> setOfChangeListeners;
	/**
	 * Default values
	 */
	private double defaultCompartmentValue, defaultParameterValue,
			defaultSpeciesValue;
	/**
	 * Backup of the original values
	 */
	private double originalValues[];
	/**
	 * Direct pointers to all important quantities in the model.
	 */
	private QuantityWithDefinedUnit[] quantities;
	/**
	 * 
	 */
	private boolean hasLocalParameters;
	/**
	 * 
	 */
	private static final String BUTTON_TOOL_TIP = "Sets all values to the original values from the model. However, previously undefined values will be set to the default values.";
	/**
	 * 
	 */
	private JButton buttonReset;

	/**
	 * 
	 */
	public InteractiveScanPanel(Model model, double maxCompartmentValue,
			double maxSpeciesValue, double maxParameterValue,
			double paramStepSize) {
		super(new BorderLayout());

		// initialize fields
		this.model = model;
		this.setOfChangeListeners = new HashSet<ChangeListener>();
		this.spinModQuantity = new SpinnerNumberModel[model
				.getNumQuantitiesWithDefinedUnit()];
		this.originalValues = new double[spinModQuantity.length];
		this.quantities = new QuantityWithDefinedUnit[originalValues.length];
		this.hasLocalParameters = false;

		// create layout
		int offset = 0;
		JTabbedPane tab = new JTabbedPane();
		tab.add("Compartments", interactiveScanScrollPane(model
				.getListOfCompartments(), maxCompartmentValue, paramStepSize,
				offset));
		offset += model.getNumCompartments();
		tab.setEnabledAt(0, model.getNumCompartments() > 0);

		tab.add("Species", interactiveScanScrollPane(model.getListOfSpecies(),
				maxParameterValue, paramStepSize, offset));
		offset += model.getNumSpecies();

		tab.setEnabledAt(1, model.getNumSpecies() > 0);
		tab
				.add("Global Parameters", interactiveScanScrollPane(model
						.getListOfParameters(), maxSpeciesValue, paramStepSize,
						offset));
		tab.setEnabledAt(2, model.getNumParameters() > 0);

		tab.add("Local Parameters",
				new JScrollPane(interactiveScanLocalParameters(
						maxParameterValue, paramStepSize),
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		tab.setEnabledAt(3, hasLocalParameters);
		for (int i = tab.getTabCount() - 1; i >= 0; i--) {
			if (tab.isEnabledAt(i)) {
				tab.setSelectedIndex(i);
			}
		}

		JPanel foot = new JPanel();
		buttonReset = GUITools.createButton("Reset", null, this, Command.RESET,
				BUTTON_TOOL_TIP);
		buttonReset.setEnabled(false);
		foot.add(buttonReset);

		add(tab, BorderLayout.CENTER);
		add(foot, BorderLayout.SOUTH);
	}

	/**
	 * 
	 * @param maxParameterValue
	 * @param paramStepSize
	 * @return
	 */
	private JPanel interactiveScanLocalParameters(double maxParameterValue,
			double paramStepSize) {
		JPanel parameterPanel = new JPanel();
		parameterPanel.setLayout(new BoxLayout(parameterPanel,
				BoxLayout.PAGE_AXIS));
		int offset = model.getNumSymbols();
		for (Reaction r : model.getListOfReactions()) {
			if (r.isSetKineticLaw() && r.getKineticLaw().getNumParameters() > 0) {
				hasLocalParameters = true;
				JPanel panel = interactiveScanTable(r.getKineticLaw()
						.getListOfParameters(), maxParameterValue,
						paramStepSize, offset);
				offset += r.getKineticLaw().getNumParameters();
				panel.setBorder(BorderFactory.createTitledBorder(String.format(
						" Reaction %s ", r.getId())));
				parameterPanel.add(panel);
			}
		}
		return parameterPanel;
	}

	/**
	 * 
	 * @param list
	 * @param maxValue
	 * @param stepSize
	 * @param offset
	 * @return
	 */
	private JScrollPane interactiveScanScrollPane(
			ListOf<? extends QuantityWithDefinedUnit> list, double maxValue,
			double stepSize, int offset) {
		return new JScrollPane(interactiveScanTable(list, maxValue, stepSize,
				offset), JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	}

	/**
	 * 
	 * @param list
	 * @param maxValue
	 * @param stepSize
	 * @param offset
	 *            index offset
	 * @return
	 */
	private JPanel interactiveScanTable(
			ListOf<? extends QuantityWithDefinedUnit> list, double maxValue,
			double stepSize, int offset) {
		JPanel panel = new JPanel();
		LayoutHelper lh = new LayoutHelper(panel);
		LinkedList<String> nans = new LinkedList<String>();
		double value = 0;
		String name = "";
		for (int i = 0; i < list.size(); i++) {
			QuantityWithDefinedUnit p = list.get(i);
			value = p.getValue();
			if (Double.isNaN(p.getValue())) {
				name = p.getClass().getSimpleName().toLowerCase();
				if (p instanceof Compartment) {
					value = defaultCompartmentValue;
				} else if (p instanceof Species) {
					value = defaultSpeciesValue;
				} else if (p instanceof Parameter) {
					value = defaultParameterValue;
				}
				p.setValue(value);
				nans.add(p.getId());
				if (!(p instanceof Species) && (list.size() > 1)) {
					name += "s";
				}
			}
			maxValue = Math.max(value, maxValue);
			spinModQuantity[i + offset] = new SpinnerNumberModel(value, Math
					.min(0d, value), maxValue, stepSize);
			originalValues[i + offset] = value; // backup.
			quantities[i + offset] = p;

			JSpinner spinner = new JSpinner(spinModQuantity[i + offset]);
			spinner.setName(p.getId());
			spinner.addChangeListener(this);
			lh.add(new JLabel(GUITools.toHTML(p.toString(), 40)), 0, i, 1, 1,
					0, 0);
			lh.add(spinner, 2, i, 1, 1, 0, 0);
			lh.add(new JLabel(GUITools.toHTML(p.isSetUnits() ? HTMLFormula
					.toHTML(p.getUnitsInstance()) : "")), 4, i, 1, 1, 0, 0);
		}
		lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
		lh.add(new JPanel(), 3, 0, 1, 1, 0, 0);
		if (nans.size() > 0) {
			String l = nans.toString().substring(1);
			String msg = String
					.format(
							"Undefined value%s for the %s %s ha%s been replaced by its default value %.3f.",
							nans.size() > 1 ? "s" : "", name, l.substring(0, l
									.length() - 1), nans.size() > 1 ? "ve"
									: "s", value);
			JEditorPane label = new JEditorPane("text/html", GUITools.toHTML(
					msg, 80));
			label.setEditable(false);
			Component component;
			if (nans.size() > 20) {
				component = new JScrollPane(label,
						JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
						JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				label.setPreferredSize(new Dimension(450, 450));
			} else {
				component = label;
			}
			JOptionPane.showMessageDialog(this, component,
					"Replacing undefined values",
					JOptionPane.INFORMATION_MESSAGE);
		}
		return panel;
	}

	/**
	 * 
	 * @param cl
	 * @return
	 */
	public boolean addChangeListener(ChangeListener cl) {
		return setOfChangeListeners.add(cl);
	}

	/**
	 * 
	 * @param cl
	 * @return
	 */
	public boolean removeChangeListener(ChangeListener cl) {
		return setOfChangeListeners.remove(cl);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent
	 * )
	 */
	public void stateChanged(ChangeEvent e) {
		checkButton();
		for (ChangeListener cl : setOfChangeListeners) {
			cl.stateChanged(e);
		}
	}

	/**
	 * Checks whether or not to enable the button to reset the values for each
	 * {@link JSpinner}.
	 */
	private void checkButton() {
		boolean allEqual = true;
		for (int i = 0; i < originalValues.length && allEqual; i++) {
			allEqual &= originalValues[i] == ((Number) spinModQuantity[i]
					.getValue()).doubleValue();
		}
		buttonReset.setEnabled(!allEqual);
	}

	/**
	 * 
	 * @return
	 */
	public Properties getProperties() {
		Properties p = new Properties();
		p.put(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE, Double
				.valueOf(defaultCompartmentValue));
		p.put(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE, Double
				.valueOf(defaultSpeciesValue));
		p.put(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS, Double
				.valueOf(defaultParameterValue));
		return p;
	}

	/**
	 * 
	 * @param properties
	 */
	public void setProperties(Properties properties) {
		defaultCompartmentValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_COMPARTMENT_INITIAL_SIZE))
				.doubleValue();
		defaultSpeciesValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_SPECIES_INITIAL_VALUE)).doubleValue();
		defaultParameterValue = ((Number) properties
				.get(CfgKeys.OPT_DEFAULT_VALUE_OF_NEW_PARAMETERS))
				.doubleValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ((e.getActionCommand() != null)
				&& (e.getActionCommand().equals(Command.RESET.toString()))) {
			for (int i = 0; i < originalValues.length; i++) {
				spinModQuantity[i].setValue(Double.valueOf(originalValues[i]));
				quantities[i].setValue(originalValues[i]);
			}
		}
	}
}
