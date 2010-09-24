/**
 * 
 */
package org.sbml.simulator.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Quantity;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.util.ValuePair;
import org.sbml.optimization.QuantityRange;
import org.sbml.squeezer.util.HTMLFormula;

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 */
public class QuantitySelectionPanel extends JPanel implements ActionListener {

	/**
	 * A data structure that contains all necessary information for one
	 * {@link Quantity}: a {@link JCheckBox} to select or de-select it and the
	 * values for the optimization. Furthermore, this data structure takes care
	 * about switching the {@link JSpinner}s for the values associated with the
	 * {@link Quantity} off. In addition, a pointer to the {@link Quantity}
	 * itself is also stored.
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2010-09-09
	 * 
	 */
	private class QuantityBlock implements QuantityRange, ItemListener {
		/**
		 * Generated serial version identifier.
		 */
		private static final long serialVersionUID = -1190252378673523294L;
		/**
		 * 
		 */
		private Quantity quantity;
		/**
		 * 
		 */
		private JCheckBox checkbox;

		/**
		 * 
		 */
		private JSpinner minSpinner, maxSpinner, minInitSpinner,
				maxInitSpinner;

		/**
		 * 
		 * @param q
		 * @param check
		 * @param initMin
		 * @param initMax
		 * @param min
		 * @param max
		 */
		public QuantityBlock(Quantity q, JCheckBox check, JSpinner initMin,
				JSpinner initMax, JSpinner min, JSpinner max) {
			quantity = q;
			checkbox = check;
			minInitSpinner = initMin;
			maxInitSpinner = initMax;
			minSpinner = min;
			maxSpinner = max;
			enableSpinners(checkbox.isSelected());
			checkbox.addItemListener(this);
			String className = q.getClass().getSimpleName();
			String name = q.isSetName() ? q.getName() : q.getId();
			checkbox.setToolTipText(GUITools.toHTML(String.format(
					checkBoxToolTip, className, name), 40));
			minInitSpinner.setToolTipText(GUITools.toHTML(String.format(
					INIT_MIN_MAX_SPINNER_TOOL_TIP, MINIMUM, className, name),
					40));
			maxInitSpinner.setToolTipText(GUITools.toHTML(String.format(
					INIT_MIN_MAX_SPINNER_TOOL_TIP, MAXIMUM, className, name),
					40));
			minSpinner.setToolTipText(GUITools.toHTML(String.format(
					MIN_MAX_SPINNER_TOOL_TIP, MINIMUM, className, name), 40));
			maxSpinner.setToolTipText(GUITools.toHTML(String.format(
					MIN_MAX_SPINNER_TOOL_TIP, MAXIMUM, className, name), 40));
		}

