package com.me3tweaks.modmanager.objects;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.ResourceUtils;
import com.me3tweaks.modmanager.modmaker.ME3TweaksUtils;

/**
 * Patch class describes a patch file with metadata about the patch. It's
 * similar to the Mod class.
 * 
 * @author mgamerz
 *
 */
public class Patch implements Comparable<Patch> {
	public static final int APPLY_SUCCESS = 0;
	public static final int APPLY_FAILED_OTHERERROR = -1;
	public static final int APPLY_FAILED_MODDESC_NOT_UPDATED = 1;
	public static final int APPLY_FAILED_SOURCE_FILE_WRONG_SIZE = 2;
	public static final int APPLY_FAILED_NO_SOURCE_FILE = 3;
	String targetPath, targetModule, patchPath;
	boolean isValid = false;

	String patchName, patchDescription, patchFolderPath;
	long targetSize;
	double patchVersion, patchCMMVer;
	private String patchAuthor;
	private int me3tweaksid;

	public Patch(String descriptorPath) {
		ModManager.debugLogger.writeMessage("Loading patch: " + descriptorPath);
		readPatch(descriptorPath);
		patchPath = descriptorPath;
	}

	private void readPatch(String path) {
		File patchDescIni = new File(path);
		if (!patchDescIni.exists()) {
			isValid = false;
			ModManager.debugLogger.writeError("Patch descriptor does not exist: " + patchDescIni.getAbsolutePath());
			return;
		}
		Wini patchini;
		try {
			patchini = new Wini(patchDescIni);

			patchFolderPath = ModManager.appendSlash(patchDescIni.getParent());
			patchDescription = patchini.get("PatchInfo", "patchdesc");
			patchName = patchini.get("PatchInfo", "patchname");
			try {
				String idstr = patchini.get("PatchInfo", "me3tweaksid");
				me3tweaksid = Integer.parseInt(idstr);
				ModManager.debugLogger.writeMessage("Patch ID on ME3Tweaks: " + me3tweaksid);
			} catch (NumberFormatException e) {
				ModManager.debugLogger.writeError("me3tweaksid is not an integer, setting to 0");
			}

			ModManager.debugLogger.writeMessage("------PATCH--------------Reading Patch " + patchName + "-----------------");
			File patchFile = new File(patchFolderPath + "patch.jsf");
			if (!patchFile.exists()) {
				ModManager.debugLogger.writeError("Patch.jsf is missing, patch is invalid");
				ModManager.debugLogger.writeMessage("------PATCH--------------End of " + patchName + "-----------------");
				isValid = false;
				return;
			}

			ModManager.debugLogger.writeMessage("Patch Folder: " + patchFolderPath);
			ModManager.debugLogger.writeMessage("Patch Name: " + patchName);
			ModManager.debugLogger.writeMessage("Patch Description: " + patchDescription);
			// Check if this mod has been made for Mod Manager 2.0 or legacy mode
			patchCMMVer = 3.2f;
			patchVersion = 1;
			try {
				patchCMMVer = Float.parseFloat(patchini.get("ModManager", "cmmver"));
				patchCMMVer = (double) Math.round(patchCMMVer * 10) / 10; //tenth rounding;
				ModManager.debugLogger.writeMessage("Patch Targets Mod Manager: " + patchCMMVer);
				patchAuthor = patchini.get("PatchInfo", "patchdev");
				ModManager.debugLogger.writeMessage("Patch Developer (if any) " + patchAuthor);
				String strPatchVersion = patchini.get("PatchInfo", "patchver");
				if (strPatchVersion != null) {
					patchVersion = Float.parseFloat(strPatchVersion);
					patchVersion = (double) Math.round(patchVersion * 10) / 10; //tenth rounding
					ModManager.debugLogger.writeMessage("Patch Version: " + patchVersion);
				} else {
					patchVersion = 1.0;
					ModManager.debugLogger.writeMessage("Patch Version: Not specified, defaulting to 1.0");
				}
			} catch (NumberFormatException e) {
				ModManager.debugLogger.writeMessage("Didn't read a target version (cmmver) in the descriptor file. Targetting 3.2.");
				patchCMMVer = 3.2f;
				ModManager.debugLogger.writeException(e);
			}

			targetModule = patchini.get("PatchInfo", "targetmodule");
			targetPath = patchini.get("PatchInfo", "targetfile");
			targetSize = Long.parseLong(patchini.get("PatchInfo", "targetsize"));
			ModManager.debugLogger.writeMessage("Patch Targets Module: " + targetModule);
			ModManager.debugLogger.writeMessage("Patch Targets File in module: " + targetPath);
			ModManager.debugLogger.writeMessage("Patch only works with files of size: " + targetSize);

			if (targetPath == null || targetModule == null || targetPath.equals("") || targetModule.equals("")) {
				ModManager.debugLogger.writeMessage("Invalid patch, targetfile or targetmodule was empty or missing");
				isValid = false;
			} else if (targetSize <= 0) {
				ModManager.debugLogger.writeMessage("Invalid patch, target size of file to patch has to be bigger than 0");
				isValid = false;
			} else if (targetPath.endsWith("Coalesced.bin")) {
				ModManager.debugLogger.writeMessage("Invalid patch, patches do not work with Coalesced.bin");
				isValid = false;
			} else {
				isValid = true;
			}
			ModManager.debugLogger.writeMessage("Finished loading patchdesc.ini for this patch.");
		} catch (InvalidFileFormatException e) {
			// TODO Auto-generated catch block
			ModManager.debugLogger.writeException(e);
			isValid = false;
		} catch (IOException e) {
			ModManager.debugLogger.writeException(e);
			isValid = false;
		} catch (NumberFormatException e) {
			ModManager.debugLogger.writeException(e);
			isValid = false;
		}
		ModManager.debugLogger.writeMessage("------PATCH--------------END OF " + patchName + "-------------------------");
	}

