package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.me3tweaks.modmanager.basegamedb.BasegameHashDB;
import com.me3tweaks.modmanager.basegamedb.RepairFileInfo;
import com.me3tweaks.modmanager.objects.Mod;
import com.me3tweaks.modmanager.objects.ModJob;
import com.me3tweaks.modmanager.objects.ModType;

@SuppressWarnings("serial")
/**
 * Window that injects the files into the game/dlc.
 * 
 * @author Mgamerz
 *
 */
public class ModInstallWindow extends JDialog {
	JLabel infoLabel;
	String bioGameDir;
	final int levelCount = 7;
	JTextArea consoleArea;
	String consoleQueue[];
	String currentText;
	JProgressBar progressBar;

	ModManagerWindow callingWindow;

	public ModInstallWindow(ModManagerWindow callingWindow, ModJob[] jobs, String bioGameDir, Mod mod) {
		// callingWindow.setEnabled(false);
		this.callingWindow = callingWindow;
		this.bioGameDir = bioGameDir;
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setTitle("Applying Mod");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setPreferredSize(new Dimension(320, 220));
		consoleQueue = new String[levelCount];

		setupWindow();

		this.setIconImages(ModManager.ICONS);
		this.pack();
		this.setLocationRelativeTo(callingWindow);

		checkModCMMVersion(mod);
		boolean installMod = validateRequiredModulesAreAvailable(callingWindow, jobs);
		if (installMod) {
			new InjectionCommander(jobs, mod).execute();
			this.setVisible(true);
		} else {
			ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Mod install cancelled");
			dispose();
		}
	}

	/**
	 * Checks to make sure that the MODDESC can be fully parsed.
	 * 
	 * @param mod
	 *            mod to check against
	 */
	private void checkModCMMVersion(Mod mod) {
		if (mod.getCMMVer() > ModManager.MODDESC_VERSION_SUPPORT) {
			JOptionPane.showMessageDialog(callingWindow, "This mod specifies it requires a newer Mod Manager " + mod.getCMMVer()
					+ ".\nMod Manager will attempt to install the mod but it may not work.", "Outdated Mod Manager", JOptionPane.WARNING_MESSAGE);
		}
	}

	/**
	 * Checks that the modjobs required modules are available and prompts if
	 * they aren't
	 * 
	 * @return true if all are available or user ignored missing
	 */
	private boolean validateRequiredModulesAreAvailable(ModManagerWindow callingWindow, ModJob[] jobs) {
		ArrayList<ModJob> missingModules = new ArrayList<ModJob>();
		for (ModJob job : jobs) {
			if (job.getJobType() == ModJob.DLC) {
				String me3exppath = ModManager.getME3ExplorerEXEDirectory(false);
				if (me3exppath.equals("")) {
					//me3explorer is missing
					ModManager.debugLogger.writeError("Unable to find ME3Explorer, cancelling mod install");
					JOptionPane.showMessageDialog(null,
							"Installation of mods requires ME3Explorer in the data directory.\nMod installation cannot continue.",
							"Required Component Missing", JOptionPane.ERROR_MESSAGE);
					return false;
				}

				//check that sfar is available
				String sfarName = "Default.sfar";
				if (job.TESTPATCH) {
					sfarName = "Patch_001.sfar";
				}
				String sfarPath = ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName;
				File sfar = new File(sfarPath);
				if (!sfar.exists()) {
					missingModules.add(job);
				}
			}
		}
		if (missingModules.size() <= 0) {
			ModManager.debugLogger.writeMessage("Mod has all required DLCs available");
			return true;
		}

		//module is missing
		StringBuilder sb = new StringBuilder();
		sb.append("This mod has tasks for the following missing DLC.\nIf the mod descriptor details the job description, they will be listed below.\n");
		for (ModJob job : missingModules) {
			ModManager.debugLogger.writeMessage("Mod requires missing DLC Module: " + job.getJobName());
			sb.append(" - ");
			sb.append(job.getJobName());
			sb.append("\n");
			if (job.getRequirementText() != null && !job.getRequirementText().equals("")) {
				sb.append("   - ");
				sb.append(job.getRequirementText());
				sb.append("\n");
			}
		}
		sb.append("\nThese jobs will be skipped. Continue with the mod install?");
		int result = JOptionPane.showConfirmDialog(callingWindow, sb.toString(), "Missing DLC", JOptionPane.WARNING_MESSAGE);
		ModManager.debugLogger.writeMessage(result == JOptionPane.YES_OPTION ? "User continuing install even with missing DLC modules"
				: "User canceled Mod Install");
		return result == JOptionPane.YES_OPTION;
	}