		/**
		 * 
		 * @param enabled
		 */
		private void enableSpinners(boolean enabled) {
			minInitSpinner.setEnabled(enabled);
			maxInitSpinner.setEnabled(enabled);
			minSpinner.setEnabled(enabled);
			maxSpinner.setEnabled(enabled);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sbml.optimization.QuantityRange#getInitialMaximum()
		 */
		public double getInitialMaximum() {
			return ((Double) maxInitSpinner.getValue()).doubleValue();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sbml.optimization.QuantityRange#getInitialMinimum()
		 */
		public double getInitialMinimum() {
			return ((Double) minInitSpinner.getValue()).doubleValue();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sbml.optimization.QuantityRange#getInitialRange()
		 */
		public ValuePair<Double, Double> getInitialRange() {
			return new ValuePair<Double, Double>(Double
					.valueOf(getInitialMinimum()), Double
					.valueOf(getInitialMaximum()));
		}

		/**
		 * 
		 * @return
		 */
		public double getMaximum() {
			return ((Double) maxSpinner.getValue()).doubleValue();
		}

		/**
		 * 
		 * @return
		 */
		public double getMinimum() {
			return ((Double) minSpinner.getValue()).doubleValue();
		}

		/**
		 * 
		 * @return
		 */
		public Quantity getQuantity() {
			return quantity;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.sbml.optimization.QuantityRange#getRange()
		 */
		public ValuePair<Double, Double> getRange() {
			return new ValuePair<Double, Double>(Double.valueOf(getMinimum()),
					Double.valueOf(getMaximum()));
		}

		/**
		 * 
		 * @return
		 */
		public boolean isSelected() {
			return checkbox.isSelected();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent
		 * )
		 */
		public void itemStateChanged(ItemEvent e) {
			if ((e.getSource() != null) && (e.getSource() == checkbox)) {
				enableSpinners(checkbox.isSelected());
			}
		}

		/**
		 * 
		 * @param select
		 */
		public void setSelected(boolean select) {
			checkbox.setSelected(select);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return StringTools.concat(Character.valueOf('['), quantity, ", ",
					Boolean.valueOf(isSelected()), ": initRange(",
					getInitialMinimum(), ", ", getInitialMaximum(),
					"), absolutRange(", getMinimum(), ", ", getMaximum(), ")]")
					.toString();
		}
	}

	/**
	 * Template {@link String} to be displayed as explanation for the minimum
	 * and maximum {@link JSpinner}s.
	 */
	private static final String MIN_MAX_SPINNER_TOOL_TIP = "Select the absolute %s allowable value for this %s named %s.";
	/**
	 * Template tool tip for the the initialization {@link JSpinner}s.
	 */
	private static final String INIT_MIN_MAX_SPINNER_TOOL_TIP = "Select the %s allowable value for this %s named %s in the initialization.";
	/**
	 * The words for minimum and maximum.
	 */
	private static final String MINIMUM = "minimum", MAXIMUM = "maximum";

	/**
	 * Template tool tip for the {@link JCheckBox}es to switch the values on or
	 * off depending on whether these are to be optimized or not.
	 */
	private static final String checkBoxToolTip = "Check this box to include %s %s in the optimization";

	/**
	 * 
	 */
	private JButton[] selectAllButtons;
	/**
	 * 
	 */
	private JButton[] deselectAllButtons;

	/**
	 * 
	 */
	private QuantityBlock quantityBlocks[];

	/**
	 * Generated serial version identifier
	 */
	private static final long serialVersionUID = -4979926949556272791L;

	/**
	 * @return the serialversionuid
	 */
	public static long getSerialVersionUId() {
		return serialVersionUID;
	}

	/**
	 * Values for {@link JSpinner}s of the initialization range
	 */
	private double initMinValue = 0d, initMaxValue = 5d;

	/**
	 * Values for {@link JSpinner}s for the absolute ranges
	 */
	private double minValue = 0d, maxValue = 2000d, stepSize = 0.01d;

	/**
	 * A pointer to the model for which this panel is being created.
	 */
	private Model model;

	/**
	 * 
	 */
	private static final String LOCAL_PARAMETERS = "Local parameters";
	/**
	 * 
	 */
	private static final String GLOBAL_PARAMETERS = "Global parameters";
	/**
	 * 
	 */
	private static final String COMPARTMENTS = "Compartments";
	/**
	 * 
	 */
	private static final String SPECIES = "Species";

	/**
	 * Text to explain what the user is supposed to do here.
	 */
	private static final String EXPLANATION = "Please select the model components, whose values are to be optimized. You can also change the allowable ranges for each element.";

	/**
	 * Tool tip that explains the purpose of each tab.
	 */
	private static final String TAB_TOOL_TIP = "In this tab you can select the %s in the model whose values are to be optimized.";

	/**
	 * This pane displays all selections for each group of different
	 * {@link Quantity}s.
	 */
	private JTabbedPane tabs;

	/**
	 * 
	 */
	public QuantitySelectionPanel(Model model) {
		super();
		this.model = model;
		quantityBlocks = new QuantityBlock[model.getNumSymbols()
				+ model.getNumLocalParameters()];
		// One button each for every group of elements
		selectAllButtons = new JButton[4];
		deselectAllButtons = new JButton[4];

		tabs = new JTabbedPane();
		int curr = 0;
		tabs.add(COMPARTMENTS, createQuantityPanel(model
				.getListOfCompartments(), curr, 0));
		curr += model.getNumCompartments();
		tabs.add(SPECIES,
				createQuantityPanel(model.getListOfSpecies(), curr, 1));
		curr += model.getNumSpecies();
		tabs.add(GLOBAL_PARAMETERS, createQuantityPanel(model
				.getListOfParameters(), curr, 2));
		curr += model.getNumParameters();
		tabs.add(LOCAL_PARAMETERS, createLocalParameterTab(model));

		/*
		 * Enable tabs with selectable elements and disable all others.
		 */
		tabs.setEnabledAt(3, model.getNumLocalParameters() > 0);
		tabs.setToolTipTextAt(3, GUITools.toHTML(String.format(TAB_TOOL_TIP,
				LOCAL_PARAMETERS), 40));
		tabs.setEnabledAt(2, model.getNumParameters() > 0);
		tabs.setToolTipTextAt(2, GUITools.toHTML(String.format(TAB_TOOL_TIP,
				GLOBAL_PARAMETERS), 40));
		tabs.setEnabledAt(1, model.getNumSpecies() > 0);
		tabs.setToolTipTextAt(1, GUITools.toHTML(String.format(TAB_TOOL_TIP,
				SPECIES), 40));
		tabs.setEnabledAt(0, model.getNumCompartments() > 0);
		tabs.setToolTipTextAt(0, GUITools.toHTML(String.format(TAB_TOOL_TIP,
				COMPARTMENTS), 40));
		int i = 3;
		while ((i < tabs.getTabCount()) && (!tabs.isEnabledAt(i))) {
			i--;
		}
		tabs.setSelectedIndex(i);

		/*
		 * Ensure same size of all tabs.
		 */
		double maxHeight = 0, maxWidth = 0, height, width;
		Dimension dim;
		for (i = 0; i < tabs.getTabCount(); i++) {
			dim = tabs.getComponentAt(i).getPreferredSize();
			height = dim.getHeight();
			width = dim.getWidth();
			if (height > maxHeight) {
				maxHeight = height;
			}
			if (width > maxWidth) {
				maxWidth = width;
			}
		}
		for (i = 0; i < tabs.getTabCount(); i++) {
			dim = tabs.getComponentAt(i).getPreferredSize();
			height = dim.getHeight();
			width = dim.getWidth();
			if (height < maxHeight) {
				dim = new Dimension((int) width, (int) maxHeight);
				tabs.getComponentAt(i).setPreferredSize(dim);
			}
			if (width < maxWidth) {
				dim = new Dimension((int) maxWidth, (int) maxHeight);
				tabs.getComponentAt(i).setPreferredSize(dim);
			}
		}

		LayoutHelper lh = new LayoutHelper(this);
		lh.add(new JLabel(GUITools.toHTML(EXPLANATION, 60)));
		lh.add(tabs);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if ((e.getActionCommand() == null) || (e.getSource() == null)
				|| !(e.getSource() instanceof JButton)) {
			return;
		}
		JButton button = (JButton) e.getSource();
		int i = 0, begin = 0, end = quantityBlocks.length;
		while (!GUITools.contains(tabs.getComponentAt(i), button)) {
			i++;
		}
		String title = tabs.getTitleAt(i);
		if (title.equals(COMPARTMENTS)) {
			end = model.getNumCompartments();
		} else if (title.equals(SPECIES)) {
			begin = model.getNumCompartments();
			end = begin + model.getNumSpecies();
		} else if (title.equals(GLOBAL_PARAMETERS)) {
			begin = model.getNumCompartments() + model.getNumSpecies();
			end = begin + model.getNumParameters();
		} else {
			// local parameters
			begin = model.getNumSymbols();
		}
		boolean select;
		Dimension d = (Dimension) button.getPreferredSize().clone();
		switch (SelectionCommand.valueOf(e.getActionCommand())) {
		case ALL:
			select = true;
			selectAllButtons[i].setEnabled(false);
			deselectAllButtons[i].setEnabled(true);
			break;
		case NONE:
			select = false;
			selectAllButtons[i].setEnabled(true);
			deselectAllButtons[i].setEnabled(false);
			break;
		default:
			System.err.printf("unknown command %s\n", e.getActionCommand());
			select = false;
			break;
		}
		button.setPreferredSize(d);
		for (i = begin; i < end; i++) {
			quantityBlocks[i].setSelected(select);
		}
	}

	/**
	 * Creates a panel with select and de-select buttons
	 * 
	 * @param buttonIndex
	 * @param select
	 * @return
	 */
	private Component createButtonPanel(int buttonIndex, boolean select) {
		JButton selectAllButton = GUITools.createButton(SelectionCommand.ALL
				.getText(), null, this, SelectionCommand.ALL,
				SelectionCommand.ALL.getToolTip());
		JButton deselectAllButton = GUITools.createButton(SelectionCommand.NONE
				.getText(), null, this, SelectionCommand.NONE,
				SelectionCommand.NONE.getToolTip());
		selectAllButton.setEnabled(!select);
		deselectAllButton.setEnabled(select);
		selectAllButton.setPreferredSize(deselectAllButton.getPreferredSize());

		selectAllButtons[buttonIndex] = selectAllButton;
		deselectAllButtons[buttonIndex] = deselectAllButton;

		JPanel panel = new JPanel();
		panel.add(selectAllButton);
		panel.add(deselectAllButton);
		return panel;
	}

	/**
	 * 
	 * @param model
	 * @return
	 */
	private Container createLocalParameterTab(Model model) {
		LayoutHelper lh = new LayoutHelper(new JPanel());
		JPanel p;
		int curr = model.getNumSymbols();
		for (Reaction r : model.getListOfReactions()) {
			if (r.isSetKineticLaw()
					&& (r.getKineticLaw().getNumParameters() > 0)) {
				p = new JPanel();
				p.setBorder(BorderFactory.createTitledBorder(String.format(
						" Reaction %s ", r.isSetName() ? r.getName() : r
								.getId())));
				p.add(createQuantityPanel(r.getKineticLaw()
						.getListOfParameters(), curr, 3));
				curr += r.getKineticLaw().getNumParameters();
				lh.add(p);
			}
		}
		return finishContainer(lh, 3, true, curr);
	}

	/**
	 * 
	 * @param listOfQuantities
	 * @param curr
	 * @param tabIndex
	 * @return
	 */
	private Container createQuantityPanel(
			ListOf<? extends Quantity> listOfQuantities, int curr, int tabIndex) {
		LayoutHelper lh = new LayoutHelper(new JPanel());
		boolean isLocalParameter = false;
		boolean select = true;
		JSeparator sep = new JSeparator(JSeparator.VERTICAL);
		lh.add(true, new JLabel(), new JLabel("<html>initial<br/>minimum</html>"),
				new JLabel("<html>initial<br/>maximum</html>"), sep,
				new JLabel("<html>absolute<br/>minimum</html>"), new JLabel(
						"<html>absolute<br/>maximum</html>"), sep, new JLabel(
						"<html>derived<br/>unit</html>"));
		for (Quantity q : listOfQuantities) {
			isLocalParameter |= q instanceof LocalParameter;
			select = isLocalParameter || (q instanceof Parameter);
			JCheckBox chck = new JCheckBox(q.isSetName() ? q.getName() : q
					.getId(), select);

			// Initialization
			JSpinner minInit = new JSpinner(new SpinnerNumberModel(
					initMinValue, initMinValue, initMaxValue, stepSize));
			JSpinner maxInit = new JSpinner(new SpinnerNumberModel(
					initMaxValue, initMinValue, initMaxValue, stepSize));

			// Optimization
			JSpinner min = new JSpinner(new SpinnerNumberModel(minValue,
					minValue, maxValue, stepSize));
			JSpinner max = new JSpinner(new SpinnerNumberModel(maxValue,
					minValue, maxValue, stepSize));

			quantityBlocks[curr++] = new QuantityBlock(q, chck, minInit,
					maxInit, min, max);
			lh.add(true, chck, minInit, maxInit, sep, min, max, sep, new JLabel(
					StringTools.concat("<html>",
							HTMLFormula.toHTML(q.getDerivedUnitDefinition()),
							"</html>").toString()));
		}
		if (!isLocalParameter) {
			return finishContainer(lh, tabIndex, select, listOfQuantities
					.size());
		}
		return lh.getContainer();
	}

	/**
	 * Finishes the panel
	 * 
	 * @param lh
	 * @param buttonIndex
	 * @param select
	 * @param numElements
	 * @return
	 */
	private Container finishContainer(LayoutHelper lh, int buttonIndex,
			boolean select, int numElements) {
		Container container = lh.getContainer();
		JScrollPane scroll = new JScrollPane(container,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		int height = 10 + 25 * numElements;
		if (buttonIndex == 3) {
			height += model.getNumReactions() * 5;
		}
		container.setPreferredSize(new Dimension(550, (int) Math.min(250,
				height)));
		lh = new LayoutHelper(new JPanel());
		lh.add(scroll);
		lh.add(new JSeparator(), 0, lh.getRow() + 1, 5, 1);
		lh
				.add(createButtonPanel(buttonIndex, select), 0,
						lh.getRow() + 2, 5, 1);

		return lh.getContainer();
	}

	/**
	 * @return the initMaxValue
	 */
	public double getInitMaxValue() {
		return initMaxValue;
	}

	/**
	 * @return the initMinValue
	 */
	public double getInitMinValue() {
		return initMinValue;
	}

	/**
	 * @return the maxValue
	 */
	public double getMaxValue() {
		return maxValue;
	}

	/**
	 * @return the minValue
	 */
	public double getMinValue() {
		return minValue;
	}

	/**
	 * 
	 * @return
	 */
	public Quantity[] getSelectedQuantities() {
		LinkedList<Quantity> quantityList = new LinkedList<Quantity>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i].getQuantity());
			}
		}
		return quantityList.toArray(new Quantity[0]);
	}

