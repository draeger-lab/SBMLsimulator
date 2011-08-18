/*
 * Copyright (C) 2009 ZBIT, University of Tübingen, Andreas Dräger
 */
package org.sbml.simulator.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class provides a parser for KEGG files.
 * 
 * @author Andreas Dr&auml;ger
 * 
 */
public class KEGGparser {

	/**
	 * These comparators are used to tell the parse method when to break up
	 * parsing.
	 * 
	 * @author Andreas Dr&auml;ger
	 * 
	 */
	private enum Comparators {
		/**
		 * Reaction id
		 */
		RN_ID,
		/**
		 * Compound name
		 */
		CPD_NAME,
		/**
		 * Compound id
		 */
		CPD_ID,
		/**
		 * EC number
		 */
		EC
	}

	/**
	 * Keywords in the compound file.
	 * 
	 * @author Andreas Dr&auml;ger <a
	 *         href="mailto:andreas.draeger@uni-tuebingen.de"
	 *         >andreas.draeger@uni-tuebingen.de</a>
	 * 
	 */
	private enum KeysCompound {
		/**
		 * 
		 */
		ENTRY,
		/**
		 * 
		 */
		NAME,
		/**
		 * 
		 */
		FORMULA,
		/**
		 * 
		 */
		MASS,
		/**
		 * 
		 */
		REACTION,
		/**
		 * 
		 */
		PATHWAY,
		/**
		 * 
		 */
		DBLINKS,
		/**
		 * 
		 */
		ATOM,
		/**
		 * 
		 */
		BOND,
		/**
		 * 
		 */
		SEQUENCE,
		/**
		 * 
		 */
		ORGANISM
	}

	/**
	 * Keywords in the reaction file.
	 * 
	 * @author Andreas Dr&auml;ger <a
	 *         href="mailto:andreas.draeger@uni-tuebingen.de"
	 *         >andreas.draeger@uni-tuebingen.de</a>
	 * 
	 */
	private enum KeysReaction {
		ENTRY, NAME, DEFINITION, EQUATION, ENZYME, COMMENT, RPAIR, PATHWAY, ORTHOLOGY, REMARK
	}

	/**
	 * For testing purposes
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Args:\t" + Arrays.toString(args));
			KEGGparser parser = new KEGGparser(args[0], args[1], args[2]);
			System.out.println("Ids:\t"
					+ Arrays.toString(parser.searchCompoundsByName("Sodium")));
			System.out
					.println("Names:\t"
							+ Arrays.toString(parser
									.searchCompoundsByID("cpd:C08241")));
			System.out.println("Reactions:\t"
					+ Arrays.toString(parser
							.getReactionsByCompound("cpd:C00003")));
			String rn = "rn:R00004";
			System.out.println("Compounds for reaction " + rn + ":\t"
					+ Arrays.toString(parser.getCompoundsByReaction(rn)));
			System.out.println(Arrays.toString(parser.getNamesOfReaction(rn)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Path of the KEGG compund file.
	 */
	private String pathToCompoundFile;

	/**
	 * Path of the KEGG reaction file.
	 */
	private String pathToReactionFile;

	/**
	 * Path of the KEGG enzyme file.
	 */
	private String pathEnzymeFile;
	/**
	 * A mapping that contains for each KEGG compound id the corresponding
	 * compound object.
	 */
	private Hashtable<String, KEGGcompound> idsToCompounds;
	/**
	 * A mapping from the KEGG reaction id to the corresponding reaction object.
	 */
	private Hashtable<String, KEGGreaction> idsToReactions;
	/**
	 * A mapping between the names (all names and synonyms) of the compounds to
	 * their ids.
	 */
	private Hashtable<String, String[]> cpdNamesToIds;
	/**
	 * A reader that actually reads the KEGG compound file line by line.
	 */
	private BufferedReader brCompoundFile;

	/**
	 * A reader for the KEGG reaction file, line by line.
	 */
	private BufferedReader brReactionFile;