	private void setupWindow() {
		JPanel rootPanel = new JPanel(new BorderLayout());
		JPanel northPanel = new JPanel(new BorderLayout());
		infoLabel = new JLabel("<html>Applying mod to Mass Effect 3...<br>This may take a few minutes.</html>");
		northPanel.add(infoLabel, BorderLayout.NORTH);
		progressBar = new JProgressBar(0, 100);
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(false);

		northPanel.add(progressBar, BorderLayout.SOUTH);
		northPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		rootPanel.add(northPanel, BorderLayout.NORTH);

		consoleArea = new JTextArea();
		consoleArea.setLineWrap(true);
		consoleArea.setWrapStyleWord(true);

		consoleArea.setEditable(false);

		rootPanel.add(consoleArea, BorderLayout.CENTER);
		getContentPane().add(rootPanel);
	}

	class InjectionCommander extends SwingWorker<Boolean, String> {
		int completed = 0;
		int numjobs = 0;
		Mod mod;
		ModJob[] jobs;
		ArrayList<String> failedJobs;
		private BasegameHashDB bghDB;
		private boolean installCancelled = false;

		protected InjectionCommander(ModJob[] jobs, Mod mod) {
			this.mod = mod;
			numjobs = jobs.length;
			failedJobs = new ArrayList<String>();
			ModManager.debugLogger.writeMessage("Starting the InjectionCommander utility. Number of jobs to do: " + numjobs);
			this.jobs = jobs;
			ModManager.debugLogger.writeMessage("Using ME3Explorer from: " + ModManager.getME3ExplorerEXEDirectory(false));
		}

		@Override
		public Boolean doInBackground() {
			ModManager.debugLogger.writeMessage("Starting the background thread for ModInstallWindow");
			ModManager.debugLogger.writeMessage("Checking for DLC Bypass.");
			if (!ModManager.hasKnownDLCBypass(bioGameDir)) {
				ModManager.debugLogger.writeMessage("No DLC bypass detected, installing LauncherWV.exe...");
				if (!ModManager.installLauncherWV(bioGameDir)) {
					ModManager.debugLogger.writeError("LauncherWV failed to install");
				}
			}

			if (precheckGameDB(jobs)) {
				ModManager.debugLogger.writeMessage("Precheck DB method has returned true, indicating user wants to open repair DB and cancel mod");
				return false;
			} else {
				ModManager.debugLogger.writeMessage("Precheck DB method has returned false, everything is OK and mod install will continue");
			}

			ModManager.debugLogger.writeMessage("Processing jobs in mod queue.");
			for (ModJob job : jobs) {
				if (installCancelled) {
					return false;
				}
				boolean result = false;
				switch (job.getJobType()) {
				case ModJob.DLC:
					result = processDLCJob(job);
					break;
				case ModJob.BASEGAME:
					result = processBasegameJob(job);
					break;
				case ModJob.CUSTOMDLC:
					result = processCustomDLCJob(job);
					break;
				}
				if (result) {
					completed++;
					ModManager.debugLogger.writeMessage("Successfully finished mod job");
				} else {
					ModManager.debugLogger.writeMessage("Mod job failed: " + job.getDLCFilePath());
					failedJobs.add(job.getDLCFilePath());
				}
				publish(Integer.toString(completed));
			}
			return true;
		}

