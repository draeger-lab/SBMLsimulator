/*
 * $Id$
 * $URL$
 * ---------------------------------------------------------------------
 * This file is part of SBMLsimulator, a Java-based simulator for models
 * of biochemical processes encoded in the modeling language SBML.
 *
 * Copyright (C) 2007-2014 by the University of Tuebingen, Germany.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation. A copy of the license
 * agreement is provided in the file named "LICENSE.txt" included with
 * this software distribution and also available online as
 * <http://www.gnu.org/licenses/lgpl-3.0-standalone.html>.
 * ---------------------------------------------------------------------
 */
package org.sbml.simulator.gui.table;

import java.awt.Color;
import java.awt.Component;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;

import de.zbit.gui.ColorPalette;
import de.zbit.util.ResourceManager;
import de.zbit.util.StringUtil;

/**
 * This class defines the appearance of entries in the legend table and hence
 * how the plot legend looks like (not the actual legend in the plot, the legend
 * in the program to interact with the display of the plot).
 * 
 * @author Andreas Dr&auml;ger
 * @date 2010-04-07
 * @version $Rev$
 * @since 1.0
 */
public class LegendTableCellRenderer extends JLabel implements TableCellRenderer {

  /**
   * The resource bundle to be used to display texts to a user.
   */
  private static final transient ResourceBundle bundle = ResourceManager
      .getBundle("org.sbml.simulator.locales.Simulator");

  /**
   * Generated serial version identifier.
   */
  private static final long serialVersionUID = -2028676603661740472L;

  /**
   * 
   */
  public LegendTableCellRenderer() {
    super();
    setOpaque(true);
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
   */
  @Override
  public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int column) {
    if (isSelected) {
      setForeground(table.getSelectionForeground());
      //super.setBackground(table.getSelectionBackground());
      setBackground(table.getSelectionBackground());
    } else {
      setForeground(table.getForeground());
      setBackground(table.getBackground());
    }
    if (value instanceof Color) {
      Color newColor = (Color) value;
      setToolTipText(MessageFormat.format(bundle.getString("RGB_TOOLTIP"), newColor
        .getRed(), newColor.getGreen(), newColor.getBlue()));
      setBackground(newColor);
      setForeground(newColor);
      setText("");
    } else {
      setForeground(Color.BLACK);
      setToolTipText(null);
      if (row % 2 == 0) {
        setBackground(ColorPalette.lightBlue);
      } else if (!isSelected) {
        setBackground(Color.WHITE);
      }
      if (value instanceof NamedSBase) {
        if (value instanceof Compartment) {
          Compartment c = (Compartment) value;
          if (c.isSetOutsideInstance()) {
            setToolTipText(StringUtil.toHTMLMessageToolTip(
              bundle.getString("OUTSIDE_TOOLTIP"), c.getOutsideInstance()));
          }
        } else if (value instanceof Species) {
          Species s = (Species) value;
          if (s.isSetCompartmentInstance()) {
            setToolTipText(StringUtil.toHTMLMessageToolTip(
              bundle.getString("SPECIES_COMPARTMENT"),
              s.getCompartmentInstance()));
          }
        } else if (value instanceof Reaction) {
          Reaction r = (Reaction) value;
          if (r.isSetCompartmentInstance()) {
            setToolTipText(StringUtil.toHTMLMessageToolTip(
              bundle.getString("REACTION_COMPARTMENT"),
              r.getCompartmentInstance()));
          }
        } else if (value instanceof LocalParameter) {
          LocalParameter param = (LocalParameter) value;
          if (param.isSetParentSBMLObject() && param.getParentSBMLObject().isSetParentSBMLObject()) {
            KineticLaw kl = (KineticLaw) param.getParentSBMLObject().getParentSBMLObject();
            if (kl.isSetParentSBMLObject()) {
              Reaction r = kl.getParentSBMLObject();
              setToolTipText(StringUtil.toHTMLMessageToolTip(
                bundle.getString("LOCAL_PARAMETER_REACTION"), r));
            }
          }
        }
        setText(value.toString());
      } else if (value instanceof Double) {
        if (Double.isNaN(((Double) value).doubleValue())) {
          setText(" - ");
        } else {
          setText(StringUtil.toString(((Double) value).doubleValue()));
        }
      } else if (value instanceof String) {
        setText(value.toString());
      }
    }
    return this;
  }

}