	/**
	 * Moves this patch into the data/patches directory
	 * 
	 * @return new patch object if successful, null otherwise
	 */
	public Patch importPatch() {
		ModManager.debugLogger.writeMessage("Importing patch to library");
		String patchDirPath = ModManager.getPatchesDir() + "patches/";
		File patchDir = new File(patchDirPath);
		patchDir.mkdirs();

		String destinationDir = patchDirPath + getPatchName();
		File destDir = new File(destinationDir);
		if (destDir.exists()) {
			ModManager.debugLogger.writeError("Cannot import patch: Destination directory already exists (patch with same name already exists in the patches folder)");
			return null;
		}
		try {
			ModManager.debugLogger.writeMessage("Moving patch to library");
			FileUtils.moveDirectory(new File(patchFolderPath), destDir);
			ModManager.debugLogger.writeMessage("Patch migrated to library");
		} catch (IOException e) {
			ModManager.debugLogger.writeErrorWithException("Failed to import patch:", e);
			return null;
		}
		ModManager.debugLogger.writeMessage("Reloading imported patch");
		return new Patch(destinationDir + File.separator + "patchdesc.ini");
	}

	/**
	 * Gets the source file that would be used if this patch was applied to the specified mod
	 * @param mod
	 * @return null if no source (error), path otherwise
	 */
	public String getSourceFilePath(Mod mod) {
		String modSourceFile = mod.getModTaskPath(targetPath, targetModule);
		if (modSourceFile == null) {
			ModManager.debugLogger.writeMessage(mod.getModName() + " does not appear to modify " + targetPath + " in module " + targetModule + ", performing file fetch");
			//we need to check if its in the patch library's source folder
			modSourceFile = ModManager.getPatchSource(targetPath, targetModule);
			return modSourceFile;
		} else {
			return modSourceFile;
		}
	}