	/**
	 * A reader for the KEGG enzyme file, line by line.
	 */
	private BufferedReader brEnzymeFile;
	/**
	 * A mapping from reaction ids to participating compounds.
	 */
	private Hashtable<String, String[]> reactionsToCompounds;
	/**
	 * A mapping from EC numbers to reactions catalyzed by the respective
	 * enzyme.
	 */
	private Hashtable<String, String[]> enzymesToReactions;

	/**
	 * Instantiates this parser with the path to the KEGG compound file to be
	 * parsed.
	 * 
	 * @param pathCompoundFile
	 *            The path to a KEGG compound file, either relative or absolute.
	 * @param pathReactionFile
	 *            The path to a KEGG reaction file, either relative or absolute.
	 * @param pathEnzymeFile
	 *            The path to a KEGG enzyme file, either relative or absolute.
	 * @throws IOException
	 *             If the file does either not exist or is not readable.
	 */
	public KEGGparser(String pathCompoundFile, String pathReactionFile,
			String pathEnzymeFile) throws IOException {
		this.pathToCompoundFile = checkFile(pathCompoundFile);
		this.pathToReactionFile = checkFile(pathReactionFile);
		this.pathEnzymeFile = checkFile(pathEnzymeFile);
		brCompoundFile = new BufferedReader(new FileReader(pathCompoundFile));
		brReactionFile = new BufferedReader(new FileReader(pathReactionFile));
		brEnzymeFile = new BufferedReader(new FileReader(pathEnzymeFile));
		idsToCompounds = new Hashtable<String, KEGGcompound>();
		idsToReactions = new Hashtable<String, KEGGreaction>();
		cpdNamesToIds = new Hashtable<String, String[]>();
		reactionsToCompounds = new Hashtable<String, String[]>();
		enzymesToReactions = new Hashtable<String, String[]>();
	}