		/**
		 * Checks the game DB for files in all jobs to see if any need to be
		 * added.
		 * 
		 * @param jobs
		 *            Jobs to check
		 * @return true if user clicks YES to open DB window, false if they
		 *         don't (or all is Ok)
		 */
		private boolean precheckGameDB(ModJob[] jobs) {
			File bgdir = new File(ModManager.appendSlash(bioGameDir));
			String me3dir = ModManager.appendSlash(bgdir.getParent());

			publish("Checking game database for files that need to be backed up");
			if (bghDB == null) {
				publish("Loading game repair database");
				bghDB = new BasegameHashDB(null, me3dir, false);
			}
			for (ModJob job : jobs) {
				publish("Checking GDB: " + job.getJobName());
				if (job.getJobType() == ModJob.BASEGAME) {
					//BGDB files are required
					ArrayList<String> filesToReplace = job.getFilesToReplaceTargets();
					int numFilesToReplace = filesToReplace.size();
					for (int i = 0; i < numFilesToReplace; i++) {
						String fileToReplace = filesToReplace.get(i);

						File basegamefile = new File(me3dir + fileToReplace);

						String relative = ResourceUtils.getRelativePath(basegamefile.getAbsolutePath(), me3dir, File.separator);
						RepairFileInfo rfi = bghDB.getFileInfo(relative);
						if (rfi == null) {
							ModManager.debugLogger.writeMessage("File not in GameDB, showing prompt: " + relative);
							// file is missing. Basegame DB likely hasn't been made
							int reply = JOptionPane
									.showConfirmDialog(
											null,
											"<html>"
													+ relative
													+ " is not in the game repair database.<br>In order to restore basegame files and unpacked DLC files this database needs to be created or updated.<br>Open the database window?</html>",
											"Mod Installation Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (reply == JOptionPane.NO_OPTION) {
								return false;
							} else {
								return true;
							}
						}
					}
				} else {
					//DLC files are not required... unless all are present
					ArrayList<String> filesToReplace = job.getFilesToReplaceTargets();
					ArrayList<String> filesToRemove = job.getFilesToRemoveTargets();
					int numFilesToReplace = filesToReplace.size();
					boolean fileIsMissing = false;
					//Check for files to replace not being present for backup
					for (int i = 0; i < numFilesToReplace; i++) {
						String fileToReplace = filesToReplace.get(i);
						File unpackeddlcfile = new File(me3dir + fileToReplace);
						if (!unpackeddlcfile.exists()) {
							fileIsMissing = true;
							ModManager.debugLogger.writeMessage("Game DB: unpacked DLC file not present. DLC is assumed to still be in SFAR: "
									+ job.getJobName());
							break;
						}
					}

					//Check for files to remove not being present for backup
					for (String removeFile : filesToRemove) {
						File unpackeddlcfile = new File(me3dir + removeFile);
						if (!unpackeddlcfile.exists()) {
							fileIsMissing = true;
							ModManager.debugLogger.writeMessage("Game DB: unpacked DLC file not present. DLC is assumed to still be in SFAR: "
									+ job.getJobName());
							break;
						}
					}

					if (fileIsMissing) {
						continue;
					}

					//DLC appears unpacked					
					for (int i = 0; i < numFilesToReplace; i++) {
						String fileToReplace = filesToReplace.get(i);
						File unpackeddlcfile = new File(me3dir + fileToReplace);

						//check if in GDB
						String relative = ResourceUtils.getRelativePath(unpackeddlcfile.getAbsolutePath(), me3dir, File.separator);
						RepairFileInfo rfi = bghDB.getFileInfo(relative);
						if (rfi == null) {
							// file is missing. Basegame DB likely hasn't been made
							int reply = JOptionPane
									.showConfirmDialog(
											null,
											"<html>One or more of the files this mod is installing is not in the game repair database.<br>In order to restore game files this database needs to be created or updated.<br>Open the database window?</html>",
											"Mod Installation Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
							if (reply == JOptionPane.NO_OPTION) {
								return false;
							} else {
								return true;
							}
						}
					}
				}
			}
			return false;
		}

		private boolean processBasegameJob(ModJob job) {
			ModManager.debugLogger.writeMessage("===Processing a basegame job===");
			publish("Processing basegame files...");
			File bgdir = new File(ModManager.appendSlash(bioGameDir));
			String me3dir = ModManager.appendSlash(bgdir.getParent());
			// Make backup folder if it doesn't exist
			String backupfolderpath = me3dir.toString() + "cmmbackup\\";
			File cmmbackupfolder = new File(backupfolderpath);
			cmmbackupfolder.mkdirs();
			ModManager.debugLogger.writeMessage("Basegame backup directory should have been created if it does not exist already.");
			// Prep replacement job
			ArrayList<String> filesToReplace = job.getFilesToReplaceTargets();
			ArrayList<String> newFiles = job.getFilesToReplace();
			int numFilesToReplace = filesToReplace.size();
			ModManager.debugLogger.writeMessage("Number of files to replace in the basegame: " + numFilesToReplace);
			for (int i = 0; i < numFilesToReplace; i++) {
				String fileToReplace = filesToReplace.get(i);
				String newFile = newFiles.get(i);

				boolean shouldContinue = checkBackupAndHash(me3dir, fileToReplace, job);
				if (!shouldContinue) {
					installCancelled = true;
					return false;
				}

				// install file.
				File unpacked = new File(me3dir + fileToReplace);
				Path originalpath = Paths.get(unpacked.toString());
				try {
					ModManager.debugLogger.writeMessage("Installing mod file: " + newFile);
					publish(ModType.BASEGAME + ": Installing " + FilenameUtils.getName(newFile));
					Path newfilepath = Paths.get(newFile);
					Files.copy(newfilepath, originalpath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Installed mod file: " + newFile);
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
					return false;
				}
			}

			//REMOVAL TASKS
			ArrayList<String> filesToRemove = job.getFilesToRemoveTargets();
			ModManager.debugLogger.writeMessage("Number of files to remove: " + filesToRemove.size());
			for (String fileToRemove : filesToRemove) {
				boolean userCanceled = checkBackupAndHash(me3dir, fileToRemove, job);
				if (userCanceled) {
					installCancelled = true;
					return false;
				}

				// install file.
				File unpacked = new File(me3dir + fileToRemove);
				Path originalpath = Paths.get(unpacked.toString());
				try {
					ModManager.debugLogger.writeMessage("Removing file: " + unpacked);
					publish(job.getJobName() + ": Removing " + FilenameUtils.getName(unpacked.getAbsolutePath()));
					Files.delete(originalpath);
					ModManager.debugLogger.writeMessage("Deleted mod file: " + unpacked);
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
					return false;
				}
			}

			return true;
		}

		/**
		 * Processes a DLC job. Installs via SFAR (injection) if files in their
		 * unpacked location do not exist.
		 * 
		 * @param job
		 *            Job to install
		 * @return true if successful, false otherwise
		 */
		private boolean processDLCJob(ModJob job) {
			ModManager.debugLogger.writeMessage("===Processing a dlc job: "+job.getJobName()+"===");

			File bgdir = new File(ModManager.appendSlash(bioGameDir));
			String me3dir = ModManager.appendSlash(bgdir.getParent());

			//Check for files to replace not being present
			for (int i = 0; i < job.filesToReplace.size(); i++) {
				String fileToReplace = job.filesToReplace.get(i);
				File unpackeddlcfile = new File(me3dir + fileToReplace);
				if (!unpackeddlcfile.exists()) {
					ModManager.debugLogger.writeMessage("Game DB: unpacked DLC file not present. DLC job will use SFAR method: " + job.getJobName());
					return processSFARDLCJob(job);
				}
			}
			
			//Check that the default.sfar file is not smaller than the normal size (typically means unpacked)
			String sfarName = "Default.sfar";
			if (job.TESTPATCH) {
				sfarName = "Patch_001.sfar";
			}
			long knownsfarsize = ModType.getSizesMap().get(job.getJobName());
			String sfarPath = ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName;
			File sfarFile = new File(sfarPath);
			if (sfarFile.exists()) {
				if (sfarFile.length() >= knownsfarsize) {
					ModManager.debugLogger.writeMessage("SFAR is same or larger in bytes than the known original. Likely is the vanilla one, or has been modified, but not unpacked. Using the SFAR method: "+job.getJobName());
					return processSFARDLCJob(job);
				}
			} else {
				ModManager.debugLogger.writeError("SFAR doesn't exist for unpacked DLC... interesting... "+sfarPath);
			}

			//We don't need to check for files to remove, as if it this is an unpacked DLC we can just skip the file. If it is missing in the DLC then there would be nothing we can do.
/*			for (int i = 0; i < job.getFilesToRemove().size(); i++) {
				String fileToRemove = job.getFilesToRemove().get(i);
				File unpackeddlcfile = new File(me3dir + fileToRemove);
				if (!unpackeddlcfile.exists()) {
					ModManager.debugLogger.writeMessage("Game DB: unpacked DLC file not present. DLC job will use SFAR method: " + job.getJobName());
					return processSFARDLCJob(job);
				}
			}*/

			//UNPACKED DLC METHOD
			return updateUnpackedDLC(job);
		}

		/**
		 * Processes a DLC job using the unpacked DLC method
		 * 
		 * @param job
		 *            job to conduct
		 * @return true is successful, false otherwise
		 */
		private boolean updateUnpackedDLC(ModJob job) {

			publish(job.getJobName() + ": Installing using unpacked DLC method");
			File bgdir = new File(ModManager.appendSlash(bioGameDir));
			String me3dir = ModManager.appendSlash(bgdir.getParent());

			// Make backup folder if it doesn't exist

			//REPLACEMENT TASKS
			ArrayList<String> filesToReplace = job.getFilesToReplaceTargets();
			ArrayList<String> newFiles = job.getFilesToReplace();
			int numFilesToReplace = filesToReplace.size();
			ModManager.debugLogger.writeMessage("Number of files to replace in the DLC: " + numFilesToReplace);
			for (int i = 0; i < numFilesToReplace; i++) {
				String fileToReplace = filesToReplace.get(i);
				String newFile = newFiles.get(i);

				boolean shouldContinue = checkBackupAndHash(me3dir, fileToReplace, job);
				if (!shouldContinue) {
					installCancelled = true;
					return false;
				}

				// install file.
				File unpacked = new File(me3dir + fileToReplace);
				Path originalpath = Paths.get(unpacked.toString());
				try {
					ModManager.debugLogger.writeMessage("Installing mod file: " + newFile);
					publish(job.getJobName() + ": Installing " + FilenameUtils.getName(newFile));
					Path newfilepath = Paths.get(newFile);
					Files.copy(newfilepath, originalpath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Installed mod file: " + newFile);
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
					return false;
				}
			}

			//ADD TASKS
			ArrayList<String> filesToAdd = job.getFilesToAdd();
			ArrayList<String> filesToAddTargets = job.getFilesToAddTargets();
			int numFilesToAdd = filesToAddTargets.size();
			ModManager.debugLogger.writeMessage("Number of files to add in the DLC: " + numFilesToAdd);
			for (int i = 0; i < numFilesToAdd; i++) {
				String addFile = filesToAdd.get(i);
				String addFileTarget = filesToAddTargets.get(i);

				// install file.
				File unpacked = new File(me3dir + addFileTarget);
				Path originalpath = Paths.get(unpacked.toString());
				try {
					ModManager.debugLogger.writeMessage("Adding new mod file: " + addFile);
					publish(job.getJobName() + ": Adding new file " + FilenameUtils.getName(addFile));
					Path newfilepath = Paths.get(addFile);
					Files.copy(newfilepath, originalpath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Added mod file: " + addFile);
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
					return false;
				}
			}

			//REMOVAL TASKS
			ArrayList<String> filesToRemove = job.getFilesToRemoveTargets();
			ModManager.debugLogger.writeMessage("Number of files to remove: " + filesToRemove.size());
			for (String fileToRemove : filesToRemove) {
				boolean shouldContinue = checkBackupAndHash(me3dir, fileToRemove, job);
				if (!shouldContinue) {
					installCancelled = true;
					return false;
				}

				// install file.
				File unpacked = new File(me3dir + fileToRemove);
				Path originalpath = Paths.get(unpacked.toString());
				if (unpacked.exists()) {
					try {
						ModManager.debugLogger.writeMessage("Removing file: " + unpacked);
						publish(job.getJobName() + ": Removing " + FilenameUtils.getName(unpacked.getAbsolutePath()));
						Files.delete(originalpath);
						ModManager.debugLogger.writeMessage("Deleted mod file: " + unpacked);
					} catch (IOException e) {
						ModManager.debugLogger.writeException(e);
						return false;
					}
				} else {
					ModManager.debugLogger.writeMessage(unpacked+" was to be removed but does not exist, skipping");
					publish(job.getJobName() + ": "+FilenameUtils.getName(unpacked.getAbsolutePath())+" not present for removal, skipping");
				}
			}
			return true;
		}

		/**
		 * Checks for a backup file and the hash of the original one in the DB
		 * to make sure they match if no backup is found.
		 * 
		 * @param me3dir
		 *            ME3 DIR to use as a base
		 * @param fileToReplace
		 *            file that will be replaced, as a relative path
		 * @param job
		 *            job (for outputting name)
		 * @return true if file is backed up (and hashed OK), false if it is not
		 *         backed up/error/hashfail
		 */
		private boolean checkBackupAndHash(String me3dir, String fileToReplace, ModJob job) {
			String backupfolderpath = me3dir.toString() + "cmmbackup\\";
			File cmmbackupfolder = new File(backupfolderpath);
			cmmbackupfolder.mkdirs();
			ModManager.debugLogger.writeMessage("Backup directory should have been created if it does not exist already.");

			// Check for backup
			File unpacked = new File(me3dir + fileToReplace);
			File backupfile = new File(backupfolderpath + fileToReplace);
			Path originalpath = Paths.get(unpacked.toString());

			ModManager.debugLogger.writeMessage("Checking for backup file at " + backupfile);
			if (!backupfile.exists()) {
				// backup the file
				if (bghDB == null) {
					publish(job.getJobName() + ": Loading repair database");
					bghDB = new BasegameHashDB(null, me3dir, false);
				}
				Path backuppath = Paths.get(backupfile.toString());
				backupfile.getParentFile().mkdirs();

				String relative = ResourceUtils.getRelativePath(unpacked.getAbsolutePath(), me3dir, File.separator);
				RepairFileInfo rfi = bghDB.getFileInfo(relative);
				// validate file to backup.
				boolean justInstall = false;
				boolean installAndUpdate = false;
				if (rfi == null) {
					int reply = JOptionPane
							.showOptionDialog(
									null,
									"<html>The file:<br>"
											+ relative
											+ "<br>is not in the repair database. "
											+ "Installing/Removing this file may overwrite your default setup if you restore and have custom mods like texture swaps installed.<br></html>",
									"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, new String[] {
											"Add to DB and install", "Install file", "Cancel mod installation" }, "default");
					switch (reply) {
					case JOptionPane.CANCEL_OPTION:
						installCancelled = true;
						return false;
					case JOptionPane.NO_OPTION:
						justInstall = true;
						break;
					case JOptionPane.YES_OPTION:
						installAndUpdate = true;
						break;
					}
				}

				// Check filesize
				if (!justInstall && !installAndUpdate) {
					if (unpacked.length() != rfi.filesize) {
						// MISMATCH!
						int reply = JOptionPane
								.showOptionDialog(
										null,
										"<html>The filesize of the file:<br>"
												+ relative
												+ "<br>does not match the one stored in the repair game database.<br>"
												+ unpacked.length()
												+ " bytes (installed) vs "
												+ rfi.filesize
												+ " bytes (database)<br><br>"
												+ "This file could be corrupted or modified since the database was created.<br>"
												+ "Backing up this file may overwrite your default setup if you use custom mods like texture swaps when you restore.<br></html>",
										"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
										new String[] { "Backup and update DB", "Backup this file", "Cancel mod installation" }, "default");
						switch (reply) {
						case JOptionPane.CANCEL_OPTION:
							return false;
						case JOptionPane.NO_OPTION:
							justInstall = true;
							break;
						case JOptionPane.YES_OPTION:
							installAndUpdate = true;
							break;
						}
					}
				}

				// Check hash
				if (!justInstall && !installAndUpdate) {
					// this is outside of the previous if statement as the
					// previous one could set the restoreAnyways variable
					// again.
					try {
						if (!MD5Checksum.getMD5Checksum(unpacked.getAbsolutePath()).equals(rfi.md5)) {
							int reply = JOptionPane
									.showOptionDialog(
											null,
											"<html>The hash of the file:<br>"
													+ relative
													+ "<br>does not match the one stored in the repair game database.<br>"
													+ "This file has changed since the database was created.<br>"
													+ "Backing up this file may overwrite your default setup if you use custom mods like texture swaps when restoring.<br></html>",
											"Backing Up Unverified File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null,
											new String[] { "Backup and update DB", "Backup this file", "Cancel mod installation" }, "default");
							switch (reply) {
							case JOptionPane.CANCEL_OPTION:
								return false;
							case JOptionPane.NO_OPTION:
								justInstall = true;
								break;
							case JOptionPane.YES_OPTION:
								installAndUpdate = true;
								break;
							}
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						ModManager.debugLogger.writeException(e);
					}
				}

				try {
					// backup and then copy file
					Files.copy(originalpath, backuppath);
					ModManager.debugLogger.writeMessage("Backed up " + fileToReplace);
					if (installAndUpdate) {
						ArrayList<File> updateFile = new ArrayList<File>();
						updateFile.add(unpacked);
						bghDB.updateDB(updateFile);
					}
				} catch (IOException e) {
					ModManager.debugLogger.writeErrorWithException("ERROR BACKING UP FILE:", e);
					return false;
				}
			}
			return true;
		}

		/**
		 * Processes a DLC job using the SFAR method
		 * 
		 * @param job
		 *            job to conduct
		 * @return true if success, false otherwise
		 */
		public boolean processSFARDLCJob(ModJob job) {
			boolean result = true;

			//REPLACE JOB
			if (job.getFilesToReplaceTargets().size() > 0) {

				ArrayList<String> commandBuilder = new ArrayList<String>();
				commandBuilder.add(ModManager.getME3ExplorerEXEDirectory(true) + "ME3Explorer.exe");
				commandBuilder.add("-dlcinject");
				String sfarName = "Default.sfar";
				if (job.TESTPATCH) {
					sfarName = "Patch_001.sfar";
				}
				File sfarFile = new File(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				if (!sfarFile.exists()) {
					//missing module
					ModManager.debugLogger.writeMessage("Missing DLC Module, skipping: " + sfarFile.getAbsolutePath());
					publish("DLC is missing: " + job.getDLCFilePath());
					return true;
				}
				commandBuilder.add(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				ArrayList<String> filesToReplace = job.getFilesToReplaceTargets();
				ArrayList<String> newFiles = job.getFilesToReplace();
				ModManager.debugLogger.writeMessage("Number of files to replace: " + filesToReplace.size());

				publish("Updating " + filesToReplace.size() + " files in " + job.getJobName());
				for (int i = 0; i < filesToReplace.size(); i++) {
					commandBuilder.add(filesToReplace.get(i));
					commandBuilder.add(newFiles.get(i));
					// System.out.println("adding file to command");
				}

				// System.out.println("Building command");
				String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
				// Debug stuff
				StringBuilder sb = new StringBuilder();
				for (String arg : command) {
					sb.append(arg + " ");
				}
				ModManager.debugLogger.writeMessage("Executing injection command: " + sb.toString());
				Process p = null;
				int returncode = 1;
				try {
					ProcessBuilder pb = new ProcessBuilder(command);
					ModManager.debugLogger.writeMessage("Executing process for DLC Injection Job.");
					// p = Runtime.getRuntime().exec(command);
					p = pb.start();
					ModManager.debugLogger.writeMessage("Executed command, waiting...");
					returncode = p.waitFor();
				} catch (IOException | InterruptedException e) {
					ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
					e.printStackTrace();
					return false;
				}

				ModManager.debugLogger.writeMessage("processDLCJob RETURN VAL: " + (p != null && returncode == 0));
				result = (p != null && returncode == 0) && result;
			}

			//ADD FILE TASK
			if (job.getFilesToAdd().size() > 0) {
				ArrayList<String> commandBuilder = new ArrayList<String>();
				commandBuilder.add(ModManager.getME3ExplorerEXEDirectory(true) + "ME3Explorer.exe");
				commandBuilder.add("-dlcaddfiles");
				String sfarName = "Default.sfar";
				if (job.TESTPATCH) {
					sfarName = "Patch_001.sfar";
				}
				File sfarFile = new File(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				if (!sfarFile.exists()) {
					//missing module
					ModManager.debugLogger.writeMessage("Missing DLC Module, skipping: " + sfarFile.getAbsolutePath());
					publish("DLC is missing: " + job.getDLCFilePath());
					return true;
				}
				commandBuilder.add(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				ArrayList<String> filesToAdd = job.getFilesToAdd();
				ArrayList<String> filesToAddTargets = job.getFilesToAddTargets();
				ModManager.debugLogger.writeMessage("Number of files to add: " + filesToAdd.size());

				publish("Adding " + filesToAdd.size() + " files to " + job.getJobName());
				for (int i = 0; i < filesToAdd.size(); i++) {
					commandBuilder.add(filesToAddTargets.get(i));
					commandBuilder.add(filesToAdd.get(i));
				}

				String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
				StringBuilder sb = new StringBuilder();
				for (String arg : command) {
					sb.append(arg + " ");
				}
				ModManager.debugLogger.writeMessage("Executing injection command: " + sb.toString());
				Process p = null;
				int returncode = 1;
				try {
					ProcessBuilder pb = new ProcessBuilder(command);
					ModManager.debugLogger.writeMessage("Executing process for DLC Injection Job.");
					p = pb.start();
					ModManager.debugLogger.writeMessage("Executed command, waiting...");
					returncode = p.waitFor();
				} catch (IOException | InterruptedException e) {
					ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
					e.printStackTrace();
					return false;
				}

				ModManager.debugLogger.writeMessage("processDLCJob RETURN VAL: " + (p != null && returncode == 0));
				result = (p != null && returncode == 0) && result;
			}

			//REMOVE FILE TASK
			if (job.getFilesToRemoveTargets().size() > 0) {
				ArrayList<String> commandBuilder = new ArrayList<String>();
				commandBuilder.add(ModManager.getME3ExplorerEXEDirectory(true) + "ME3Explorer.exe");
				commandBuilder.add("-dlcremovefiles");
				String sfarName = "Default.sfar";
				if (job.TESTPATCH) {
					sfarName = "Patch_001.sfar";
				}
				File sfarFile = new File(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				if (!sfarFile.exists()) {
					//missing module
					ModManager.debugLogger.writeMessage("Missing DLC Module, skipping: " + sfarFile.getAbsolutePath());
					publish("DLC is missing: " + job.getDLCFilePath());
					return true;
				}
				commandBuilder.add(ModManager.appendSlash(bioGameDir) + ModManager.appendSlash(job.getDLCFilePath()) + sfarName);
				ArrayList<String> filesToRemove = job.getFilesToRemoveTargets();
				ModManager.debugLogger.writeMessage("Number of files to remove: " + filesToRemove.size());

				publish("Removing " + filesToRemove.size() + " files from " + job.getJobName() + ", this may take some time...");
				for (int i = 0; i < filesToRemove.size(); i++) {
					commandBuilder.add(filesToRemove.get(i));
				}

				String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
				StringBuilder sb = new StringBuilder();
				for (String arg : command) {
					sb.append(arg + " ");
				}
				ModManager.debugLogger.writeMessage("Executing removal command: " + sb.toString());
				Process p = null;
				int returncode = 1;
				try {
					ProcessBuilder pb = new ProcessBuilder(command);
					// p = Runtime.getRuntime().exec(command);
					p = pb.start();
					ModManager.debugLogger.writeMessage("Executed command, waiting...");
					returncode = p.waitFor();
				} catch (IOException | InterruptedException e) {
					ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
					e.printStackTrace();
					return false;
				}

				ModManager.debugLogger.writeMessage("The process has exited.");
				result = (p != null && returncode == 0) && result;
			}
			return result;
		}

		/**
		 * Copies the CUSTOMDLC folder to the specified directory
		 * 
		 * @param job
		 *            job describing the customDLC job
		 * @return true if successful, false otherwise.
		 */
		private boolean processCustomDLCJob(ModJob job) {
			ModManager.debugLogger.writeMessage("===Processing a customdlc job===");

			File dlcdir = new File(ModManager.appendSlash(bioGameDir) + "DLC" + File.separator);

			for (int i = 0; i < job.getFilesToReplaceTargets().size(); i++) {
				String fileDestination = dlcdir + job.getFilesToReplaceTargets().get(i);
				String fileSource = job.getFilesToReplace().get(i);
				// install file.
				try {
					ModManager.debugLogger.writeMessage("Processing CustomDLC Job.");
					publish(ModType.CUSTOMDLC + ": Installing " + FilenameUtils.getName(fileSource));
					Path sourcePath = Paths.get(fileSource);
					Path destPath = Paths.get(fileDestination);
					File dest = new File(fileDestination);
					dest.mkdirs();
					Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
					ModManager.debugLogger.writeMessage("Installed mod file: " + dest.getAbsolutePath());
				} catch (IOException e) {
					ModManager.debugLogger.writeException(e);
					return false;
				}
			}
			return true;
		}

		@Override
		protected void process(List<String> updates) {
			// System.out.println("Restoring next DLC");
			for (String update : updates) {
				try {

					Integer.parseInt(update); // see if we got a number. if we
												// did that means we should
												// update the bar
					ModManager.debugLogger.writeMessage("Job completed with code "+update);
					if (numjobs != 0) {
						progressBar.setValue((int) (((float) completed / numjobs) * 100));
					}
				} catch (NumberFormatException e) {
					// this is not a progress update, it's a string update
					addToQueue(update);
				}
			}

		}

		@Override
		protected void done() {
			boolean success = false;
			boolean hasException = false;
			try {
				success = get();
			} catch (InterruptedException e) {
				ModManager.debugLogger.writeException(e);
				hasException = true;
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeException(e);
				hasException = true;
			}

			if (success) {
				if (numjobs != completed) {
					// failed something
					StringBuilder sb = new StringBuilder();
					sb.append("Failed to process mod installation.\nSome parts of the install may have succeeded.\nTurn on debugging via Help>About and check the log file.");
					callingWindow.labelStatus.setText("Failed to install at least 1 part of mod");
					JOptionPane.showMessageDialog(null, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
					ModManager.debugLogger.writeMessage(mod.getModName() + " failed to fully install.");
				} else {
					// we're good
					callingWindow.labelStatus.setText(" " + mod.getModName() + " installed");
				}
			} else {
				if (!hasException) {
					ModManager.debugLogger.writeMessage("Installation canceled by user because basegame database update is required.");
					bghDB.shutdownDB();
					bghDB = null;
					System.gc();//force shutdown the old DB
					File bgdir = new File(ModManager.appendSlash(bioGameDir));
					String me3dir = ModManager.appendSlash(bgdir.getParent());
					BasegameHashDB bghDB = new BasegameHashDB(ModManagerWindow.ACTIVE_WINDOW, me3dir, true);
					callingWindow.labelStatus.setText("Mod install cancelled.");
					dispose();
					bghDB.setVisible(true);
				} else {
					ModManager.debugLogger.writeError("Mod Injection thread encountered an error. See the exception above.");
				}
			}
			finishInstall();
			return;
		}
	}

	protected void finishInstall() {
		ModManager.debugLogger.writeMessage("Finished installing mod.");
		dispose();
	}

	public void addToQueue(String newLine) {
		for (int i = consoleQueue.length - 1; i >= 1; i--) {
			consoleQueue[i] = consoleQueue[i - 1];
		}
		consoleQueue[0] = newLine;
		updateInfo();
	}

	public String getConsoleString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < consoleQueue.length; i++) {
			sb.append((consoleQueue[i] != null) ? consoleQueue[i] : "");
			if (i < consoleQueue.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}

	private void updateInfo() {
		consoleArea.setText(getConsoleString());
	}
}
