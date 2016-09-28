package com.me3tweaks.modmanager.objects;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.valueparsers.ValueParserLib;

public class AlternateCustomDLC {

	//public static final String OPERATION_SUBSTITUTE = "OP_SUBSTITUTE"; //swap a file in a job
	public static final String OPERATION_ADD_CUSTOMDLC_JOB = "OP_ADD_CUSTOMDLC"; //do not install a file
	public static final String OPERATION_ADD_FILES_TO_CUSTOMDLC_FOLDER = "OP_ADD_FOLDERFILES_TO_CUSTOMDLC"; //install a file
	public static final String CONDITION_MANUAL = "COND_MANUAL"; //user must choose alt
	public static final String CONDITION_DLC_PRESENT = "COND_DLC_PRESENT"; //automatically choose alt if DLC listed is present
	public static final String CONDITION_DLC_NOT_PRESENT = "COND_DLC_NOT_PRESENT"; //automatically choose if DLC is not present
	public static final String CONDITION_ANY_DLC_NOT_PRESENT = "COND_ANY_DLC_NOT_PRESENT"; //multiple DLC, any of which are missing
	public static final String CONDITION_ALL_DLC_PRESENT = "COND_ALL_DLC_PRESENT";

	private String altDLC;
	private String destDLC;
	private String conditionalDLC;
	private String condition;
	private String description;
	private String operation;
	private ArrayList<String> conditionalDLCs = new ArrayList<String>();

	public AlternateCustomDLC(String altfileText) {
		conditionalDLC = ValueParserLib.getStringProperty(altfileText, "ConditionalDLC", false);
		altDLC = ValueParserLib.getStringProperty(altfileText, "ModAltDLC", false);
		condition = ValueParserLib.getStringProperty(altfileText, "Condition", false);
		if (condition.equals(CONDITION_ANY_DLC_NOT_PRESENT)) {
			parseConditionalDLC();
		}
		description = ValueParserLib.getStringProperty(altfileText, "Description", true);
		operation = ValueParserLib.getStringProperty(altfileText, "ModOperation", false);
		destDLC = ValueParserLib.getStringProperty(altfileText,"ModDestDLC", false);
	}

	private void parseConditionalDLC() {
		String str = conditionalDLC.replaceAll("\\(", "");
		str = str.replaceAll("\\)", "");
		StringTokenizer strok = new StringTokenizer(str, ";");
		while (strok.hasMoreTokens()) {
			String dlc = strok.nextToken();
			conditionalDLCs.add(dlc);
			System.out.println("Read conditional DLC in multi dlc "+dlc);
		}
	}

	@Override
	public String toString() {
		return "AlternateCustomDLC [condition=" + condition + " on conditionalDLC=" + conditionalDLC+" ||| altDLC=" + altDLC + ", destDLC=" + destDLC + ", operation=" + operation + ", conditionalDLCs=" + conditionalDLCs + "]";
	}

	/**
	 * Copy constructor
	 * 
	 * @param alt
	 *            alternate file to copy
	 */
	public AlternateCustomDLC(AlternateCustomDLC alt) {
		altDLC = alt.altDLC;
		conditionalDLC = alt.conditionalDLC;
		condition = alt.condition;
		description = alt.description;
		operation = alt.operation;
		destDLC = alt.destDLC;
		for (String str: alt.conditionalDLCs){
			conditionalDLCs.add(str);
		}
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAltDLC() {
		return altDLC;
	}

	public void setAltDLC(String altDLC) {
		this.altDLC = altDLC;
	}

	public String getConditionalDLC() {
		return conditionalDLC;
	}

	public void setTask(String task) {
		this.conditionalDLC = task;
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	/**
	 * Verifies this alternate dlc specification has all the required info to
	 * do its task
	 * 
	 * @return true if usable, false otherwise
	 */
	public boolean isValidLocally(String modPath) {
		return true;
/*		try {
			if (!condition.equals(CONDITION_DLC_NOT_PRESENT) && !condition.equals(CONDITION_DLC_PRESENT) && !condition.equals(CONDITION_MANUAL)) {
				ModManager.debugLogger.writeError("Condition is not one of the allowed values: " + condition);
				return false;
			}
			ArrayList<String> officialHeaders = new ArrayList<String>(Arrays.asList(ModType.getDLCHeaderNameArray()));
			if (!officialHeaders.contains(conditionalDLC)) {
				File f = new File(modPath + conditionalDLC);
				if (f.exists() && f.isDirectory()) {
					ModManager.debugLogger.writeError("ConditionalDLC is listed as part of the custom dlc this mod will install: " + conditionalDLC
							+ ". On mod's first install this will have no effect, and on subsequent will change what is being installed.");
					return false;
				} else {
					if (!conditionalDLC.startsWith("DLC_")) {
						ModManager.debugLogger.writeError("ConditionalDLC is not an official header and does not start with DLC_: " + conditionalDLC + ".");
						return false;
					}
				}
			}
			File alternateFile = new File(modPath + altDLC);
			if (!alternateFile.exists() && !operation.equals(OPERATION_NOINSTALL)) {
				ModManager.debugLogger.writeError("Listed altfile doesn't exist: " + altDLC);
				return false;
			}

			File normalModFile = new File(modPath + modFile);
			if (!normalModFile.exists() && (operation.equals(OPERATION_SUBSTITUTE) || operation.equals(OPERATION_NOINSTALL))) {
				ModManager.debugLogger.writeError("Listed modfile (normal mod file) doesn't exist: " + normalModFile);
				return false;
			}
			return true;
		} catch (Exception e) {
			ModManager.debugLogger.writeErrorWithException("Exception validating alternate file:", e);
			return false;
		}
*/
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public boolean isApplicable() {
		// TODO Auto-generated method stub
		return false;
	}

	public ArrayList<String> getConditionalDLCList() {
		return conditionalDLCs;
	}
	public String getDestDLC() {
		return destDLC;
	}

	public void setDestDLC(String destDLC) {
		this.destDLC = destDLC;
	}
}