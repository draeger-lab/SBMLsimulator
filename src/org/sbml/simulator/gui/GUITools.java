/**
 * 
 */
package org.sbml.simulator.gui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.sbml.simulator.resources.Resource;

/**
 * @author Andreas Dr&auml;ger
 * @date 18:19:20
 */
public class GUITools extends de.zbit.gui.GUITools {

	static {
		UIManager.put("ICON_SAVE", getIconSave());
		UIManager.put("ICON_OPEN", getIconFolder());
	}
	
	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconCamera() {
		return new ImageIcon(Resource.class.getResource("img/camera_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconEvA2() {
		return new ImageIcon(Resource.class.getResource("/images/icon1.gif"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconFolder() {
		return new ImageIcon(Resource.class.getResource("img/folder_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconGear() {
		return new ImageIcon(Resource.class.getResource("img/gear_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconHelp() {
		return new ImageIcon(Resource.class.getResource("img/help_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static Icon getIconHelp48() {
		return new ImageIcon(Resource.class.getResource("img/help_48.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconInfo() {
		return new ImageIcon(Resource.class.getResource("img/info_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconLicense() {
		return new ImageIcon(Resource.class.getResource("img/licence_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static Icon getIconLicense48() {
		return new ImageIcon(Resource.class.getResource("img/licence_48.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconSave() {
		return new ImageIcon(Resource.class.getResource("img/save_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconSettings() {
		return new ImageIcon(Resource.class.getResource("img/settings_16.png"));
	}

	/**
	 * 
	 * @return
	 */
	public static ImageIcon getIconTrash() {
		return new ImageIcon(Resource.class.getResource("img/trash_16.png"));
	}

}
