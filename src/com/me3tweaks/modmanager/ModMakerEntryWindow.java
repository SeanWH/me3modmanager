package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

@SuppressWarnings("serial")
public class ModMakerEntryWindow extends JDialog implements ActionListener {
	private static final String ALL_LANG = "All languages";
	private static final String ENGLISH = "English";
	private static final String RUSSIAN = "Russian";
	private static final String SPANISH = "Spanish";
	private static final String POLISH = "Polish";
	private static final String FRENCH = "French";
	private static final String ITALIAN = "Italian";
	private static final String GERMAN = "German";
	private static final int DIALOG_WIDTH = 400;
	private static final int DIALOG_HEIGHT = 200;
	JLabel infoLabel;
	JButton downloadButton;
	JTextField codeField;
	String biogameDir;
	private JComboBox<String> languageChoices;
	private String[] languages = { ALL_LANG, ENGLISH, RUSSIAN, SPANISH, POLISH, FRENCH, ITALIAN, GERMAN };
	boolean hasDLCBypass = false;
	ModManagerWindow callingWindow;
	private JButton makeModButton;
	private JButton browseModsButton;

	public ModMakerEntryWindow(JFrame callingWindow, String biogameDir) {
		this.callingWindow = (ModManagerWindow) callingWindow;
		this.biogameDir = biogameDir;
		this.setTitle("ME3Tweaks ModMaker");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		boolean shouldshow = validateModMakerPrereqs();
		if (shouldshow) {
			setupWindow();
			this.setVisible(true);
			this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		} else {
			dispose();
		}

	}