	/**
	 * 
	 * @return
	 */
	public String[] getSelectedQuantityIds() {
		LinkedList<String> quantityList = new LinkedList<String>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i].getQuantity().getId());
			}
		}
		return quantityList.toArray(new String[0]);
	}

	/**
	 * 
	 * @return
	 */
	public QuantityRange[] getSelectedQuantityRanges() {
		LinkedList<QuantityRange> quantityList = new LinkedList<QuantityRange>();
		for (int i = 0; i < quantityBlocks.length; i++) {
			if (quantityBlocks[i].isSelected()) {
				quantityList.add(quantityBlocks[i]);
			}
		}
		return quantityList.toArray(new QuantityRange[0]);
	}

	/**
	 * @return the stepSize
	 */
	public double getStepSize() {
		return stepSize;
	}

	/**
	 * @param initMaxValue
	 *            the initMaxValue to set
	 */
	public void setInitMaxValue(double initMaxValue) {
		this.initMaxValue = initMaxValue;
	}

	/**
	 * @param initMinValue
	 *            the initMinValue to set
	 */
	public void setInitMinValue(double initMinValue) {
		this.initMinValue = initMinValue;
	}

	/**
	 * @param maxValue
	 *            the maxValue to set
	 */
	public void setMaxValue(double maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * @param minValue
	 *            the minValue to set
	 */
	public void setMinValue(double minValue) {
		this.minValue = minValue;
	}

	/**
	 * @param stepSize
	 *            the stepSize to set
	 */
	public void setStepSize(double stepSize) {
		this.stepSize = stepSize;
	}

}