	/**
	 * Applies this patch. Inserts itself as a task in the specified mod.
	 * 
	 * @param mod
	 *            Mod to apply with
	 * @return APPLY_SUCCESS if successful, otherwise other constants if failed.
	 */
	public int applyPatch(Mod mod) {
		//We must check if the mod we are applying to already has this file. If it does we will apply to that mod.
		//If it does not we will add new task for it.
		//If the files are not the right size we will not apply.
		ModManager.debugLogger.writeMessage("=============APPLY PATCH " + getPatchName() + "=============");
		try {

			File jpatch = new File(ModManager.getToolsDir() + "jptch.exe");
			if (!jpatch.exists()) {
				ME3TweaksUtils.downloadJDiffTools();
			}

			if (!ModManager.hasPristineTOC(targetModule, ME3TweaksUtils.HEADER)) {
				ME3TweaksUtils.downloadPristineTOC(targetModule, ME3TweaksUtils.HEADER);
			}

			//Prepare mod
			String modSourceFile = mod.getModTaskPath(targetPath, targetModule);
			if (modSourceFile == null) {
				ModManager.debugLogger.writeMessage(mod.getModName() + " does not appear to modify " + targetPath + " in module " + targetModule + ", performing file fetch");
				//we need to check if its in the patch library's source folder
				modSourceFile = ModManager.getPatchSource(targetPath, targetModule);

				if (modSourceFile == null) {
					//couldn't copy or extract file, have nothing we can patch
					ModManager.debugLogger.writeMessage(mod.getModName() + "'s patch " + getPatchName() + " was not able to acquire a source file to patch.");
					return APPLY_FAILED_NO_SOURCE_FILE;
				}

				//copy sourcefile to mod dir
				File libraryFile = new File(modSourceFile);
				if (libraryFile.length() != targetSize) {
					ModManager.debugLogger.writeError("File that is going to be patched does not match patch descriptor size (" + libraryFile.length()
							+ " vs one can be applied to: " + targetSize + ")! Unable to apply patch");
					return APPLY_FAILED_SOURCE_FILE_WRONG_SIZE;
				}

				File modFile = new File(ModManager.appendSlash(mod.getModPath()) + Mod.getStandardFolderName(targetModule) + File.separator + FilenameUtils.getName(targetPath));
				ModManager.debugLogger.writeMessage("Copying libary file to mod package: " + libraryFile.getAbsolutePath() + " => " + modFile.getAbsolutePath());
				FileUtils.copyFile(libraryFile, modFile);

				//we need to add a task for this, lookup if job exists already
				ModJob targetJob = null;
				String standardFolder = ModManager.appendSlash(Mod.getStandardFolderName(targetModule));
				String filename = FilenameUtils.getName(targetPath);
				for (ModJob job : mod.jobs) {
					if (job.getJobName().equals(targetModule)) {
						ModManager.debugLogger.writeMessage("Checking existing job: " + targetModule);
						targetJob = job;
						String jobFolder = ModManager.appendSlash(new File(job.getNewFiles()[0]).getParentFile().getAbsolutePath());
						String relativepath = ModManager.appendSlash(ResourceUtils.getRelativePath(jobFolder, mod.getModPath(), File.separator));

						//ADD PATCH FILE TO JOB
						File modFilePath = new File(ModManager.appendSlash(mod.getModPath()) + relativepath + filename);
						ModManager.debugLogger.writeMessage("Adding new mod task => " + targetModule + ": add " + modFilePath.getAbsolutePath());
						job.addFileReplace(modFilePath.getAbsolutePath(), targetPath);

						//CHECK IF JOB HAS TOC - SOME MIGHT NOT, FOR SOME WEIRD REASON
						//copy toc
						File tocFile = new File(mod.getModPath() + relativepath + "PCConsoleTOC.bin");
						if (!tocFile.exists()) {
							FileUtils.copyFile(new File(ModManager.getPristineTOC(targetModule, ME3TweaksUtils.HEADER)), tocFile);
						} else {
							ModManager.debugLogger.writeMessage("Toc file already exists in module: " + targetModule);
						}
						//add toc to jobs
						String tocTask = mod.getModTaskPath(ME3TweaksUtils.coalFileNameToDLCTOCDir(ME3TweaksUtils.headerNameToCoalFilename(targetModule)), targetModule);
						if (tocTask == null) {
							//add toc replacejob
							job.addFileReplace(tocFile.getAbsolutePath(), targetPath);
						}
						break;
					}
				}

				if (targetJob == null) {
					ModManager.debugLogger.writeMessage("Creating new job: " + targetModule);
					//no job for the module this task needs
					//we need to add it as a new task and then add add a PCConsoleTOC for it
					double newCmmVer = Math.max(mod.modCMMVer, 3.2);
					ModJob job;
					if (targetModule.equals(ModType.BASEGAME)) {
						job = new ModJob();
					} else {
						job = new ModJob(ModType.getDLCPath(targetModule), targetModule);
					}
					File modulefolder = new File(ModManager.appendSlash(mod.getModPath() + standardFolder));
					modulefolder.mkdirs();
					ModManager.debugLogger.writeMessage("Adding PCConsoleTOC.bin to new job");
					File tocSource = new File(ModManager.getPristineTOC(targetModule, ME3TweaksUtils.HEADER));
					File tocDest = new File(modulefolder + File.separator + "PCConsoleTOC.bin");
					FileUtils.copyFile(tocSource, tocDest);
					job.addFileReplace(tocDest.getAbsolutePath(), ME3TweaksUtils.coalFileNameToDLCTOCDir(ME3TweaksUtils.headerNameToCoalFilename(targetModule)));

					ModManager.debugLogger.writeMessage("Adding " + filename + " to new job");
					/*
					 * File modFile = new File(modulefolder + File.separator +
					 * filename); FileUtils.copyFile(libraryFile, modFile);
					 */
					job.addFileReplace(modFile.getAbsolutePath(), targetPath);
					mod.addTask(targetModule, job);
					mod.modCMMVer = newCmmVer;
				}

				//write new moddesc.ini file
				String descini = mod.createModDescIni(mod.modCMMVer);
				ModManager.debugLogger.writeMessage("Updating moddesc.ini with updated job");
				FileUtils.writeStringToFile(mod.modDescFile, descini);

				//reload mod in staging with new job added
				ModManager.debugLogger.writeMessage("Reloading updated mod with new moddesc.ini file");
				mod = new Mod(mod.modDescFile.getAbsolutePath());
				modSourceFile = mod.getModTaskPath(targetPath, targetModule);
			}
			if (modSourceFile == null) {
				ModManager.debugLogger
						.writeError("Source file should have been copied to mod directory already. ModDesc.ini however is missing a newfiles/replacefiles task in the job.");
				return APPLY_FAILED_MODDESC_NOT_UPDATED;
			}
			//rename file (so patch doesn't continuously recalculate itself)
			File stagingFile = new File(ModManager.getTempDir() + "patch_staging"); //this file is used as base, and then patch puts file back in original place
			ModManager.debugLogger.writeMessage("Staging source file: " + modSourceFile + " => " + stagingFile.getAbsolutePath());

			stagingFile.delete();
			FileUtils.moveFile(new File(modSourceFile), stagingFile);

			//apply patch
			ArrayList<String> commandBuilder = new ArrayList<String>();
			commandBuilder.add(ModManager.getToolsDir() + "jptch.exe");
			commandBuilder.add(stagingFile.getAbsolutePath());
			commandBuilder.add(getPatchFolderPath() + "patch.jsf");
			commandBuilder.add(modSourceFile);
			StringBuilder sb = new StringBuilder();
			for (String arg : commandBuilder) {
				sb.append("\"" + arg + "\" ");
			}

			ModManager.debugLogger.writeMessage("Executing JPATCH patch command: " + sb.toString());

			ProcessBuilder patchProcessBuilder = new ProcessBuilder(commandBuilder);
			//patchProcessBuilder.redirectErrorStream(true);
			//patchProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
			Process patchProcess = patchProcessBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(patchProcess.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null)
				System.out.println("tasklist: " + line);
			patchProcess.waitFor();
			System.out.println("BREAK");
			stagingFile.delete();
			ModManager.debugLogger.writeMessage("File has been patched.");
			return APPLY_SUCCESS;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			ModManager.debugLogger.writeErrorWithException("IOException applying mod:", e);
			return APPLY_FAILED_OTHERERROR;
		} catch (InterruptedException e) {
			ModManager.debugLogger.writeErrorWithException("Patching process was interrupted:", e);
			return APPLY_FAILED_OTHERERROR;
		}
	}

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}

	public String getTargetModule() {
		return targetModule;
	}

	public void setTargetModule(String targetModule) {
		this.targetModule = targetModule;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public String getPatchName() {
		return patchName;
	}

	public void setPatchName(String patchName) {
		this.patchName = patchName;
	}

	public String getPatchDescription() {
		return patchDescription;
	}

	public void setPatchDescription(String patchDescription) {
		this.patchDescription = patchDescription;
	}

	public String getPatchFolderPath() {
		return patchFolderPath;
	}

	public void setPatchFolderPath(String patchFolderPath) {
		this.patchFolderPath = patchFolderPath;
	}

	public long getTargetSize() {
		return targetSize;
	}

	public void setTargetSize(long targetSize) {
		this.targetSize = targetSize;
	}

	public double getPatchVersion() {
		return patchVersion;
	}

	public void setPatchVersion(double patchVersion) {
		this.patchVersion = patchVersion;
	}

	public double getPatchCMMVer() {
		return patchCMMVer;
	}

	public void setPatchCMMVer(double patchCMMVer) {
		this.patchCMMVer = patchCMMVer;
	}

	public String getPatchAuthor() {
		return patchAuthor;
	}

	@Override
	public int compareTo(Patch otherPatch) {
		return getPatchName().compareTo(otherPatch.getPatchName());
	}

	public static String generatePatchDesc(ME3TweaksPatchPackage pack) {
		Wini ini = new Wini();

		// put modmanager, PATCHINFO
		ini.put("ModManager", "cmmver", pack.getTargetversion());
		ini.put("PatchInfo", "patchname", pack.getPatchname());
		ini.put("PatchInfo", "patchdesc", pack.getPatchdesc());
		ini.put("PatchInfo", "patchdev", pack.getPatchdev());
		ini.put("PatchInfo", "patchver", pack.getPatchver());
		ini.put("PatchInfo", "targetmodule", pack.getTargetmodule());
		ini.put("PatchInfo", "targetfile", pack.getTargetfile());
		ini.put("PatchInfo", "targetsize", pack.getTargetsize());
		ini.put("PatchInfo", "finalizer", pack.isFinalizer());
		ini.put("PatchInfo", "me3tweaksid", pack.getMe3tweaksid());

		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			ini.store(os);
			return new String(os.toByteArray(), "ASCII");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}

	public String convertToME3TweaksSQLInsert() {
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO mixinlibrary VALUES (\n\tnull,\n");
		sb.append("\t\"" + patchName + "\",\n");
		sb.append("\t\"" + patchDescription + "\",\n");
		sb.append("\t\"" + ((patchAuthor == null) ? "FemShep" : patchAuthor) + "\",\n");
		sb.append("\t" + patchVersion + ",\n");
		if (patchCMMVer < 4.0) {
			patchCMMVer = 4.0;
		}
		sb.append("\t" + patchCMMVer + ",\n");
		sb.append("\t\"" + targetModule + "\",\n");
		String sqlPath = targetPath.replaceAll("\\\\", "\\\\\\\\");
		sb.append("\t\"" + sqlPath + "\",\n");

		sb.append("\t" + targetSize + ",\n");
		sb.append("\tfalse, /*FINALIZER*/\n");

		String serverfolder = patchName.toLowerCase().replaceAll(" - ", "-").replaceAll(" ", "-");
		sb.append("\t\"http://me3tweaks.com/mixins/library/" + serverfolder + "/patch.jsf\",\n");
		sb.append("\t\"" + patchName + "\",\n");
		sb.append("\tnull\n");
		sb.append(");");
		File copyTo = new File("server/"+serverfolder+"/patch.jsf");
		File dirHeader = copyTo.getParentFile();
		dirHeader.mkdirs();
		if (ModManager.IS_DEBUG){
			try {
				FileUtils.copyFile(new File(patchFolderPath + "patch.jsf"), copyTo);
				System.out.println("Copied to "+copyTo.getAbsolutePath());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public int getMe3tweaksid() {
		return me3tweaksid;
	}
}
