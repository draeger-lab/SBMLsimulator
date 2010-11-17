/**
 * 
 */
package de.zbit.gui.prefs;

import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.zbit.gui.LayoutHelper;
import de.zbit.gui.prefs.FileSelector.Type;
import de.zbit.io.CSVOptions;
import de.zbit.io.SBFileFilter;
import de.zbit.util.prefs.SBPreferences;

/**
 * @author Andreas Dr&auml;ger
 * @date 2010-09-14
 */
public class PrefsPanelParsing extends PreferencesPanel {

    /**
     * Generated serial version identifier.
     */
    private static final long serialVersionUID = 4424293629377476108L;
    /**
	 * 
	 */
    private FileSelector chooser;
    /**
	 * 
	 */
    private JTextField tfQuoteChar, tfSeparatorChar;

    /**
     * @throws IOException
     */
    public PrefsPanelParsing() throws IOException {
	super();
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#accepts(java.lang.Object)
     */
    public boolean accepts(Object key) {
	return key.toString().startsWith("CSV_");
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#getTitle()
     */
    public String getTitle() {
	return "Parsing and writing";
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#init()
     */
    public void init() {
	chooser = new FileSelector(Type.OPEN, SBFileFilter.CSV_FILE_FILTER);
	chooser.setBorder(BorderFactory
		.createTitledBorder(" Default directories for CSV files "));

	tfQuoteChar = new JTextField(properties
		.get(CSVOptions.CSV_FILES_QUOTE_CHAR));
	tfQuoteChar.addKeyListener(this);
	tfSeparatorChar = new JTextField(properties
		.get(CSVOptions.CSV_FILES_SEPARATOR_CHAR));
	tfSeparatorChar.addKeyListener(this);
	JPanel panel = new JPanel();
	LayoutHelper lh = new LayoutHelper(panel);
	lh.add(new JLabel("Element separator:"), 0, 0, 1, 1, 0, 0);
	lh.add(new JPanel(), 1, 0, 1, 1, 0, 0);
	lh.add(tfSeparatorChar, 2, 0, 1, 1, 1, 0);
	lh.add(new JPanel(), 0, 1, 3, 1, 1, 0);
	lh.add(new JLabel("Comment symbol:"), 0, 2, 1, 1, 0, 0);
	lh.add(tfQuoteChar, 2, 2, 1, 1, 1, 0);
	panel.setBorder(BorderFactory
		.createTitledBorder(" Separator and comment character "));

	lh = new LayoutHelper(this);
	lh.add(chooser, 0, 0, 1, 1, 1, 0);
	lh.add(panel, 0, 1, 1, 1, 1, 0);
    }

    /*
     * (non-Javadoc)
     * @see de.zbit.gui.prefs.PreferencesPanel#loadPreferences()
     */
    protected SBPreferences loadPreferences() throws IOException {
	return SBPreferences.getPreferencesFor(CSVOptions.class);
    }
}
