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

import de.zbit.gui.GUITools;
import de.zbit.gui.LayoutHelper;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-08
 */
public class QuantitySelectionPanel extends JPanel implements ActionListener {

	/**
	 * 
	 * @author Andreas Dr&auml;ger
	 * @date 2010-09-08
	 */
	public enum Command {
		/**
		 * Action to select all elements of one group
		 */
		SELECT_ALL,
		/**
		 * Action to deselect all elements of one groupo
		 */
		SELECT_NONE;

		/**
		 * Human readable text corresponding to these commands.
		 * 
		 * @return
		 */
		public String getText() {
			switch (this) {
			case SELECT_ALL:
				return "Select all";
			default:
				return "Deselect all";
			}
		}

		/**
		 * 
		 * @return
		 */
		public String getToolTip() {
			switch (this) {
			case SELECT_ALL:
				return "With this button you can easily select all elements on this panel";
			default:
				return "With this button you can easily deselect all elements on this panel";
			}
		}
	}

	/**
	 * Template {@link String} to be displayed as explanation for the minimum
	 * and maximum {@link JSpinner}s.
	 */
	private static final String minMaxSpinnerToolTip = "Select the %s allowable value for this %s.";

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
	private class QuantityBlock implements ItemListener {
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
		private JSpinner minSpinner, maxSpinner;

		/**
		 * 
		 * @param q
		 * @param check
		 * @param min
		 * @param max
		 */
		public QuantityBlock(Quantity q, JCheckBox check, JSpinner min,
				JSpinner max) {
			quantity = q;
			checkbox = check;
			minSpinner = min;
			maxSpinner = max;
			enableSpinners(checkbox.isSelected());
			checkbox.addItemListener(this);
			minSpinner.setToolTipText(String.format(minMaxSpinnerToolTip,
					"minimum", q.getClass().getSimpleName()));
			maxSpinner.setToolTipText(String.format(minMaxSpinnerToolTip,
					"maximum", q.getClass().getSimpleName()));
		}

		/**
		 * 
		 * @param enable
		 */
		private void enableSpinners(boolean enable) {
			minSpinner.setEnabled(enable);
			maxSpinner.setEnabled(enable);
		}

		/**
		 * 
		 * @return
		 */
		public Quantity getQuantity() {
			return quantity;
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
		 * @return
		 */
		public double max() {
			return ((Double) maxSpinner.getValue()).doubleValue();
		}

		/**
		 * 
		 * @return
		 */
		public double min() {
			return ((Double) minSpinner.getValue()).doubleValue();
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
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			sb.append(quantity.toString());
			sb.append(", ");
			sb.append(isSelected());
			sb.append(", ");
			sb.append(min());
			sb.append(", ");
			sb.append(max());
			sb.append(']');
			return sb.toString();
		}
	}

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
	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	/**
	 * Values for {@link JSpinner}s of the initialization range
	 */
	private double initMinValue = 0d, initMaxValue = 14000d;

	/**
	 * Values for {@link JSpinner}s for the absolute ranges
	 */
	private double minValue = 0d, maxValue = 14000d, stepSize = 0.01d;

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
	private static final String explanation = "Please select the model components, whose values are to be optimized. You can also change the allowable ranges for each element.";

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
		tabs.setEnabledAt(3, model.getNumLocalParameters() > 0);
		tabs.setEnabledAt(2, model.getNumParameters() > 0);
		tabs.setEnabledAt(1, model.getNumCompartments() > 0);
		tabs.setEnabledAt(0, model.getNumSpecies() > 0);
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
		lh.add(new JLabel(GUITools.toHTML(explanation, 60)));
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
		switch (Command.valueOf(e.getActionCommand())) {
		case SELECT_ALL:
			select = true;
			selectAllButtons[i].setEnabled(false);
			deselectAllButtons[i].setEnabled(true);
			break;
		case SELECT_NONE:
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
		for (Quantity q : listOfQuantities) {
			isLocalParameter |= q instanceof LocalParameter;
			select = isLocalParameter || (q instanceof Parameter);
			JCheckBox chck = new JCheckBox(q.isSetName() ? q.getName() : q
					.getId(), select);
			JSpinner min = new JSpinner(new SpinnerNumberModel(minValue,
					minValue, maxValue, stepSize));
			JSpinner max = new JSpinner(new SpinnerNumberModel(maxValue,
					minValue, maxValue, stepSize));
			quantityBlocks[curr++] = new QuantityBlock(q, chck, min, max);
			lh.add(chck, min, max);
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
		container.setPreferredSize(new Dimension(450, (int) Math.min(250,
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
	 * Creates a panel with select and de-select buttons
	 * 
	 * @param buttonIndex
	 * @param select
	 * @return
	 */
	private Component createButtonPanel(int buttonIndex, boolean select) {
		JButton selectAllButton = GUITools.createButton(Command.SELECT_ALL
				.getText(), null, this, Command.SELECT_ALL, Command.SELECT_ALL
				.getToolTip());
		JButton deselectAllButton = GUITools.createButton(Command.SELECT_NONE
				.getText(), null, this, Command.SELECT_NONE,
				Command.SELECT_NONE.getToolTip());
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
		// TODO
		return null;
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
