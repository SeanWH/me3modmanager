package com.me3tweaks.modmanager;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

@SuppressWarnings("serial")
public class OptionsWindow extends JDialog {
	JCheckBox loggingMode;
	private JCheckBox autoInjectKeybindsModMaker;
	private JCheckBox autoUpdateModManager;
	private JCheckBox autoUpdateMods;
	private JCheckBox autoApplyMixins;
	private JCheckBox autoUpdateME3Explorer;
	private JCheckBox skipUpdate;
	private JCheckBox autoTocUnpackedOnInstall;

	public OptionsWindow(JFrame callingWindow) {
		setupWindow();
		this.setLocationRelativeTo(callingWindow);
		this.setVisible(true);
	}

	private void setupWindow() {
		this.setTitle("Mod Manager Options");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		this.setIconImages(ModManager.ICONS);

		JPanel optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.PAGE_AXIS));

		loggingMode = new JCheckBox("Write debugging log to file");
		loggingMode.setToolTipText(
				"<html>Turning this on will write a session log to me3cmm_last_run_log.txt next to ME3CMM.exe.<br>This log can be used by FemShep to help diagnose issues with Mod Manager.<br>It will also tell you why mods aren't loading and other things.</html>");
		loggingMode.setSelected(ModManager.logging);
		loggingMode.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);

					if (loggingMode.isSelected()) {
						ini.put("Settings", "logging_mode", "1");
						JOptionPane.showMessageDialog(null,
								"<html>Logs will be written to a file named " + DebugLogger.LOGGING_FILENAME
										+ ", next to the ME3CMM.exe file.<br>This log will help you debug mods that fail to show up in the list and can be used by FemShep to fix problems.<br>Mod Manager must be fully restarted for logging to start.</html>",
								"Logging Mode", JOptionPane.INFORMATION_MESSAGE);
					} else {
						ini.put("Settings", "logging_mode", "0");
						ModManager.logging = false;
					}
					ini.store();
				} catch (InvalidFileFormatException error) {
					error.printStackTrace();
				} catch (IOException error) {
					ModManager.debugLogger.writeMessage("Settings file encountered an I/O error while attempting to write it. Settings not saved.");
				}
			}
		});

		autoInjectKeybindsModMaker = new JCheckBox("Auto-inject custom keybinds into ModMaker mods");
		autoInjectKeybindsModMaker.setToolTipText(
				"<html>If you use a custom keybinds file (BioInput.xml) and place it in the data/override directory,<br>at the end of compiling ModMaker mods Mod Manager will auto-inject them for you.</html>");
		autoInjectKeybindsModMaker.setSelected(ModManager.AUTO_INJECT_KEYBINDS);
		autoInjectKeybindsModMaker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					if (autoInjectKeybindsModMaker.isSelected()) {
						ini.put("Settings", "autoinjectkeybinds", "1");
					} else {
						ini.put("Settings", "autoinjectkeybinds", "0");
					}
					ModManager.AUTO_INJECT_KEYBINDS = autoInjectKeybindsModMaker.isSelected();
					ini.store();
				} catch (InvalidFileFormatException error) {
					error.printStackTrace();
				} catch (IOException error) {
					ModManager.debugLogger.writeMessage("Settings file encountered an I/O error while attempting to write it. Settings not saved.");
				}
			}
		});

		autoUpdateME3Explorer = new JCheckBox("Auto-download required ME3Explorer updates");
		autoUpdateME3Explorer.setToolTipText("<html>Mod Manager requires specific versions of ME3Explorer and will not work without them</html>");
		autoUpdateME3Explorer.setSelected(ModManager.AUTO_UPDATE_ME3EXPLORER);
		autoUpdateME3Explorer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					if (autoUpdateME3Explorer.isSelected()) {
						ini.put("Settings", "autodownloadme3explorer", "1");
					} else {
						ini.put("Settings", "autodownloadme3explorer", "0");
					}
					ModManager.AUTO_UPDATE_ME3EXPLORER = autoUpdateME3Explorer.isSelected();
					ini.store();
				} catch (InvalidFileFormatException error) {
					error.printStackTrace();
				} catch (IOException error) {
					ModManager.debugLogger.writeMessage("Settings file encountered an I/O error while attempting to write it. Settings not saved.");
				}
			}
		});

		autoUpdateModManager = new JCheckBox("Check for updates at startup");
		autoUpdateModManager.setToolTipText("<html>Keep Mod Manager up to date by checking for updates at startup</html>");
		autoUpdateModManager.setSelected(ModManager.AUTO_UPDATE_MOD_MANAGER);
		autoUpdateModManager.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					if (autoUpdateModManager.isSelected()) {
						ini.put("Settings", "checkforupdates", "1");
					} else {
						ini.put("Settings", "checkforupdates", "0");
					}
					ModManager.AUTO_UPDATE_MOD_MANAGER = autoUpdateModManager.isSelected();
					ini.store();
				} catch (InvalidFileFormatException error) {
					error.printStackTrace();
				} catch (IOException error) {
					ModManager.debugLogger.writeMessage("Settings file encountered an I/O error while attempting to write it. Settings not saved.");
				}
			}
		});

		autoUpdateMods = new JCheckBox("Keep mods up to date from ME3Tweaks.com");
		autoUpdateMods.setToolTipText("<html>Checks every 3 days for updates to mods from ME3Tweaks.com</html>");
		autoUpdateMods.setSelected(ModManager.AUTO_UPDATE_MOD_MANAGER);
		autoUpdateMods.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					ini.put("Settings", "autoupdatemods", autoUpdateMods.isSelected() ? "true" : "false");
					ini.put("Settings", "declinedautoupdate", autoUpdateMods.isSelected() ? "false" : "true");
					ini.store();
					ModManager.ASKED_FOR_AUTO_UPDATE = true;
					ModManager.AUTO_UPDATE_MODS = autoUpdateMods.isSelected();
				} catch (InvalidFileFormatException x) {
					x.printStackTrace();
				} catch (IOException x) {
					ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", x);
				}
			}
		});

		autoApplyMixins = new JCheckBox("<html>Automatically apply recommended MixIns to ModMaker mods</html>");
		autoApplyMixins.setToolTipText("<html>Automatically accepts recommended MixIns when compiling a ModMaker mod</html>");
		autoApplyMixins.setSelected(ModManager.AUTO_APPLY_MODMAKER_MIXINS);
		autoApplyMixins.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					ini.put("Settings", "autoinstallmixins", autoApplyMixins.isSelected() ? "1" : "0");
					ini.store();
					ModManager.AUTO_APPLY_MODMAKER_MIXINS = autoApplyMixins.isSelected();
				} catch (InvalidFileFormatException x) {
					x.printStackTrace();
				} catch (IOException x) {
					ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", x);
				}
			}
		});

		if (ModManager.SKIP_UPDATES_UNTIL_BUILD > ModManager.BUILD_NUMBER) {
			skipUpdate = new JCheckBox("Only show update if it is build " + ModManager.SKIP_UPDATES_UNTIL_BUILD + " or higher");
			skipUpdate.setToolTipText("<html>Automatically accepts recommended MixIns when compiling a ModMaker mod</html>");
			skipUpdate.setSelected(ModManager.SKIP_UPDATES_UNTIL_BUILD > ModManager.BUILD_NUMBER);
			skipUpdate.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					Wini ini;
					try {
						File settings = new File(ModManager.SETTINGS_FILENAME);
						if (!settings.exists())
							settings.createNewFile();
						ini = new Wini(settings);
						if (skipUpdate.isSelected()) {
							ModManager.debugLogger
									.writeMessage("OPTIONS: User is skipping (has already skipped to see this checkbox) the next update, build " + (ModManager.BUILD_NUMBER + 1));
							ini.put("Settings", "nextupdatedialogbuild", ModManager.BUILD_NUMBER + 1);
							ModManager.SKIP_UPDATES_UNTIL_BUILD = ModManager.BUILD_NUMBER + 1;
						} else {
							ModManager.debugLogger.writeMessage("OPTIONS: User is turning off the next skipped update");
							ini.remove("Settings", "nextupdatedialogbuild");
							ModManager.SKIP_UPDATES_UNTIL_BUILD = 0;
						}
						ini.store();
					} catch (InvalidFileFormatException ex) {
						ex.printStackTrace();
					} catch (IOException ex) {
						ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", ex);
					}
				}
			});
		}

		autoTocUnpackedOnInstall = new JCheckBox("Run AutoTOC on modified modules after install");
		autoTocUnpackedOnInstall.setToolTipText(
				"<html>After mod install, run AutoTOC on the game's modified modules.<br>If you use mods other than Mod Manager mods, you will need to use this option or the game will crash.<br>This does not work with SFAR injection mods, DLC must be unpacked.<br>Mixing mods outside of Mod Manager is not supported by FemShep. This option is provided as a convenience to ME3Explorer users.</html>");
		autoTocUnpackedOnInstall.setSelected(ModManager.RUN_AUTOTOC_AFTER_MOD_INSTALL);
		autoTocUnpackedOnInstall.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Wini ini;
				try {
					File settings = new File(ModManager.SETTINGS_FILENAME);
					if (!settings.exists())
						settings.createNewFile();
					ini = new Wini(settings);
					ModManager.debugLogger.writeMessage("User changing run autotoc post install to " + autoTocUnpackedOnInstall.isSelected());
					ini.put("Settings", "runautotocpostinstall", autoTocUnpackedOnInstall.isSelected() ? "1" : "0");
					ModManager.RUN_AUTOTOC_AFTER_MOD_INSTALL = autoTocUnpackedOnInstall.isSelected();
					if (ModManager.RUN_AUTOTOC_AFTER_MOD_INSTALL) {
						JOptionPane.showMessageDialog(OptionsWindow.this,
								"<html><div style=\"width: 300px\">Turning this on makes AutoTOC run after a mod installs. Modules (basegame and DLC) that are modified have their PCConsoleTOC files updated to reflect the newly inserted files, while keeping the original entries. This only applies if a DLC is unpacked.<br>"
										+ "This will allow you to mix Mod Manager mods with non Mod Manager mods to some degree.<br><br>"
										+ "Mixing mods this way is not officially supported and this option is simply a convenience for ME3Explorer users.<br><br>"
										+ "Additionally you cannot mix mods that modify TESTPATCH (Patch_001.sfar) in this manner, as the DLC cannot be unpacked. You must use Mod Manager's Mod Merging utility.<br><br>"
										+ "This is an advanced, experimental feature. You should only turn this on if you know what you are doing.</div></html>",
								"Partially unsupported", JOptionPane.WARNING_MESSAGE);

					}
					ini.store();
				} catch (InvalidFileFormatException ex) {
					ex.printStackTrace();
				} catch (IOException ex) {
					ModManager.debugLogger.writeErrorWithException("Settings file encountered an I/O error while attempting to write it. Settings not saved.", ex);
				}
			}
		});

		optionsPanel.add(autoApplyMixins);
		optionsPanel.add(autoInjectKeybindsModMaker);
		optionsPanel.add(autoTocUnpackedOnInstall);
		optionsPanel.add(new JSeparator(JSeparator.HORIZONTAL));
		optionsPanel.add(loggingMode);
		optionsPanel.add(autoUpdateModManager);
		if (skipUpdate != null) {
			optionsPanel.add(skipUpdate);
		}
		optionsPanel.add(autoUpdateMods);
		optionsPanel.add(autoUpdateME3Explorer);
		optionsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		this.getContentPane().add(optionsPanel);
		this.pack();
	}
}