	private void setupWindow() {
		JPanel modMakerPanel = new JPanel();
		modMakerPanel.setLayout(new BoxLayout(modMakerPanel, BoxLayout.Y_AXIS));
		JPanel infoPane = new JPanel();
		infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.LINE_AXIS));
		infoLabel = new JLabel(
				"<html>ME3Tweaks ModMaker allows you to easily create Mass Effect 3 mods in a simple to use interface.<br>Enter a download code to download and compile a mod.</html>");
		infoPane.add(Box.createHorizontalGlue());
		infoPane.add(infoLabel);
		infoPane.add(Box.createHorizontalGlue());
		modMakerPanel.add(infoPane);

		JPanel languageChoicesPanel = new JPanel();
		languageChoicesPanel.setLayout(new BoxLayout(languageChoicesPanel, BoxLayout.LINE_AXIS));
		languageChoicesPanel.setMaximumSize(new Dimension(550, 30));
		TitledBorder languageBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Languages to compile");
		languageChoicesPanel.setBorder(languageBorder);
		languageChoices = new JComboBox<String>(languages);
		languageChoicesPanel.add(languageChoices);
		modMakerPanel.add(languageChoicesPanel);

		//download panel
		JPanel codeDownloadPanel = new JPanel();
		codeDownloadPanel.setLayout(new BoxLayout(codeDownloadPanel, BoxLayout.LINE_AXIS));
		codeDownloadPanel.add(Box.createHorizontalGlue());

		codeField = new JTextField(6);
		//validation
		((AbstractDocument) codeField.getDocument()).setDocumentFilter(new DocumentFilter() {
			Pattern pattern = Pattern.compile("-{0,1}\\d+");

			@Override
			public void replace(FilterBypass arg0, int arg1, int arg2, String arg3, AttributeSet arg4) throws BadLocationException {
				String text = arg0.getDocument().getText(0, arg0.getDocument().getLength()) + arg3;
				Matcher matcher = pattern.matcher(text);
				if (!matcher.matches()) {
					return;
				}
				if (text.length() > 7) {
					return;
				}
				super.replace(arg0, arg1, arg2, arg3, arg4);
			}
		});
		codeField.setMaximumSize(new Dimension(60, 20));
		codeDownloadPanel.add(codeField);
		codeDownloadPanel.add(Box.createRigidArea(new Dimension(10, 10)));

		downloadButton = new JButton("Download & Compile");
		downloadButton.setPreferredSize(new Dimension(185, 22));
		//codeDownloadPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		codeDownloadPanel.add(downloadButton);
		codeDownloadPanel.add(Box.createHorizontalGlue());
		modMakerPanel.add(codeDownloadPanel);
		modMakerPanel.add(Box.createVerticalGlue());

		JPanel getCodePane = new JPanel();
		getCodePane.setLayout(new BoxLayout(getCodePane, BoxLayout.LINE_AXIS));
		getCodePane.add(Box.createHorizontalGlue());

		makeModButton = new JButton("Create a mod");
		browseModsButton = new JButton("Browse mods");
		makeModButton.addActionListener(this);
		browseModsButton.addActionListener(this);
		getCodePane.add(makeModButton);
		getCodePane.add(Box.createRigidArea(new Dimension(10, 5)));
		getCodePane.add(browseModsButton);
		getCodePane.add(Box.createHorizontalGlue());
		modMakerPanel.add(getCodePane);
		modMakerPanel.add(Box.createVerticalGlue());
		if (!hasDLCBypass) {
			JPanel launcherWVPanel = new JPanel();
			launcherWVPanel.setLayout(new BoxLayout(launcherWVPanel, BoxLayout.LINE_AXIS));
			launcherWVPanel.add(Box.createHorizontalGlue());
			launcherWVPanel.add(
					new JLabel(
							"<html>The Launcher_WV.exe DLC bypass will be installed so your mod will work.<br>To use mods you will need to use Start Game from Mod Manager.<br>Tab and ` will open the console in game.<br>Your game will not be modified by this file.</html>"),
					BorderLayout.CENTER);
			launcherWVPanel.add(Box.createHorizontalGlue());
			modMakerPanel.add(launcherWVPanel);
			setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT + 90));
		} else {
			setPreferredSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));

		}
		codeField.addActionListener(this);
		downloadButton.addActionListener(this);

		//set focus to codeField
		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				codeField.requestFocus();
			}
		});

		modMakerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add(modMakerPanel);

		setResizable(false);
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/resource/icon32.png")));
		pack();
		setLocationRelativeTo(callingWindow);

		//set combobox from settings
		Wini settingsini;
		try {
			settingsini = new Wini(new File(ModManager.settingsFilename));
			String modmakerLanguage = settingsini.get("Settings", "modmaker_language");
			if (modmakerLanguage != null && !modmakerLanguage.equals("")) {
				//language setting exists
				languageChoices.setSelectedItem(modmakerLanguage);
			}
		} catch (InvalidFileFormatException e) {
			ModManager.debugLogger.writeMessage("Invalid INI! Did the user modify it by hand?");
			e.printStackTrace();
		} catch (IOException e) {
			ModManager.debugLogger.writeMessage("I/O Error reading settings file. It may not exist yet. It will be created when a setting is stored to disk.");
		}
	}

	/**
	 * Validates that all required components are available before starting a
	 * ModMaker session.
	 * 
	 * @return
	 */
	private boolean validateModMakerPrereqs() {
		String wvdlcBink32MD5 = "5a826dd66ad28f0099909d84b3b51ea4"; //Binkw32.dll that bypasses DLC check (WV) - from Private Server SVN
		String wvdlcBink32MD5_2 = "05540bee10d5e3985608c81e8b6c481a"; //Binkw32.dll that bypasses DLC check (WV) - from Private Server SVN

		File bgdir = new File(biogameDir);
		File gamedir = bgdir.getParentFile();
		ModManager.debugLogger.writeMessage("Game directory: " + gamedir.toString());
		File bink32 = new File(gamedir.toString() + "\\Binaries\\Win32\\binkw32.dll");
		try {
			String binkhash = MD5Checksum.getMD5Checksum(bink32.toString());
			if (binkhash.equals(wvdlcBink32MD5) || binkhash.equals(wvdlcBink32MD5_2)) {
				ModManager.debugLogger.writeMessage("Binkw32 DLC bypass installed");
				hasDLCBypass = true;
			} else {
				// Check for LauncherWV.
				File Launcher_WV = new File(gamedir.toString() + "\\Binaries\\Win32\\Launcher_WV.exe");
				File LauncherWV = new File(gamedir.toString() + "\\Binaries\\Win32\\LauncherWV.exe");
				if (Launcher_WV.exists() || LauncherWV.exists()) {
					//does exist
					hasDLCBypass = true;
					ModManager.debugLogger.writeMessage("Launcher WV DLC bypass installed");
				} else {
					// it doesn't exist... extract our copy of binkw32.dll
					hasDLCBypass = false;
					//Failure
					/*
					 * dispose(); JOptionPane.showMessageDialog(null,
					 * "<html>You don't have a way to bypass the DLC check.<br>To satisfy the requirement you need one of the following:<br> - Binkw32.dll DLC bypass in the binaries folder<br> - LauncherWV.exe in the Binaries folder<br><br>Information on how to fulfill this requirement can be found on me3tweaks.com.</html>"
					 * , "Prerequesites Error", JOptionPane.ERROR_MESSAGE);
					 */
					ModManager.debugLogger.writeMessage("Binkw32.dll bypass hash failed, hash is: " + binkhash);
					ModManager.debugLogger.writeMessage("LauncherWV was not found in Win32 as Launcher_WV or LauncherWV.");
					ModManager.debugLogger.writeMessage("Advertising the DLC bypass install.");
					return true; //we will install binkw32.
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		File tankMasterCompiler = new File("Tankmaster Compiler/MassEffect3.Coalesce.exe");

		if (!tankMasterCompiler.exists()) {
			dispose();
			JOptionPane.showMessageDialog(null,
					"<html>You need TankMaster's Coalesced Compiler in order to use ModMaker.<br><br>It should have been bundled with Mod Manager 3 in the TankMaster Compiler folder.</html>",
					"Prerequesites Error", JOptionPane.ERROR_MESSAGE);

			ModManager.debugLogger.writeMessage("Tankmaster's compiler not detected. Abort. Searched at: " + tankMasterCompiler.toString());
			return false;
		}
		ModManager.debugLogger.writeMessage("Detected TankMaster coalesced compiler");

		String me3explorerdir = ModManager.getME3ExplorerEXEDirectory(false);
		if (me3explorerdir == null) {
			dispose();
			JOptionPane.showMessageDialog(null,
					"<html>You need ME3Explorer in order to use ModMaker.<br><br>It should have been bundled with Mod Manager 3 in the ME3Explorer folder.</html>",
					"Prerequesites Error", JOptionPane.ERROR_MESSAGE);

			ModManager.debugLogger.writeMessage("ME3Explorer not detected. Abort.");
			return false;
		}
		//All prereqs met.
		return true;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == downloadButton) {
			startModmaker(getLanguages(languageChoices.getSelectedItem().toString()));
		} else if (e.getSource() == codeField) {
			//enter button
			startModmaker(getLanguages(languageChoices.getSelectedItem().toString()));
		} else if (e.getSource() == makeModButton) {
			URI theURI;
			try {
				theURI = new URI("https://me3tweaks.com/modmaker");
				java.awt.Desktop.getDesktop().browse(theURI);
				dispose();
			} catch (URISyntaxException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		} else if (e.getSource() == browseModsButton) {
			URI theURI;
			try {
				theURI = new URI("https://me3tweaks.com/modmaker/gallery");
				java.awt.Desktop.getDesktop().browse(theURI);
				dispose();
			} catch (URISyntaxException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (IOException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			}
		}

	}

	private static ArrayList<String> getLanguages(String chosenLang) {
		ArrayList<String> languagesToCompile = new ArrayList<String>();
		switch (chosenLang) {
		case ALL_LANG:
			languagesToCompile.add("INT");
			languagesToCompile.add("RUS");
			languagesToCompile.add("ESN");
			languagesToCompile.add("POL");
			languagesToCompile.add("FRA");
			languagesToCompile.add("ITA");
			languagesToCompile.add("DEU");
			break;
		case ENGLISH:
			languagesToCompile.add("INT");
			break;
		case RUSSIAN:
			languagesToCompile.add("RUS");
			break;
		case SPANISH:
			languagesToCompile.add("ESN");
			break;
		case POLISH:
			languagesToCompile.add("POL");
			break;
		case FRENCH:
			languagesToCompile.add("FRA");
			break;
		case ITALIAN:
			languagesToCompile.add("ITA");
			break;
		case GERMAN:
			languagesToCompile.add("DEU");
			break;
		default:
			break;
		}
		return languagesToCompile;
	}

	public static ArrayList<String> getDefaultLanguages(){
		String defaultLang = ALL_LANG;
		Wini settingsini;
		try {
			settingsini = new Wini(new File(ModManager.settingsFilename));
			String modmakerLanguage = settingsini.get("Settings",
					"modmaker_language");
			if (modmakerLanguage != null && !modmakerLanguage.equals("")) {
				//language setting exists
				defaultLang = modmakerLanguage;
			}
		} catch (InvalidFileFormatException e) {
			ModManager.debugLogger
					.writeMessage("Invalid INI! Did the user modify it by hand?");
			e.printStackTrace();
		} catch (IOException e) {
			ModManager.debugLogger
					.writeMessage("I/O Error reading settings file. It may not exist yet. It will be created when a setting is stored to disk.");
		}
		return getLanguages(defaultLang);
	}

	private void startModmaker(ArrayList<String> languages) {
		dispose();
		boolean shouldContinue = true;
		if (!hasDLCBypass) {
			shouldContinue = installBypass();
		}
		if (shouldContinue) {
			Wini ini;
			try {
				File settings = new File(ModManager.settingsFilename);
				if (!settings.exists())
					settings.createNewFile();
				ini = new Wini(settings);
				ini.put("Settings", "modmaker_language", languageChoices.getSelectedItem());
				ini.store();
			} catch (InvalidFileFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Settings file encountered an I/O error while attempting to write it. Settings not saved.");
			}
			callingWindow.startModMaker(codeField.getText().toString(), languages);
		}
	}

	private boolean installBypass() {
		return ModManager.installLauncherWV(biogameDir);
	}
}