	@Override
	public void finalize() {
		try {
			brCompoundFile.close();
			brReactionFile.close();
			brEnzymeFile.close();
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public KEGGcompound getCompoundForURI(String uri) throws IOException {
		String id = "cpd".concat(uri.substring(uri.lastIndexOf(':'))
				.toLowerCase());
		if (!idsToCompounds.containsKey(id))
			searchCompoundsByID(id);
		return idsToCompounds.get(id);
	}

	/**
	 * Returns an array of compound KEGG ids that participate in the reaction
	 * with the given KEGG reaction id, no matter if these are reactants,
	 * products or modifiers.
	 * 
	 * @param id
	 *            A KEGG reaction id
	 * @return An array of KEGG compound ids that participate in the given
	 *         reaction.
	 * @throws IOException
	 */
	public String[] getCompoundsByReaction(String rid) throws IOException {
		if (!reactionsToCompounds.containsKey(rid))
			parseCompoundFile(rid, Comparators.RN_ID);
		return reactionsToCompounds.get(rid);
	}

	/**
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	public String[] getNamesOfReaction(String id) throws IOException {
		id = id.toLowerCase();
		if (!idsToReactions.containsKey(id))
			parseReactionFile(id, Comparators.RN_ID);
		return idsToReactions.containsKey(id) ? idsToReactions.get(id)
				.getNames() : new String[] {};
	}

	/**
	 * Returns the absolute path of the KEGG compound file parsed by this
	 * object.
	 * 
	 * @return A string representing the absolute path to the KEGG compound
	 *         file.
	 */
	public String getPath() {
		return pathToCompoundFile;
	}

	/**
	 * 
	 * @return
	 */
	public String getPathEnzymeFile() {
		return pathEnzymeFile;
	}

	/**
	 * 
	 * @return
	 */
	public String getPathToReactionFile() {
		return pathToReactionFile;
	}

	/**
	 * 
	 * @param uri
	 * @return
	 * @throws IOException
	 */
	public KEGGreaction getReactionForURI(String uri) throws IOException {
		String id = "rn".concat(uri.substring(uri.lastIndexOf(':'))
				.toLowerCase());
		if (!idsToReactions.containsKey(id))
			parseReactionFile(id, Comparators.RN_ID);
		return idsToReactions.get(id);
	}

	/**
	 * This method takes the KEGG id of a compound as an input and returns the
	 * ids of all reactions in an array this compound is involved in.
	 * 
	 * @param id
	 *            A KEGG compound id
	 * @return An array of KEGG reaction ids for a given KEGG compound id.
	 * @throws IOException
	 */
	public String[] getReactionsByCompound(String id) throws IOException {
		id = id.toLowerCase();
		if (!idsToCompounds.containsKey(id))
			parseCompoundFile(id, Comparators.CPD_ID);
		return idsToCompounds.containsKey(id) ? idsToCompounds.get(id)
				.getReactions() : new String[] {};
	}

	/**
	 * This method returns all KEGG reaction ids catalyzed by the enzyme with
	 * the given KEGG id.
	 * 
	 * @param id
	 *            The EC number of the enzyme
	 * @return An array of KEGG reaction ids that are catalyzed by the given
	 *         enzyme.
	 * @throws IOException
	 */
	public String[] getReactionsByEnzyme(String ec) throws IOException {
		if (!enzymesToReactions.containsKey(ec))
			parseReactionFile(ec, Comparators.EC);
		return enzymesToReactions.containsKey(ec) ? enzymesToReactions.get(ec)
				: new String[] {};
	}

	/**
	 * This method returns all names of the KEGG compound with the given KEGG
	 * compound id.
	 * 
	 * @param id
	 *            The KEGG id should start wit the prefix "cpd:" followed by an
	 *            upper case "C" and a certain number.
	 * @return Names of the compound with the given KEGG id.
	 * @throws IOException
	 */
	public String[] searchCompoundsByID(String id) throws IOException {
		id = id.toLowerCase();
		if (!id.startsWith("cpd:"))
			id = "cpd:" + id;
		if (!idsToCompounds.containsKey(id))
			parseCompoundFile(id, Comparators.CPD_ID);
		return idsToCompounds.containsKey(id) ? idsToCompounds.get(id)
				.getNames() : new String[] {};
	}

	/**
	 * For a given name of a compound this method searches the KEGG compound ids
	 * corresponding to this name.
	 * 
	 * @param name
	 *            One name of the desired compound.
	 * @return KEGG ids of the compound with the given name.
	 * @throws IOException
	 */
	public String[] searchCompoundsByName(String name) throws IOException {
		if (name != null) {
			String n = new String(name.toLowerCase());
			if (!cpdNamesToIds.containsKey(n))
				parseCompoundFile(n, Comparators.CPD_NAME);
			if (cpdNamesToIds.containsKey(n))
				return cpdNamesToIds.get(n);
		}
		return new String[] {};
	}

	/**
	 * 
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public String[] searchClosestCompoundsByName(String name)
			throws IOException {
		String kids[] = searchCompoundsByName(name);
		if (kids.length == 0) {
			int i;
			String n = unifyName(name);
			for (String id : cpdNamesToIds.keySet()) {
				String nn = unifyName(id);
				if (n.replace("-", "").equals(nn.replace("-", "")))
					return cpdNamesToIds.get(id);
				StringTokenizer st = new StringTokenizer(nn, "-");
				boolean containsAll = true;
				String elem;
				for (i = 0; st.hasMoreElements() && containsAll; i++) {
					elem = st.nextElement().toString();
					if (!n.contains(elem)
							&& !((elem.equals("oxo") && n.contains("keto")) || (elem
									.equals("keto") && n.contains("keto"))))
						containsAll = false;
				}
				st = new StringTokenizer(n, "-");
				for (i = 0; st.hasMoreElements() && containsAll; i++) {
					elem = st.nextElement().toString();
					if (!nn.contains(elem)
							&& !((elem.equals("oxo") && nn.contains("keto")) || (elem
									.equals("keto") && nn.contains("keto"))))
						containsAll = false;
				}
				if (containsAll)
					return cpdNamesToIds.get(id);
				n = n.replace("-", "");
				nn = nn.replace("-", "");
				if (n.length() == nn.length()) {
					Hashtable<Character, Integer> symbols = new Hashtable<Character, Integer>();
					Character c;
					for (i = 0; i < n.length(); i++) {
						c = Character.valueOf(n.charAt(i));
						if (symbols.containsKey(c))
							symbols.put(c, Integer.valueOf(symbols.get(c)
									.intValue() + 1));
						else
							symbols.put(c, Integer.valueOf(1));
					}
					for (i = 0; i < nn.length(); i++) {
						c = Character.valueOf(nn.charAt(i));
						if (symbols.containsKey(c))
							symbols.put(c, Integer.valueOf(symbols.get(c)
									.intValue() - 1));
						else
							symbols.put(c, Integer.valueOf(-1));
					}
					boolean allZero = true;
					for (Character ch : symbols.keySet())
						if (symbols.get(ch).intValue() != 0) {
							allZero = false;
							break;
						}
					if (allZero)
						return cpdNamesToIds.get(id);
				}
			}
		}
		return kids;
	}

	private String unifyName(String name) {
		String n = new String(name);
		// for (int i = 0; i < n.length(); i++)
		// if (Character.isDigit(n.charAt(i)))
		// n = n.replace(n.charAt(i), '-');
		n = n.replace("(R)", "-");
		n = n.replace("(S)", "-");
		n = n.replace("(r)", "-");
		n = n.replace("(s)", "-");
		n = n.replace(",", "");
		n = n.replace("(", "-");
		n = n.replace(")", "-");
		n = n.replace("'", "");
		n = n.replace("D-", "");
		n = n.replace("L-", "");
		n = n.replace(" ", "-");
		n = n.replace("--", "-");
		return n.toLowerCase();
	}

	/**
	 * Checks whether the given file exists and is readable and returns the
	 * absolute path of the file if both conditions are fulfilled. Throws an
	 * exception otherwise.
	 * 
	 * @param path
	 *            Absolute or relative path of a file.
	 * @return Absolute path of the given file if it exists and can be read.
	 * @throws IOException
	 *             If the file does either not exist or is not readable.
	 */
	private String checkFile(String path) throws IOException {
		File f = new File(path);
		if (!f.exists())
			throw new FileNotFoundException(path);
		if (!f.canRead())
			throw new IOException("Unable to read file " + path);
		return f.getAbsolutePath();
	}

	private boolean contains(Vector<String> entries, String comparator) {
		boolean contains = false;
		for (String c : entries)
			if (c.equalsIgnoreCase(comparator)) {
				contains = true;
				break;
			}
		return contains;
	}

	/**
	 * This method actually parses the KEGG compound file.
	 * 
	 * @param comparator
	 *            A string that can be compared with several entries within the
	 *            file.
	 * @param flag
	 *            This flag determines to what the comparator should be
	 *            compared.
	 * @throws IOException
	 */
	private void parseCompoundFile(String comparator, Comparators flag)
			throws IOException {
		String line = null, formula = null;
		Vector<String> entries = new Vector<String>();
		String names = "";
		Vector<String> reactions = new Vector<String>();
		Vector<String> patways = new Vector<String>();
		Vector<String> enzymes = new Vector<String>();
		Vector<String> dblinks = new Vector<String>();
		if (comparator.startsWith("rn:"))
			comparator = comparator.substring(3);
		boolean readingNames = false;
		boolean readingReactions = false;
		while ((line = brCompoundFile.readLine()) != null) {
			if (!line.startsWith(" ")) {
				readingNames = false;
				readingReactions = false;
			}
			if (line.startsWith("///")) {
				KEGGcompound compound = new KEGGcompound(entries.lastElement());
				StringTokenizer st = new StringTokenizer(names, ";");
				String namesTmp[] = new String[st.countTokens()];
				int i = 0;
				while (st.hasMoreElements())
					namesTmp[i++] = st.nextElement().toString().trim();
				compound.addNames(namesTmp);
				compound.addReactions(reactions.toArray(new String[] {}));
				if (idsToCompounds.containsKey(compound.getKEGGid()
						.toLowerCase())) {
					KEGGcompound old = idsToCompounds.get(compound.getKEGGid()
							.toLowerCase());
					compound.addNames(old.getNames());
					compound.addReactions(old.getReactions());
				}
				idsToCompounds
						.put(compound.getKEGGid().toLowerCase(), compound);
				for (String rid : compound.getReactions()) {
					String cpds[] = reactionsToCompounds.get(rid);
					if (cpds != null)
						cpds = StringOperations.merge(cpds, compound
								.getKEGGid());
					else
						cpds = new String[] { compound.getKEGGid() };
					reactionsToCompounds.put(rid, cpds);
				}
				if (formula != null)
				compound.setFormula(formula);
				String ids[] = entries.toArray(new String[] {});
				for (String id : ids)
					if (!id.startsWith("cpd:"))
						id = "cpd:".concat(id);
				for (String name : namesTmp)
					cpdNamesToIds.put(name.toLowerCase(), ids);
				if ((flag.equals(Comparators.CPD_NAME) && names.toLowerCase()
						.contains(comparator.toLowerCase()))
						|| (flag.equals(Comparators.CPD_ID) && contains(
								entries, comparator)))
					break;
				entries.removeAllElements();
				names = "";
				formula = null;
				reactions.removeAllElements();
				patways.removeAllElements();
				enzymes.removeAllElements();
				dblinks.removeAllElements();
			} else if (line.startsWith(KeysCompound.ENTRY.toString())) {
				String entry = line.substring(
						KeysCompound.ENTRY.toString().length()).trim();
				entry = entry.substring(0, entry.lastIndexOf(' ')).trim();
				String elem[] = entry.split(" ");
				entries.add(elem.length > 0 ? elem[0] : entry);
			} else if (line.startsWith(KeysCompound.NAME.toString())) {
				readingNames = true;
				names += line.substring(KeysCompound.NAME.toString().length())
						.trim();
				if (!names.endsWith(";") && !names.endsWith("-"))
					names += ' ';
			} else if (line.startsWith(KeysCompound.FORMULA.toString())) {
				formula = line.substring(7).trim();
			} else if (line.startsWith(KeysCompound.MASS.toString())) {

			} else if (line.startsWith(KeysCompound.REACTION.toString())) {
				readingReactions = true;
				StringTokenizer st = new StringTokenizer(line
						.substring(KeysCompound.REACTION.toString().length()));
				while (st.hasMoreElements())
					reactions.add(st.nextElement().toString().trim());
			} else if (line.startsWith(KeysCompound.PATHWAY.toString())) {

			} else if (line.startsWith(KeysCompound.DBLINKS.toString())) {

			} else if (line.startsWith(KeysCompound.ATOM.toString())) {

			} else if (line.startsWith(KeysCompound.BOND.toString())) {

			} else {
				if (readingNames) {
					names += line.trim();
					if (!names.endsWith(";") && !names.endsWith("-"))
						names += ' ';
				} else if (readingReactions) {
					StringTokenizer st = new StringTokenizer(line);
					while (st.hasMoreElements())
						reactions.add(st.nextElement().toString().trim());
				}
			}
		}
	}

	/**
	 * This method parses only KEGG reaction files.
	 * 
	 * @param comparator
	 * @param flag
	 * @throws IOException
	 */
	private void parseReactionFile(String comparator, Comparators flag)
			throws IOException {
		String line = null;
		boolean readingNames = false;
		KEGGreaction reaction = null;
		String names = "";
		while ((line = brReactionFile.readLine()) != null) {
			if (!line.startsWith(" "))
				readingNames = false;
			if (line.startsWith("///")) {
				StringTokenizer st = new StringTokenizer(names, ";");
				String n[] = new String[st.countTokens()];
				int i = 0;
				while (st.hasMoreElements())
					n[i++] = st.nextElement().toString().trim();
				reaction.addNames(n);
				names = "";
				for (String ec : reaction.getEnzymes()) {
					if (enzymesToReactions.containsKey(ec))
						enzymesToReactions.put(ec, StringOperations.merge(
								enzymesToReactions.get(ec), reaction
										.getKEGGid()));
					else
						enzymesToReactions.put(ec, new String[] { reaction
								.getKEGGid() });
				}
				idsToReactions
						.put(reaction.getKEGGid().toLowerCase(), reaction);
				if (comparator.equalsIgnoreCase(reaction.getKEGGid())
						&& flag.equals(Comparators.RN_ID))
					break;
				else if (flag == Comparators.EC) {
					int comp = Arrays.binarySearch(reaction.getEnzymes(),
							comparator);
					if ((0 < comp) && (comp < reaction.getEnzymes().length))
						break;
				}
			} else if (line.startsWith(KeysReaction.ENTRY.toString())) {
				String entry = line.substring(
						KeysReaction.ENTRY.toString().length()).trim();
				entry = entry.substring(0, entry.lastIndexOf(' ')).trim();
				reaction = new KEGGreaction(entry);
			} else if (line.startsWith(KeysReaction.NAME.toString())) {
				readingNames = true;
				names += line.substring(KeysReaction.NAME.toString().length())
						.trim();
				if (!names.endsWith(";") && !names.endsWith("-"))
					names += ' ';
			} else if (line.startsWith(KeysReaction.ENZYME.toString())) {
				StringTokenizer st = new StringTokenizer(line.substring(
						KeysReaction.ENZYME.toString().length()).trim());
				Vector<String> enzymes = new Vector<String>();
				while (st.hasMoreElements())
					enzymes.add(st.nextElement().toString().trim());
				reaction.addEnzymes(enzymes.toArray(new String[] {}));
				Arrays.sort(reaction.getEnzymes());
			} else if (line.startsWith(KeysReaction.PATHWAY.toString())) {

			} else {
				if (readingNames) {
					names += line.trim();
					if (!names.endsWith(";") && !names.endsWith("-"))
						names += ' ';
				}
			}
		}
	}
}

/**
 * This object stores the information about one compound in KEGG.
 * 
 * @author Andreas Dr&auml;ger <a
 *         href="mailto:andreas.draeger@uni-tuebingen.de">
 *         andreas.draeger@uni-tuebingen.de</a>
 * 
 */
class KEGGcompound {
	/**
	 * The unique KEGG id of this compound
	 */
	private String keggId;

	/**
	 * All names of this compound.
	 */
	private String names[];

	/**
	 * All reactions this compound is involved in, no matter if it takes part as
	 * a reactant, product or some modifier.
	 */
	private String reactions[];

	/**
	 * All pathways this compound is involved in, no matter which role it
	 * fulfills.
	 */
	private String pathways[];

	/**
	 * The empirical formula
	 */
	private String formula;

	/**
	 * 
	 * @return
	 */
	public String getFormula() {
		return formula;
	}

	/**
	 * 
	 * @param formula
	 */
	public void setFormula(String formula) {
		this.formula = formula;
	}

	/**
	 * Creates a new KEGG compound with the given id. If this id does not start
	 * with the prefix "cpd:" this will be corrected.
	 * 
	 * @param id
	 *            The id should start with the prefix "cpd:" followed by an
	 *            upper case "C" and some specific number.
	 */
	public KEGGcompound(String id) {
		keggId = id;
		if (!keggId.startsWith("cpd:"))
			keggId = "cpd:".concat(keggId);
	}

	/**
	 * Adds or sets the names of this compound. This can be trivial or
	 * systematic names of this compound.
	 * 
	 * @param names
	 */
	public void addNames(String... names) {
		this.names = (this.names == null) ? names : StringOperations.merge(
				this.names, names);
	}

	/**
	 * Adds the array of pathways to the already stored array of pathways in
	 * this object. If now pathways are stored in this object so far, the given
	 * pathways are adopted.
	 * 
	 * @param pathways
	 *            An array of additional pathways this compound is involved in.
	 */
	public void addPathways(String... pathways) {
		this.pathways = (this.pathways == null) ? pathways : StringOperations
				.merge(this.pathways, pathways);
	}

	/**
	 * Adds or sets the given array of KEGG reaction ids for this compound.
	 * Every KEGG id should start with the prefix "rn:". If this is not the case
	 * the respective element will be changed accordingly.
	 * 
	 * @param reactions
	 *            An array of KEGG reaction ids to be stored in this element.
	 */
	public void addReactions(String... reactions) {
		this.reactions = (this.reactions == null) ? reactions
				: StringOperations.merge(this.reactions, reactions);
		for (int i = 0; i < this.reactions.length; i++)
			if (!this.reactions[i].startsWith("rn:"))
				this.reactions[i] = "rn:".concat(this.reactions[i]);
	}

	/**
	 * Returns the unique KEGG id of this compound.
	 * 
	 * @return A string with the prefix "cpd:" usually followed by an upper case
	 *         "C" and a certain number.
	 */
	public String getKEGGid() {
		return keggId;
	}

	/**
	 * Returns the KEGG id of this compound in a MIRIAM compliant way.
	 * 
	 * @return
	 */
	public String getMIRIAM_URI() {
		return "urn:miriam:kegg.compound" + getKEGGid().substring(3);
	}

	/**
	 * Returns all names of this compound, i.e., trivial or systematic names or
	 * synonyms.
	 * 
	 * @return An array of strings with the names of this compound.
	 */
	public String[] getNames() {
		if (names == null)
			names = new String[] {};
		return names;
	}

	/**
	 * Returns the KEGG ids of all pathways this compound is involved in.
	 * 
	 * @return An array of KEGG pathway ids.
	 */
	public String[] getPathways() {
		if (pathways == null)
			pathways = new String[] {};
		return pathways;
	}

	/**
	 * Returns an array of the KEGG ids of all reactions this compound takes
	 * part in, either as reactant, product or modifier.
	 * 
	 * @return An array of KEGG reaction ids, in which every id has the prefix
	 *         "rn:".
	 */
	public String[] getReactions() {
		if (reactions == null)
			reactions = new String[] {};
		return reactions;
	}
}

/**
 * 
 * @author Andreas Dr&auml;ger <a
 *         href="mailto:andreas.draeger@uni-tuebingen.de">
 *         andreas.draeger@uni-tuebingen.de</a>
 * 
 */
class KEGGreaction {
	/**
	 * 
	 */
	private String id;
	/**
	 * 
	 */
	private String compounds[];
	/**
	 * 
	 */
	private String enzymes[];
	/**
	 * 
	 */
	private String names[];

	/**
	 * 
	 * @param id
	 */
	public KEGGreaction(String id) {
		this.id = id.startsWith("rn:") ? id : "rn:".concat(id);
	}

	/**
	 * 
	 * @param compounds
	 */
	public void addCompounds(String... compounds) {
		for (String cpd : compounds)
			if (!cpd.startsWith("cpd:"))
				cpd = "cpd:".concat(cpd);
		this.compounds = (this.compounds == null) ? compounds
				: StringOperations.merge(this.compounds, compounds);
	}

	public void addEnzymes(String... enzymes) {
		this.enzymes = (this.enzymes == null) ? enzymes : StringOperations
				.merge(this.enzymes, enzymes);
	}

	/**
	 * 
	 * @param string
	 */
	public void addNames(String... names) {
		this.names = (this.names == null) ? names : StringOperations.merge(
				this.names, names);
	}

	/**
	 * 
	 * @return
	 */
	public String[] getCompounds() {
		if (compounds == null)
			compounds = new String[] {};
		return compounds;
	}

	public String[] getEnzymes() {
		if (enzymes == null)
			enzymes = new String[] {};
		return enzymes;
	}

	/**
	 * 
	 * @return
	 */
	public String getKEGGid() {
		return id;
	}

	/**
	 * Returns the KEGG id of this reaction in a MIRIAM compliant way.
	 * 
	 * @return
	 */
	public String getMIRIAM_URI() {
		return "urn:miriam:kegg.reaction" + getKEGGid().substring(2);
	}

	/**
	 * 
	 * @return
	 */
	public String[] getNames() {
		if (names == null)
			names = new String[] {};
		return names;
	}
}
