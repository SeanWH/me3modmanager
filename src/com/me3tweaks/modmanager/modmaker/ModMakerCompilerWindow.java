package com.me3tweaks.modmanager.modmaker;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.me3tweaks.modmanager.ASIModWindow;
import com.me3tweaks.modmanager.AutoTocWindow;
import com.me3tweaks.modmanager.KeybindsInjectionWindow;
import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.ModManagerWindow;
import com.me3tweaks.modmanager.PatchLibraryWindow;
import com.me3tweaks.modmanager.objects.Mod;
import com.me3tweaks.modmanager.objects.ModDelta;
import com.me3tweaks.modmanager.objects.ModJob;
import com.me3tweaks.modmanager.objects.ModTypeConstants;
import com.me3tweaks.modmanager.objects.ThreadCommand;
import com.me3tweaks.modmanager.utilities.ResourceUtils;
import com.me3tweaks.modmanager.valueparsers.biodifficulty.Category;
import com.me3tweaks.modmanager.valueparsers.enemytype.EnemyType;
import com.me3tweaks.modmanager.valueparsers.id.ID;
import com.me3tweaks.modmanager.valueparsers.possessionwaves.Difficulty;
import com.me3tweaks.modmanager.valueparsers.sharedassignment.SharedDifficulty;
import com.me3tweaks.modmanager.valueparsers.waveclass.WaveClass;
import com.me3tweaks.modmanager.valueparsers.wavelist.Wave;

@SuppressWarnings("serial")
/**
 * ModMaker Compiler Window + Compiler.
 * 
 * @author Mgamerz
 *
 */
public class ModMakerCompilerWindow extends JDialog {
	boolean modExists = false, error = false;
	String code, modName, modDescription, modId, modDev, modVer;
	private static double TOTAL_STEPS = 9;
	private int stepsCompleted = 1;
	private double modMakerVersion;
	ArrayList<String> requiredCoals = new ArrayList<String>();
	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	Document doc;
	ArrayList<String> languages;
	Element infoElement, dataElement, tlkElement;
	JLabel infoLabel, currentOperationLabel;
	JProgressBar overallProgress, currentStepProgress;
	private Mod mod;
	private ArrayList<String> requiredMixinIds = new ArrayList<String>();
	private ArrayList<DynamicPatch> dynamicMixins = new ArrayList<DynamicPatch>();
	private int jobCode;

	/**
	 * Starts a modmaker session for a user-selected download
	 * 
	 * @param code
	 *            code to download (if not an integer, use sideload method,
	 *            which assumes the value is a filepath to an xml file.)
	 * @param languages
	 *            languages to compile
	 */
	public ModMakerCompilerWindow(String code, ArrayList<String> languages) {
		super(null, Dialog.ModalityType.MODELESS);
		this.code = code;
		this.languages = languages;
		setupWindow();
		jobCode = ModManagerWindow.ACTIVE_WINDOW.submitBackgroundJob("Compiling ModMaker Mod");
		ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compiling ModMaker Mod...");
		new ModDownloadWorker().execute();

		if (!error) {
			this.setVisible(true);
		}
	}

	/**
	 * Starts a modmaker session for an update download - deletes the specified
	 * mod's source directory before compiling
	 * 
	 * @param mod
	 *            Mod to delete before createCMMMod()
	 * @param languages
	 *            languages to compile
	 */
	public ModMakerCompilerWindow(Mod mod, ArrayList<String> languages) {
		super(null, Dialog.ModalityType.APPLICATION_MODAL);
		this.code = Integer.toString(mod.getModMakerCode());
		this.languages = languages;
		this.mod = mod;
		setupWindow();
		new ModDownloadWorker().execute();

		if (!error) {
			this.setVisible(true);
		}
	}

	/**
	 * Applies a mod delta to a mod
	 * 
	 * @param mod
	 *            Mod to apply delta to
	 * @param delta
	 *            Delta to apply
	 */
	public ModMakerCompilerWindow(Mod mod, ModDelta delta) {
		super(null, Dialog.ModalityType.APPLICATION_MODAL);
		this.mod = mod;

		setupWindow();
		new ModDownloadWorker().execute();

		if (!error) {
			this.setVisible(true);
		}
	}

	class ModDownloadWorker extends SwingWorker<Void, Object> {
		boolean running = true;

		@Override
		protected Void doInBackground() throws Exception {
			getModInfo();
			return null;
		}

		private void getModInfo() {
			int mmcode = 0;
			try {
				mmcode = Integer.parseInt(code);
			} catch (Exception e) {
				// It's a sideload
			}
			String link = null;
			String lzmalink = null;
			if (mmcode > 0) {
				//Download
				ModManager.debugLogger.writeMessage("================DOWNLOADING MOD INFORMATION==============");
				//ATTEMPT LZMA
				link = "https://me3tweaks.com/modmaker/download.php?id=" + code;
				lzmalink = link + "&method=lzma";
				ModManager.debugLogger.writeMessage("Fetching mod from " + lzmalink);
			} else {
				//Sideload
				ModManager.debugLogger.writeMessage("================SIDELOADING MODMAKER MOD==============");
				ModManager.debugLogger.writeMessage("Sideloading mod from " + code);
			}
			try {
				String modDelta = null;
				if (mmcode > 0) {
					try {
						String downloadedfile = ModManager.getTempDir() + code + ".xml";
						File lzmafile = new File(downloadedfile + ".lzma");
						publish(new ThreadCommand("UPDATE_INFO", "<html>Downloading mod delta from ME3Tweaks</html>"));
						FileUtils.copyURLToFile(new URL(lzmalink), lzmafile);
						publish(new ThreadCommand("UPDATE_INFO", "<html>Decompressing mod delta</html>"));
						if (ResourceUtils.decompressLZMAFile(lzmafile.getAbsolutePath(), null)) {
							//decompressed OK
							modDelta = FileUtils.readFileToString(new File(downloadedfile), StandardCharsets.UTF_8);
							FileUtils.deleteQuietly(new File(downloadedfile));
							FileUtils.deleteQuietly(lzmafile);
						} else {
							FileUtils.deleteQuietly(new File(downloadedfile));
							FileUtils.deleteQuietly(lzmafile);
							throw new IOException("Failed to decompress LZMA file, falling back...");
						}
					} catch (IOException e) {
						FileUtils.deleteQuietly(new File(ModManager.getTempDir() + code + ".xml"));
						FileUtils.deleteQuietly(new File(ModManager.getTempDir() + code + ".xml.lzma"));
						try {
							ModManager.debugLogger.writeMessage("I/O Exception using LZMA link, falling back to decompressed link...");
							publish(new ThreadCommand("UPDATE_INFO", "<html>Downloading mod delta from ME3Tweaks</html>"));
							modDelta = IOUtils.toString(new URL(link), StandardCharsets.UTF_8);
						} catch (IOException ex) {
							ModManager.debugLogger.writeErrorWithException("I/O Exception using LZMA and fallback links, giving up. ", ex);
							dispose();
							publish(new ThreadCommand("ERROR", "<html>Unable to download ME3Tweaks ModMaker mod:<br>" + ex.getMessage() + "</html>"));
							running = false;
							return;
						}
					}
				} else {
					//load sideload
					modDelta = FileUtils.readFileToString(new File(code), StandardCharsets.UTF_8);
				}
				//File has been downloaded
				File downloadedFile = new File(ModManager.getModmakerCacheDir() + mmcode + ".xml");
				if (mmcode != 0) {
					FileUtils.writeStringToFile(downloadedFile, modDelta, StandardCharsets.UTF_8);
				}
				ModManager.debugLogger.writeMessage("Cached xml file to cache directory.");
				publish(new ThreadCommand("UPDATE_INFO", "<html>Parsing Mod Delta</html>"));
				ModManager.debugLogger.writeMessage("Mod delta downloaded to memory");
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				ModManager.debugLogger.writeMessage("Loading mod delta into document into memory.");
				doc = dBuilder.parse(new InputSource(new ByteArrayInputStream(modDelta.getBytes("utf-8")))); //http://stackoverflow.com/questions/1706493/java-net-malformedurlexception-no-protocol
				ModManager.debugLogger.writeMessage("Mod information loaded into memory.");
				doc.getDocumentElement().normalize();
				NodeList errors = doc.getElementsByTagName("error");
				if (errors.getLength() > 0) {
					//error occured.
					dispose();
					publish(new ThreadCommand("ERROR", "<html>No mod with code " + code + " was found on ME3Tweaks ModMaker.</html>"));
					running = false;
					FileUtils.deleteQuietly(downloadedFile);
					return;
				}
				parseModInfo();
			} catch (MalformedURLException e) {
				running = false;
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while preparing to compile the mod:\n" + e.getMessage() + "\nCheck the Mod Manager log for more info (in the help menu).",
						"Pre-compilation error", JOptionPane.ERROR_MESSAGE);
				ModManagerWindow.ACTIVE_WINDOW.reloadModlist();
			} catch (IOException e) {
				running = false;
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while preparing to compile the mod:\n" + e.getMessage() + "\nCheck the Mod Manager log for more info (in the help menu).",
						"Pre-compilation error", JOptionPane.ERROR_MESSAGE);
				ModManagerWindow.ACTIVE_WINDOW.reloadModlist();
			} catch (ParserConfigurationException e) {
				running = false;
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while preparing to compile the mod:\n" + e.getMessage() + "\nCheck the Mod Manager log for more info (in the help menu).",
						"Pre-compilation error", JOptionPane.ERROR_MESSAGE);
				dispose();
				ModManagerWindow.ACTIVE_WINDOW.reloadModlist();
			} catch (SAXException e) {
				running = false;
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while preparing to compile the mod:\n" + e.getMessage() + "\nCheck the Mod Manager log for more info (in the help menu).",
						"Pre-compilation error", JOptionPane.ERROR_MESSAGE);
				dispose();
				ModManagerWindow.ACTIVE_WINDOW.reloadModlist();
			} catch (Exception e) {
				running = false;
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while preparing to compile the mod:\n" + e.getMessage() + "\nCheck the Mod Manager log for more info (in the help menu).",
						"Pre-compilation error", JOptionPane.ERROR_MESSAGE);
				dispose();
			}
		}

		public void process(List<Object> chunks) {
			for (Object obj : chunks) {
				if (obj instanceof ThreadCommand) {
					ThreadCommand error = (ThreadCommand) obj;
					String cmd = error.getCommand();
					switch (cmd) {
					case "ERROR":
						JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, error.getMessage(), "Compiling Error", JOptionPane.ERROR_MESSAGE);
						ModManager.debugLogger.writeMessage(error.getMessage());
						dispose();
						break;
					case "UPDATE_INFO":
						infoLabel.setText(error.getMessage());
						break;
					case "UPDATE_OPERATION":
						currentOperationLabel.setText(error.getMessage());
						break;
					}
				}
			}
		}

		/**
		 * Parses information from the server - pre-step
		 */
		protected void parseModInfo() {
			try {
				ModManager.debugLogger.writeMessage("============Parsing modinfo==============");
				NodeList infoNodeList = doc.getElementsByTagName("ModInfo");
				infoElement = (Element) infoNodeList.item(0); //it'll be the only element. Hopefully!
				NodeList nameElement = infoElement.getElementsByTagName("Name");
				modName = nameElement.item(0).getTextContent();
				ModManager.debugLogger.writeMessage("Mod Name: " + modName);

				publish(new ThreadCommand("UPDATE_INFO", "Preparing to compile " + modName));
				NodeList descElement = infoElement.getElementsByTagName("Description");
				modDescription = descElement.item(0).getTextContent();
				ModManager.debugLogger.writeMessage("Mod Description: " + modDescription);

				NodeList devElement = infoElement.getElementsByTagName("Author");
				modDev = devElement.item(0).getTextContent();
				ModManager.debugLogger.writeMessage("Mod Dev: " + modDev);

				NodeList versionElement = infoElement.getElementsByTagName("Revision");
				if (versionElement.getLength() > 0) {
					modVer = versionElement.item(0).getTextContent();
					ModManager.debugLogger.writeMessage("Mod Version: " + modVer);
				}

				NodeList idElement = infoElement.getElementsByTagName("id");
				modId = idElement.item(0).getTextContent();
				ModManager.debugLogger.writeMessage("ModMaker ID: " + modId);

				NodeList modmakerVersionElement = infoElement.getElementsByTagName("ModMakerVersion");
				String modModMakerVersion = modmakerVersionElement.item(0).getTextContent();
				ModManager.debugLogger.writeMessage("Mod information file was built using modmaker " + modModMakerVersion);

				modMakerVersion = Double.parseDouble(modModMakerVersion);
				if (modMakerVersion > ModManager.MODMAKER_VERSION_SUPPORT) {
					//ERROR! We can't compile this version.
					ModManager.debugLogger.writeError("This version of mod manager does not support the server version of ModMaker that was used to compile this mod delta.");
					ModManager.debugLogger.writeError("FATAL ERROR: This version supports up to ModMaker version: " + ModManager.MODMAKER_VERSION_SUPPORT);
					ModManager.debugLogger.writeError("FATAL ERROR: This mod was built with ModMaker version: " + modModMakerVersion);
					publish(new ThreadCommand("ERROR",
							"<html>This mod was built with a newer version of ModMaker than this version of Mod Manager can support.<br>You need to download the latest copy of Mod Manager to compile this mod.</html>"));
					error = true;
					return;
				}

				//Get required mixins and dynamic mixins
				NodeList mixinNodeList = doc.getElementsByTagName("MixInData");
				if (mixinNodeList.getLength() > 0) {
					Element mixinsElement = (Element) mixinNodeList.item(0);
					{
						NodeList mixinsNodeList = mixinsElement.getElementsByTagName("MixIn");
						for (int j = 0; j < mixinsNodeList.getLength(); j++) {
							Node mixinNode = mixinsNodeList.item(j);
							if (mixinNode.getNodeType() == Node.ELEMENT_NODE) {
								requiredMixinIds.add(mixinNode.getTextContent());
								ModManager.debugLogger.writeMessage("Mod recommends mixin with id " + mixinNode.getTextContent());
							}
						}
					}
					NodeList dynamicmixinsNodeList = mixinsElement.getElementsByTagName("DynamicMixIn");
					for (int j = 0; j < dynamicmixinsNodeList.getLength(); j++) {
						Node dynamicmixinNode = dynamicmixinsNodeList.item(j);
						if (dynamicmixinNode.getNodeType() == Node.ELEMENT_NODE) {
							DynamicPatch dp;
							try {
								dp = new DynamicPatch(dynamicmixinNode);

								dynamicMixins.add(dp);
								ModManager.debugLogger.writeMessage("Mod contains dynamic mixin: " + dp.getFinalPatch().getPatchName());
							} catch (DOMException | IOException e) {
								ModManager.debugLogger.writeErrorWithException("Error preparing dynamic mixin, skipping:", e);
							}
						}
					}
				}

				//Check the name
				File moddir = new File(ModManager.getModsDir() + modName);
				if (moddir.isDirectory()) {
					try {
						ModManager.debugLogger.writeMessage("Removing existing mod directory, will recreate when complete");
						FileUtils.deleteDirectory(moddir);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						ModManager.debugLogger.writeException(e);
					}
				}

				//Debug remove
				publish(new ThreadCommand("UPDATE_INFO", "Compiling " + modName + "..."));
				NodeList dataNodeList = doc.getElementsByTagName("ModData");
				dataElement = (Element) dataNodeList.item(0);
				NodeList fileNodeList = dataElement.getChildNodes();
				//Find the coals it needs, iterate over the files list.
				for (int i = 0; i < fileNodeList.getLength(); i++) {
					Node fileNode = fileNodeList.item(i);
					if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
						//filters out the #text nodes. Don't know what those really are.
						String intCoalName = fileNode.getNodeName();
						ModManager.debugLogger.writeMessage("ini file descriptor found in mod: " + intCoalName);
						requiredCoals.add(ME3TweaksUtils.internalNameToCoalFilename(intCoalName));
					}
				}

				// Check Coalesceds
				File coalDir = new File(ModManager.getCompilingDir() + "coalesceds");
				coalDir.mkdirs(); // creates if it doens't exist. otherwise nothing.
				ArrayList<String> coals = new ArrayList<String>(requiredCoals); //copy

				currentOperationLabel.setText("Downloading Coalesced files...");
				currentStepProgress.setIndeterminate(false);
				// Check and download
				new CoalDownloadWorker(coals, currentStepProgress).execute();
			} catch (Exception e) {
				ModManager.debugLogger.writeException(e);
			}
		}

		protected void done() {
			if (!running) {
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compile failed");
				ModManagerWindow.ACTIVE_WINDOW.submitJobCompletion(jobCode);
			}
		}
	}

	private void setupWindow() {
		setTitle("ModMaker Compiler");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(420, 167));
		setIconImages(ModManager.ICONS);
		setResizable(false);

		JPanel modMakerPanel = new JPanel();
		modMakerPanel.setLayout(new BoxLayout(modMakerPanel, BoxLayout.PAGE_AXIS));
		JPanel infoPane = new JPanel();
		infoPane.setLayout(new BoxLayout(infoPane, BoxLayout.LINE_AXIS));
		infoLabel = new JLabel("Downloading mod delta for code " + code + "...", SwingConstants.CENTER);
		infoPane.add(Box.createHorizontalGlue());
		infoPane.add(infoLabel);
		infoPane.add(Box.createHorizontalGlue());

		modMakerPanel.add(infoPane);
		//JLabel overall = new JLabel("Overall progress");
		TitledBorder overallBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Overall Progress");
		//JLabel current = new JLabel("Current operation");
		TitledBorder currentBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Current Operation");
		currentOperationLabel = new JLabel("Downloading mod information...", SwingConstants.CENTER);
		overallProgress = new JProgressBar(0, 100);
		overallProgress.setStringPainted(true);
		overallProgress.setIndeterminate(false);
		overallProgress.setEnabled(false);

		currentStepProgress = new JProgressBar(0, 100);
		currentStepProgress.setStringPainted(true);
		currentStepProgress.setIndeterminate(true);
		currentStepProgress.setEnabled(false);

		JPanel overallPanel = new JPanel(new BorderLayout());
		overallPanel.setBorder(overallBorder);
		overallPanel.add(overallProgress, BorderLayout.CENTER);

		modMakerPanel.add(overallPanel);
		modMakerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
		JPanel currentPanel = new JPanel(new BorderLayout());
		currentPanel.setBorder(currentBorder);

		currentPanel.add(currentOperationLabel);
		currentPanel.add(currentStepProgress, BorderLayout.SOUTH);
		modMakerPanel.add(currentPanel);
		modMakerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		getContentPane().add(modMakerPanel);
		pack();
		setLocationRelativeTo(ModManagerWindow.ACTIVE_WINDOW);

	}

	/**
	 * Converts the Coalesced.bin filenames to their respective directory in the
	 * .sfar files.
	 * 
	 * @param coalName
	 *            name of coal being packed into the mod
	 * @return path to the file to repalce
	 */
	protected String coalFileNameToDLCDir(String coalName) {
		switch (coalName) {
		case "Default_DLC_CON_MP1.bin":
			return "/BIOGame/DLC/DLC_CON_MP1/CookedPCConsole/Default_DLC_CON_MP1.bin";
		case "Default_DLC_CON_MP2.bin":
			return "/BIOGame/DLC/DLC_CON_MP2/CookedPCConsole/Default_DLC_CON_MP2.bin";
		case "Default_DLC_CON_MP3.bin":
			return "/BIOGame/DLC/DLC_CON_MP3/CookedPCConsole/Default_DLC_CON_MP3.bin";
		case "Default_DLC_CON_MP4.bin":
			return "/BIOGame/DLC/DLC_CON_MP4/CookedPCConsole/Default_DLC_CON_MP4.bin";
		case "Default_DLC_CON_MP5.bin":
			return "/BIOGame/DLC/DLC_CON_MP5/CookedPCConsole/Default_DLC_CON_MP5.bin";
		case "Default_DLC_UPD_Patch01.bin":
			return "/BIOGame/DLC/DLC_UPD_Patch01/CookedPCConsole/Default_DLC_UPD_Patch01.bin";
		case "Default_DLC_UPD_Patch02.bin":
			return "/BIOGame/DLC/DLC_UPD_Patch02/CookedPCConsole/Default_DLC_UPD_Patch02.bin";
		case "Coalesced.bin":
			return "\\BIOGame\\CookedPCConsole\\Coalesced.bin";
		case "Default_DLC_TestPatch.bin":
			return "/BIOGame/DLC/DLC_TestPatch/CookedPCConsole/Default_DLC_TestPatch.bin";
		case "Default_DLC_HEN_PR.bin":
			return "/BIOGame/DLC/DLC_HEN_PR/CookedPCConsole/Default_DLC_HEN_PR.bin";
		case "Default_DLC_CON_APP01.bin":
			return "/BIOGame/DLC/DLC_CON_APP01/CookedPCConsole/Default_DLC_CON_APP01.bin";
		case "Default_DLC_CON_GUN01.bin":
			return "/BIOGame/DLC/DLC_CON_GUN01/CookedPCConsole/Default_DLC_CON_GUN01.bin";
		case "Default_DLC_CON_GUN02.bin":
			return "/BIOGame/DLC/DLC_CON_GUN02/CookedPCConsole/Default_DLC_CON_GUN02.bin";
		case "Default_DLC_CON_END.bin":
			return "/BIOGame/DLC/DLC_CON_END/CookedPCConsole/Default_DLC_CON_END.bin";
		case "Default_DLC_EXP_Pack001.bin":
			return "/BIOGame/DLC/DLC_EXP_Pack001/CookedPCConsole/Default_DLC_EXP_Pack001.bin";
		case "Default_DLC_EXP_Pack002.bin":
			return "/BIOGame/DLC/DLC_EXP_Pack002/CookedPCConsole/Default_DLC_EXP_Pack002.bin";
		case "Default_DLC_EXP_Pack003.bin":
			return "/BIOGame/DLC/DLC_EXP_Pack003/CookedPCConsole/Default_DLC_EXP_Pack003.bin";
		case "Default_DLC_EXP_Pack003_Base.bin":
			return "/BIOGame/DLC/DLC_EXP_Pack003_Base/CookedPCConsole/Default_DLC_EXP_Pack003_Base.bin";
		case "ServerCoalesced.bin":
			return "\\Binaries\\win32\\asi\\ServerCoalesced.bin";
		default:
			ModManager.debugLogger.writeMessage("ERROR: UNRECOGNIZED COAL FILE: " + coalName);
			return null;
		}
	}

	/**
	 * Converts the server TLKData tag name directory in the cookedpcconsole
	 * directory.
	 * 
	 * @param coalName
	 *            name of coal being packed into the mod
	 * @return path to the file to repalce
	 */
	protected String tlkShortNameToFileName(String shortTLK) {
		switch (shortTLK) {
		case "INT":
			return "BIOGame_INT.tlk";
		case "ESN":
			return "BIOGame_ESN.tlk";
		case "DEU":
			return "BIOGame_DEU.tlk";
		case "ITA":
			return "BIOGame_ITA.tlk";
		case "FRA":
			return "BIOGame_FRA.tlk";
		case "RUS":
			return "BIOGame_RUS.tlk";
		case "POL":
			return "BIOGame_POL.tlk";
		case "JPN":
			return "BIOGame_JPN.tlk";
		default:
			ModManager.debugLogger.writeMessage("UNRECOGNIZED TLK FILE: " + shortTLK);
			return null;
		}
	}

	/**
	 * Runs the Coalesced files through Tankmasters decompiler
	 */
	public void decompileMods() {
		new DecompilerWorker(requiredCoals, currentStepProgress).execute();
	}

	/**
	 * 
	 * /** Decompiles a coalesced into .xml files using tankmaster's tools.
	 * 
	 * @author Mgamerz
	 */

	class DecompilerWorker extends SwingWorker<Void, Integer> {
		private ArrayList<String> coalsToDecompile;
		private JProgressBar progress;
		private int numCoals;
		public AtomicInteger COMPLETED;

		public DecompilerWorker(ArrayList<String> coalsToDecompile, JProgressBar progress) {
			ModManager.debugLogger.writeMessage("==================DecompilerWorker==============");
			COMPLETED = new AtomicInteger(0);
			progress.setIndeterminate(true);
			this.coalsToDecompile = coalsToDecompile;
			this.numCoals = coalsToDecompile.size();
			this.progress = progress;
			currentOperationLabel.setText("Decompiling Coalesced Files");
			progress.setValue(0);
		}

		protected Void doInBackground() throws Exception {
			ExecutorService decompilerExecutor = Executors.newFixedThreadPool(4);
			ArrayList<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
			for (String coalesced : coalsToDecompile) {
				CoalescedDecompilerTask cct = new CoalescedDecompilerTask(coalesced, this);
				futures.add(decompilerExecutor.submit(cct));
			}

			decompilerExecutor.shutdown();
			decompilerExecutor.awaitTermination(60, TimeUnit.MINUTES);
			for (Future<Boolean> f : futures) {
				try {
					Boolean result = f.get();
					if (result == false) {
						error = true;
						ModManager.debugLogger.writeError("A coalesced file failed to decompile.");
						throw new Exception("A coalesced file failed to decompile. Check the logs.");
					}
				} catch (ExecutionException e) {
					ModManager.debugLogger.writeErrorWithException("EXECUTION EXCEPTION WHILE DECOMPILING COALESCED FILE (AFTER EXECUTOR FINISHED): ", e);
				}
			}

			return null;
		}

		@Override
		protected void process(List<Integer> numCompleted) {
			progress.setIndeterminate(false);
			progress.setValue((int) (100 / (numCoals / (float) numCompleted.get(0))));
		}

		public void publishProgress(int numberdone) {
			publish(numberdone);
		}

		protected void done() {
			// Coals decompiled
			stepsCompleted++;
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeMessage("Error occured in DecompilerWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while decompiling coalesced files:\n" + e.getMessage() + "\n\nYou should report this to FemShep via the Forums link in the help menu.",
						"Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			if (error) {
				dispose();
				return;
			}

			overallProgress.setValue((int) ((100 / (TOTAL_STEPS / stepsCompleted)) + 0.5));
			ModManager.debugLogger.writeMessage("COALS: DECOMPILED...");
			new MergeWorker(progress).execute();
		}
	}

	/**
	 * Runs the modified coalesced files through Tankmaster's compiler
	 * 
	 * @author Mgamerz
	 *
	 */
	class CompilerWorker extends SwingWorker<Void, Integer> {
		private ArrayList<String> coalsToCompile;
		private JProgressBar progress;
		private int numCoals;
		public AtomicInteger COMPLETED;

		public CompilerWorker(ArrayList<String> coalsToCompile, JProgressBar progress) {
			ModManager.debugLogger.writeMessage("==================CompilerWorker==============");
			COMPLETED = new AtomicInteger(0);
			this.coalsToCompile = coalsToCompile;
			this.numCoals = coalsToCompile.size();
			this.progress = progress;
			currentOperationLabel.setText("Recompiling Coalesced Files");
			progress.setIndeterminate(true);
			progress.setValue(0);
		}

		protected Void doInBackground() throws Exception {
			int cores = Runtime.getRuntime().availableProcessors();
			if (cores > 4) {
				cores = 4;
			}
			ExecutorService compilerExecutor = Executors.newFixedThreadPool(cores);
			ArrayList<Future<Boolean>> futures = new ArrayList<Future<Boolean>>();
			for (String coalesced : coalsToCompile) {
				CoalescedCompilerTask cct = new CoalescedCompilerTask(coalesced, COMPLETED, this);
				futures.add(compilerExecutor.submit(cct));
			}

			compilerExecutor.shutdown();
			compilerExecutor.awaitTermination(60, TimeUnit.MINUTES);
			for (Future<Boolean> f : futures) {
				try {
					Boolean result = f.get();
					if (result == false) {
						error = true;
						ModManager.debugLogger.writeError("A coalesced file failed to compile.");
						throw new Exception("A coalesced file failed to compile. Check Mod Manager logs.");
					}
				} catch (ExecutionException e) {
					ModManager.debugLogger.writeErrorWithException("EXECUTION EXCEPTION WHILE COMPILING COALESCED FILE (AFTER EXECUTOR FINISHED): ", e);
				}
			}

			return null;
		}

		@Override
		protected void process(List<Integer> numCompleted) {
			progress.setIndeterminate(false);
			int progressVal = (int) ((COMPLETED.get() * 100.0) / numCoals);
			progress.setValue(progressVal);
		}

		protected void done() {
			// Coals recompiled
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeMessage("Error occured in CompilerWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "An error occured while trying to recompile modified coalesced xml files into a coalesced.bin file:\n"
						+ e.getMessage() + "\n\nYou should report this to FemShep via the Forums link in the help menu.", "Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			if (error) {
				dispose();
				return;
			}
			stepsCompleted += 2;
			overallProgress.setValue((int) ((100 / (TOTAL_STEPS / stepsCompleted))));
			ModManager.debugLogger.writeMessage("COALS: RECOMPILED...");
			new TLKWorker(progress, languages).execute();
		}

		public void publishProgress() {
			publish(1); //don't care
		}
	}

	class CoalDownloadWorker extends SwingWorker<Void, Integer> {
		private ArrayList<String> coalsToDownload;
		private JProgressBar progress;
		private int numCoals;

		public CoalDownloadWorker(ArrayList<String> coalsToDownload, JProgressBar progress) {
			progress.setIndeterminate(true);
			this.coalsToDownload = coalsToDownload;
			this.numCoals = coalsToDownload.size();
			this.progress = progress;
			if (numCoals > 0) {
				currentOperationLabel.setText("Downloading " + coalsToDownload.get(0));
			}
		}

		protected Void doInBackground() throws Exception {
			int coalsCompeted = 0;
			for (String coal : coalsToDownload) {
				//try {
				if (!ModManager.hasPristineCoalesced(coal, ME3TweaksUtils.FILENAME)) {
					ME3TweaksUtils.downloadPristineCoalesced(coal, ME3TweaksUtils.FILENAME);
				}
				FileUtils.copyFile(new File(ModManager.getPristineCoalesced(coal, ME3TweaksUtils.FILENAME)), new File(ModManager.getCompilingDir() + "coalesceds/" + coal));
				ModManager.debugLogger.writeMessage("Copied pristine coalesced of " + coal + " to: " + (new File("coalesceds/" + coal)).getAbsolutePath());
				coalsCompeted++;
				this.publish(coalsCompeted);
				//} catch (IOException e) {
				//	ModManager.debugLogger.writeMessage("Failed to download coalesced file due to IO Exception.");
				//	ModManager.debugLogger.writeException(e);
				//	error = true;
				//}
			}
			return null;
		}

		@Override
		protected void process(List<Integer> numCompleted) {
			progress.setIndeterminate(false);
			if (numCoals > numCompleted.get(0)) {
				currentOperationLabel.setText("Downloading " + coalsToDownload.get(numCompleted.get(0)));
			}
			progress.setValue((int) (100 / (numCoals / (float) numCompleted.get(0))));
			overallProgress.setValue((int) (100 / ((double) TOTAL_STEPS / (stepsCompleted + ((numCoals / (double) numCompleted.get(0)) / 100)))));
		}

		protected void done() {
			// Coals downloaded
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeError("Error occured in CoalDownloadWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
						"An error occured while trying to download pristine Coalesced files from ME3Tweaks:\n" + e.getMessage()
								+ "\n\nYou should report this to FemShep via the Forums link in the help menu.\nInclude a Mod Manager log from this session.",
						"Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				error = true;
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("ModMaker mod failed to compile");
				return;
			}
			if (error) {
				dispose();
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("ModMaker mod failed to compile");
				return;
			}
			ModManager.debugLogger.writeMessage("Required coalesceds downloaded");
			stepsCompleted++;
			overallProgress.setValue((int) (100 / (TOTAL_STEPS / (float) stepsCompleted)));
			decompileMods();
		}
	}

	/**
	 * After coals are downloaded and decompiled, this worker is created and
	 * merges the contents of the downloaded mod into all of the decompiled json
	 * files.
	 */
	class MergeWorker extends SwingWorker<Void, ThreadCommand> {
		boolean error = false;
		JProgressBar progress;
		int coalsMerged = 0;
		int numCoalSections = 1;

		public MergeWorker(JProgressBar progress) {
			ModManager.debugLogger.writeMessage("=============MERGEWORKER=============");
			this.progress = progress;
			currentOperationLabel.setText("Merging Coalesced files...");
			progress.setIndeterminate(true);
			progress.setValue(0);
		}

		protected Void doInBackground() throws Exception {
			// we are going to parse the mod_data array and then look at all the
			// files in the array.
			// Haha wow this is going to be ugly.

			/*
			 * Structure of mod_data array and elements <ModInfo> <Coalesced ID>
			 * <Filename> <Properties (with path attribute)>
			 */

			NodeList coalNodeList = dataElement.getChildNodes();
			for (int i = 0; i < coalNodeList.getLength(); i++) {
				//coalNode is a node containing the coalesced module, such as <MP1> or <BASEGAME>
				Node coalNode = coalNodeList.item(i);
				if (coalNode.getNodeType() == Node.ELEMENT_NODE) {
					numCoalSections++;
				}
			}

			//Iterate over the coalesceds.
			for (int i = 0; i < coalNodeList.getLength(); i++) {
				//coalNode is a node containing the coalesced module, such as <MP1> or <BASEGAME>
				Node coalNode = coalNodeList.item(i);
				if (coalNode.getNodeType() == Node.ELEMENT_NODE) {
					String intCoalName = coalNode.getNodeName(); //get the coal name so we can figure out what folder to look in.
					ModManager.debugLogger.writeMessage("Read coalecesed ID: " + intCoalName);
					ModManager.debugLogger.writeMessage("---------------------MODMAKER COMPILER START OF " + intCoalName + "-------------------------");

					String foldername = FilenameUtils.removeExtension(ME3TweaksUtils.internalNameToCoalFilename(intCoalName));
					NodeList filesNodeList = coalNode.getChildNodes();
					for (int j = 0; j < filesNodeList.getLength(); j++) {
						Node fileNode = filesNodeList.item(j);
						if (fileNode.getNodeType() == Node.ELEMENT_NODE) {
							publish(new ThreadCommand("MERGING_FILE", coalNode.getNodeName() + " " + fileNode.getNodeName()));

							//we now have a file ID such as biogame.
							//We need to load that XML file now.
							String iniFileName = fileNode.getNodeName() + ".xml";
							ModManager.debugLogger
									.writeMessage("Loading Coalesced XML fragment into memory: " + ModManager.getCompilingDir() + "coalesceds\\" + foldername + "\\" + iniFileName);
							Document iniFile = dbFactory.newDocumentBuilder().parse("file:///" + ModManager.getCompilingDir() + "coalesceds\\" + foldername + "\\" + iniFileName);
							iniFile.getDocumentElement().normalize();
							ModManager.debugLogger.writeMessage("Loaded " + iniFile.getDocumentURI() + " into memory.");
							//ModManager.printDocument(iniFile, System.out);
							NodeList assetList = iniFile.getElementsByTagName("CoalesceAsset");
							Element coalesceAsset = (Element) assetList.item(0);
							NodeList sectionsTagList = coalesceAsset.getElementsByTagName("Sections");
							Element sections = (Element) sectionsTagList.item(0);
							NodeList SectionList = sections.getElementsByTagName("Section");

							//We are now at at the "sections" array.
							//We now need to iterate over the dataElement list of properties's path attribute, and drill into this one so we know where to replace.
							NodeList mergeList = fileNode.getChildNodes();
							for (int k = 0; k < mergeList.getLength(); k++) {
								//for every property in this filenode (of the data to merge)...
								Node newproperty = mergeList.item(k);
								if (newproperty.getNodeType() == Node.ELEMENT_NODE) {
									//<Property type="2" name="defaultgravityz" path="engine.worldinfo">-50</Property>
									boolean isArrayProperty = false;
									boolean isSection = false;
									Element property = (Element) newproperty;
									String newPropName = null;
									String arrayType = null;
									String operation = null;
									String matchontype = null;
									String UE3type = null;
									String nodeName = property.getNodeName();
									switch (nodeName) {
									case "Property":
										newPropName = property.getAttribute("name");
										operation = property.getAttribute("operation");
										UE3type = property.getAttribute("type");
										//System.out.println(newPropName + " is a property");

										isArrayProperty = false;
										break;
									case "ArrayProperty":
										arrayType = property.getAttribute("arraytype");
										matchontype = property.getAttribute("matchontype");
										operation = property.getAttribute("operation");
										UE3type = property.getAttribute("type");
										//System.out.println("Array property");

										isArrayProperty = true;
										break;
									case "Section":

										newPropName = property.getAttribute("name");
										//System.out.println(newPropName + " is a section");
										operation = property.getAttribute("operation");
										isArrayProperty = false;
										isSection = true;
										break;
									}

									if (isSection) {
										//can't drill to it.
										switch (operation) {
										case "addition":
											//adds a section
											ModManager.debugLogger.writeMessageConditionally("Creating new section: " + newPropName, ModManager.LOG_MODMAKER);
											Element newElement;
											newElement = iniFile.createElement("Section");
											newElement.setAttribute("name", newPropName);
											sections.appendChild(newElement);
											break;
										case "subtraction":
											//remove this section
											ModManager.debugLogger.writeMessageConditionally("Subtracting section: " + newPropName, ModManager.LOG_MODMAKER);
											boolean sectionFound = false;
											for (int l = 0; l < SectionList.getLength(); l++) {
												//iterate over all sections...
												Node n = SectionList.item(l); //L, not a 1.
												if (n.getNodeType() == Node.ELEMENT_NODE) {
													Element sectionElem = (Element) n;
													if (sectionElem.getAttribute("name").equals(newPropName)) {
														//this is the one to remove.
														sections.removeChild(n);
														sectionFound = true;
														break;
													}
												}
											}
											if (sectionFound) {
												continue;
											} else {
												System.err.println("SHOULDNT REACH THIS! SUBTRACT SECTION");
												continue;
											}
										case "clear":
											//gets rid of all children, leaving the node
											ModManager.debugLogger.writeMessageConditionally("Clearing section: " + newPropName, ModManager.LOG_MODMAKER);
											boolean cleared = false;
											for (int l = 0; l < SectionList.getLength(); l++) {
												//iterate over all sections...
												Node n = SectionList.item(l); //L, not a 1.
												if (n.getNodeType() == Node.ELEMENT_NODE) {
													Element sectionElem = (Element) n;
													if (sectionElem.getAttribute("name").equals(newPropName)) {
														//this is the one to remove.
														while (n.hasChildNodes()) {
															n.removeChild(n.getFirstChild());
														}
														cleared = true;
														break;
													}
												}
											}
											if (cleared) {
												continue;
											} else {
												System.err.println("SHOULDNT REACH THIS! CLEAR SECTION");
												continue;
											}
										}
										continue;
									}

									String newValue = property.getTextContent();

									//first tokenize the path...
									String path = property.getAttribute("path");
									if (path.equals("")) {
										//will never find.
										dispose();
										JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "<html>Unable to compile mod.<br>Path for property is empty: " + newPropName
												+ ".<br>Module: " + intCoalName + "<br>File: " + iniFileName + "</html>", "Compiling Error", JOptionPane.ERROR_MESSAGE);
										error = true;
										return null;
									}
									StringTokenizer drillTokenizer = new StringTokenizer(path, "&"); // - splits this in the event we need to drill down. Spaces are valid it seems in the path.
									Element drilled = null;
									NodeList drilledList = SectionList;
									while (drillTokenizer.hasMoreTokens()) {
										//drill
										String drillTo = drillTokenizer.nextToken();
										ModManager.debugLogger.writeMessageConditionally("Drilling to find: " + drillTo, ModManager.LOG_MODMAKER);
										boolean pathfound = false;
										for (int l = 0; l < drilledList.getLength(); l++) {
											//iterate over all sections...
											Node drilledNode = drilledList.item(l); //L, not a 1.
											if (drilledNode.getNodeType() == Node.ELEMENT_NODE) {
												drilled = (Element) drilledNode;
												//ModManager.debugLogger.writeMessage("Checking attribute: "+drilled.getAttribute("name"));
												if (!drilled.getAttribute("name").equals(drillTo)) {
													continue;
												} else {
													//this is the section we want.
													ModManager.debugLogger.writeMessageConditionally("Found " + drillTo, ModManager.LOG_MODMAKER);
													drilledList = drilled.getChildNodes();
													pathfound = true;
													break;
												}
											}
										}
										if (!pathfound) {
											dispose();
											ModManager.debugLogger.writeError("Could not find property to update: " + path + " for property " + newPropName + " in module "
													+ intCoalName + ". PArt of the file: " + iniFileName);

											JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "<html>Could not find the path " + path + " for property " + newPropName
													+ ".<br>Module: " + intCoalName + "<br>File: " + iniFileName + "</html>", "Compiling Error", JOptionPane.ERROR_MESSAGE);
											error = true;
											return null;
										}
									}
									if (drilled == null) {
										//we didn't find what we wanted...
										dispose();
										error = true;
										JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "<html>Could not find the path " + path + " for property " + newPropName
												+ ".<br>Module: " + intCoalName + "<br>File: " + iniFileName + "</html>", "Compiling Error", JOptionPane.ERROR_MESSAGE);
										return null;
									}
									if (operation.equals("addition")) {
										//only for arrays
										//we won't find anything to match, since it obviously can't exist. Add it from here.
										ModManager.debugLogger.writeMessageConditionally("Creating new property with operation ADDITION", ModManager.LOG_MODMAKER);
										Element newElement;
										if (isArrayProperty) {
											newElement = drilled.getOwnerDocument().createElement("Value");
										} else {
											newElement = drilled.getOwnerDocument().createElement("Property");
											newElement.setAttribute("name", newPropName);
										}
										if (UE3type != null && !UE3type.equals("")) {
											newElement.setAttribute("type", UE3type);
										}
										newElement.setTextContent(newValue);
										drilled.appendChild(newElement);
										continue; //continue property loop
									}

									//we've drilled down as far as we can.

									//we are where we want to be. Now we can set the property or array value.
									//drilled is the element (parent of our property) that we want.
									NodeList props = drilled.getChildNodes(); //get children of the path (<property> list)
									ModManager.debugLogger.writeMessageConditionally("Number of child property/elements to search: " + props.getLength(), ModManager.LOG_MODMAKER);
									boolean foundProperty = false;
									for (int m = 0; m < props.getLength(); m++) {
										Node propertyNode = props.item(m);
										if (propertyNode.getNodeType() == Node.ELEMENT_NODE) {
											Element itemToModify = (Element) propertyNode;
											//Check on property
											if (!isArrayProperty) {
												//property
												boolean shouldBreak = false;
												switch (operation) {
												case "assignment":
													if (itemToModify.getAttribute("name").equals(newPropName)) {
														itemToModify.setTextContent(newValue);
														ModManager.debugLogger.writeMessageConditionally("Assigning " + newPropName + " to " + newValue, ModManager.LOG_MODMAKER);
														foundProperty = true;
														shouldBreak = true;
													}
													break;
												case "subtraction":
													if (itemToModify.getAttribute("name").equals(newPropName)) {
														ModManager.debugLogger.writeMessageConditionally("Subtracting property " + newPropName, ModManager.LOG_MODMAKER);
														Node itemParent = itemToModify.getParentNode();
														itemParent.removeChild(itemToModify);
														foundProperty = true;
														shouldBreak = true;
														break;
													}
												}
												if (shouldBreak) {
													break;
												}
											} else {
												//Check on ArrayProperty
												//ModManager.debugLogger.writeMessage("Candidates only will be returned if they are of type: "+matchontype);
												//ModManager.debugLogger.writeMessage("Scanning property type: "+itemToModify.getAttribute("type"));
												if (itemToModify.getAttribute("type").equals(matchontype)) {
													//potential array value candidate...
													boolean match = false;
													ModManager.debugLogger.writeMessageConditionally(
															"Found type candidate (" + matchontype + ") for arrayreplace: " + itemToModify.getTextContent(),
															ModManager.LOG_MODMAKER);
													switch (arrayType) {
													//Must use individual matching algorithms so we can figure out if something matches.
													case "exactvalue": {
														if (itemToModify.getTextContent().equals(newValue)) {
															ModManager.debugLogger.writeMessageConditionally("exact Property match found.", ModManager.LOG_MODMAKER);
															match = true;
														}
													}
														break;
													case "biodifficulty": {
														//Match on Category (name)
														Category existing = new Category(itemToModify.getTextContent());
														Category importing = new Category(newValue);
														if (existing.matchIdentifiers(importing)) {
															ModManager.debugLogger.writeMessageConditionally("Match found: " + existing.categoryname, ModManager.LOG_MODMAKER);
															existing.merge(importing);
															newValue = existing.createCategoryString();
															match = true;
														} else {
															ModManager.debugLogger.writeMessageConditionally("Match failed: " + existing.categoryname, ModManager.LOG_MODMAKER);
														}
													}
														break;
													case "wavelist": {
														//Match on Difficulty
														Wave existing = new Wave(itemToModify.getTextContent());
														Wave importing = new Wave(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
															ModManager.debugLogger.writeMessageConditionally("Wavelist match on " + existing.difficulty, ModManager.LOG_MODMAKER);
															newValue = importing.createWaveString(); //doens't really matter, but makes me feel good my code works
														} else {
															//CHECK FOR COLLECTOR PLAT WAVE 5. It has a bug where the dolevel is 3 even though it is plat.
															String cplatwave5 = "(Difficulty=DO_Level3,Enemies=( (EnemyType=\"WAVE_COL_Scion\"), (EnemyType=\"WAVE_COL_Praetorian\", MinCount=1, MaxCount=1), (EnemyType=\"WAVE_CER_Phoenix\", MinCount=2, MaxCount=2), (EnemyType=\"WAVE_CER_Phantom\", MinCount=3, MaxCount=3) ))";
															if (itemToModify.getTextContent().equals(cplatwave5)
																	&& path.equals("sfxwave_horde_collector5 sfxwave_horde_collector&enemies")
																	&& importing.difficulty.equals("DO_Level4")) {
																match = true;
																newValue = importing.createWaveString(); //doens't really matter, but makes me feel good my code works
															}
														}
													}
														break;
													case "possessionwaves": {
														//Match on Difficulty/DoLevel
														//Match on Difficulty
														Difficulty existing = new Difficulty(itemToModify.getTextContent());
														Difficulty importing = new Difficulty(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
															//newValue = importing.createDifficultyString(); //doens't really matter, but makes me feel good my code works
															//and it was broken
														}
													}
														break;
													case "shareddifficulty":
													case "wavebudget": {
														//Match on SharedDifficulty (DO_Level)
														SharedDifficulty existing = new SharedDifficulty(itemToModify.getTextContent());
														SharedDifficulty importing = new SharedDifficulty(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
														}
													}
														break;
													case "enemytype":
													case "wavecost": { //wavecost is old name for enemytype (modmaker 1.6)
														EnemyType existing = new EnemyType(itemToModify.getTextContent());
														EnemyType importing = new EnemyType(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
														}
													}
														break;
													case "waveclass": {
														WaveClass existing = new WaveClass(itemToModify.getTextContent());
														WaveClass importing = new WaveClass(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
														}
													}
														break;
													case "id": {
														ID existing = new ID(itemToModify.getTextContent());
														ID importing = new ID(newValue);
														if (existing.matchIdentifiers(importing)) {
															match = true;
														}
													}
														break;
													default:
														ModManager.debugLogger.writeError(
																"ERROR: Unknown matching algorithm: " + arrayType + ". does this client need updated? Aborting this stat update.");
														JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
																"<html>Unknown matching algorithm from ME3Tweaks: " + arrayType
																		+ ".<br>You should check for updates to Mod Manager.<br>This mod will not fully compile.</html>",
																"Compiling Error", JOptionPane.ERROR_MESSAGE);
														break;
													} //end matching algorithm switch
													if (match) {
														foundProperty = true;
														switch (operation) {
														case "subtraction":
															Node itemParent = itemToModify.getParentNode();
															itemParent.removeChild(itemToModify);
															ModManager.debugLogger.writeMessageConditionally("Removed array value: " + newValue, ModManager.LOG_MODMAKER);
															break;
														case "modify": //same as assignment right now
														case "assignment":
															itemToModify.setTextContent(newValue);
															ModManager.debugLogger.writeMessageConditionally("Assigned array value: " + newValue, ModManager.LOG_MODMAKER);
															break;
														default:
															ModManager.debugLogger.writeError(
																	"Unknown matching algorithm: " + arrayType + " does this client need updated? Aborting this stat update.");
															JOptionPane.showMessageDialog(ModMakerCompilerWindow.this,
																	"<html>Unknown operation from ME3Tweaks: " + operation
																			+ ".<br>You should check for updates to Mod Manager.<br>This mod will not fully compile.</html>",
																	"Compiling Error", JOptionPane.ERROR_MESSAGE);
															break;
														} //end operation [switch]
														break;
													} //end of match = true [if]
												} //end of array matchontype check [if]
											} //end of array property [if]
										} //end of property = element node (not junk) [if]
									} //end of props.length to search through. [for loop]
									if (foundProperty != true) {
										if (modMakerVersion > 1.0) {
											StringBuilder sb = new StringBuilder();
											sb.append("<html>Could not find the following attribute:<br>");
											sb.append("Coalesced File: ");
											sb.append(intCoalName);
											sb.append("<br>");
											sb.append("Subfile: ");
											sb.append(fileNode.getNodeName());
											sb.append("<br>");

											sb.append("Path: ");
											sb.append(path);
											sb.append("<br>");
											sb.append("Operation: ");
											sb.append(operation);
											sb.append("<br>");
											if (isArrayProperty) {
												sb.append("====ARRAY ATTRIBUTE INFO=======<br>");
												sb.append("Array matching algorithm: ");
												sb.append(arrayType);
												sb.append("<br>Matching type: ");
												sb.append(matchontype);
											} else {
												sb.append("====STANDARD ATTRIBUTE INFO====<br>");
												sb.append("Keyed Property Name: ");
												sb.append(newPropName);
											}
											sb.append("<br>=================");
											sb.append("<br>");
											sb.append("New data: ");
											sb.append(newValue);
											sb.append("</html>");

											JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, sb.toString(), "Compiling Error", JOptionPane.ERROR_MESSAGE);
											ModManager.debugLogger.writeError(sb.toString());
										}
									}
								}
							}
							//end of the file node.
							//Time to save the file...
							Transformer transformer = TransformerFactory.newInstance().newTransformer();
							transformer.setOutputProperty(OutputKeys.INDENT, "yes");
							transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "1");
							File outputFile = new File(ModManager.getCompilingDir() + "coalesceds\\" + foldername + "\\" + iniFileName);
							Result output = new StreamResult(outputFile);
							Source input = new DOMSource(iniFile);
							ModManager.debugLogger.writeMessage("Saving file: " + outputFile.toString());
							transformer.transform(input, output);
						}
					}
					coalsMerged++;
					//System.err.println((coalsMerged / (numCoalSections * 1.0)) * 100);
					publish(new ThreadCommand("MERGE_TASK_COMPLETED"));
				}
			}

			return null;
		}

		@Override
		protected void process(List<ThreadCommand> commands) {
			for (ThreadCommand tc : commands) {
				switch (tc.getCommand()) {
				case "MERGE_TASK_COMPLETED":
					if (coalsMerged != 0) {
						//System.err.println((coalsMerged / (float) numCoalSections) * 100);
						progress.setValue((int) Math.ceil(((coalsMerged / (numCoalSections * 1.0)) * 100)));
					} else {
						progress.setValue(0);
					}
					break;
				case "MERGING_FILE":
					String currentoperation = tc.getMessage();
					currentOperationLabel.setText("Merging " + currentoperation);
					break;
				}
			}
		}

		protected void done() {
			// Merge thread finished
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeMessage("Error occured in MergeWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "An error occured while trying to merge mod delta into coalesced files:\n" + e.getMessage()
						+ "\n\nYou should report this to FemShep via the Forums link in the help menu.", "Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				dispose();
				error = true;
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("ModMaker mod failed to compile");
				return;
			}
			if (error) {
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("ModMaker mod failed to compile");
				dispose();
				return;
			}
			ModManager.debugLogger.writeMessage("Finished merging coals.");

			stepsCompleted++;
			overallProgress.setValue((int) ((100 / (TOTAL_STEPS / stepsCompleted)) + 0.5));
			new CompilerWorker(requiredCoals, progress).execute();
		}
	}

	class TLKWorker extends SwingWorker<Void, Object> {
		private JProgressBar progress;
		boolean error = false;
		ArrayList<String> languages;
		int jobsToDo = 0, jobsDone = 0;

		public TLKWorker(JProgressBar progress, ArrayList<String> languages) {
			ModManager.debugLogger.writeMessage("Beginning TLK Decompile operation.");
			this.progress = progress;
			this.languages = languages;
			currentOperationLabel.setText("Creating language specific changes...");
			progress.setIndeterminate(true);
			progress.setValue(0);
		}

		protected Void doInBackground() throws Exception {
			NodeList tlkElementNodeList = doc.getElementsByTagName("TLKData");
			if (tlkElementNodeList.getLength() < 1) {
				ModManager.debugLogger.writeMessage("No TLK in mod file, or length is 0.");
				return null; //skip tlk, mod doesn't have it
			}
			tlkElement = (Element) tlkElementNodeList.item(0);

			NodeList tlkNodeList = tlkElement.getChildNodes();
			//Iterate over the tlk entries.
			//get number of jobs.

			for (int i = 0; i < tlkNodeList.getLength(); i++) {
				//coalNode is a node containing the coalesced module, such as <MP1> or <BASEGAME>
				Node tlkNode = tlkNodeList.item(i);
				if (tlkNode.getNodeType() == Node.ELEMENT_NODE) {
					if (languages.contains(tlkNode.getNodeName())) {
						jobsToDo += 3; //decomp, edit, comp
					}
				}
			}

			for (int i = 0; i < tlkNodeList.getLength(); i++) {
				Node tlkNode = tlkNodeList.item(i);
				if (tlkNode.getNodeType() == Node.ELEMENT_NODE) {
					String tlkType = tlkNode.getNodeName(); //get the tlk name so we can figure out what tlk to modify
					if (languages.contains(tlkNode.getNodeName())) {
						ModManager.debugLogger.writeMessage("Read TLK ID: " + tlkType);
						ModManager.debugLogger.writeMessage("---------------------START OF " + tlkType + "-------------------------");
						publish(new ThreadCommand("SET_STATUS", "Decompiling " + tlkType + " language file"));
						//decompile TLK to tlk folder
						File tlkdir = new File(ModManager.getCompilingDir() + "tlk/");
						tlkdir.mkdirs(); // created tlk directory

						//START OF TLK DECOMPILE=========================================================
						ArrayList<String> commandBuilder = new ArrayList<String>();

						String compilerPath = ModManager.getTankMasterTLKDir() + "MassEffect3.TlkEditor.exe";
						commandBuilder.add(compilerPath);
						commandBuilder.add(ModManager.appendSlash(ModManagerWindow.GetBioGameDir()) + "CookedPCConsole\\" + tlkShortNameToFileName(tlkType));
						commandBuilder.add(ModManager.appendSlash(tlkdir.getAbsolutePath().toString()) + "BIOGame_" + tlkType + ".xml");
						commandBuilder.add("--mode");
						commandBuilder.add("ToXml");
						commandBuilder.add("--no-ui");

						//System.out.println("Building command");
						String[] command = commandBuilder.toArray(new String[commandBuilder.size()]);
						//Debug stuff
						StringBuilder sb = new StringBuilder();
						for (String arg : command) {
							sb.append(arg + " ");
						}
						ModManager.debugLogger.writeMessage("Executing TLK Decompile command: " + sb.toString());
						Process p = null;
						int returncode = 1;
						try {
							ProcessBuilder pb = new ProcessBuilder(command);
							pb.redirectErrorStream(true);
							ModManager.debugLogger.writeMessage("Executing process for TLK Decompile Job.");
							p = pb.start();
							returncode = p.waitFor();
							ModManager.debugLogger.writeMessage("TLK Job return code: " + returncode);
						} catch (IOException | InterruptedException e) {
							ModManager.debugLogger.writeException(e);
						}
						this.publish(++jobsDone);
						//END OF DECOMPILE==================================================
						//iterate over TLK indexes and load into memory
						publish(new ThreadCommand("SET_STATUS", "Modifying " + tlkType + " language file"));

						HashMap<Integer, TLKFragment> indexMap = new HashMap<Integer, TLKFragment>();
						NodeList localizedNodeList = tlkNode.getChildNodes();
						for (int j = 0; j < localizedNodeList.getLength(); j++) {
							Node stringNode = localizedNodeList.item(j);
							if (stringNode.getNodeType() == Node.ELEMENT_NODE) {
								Element stringElement = (Element) stringNode;
								int index = 0; //<1.6 mods
								String markedIndex = stringElement.getAttribute("index");
								if (markedIndex != null && !markedIndex.equals("")) {
									index = Integer.parseInt(markedIndex);
								}

								if (!indexMap.containsKey(index)) {
									//load the required XML file into memory, store its nodelist
									ModManager.debugLogger.writeMessage("Loading TLK XML (index " + index + ") " + tlkType + " into memory.");
									String tlkIndexedFrag = ModManager.appendSlash(tlkdir.getAbsolutePath().toString()) + "BIOGame_" + tlkType + "\\" + "BIOGame_" + tlkType + index
											+ ".xml"; //tankmaster's compiler splits it into files.
									DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
									Document tlkXMLFile = dBuilder.parse("file:///" + tlkIndexedFrag);
									tlkXMLFile.getDocumentElement().normalize();
									ModManager.debugLogger.writeMessage("Loaded TLK " + tlkXMLFile.getDocumentURI() + " into memory.");
									NodeList stringsInTLK = tlkXMLFile.getElementsByTagName("String");
									indexMap.put(index, new TLKFragment(tlkIndexedFrag, tlkXMLFile, stringsInTLK, index));
								}
							}
						}

						//Iterate over the tlk entries and update them
						for (int j = 0; j < localizedNodeList.getLength(); j++) {
							//stringNode  is a node containing the String module <String id=x>
							Node stringNode = localizedNodeList.item(j);
							if (stringNode.getNodeType() == Node.ELEMENT_NODE) {
								Element stringElement = (Element) stringNode;

								String id = stringElement.getAttribute("id");
								int index = 0; //<1.6 mods
								String markedIndex = stringElement.getAttribute("index");
								if (markedIndex != null && !markedIndex.equals("")) {
									index = Integer.parseInt(markedIndex);
								}
								String content = stringElement.getTextContent();
								ModManager.debugLogger.writeMessage("Scanning for string id " + id + "...");

								//get proper TLKFragment, scan XML for it...
								TLKFragment tfrag = indexMap.get(index);
								NodeList stringsInTLK = tfrag.getStringsList();
								for (int s = 0; s < stringsInTLK.getLength(); s++) {
									//get node
									Node tlkStringNode = stringsInTLK.item(s);
									if (tlkStringNode.getNodeType() == Node.ELEMENT_NODE) {
										Element stringTLKElement = (Element) tlkStringNode;
										//System.out.println("Checking "+id+" vs "+stringTLKElement.getAttribute("id"));
										if (stringTLKElement.getAttribute("id").equals(id)) {
											ModManager.debugLogger.writeMessage("Updating string id " + id + " to " + content);
											stringTLKElement.setTextContent(content);
										}
									}
								}
							}
						}
						//end of the file node.
						//Save XML files
						Transformer transformer = TransformerFactory.newInstance().newTransformer();
						for (Map.Entry<Integer, TLKFragment> entry : indexMap.entrySet()) {
							TLKFragment fragment = entry.getValue();
							File outputFile = new File(fragment.getFilepath());
							Result output = new StreamResult(outputFile);
							Source input = new DOMSource(fragment.getOwningDocument());
							ModManager.debugLogger.writeMessage("Saving file: " + outputFile.toString());
							transformer.transform(input, output);
						}

						this.publish(++jobsDone);
						//create new TLK file from this.
						//START OF TLK COMPILE=========================================================
						publish(new ThreadCommand("SET_STATUS", "Recompiling " + tlkType + " language file"));
						ArrayList<String> tlkCompileCommandBuilder = new ArrayList<String>();
						tlkCompileCommandBuilder.add(compilerPath);
						tlkCompileCommandBuilder.add(ModManager.appendSlash(tlkdir.getAbsolutePath().toString()) + "BIOGame_" + tlkType + ".xml");
						tlkCompileCommandBuilder.add(ModManager.appendSlash(tlkdir.getAbsolutePath().toString()) + "BIOGame_" + tlkType + ".tlk");
						tlkCompileCommandBuilder.add("--mode");
						tlkCompileCommandBuilder.add("ToTlk");
						tlkCompileCommandBuilder.add("--no-ui");

						//System.out.println("Building command");
						String[] tlkCompilecommand = tlkCompileCommandBuilder.toArray(new String[commandBuilder.size()]);
						//Debug stuff
						StringBuilder tlkCompileCommandsb = new StringBuilder();
						for (String arg : tlkCompilecommand) {
							tlkCompileCommandsb.append(arg + " ");
						}
						ModManager.debugLogger.writeMessage("Executing TLK Compile command: " + tlkCompileCommandsb.toString());
						p = null;
						try {
							ProcessBuilder pb = new ProcessBuilder(tlkCompilecommand);
							pb.redirectErrorStream(true);
							ModManager.debugLogger.writeMessage("Executing process for TLK Compile Job.");
							//p = Runtime.getRuntime().exec(command);
							p = pb.start();
							returncode = p.waitFor();
						} catch (IOException | InterruptedException e) {
							ModManager.debugLogger.writeMessage(ExceptionUtils.getStackTrace(e));
							e.printStackTrace();
						}
						//END OF COMPILE==================================================
						this.publish(++jobsDone);
					} else {
						ModManager.debugLogger.writeMessage("Language not chosen for TLK compiling: skipping " + tlkType);
					}
				}
			}
			return null;
		}

		public void process(List<Object> chunks) {
			for (Object obj : chunks) {
				if (obj instanceof ThreadCommand) {
					ThreadCommand error = (ThreadCommand) obj;
					String cmd = error.getCommand();
					switch (cmd) {
					case "SET_STATUS":
						currentOperationLabel.setText(error.getMessage());
						break;
					}
				} else if (obj instanceof Integer) {
					Integer progressVal = (Integer) obj;
					float progressAsFloat = (float) (progressVal * 1.0);
					progress.setValue((int) (100 / (jobsToDo / (float) progressAsFloat)));
				}
			}
		}

		protected void done() {
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeError("Error occured in TLKWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "An error occured while trying to decompile the TLK (translations) file:\n" + e.getMessage()
						+ "\n\nYou should report this to FemShep via the Forums link in the help menu.", "Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			if (error) {
				dispose();
				return;
			}

			// tlks decompiled.
			if (error) {
				dispose();
				return;
			}
			ModManager.debugLogger.writeMessage("Finished decompiling TLK files.");

			stepsCompleted++;
			overallProgress.setValue((int) ((100 / (TOTAL_STEPS / stepsCompleted)) + 0.5));
			new TOCDownloadWorker(requiredCoals, progress).execute();

		}
	}

	/**
	 * Creates a CMM Mod package from the completed previous steps.
	 */
	private void createCMMMod() {
		ModManager.debugLogger.writeMessage("----Creating CMM Mod Package and descriptor-----");

		boolean error = false;
		currentOperationLabel.setText("Creating mod directory and descriptor...");
		File moddir = new File(ModManager.getModsDir() + modName);
		ModManager.debugLogger.writeMessage("Mod package directory set to: " + moddir.getAbsolutePath());
		moddir.mkdirs(); // created mod directory

		// Write mod descriptor file
		Wini ini;
		File moddesc = new File(moddir + "\\moddesc.ini");
		try {
			ModManager.debugLogger.writeMessage("Checking for moddesc.ini: " + moddesc.getAbsolutePath());
			if (!moddesc.exists()) {
				ModManager.debugLogger.writeMessage("moddesc.ini does not exist, creating new one for this mod.");
				moddesc.createNewFile();
			}
			ModManager.debugLogger.writeMessage("Creating in-memory moddesc.ini");
			ini = new Wini(moddesc);
			ini.put("ModManager", "cmmver", 4.3);
			ini.put("ModInfo", "modname", modName);
			ini.put("ModInfo", "moddev", modDev);
			ini.put("ModInfo", "moddesc", modDescription + "<br>Created with ME3Tweaks ModMaker.");
			ini.put("ModInfo", "modsite", "https://me3tweaks.com/modmaker/mods/" + modId);
			ini.put("ModInfo", "modid", modId);
			ini.put("ModInfo", "compiledagainst", modMakerVersion);
			if (modVer != null) {
				ini.put("ModInfo", "modver", modVer);
			}

			// Create directories, move files to them
			for (String reqcoal : requiredCoals) {
				File compCoalDir = new File(moddir.toString() + "\\" + ME3TweaksUtils.coalFilenameToInternalName(reqcoal)); //MP4, PATCH2 folders in mod package
				compCoalDir.mkdirs();
				String fileNameWithOutExt = FilenameUtils.removeExtension(reqcoal);
				//copy coal
				File coalFile = new File(ModManager.getCompilingDir() + "coalesceds\\" + fileNameWithOutExt + "\\" + reqcoal);
				File destCoal = new File(compCoalDir + "\\" + reqcoal);
				destCoal.delete();
				ModManager.debugLogger.writeMessage("Moving Coalesced file: " + coalFile.getAbsolutePath() + " to " + destCoal.getAbsolutePath());
				if (coalFile.renameTo(destCoal)) {
					ModManager.debugLogger.writeMessage("Moved " + reqcoal + " to proper mod element directory");
				} else {
					ModManager.debugLogger.writeError("ERROR! Didn't move " + reqcoal + " to the proper mod element directory. Could already exist.");
				}
				//copy pcconsoletoc
				if (!reqcoal.equals("ServerCoalesced.bin")) {
					File tocFile = new File(ModManager.getCompilingDir() + "toc\\" + ME3TweaksUtils.coalFilenameToInternalName(reqcoal) + "\\PCConsoleTOC.bin");
					File destToc = new File(compCoalDir + "\\PCConsoleTOC.bin");
					destToc.delete();
					ModManager.debugLogger.writeMessage("Moving TOC file: " + tocFile.getAbsolutePath() + " to " + destToc.getAbsolutePath());
					if (tocFile.renameTo(destToc)) {
						ModManager.debugLogger.writeMessage("Moved " + reqcoal + " TOC to proper mod element directory");
					} else {
						ModManager.debugLogger.writeError("ERROR! Didn't move " + reqcoal + " TOC to the proper mod element directory. Could already exist.");
					}
				} else {
					ModManager.debugLogger.writeMessage("Skipping BALANCE_CHANGES PCConsoleTOC.bin");
				}

				//copy tlk
				if (reqcoal.equals("Coalesced.bin")) {
					ModManager.debugLogger.writeMessage("Coalesced pass: Checking for TLK files");
					//it is basegame. copy the tlk files!
					String[] tlkFiles = ModManager.SUPPORTED_GAME_LANGUAGES;
					for (String tlkFilename : tlkFiles) {
						File compiledTLKFile = new File(ModManager.getCompilingDir() + "tlk\\" + "BIOGame_" + tlkFilename + ".tlk");
						if (!compiledTLKFile.exists()) {
							ModManager.debugLogger.writeMessage("TLK file " + compiledTLKFile + " is missing, might not have been selected for compilation. skipping.");
							continue;
						}
						File destTLKFile = new File(compCoalDir + "\\BIOGame_" + tlkFilename + ".tlk");
						ModManager.debugLogger.writeMessage("Moving TLK file: " + compiledTLKFile.getAbsolutePath() + " to " + destTLKFile.getAbsolutePath());
						if (compiledTLKFile.renameTo(destTLKFile)) {
							ModManager.debugLogger.writeMessage("Moved " + compiledTLKFile + " TLK to BASEGAME directory");
						} else {
							ModManager.debugLogger.writeError("Didn't move " + compiledTLKFile + " TLK to the BASEGAME directory. Could already exist.");
						}
					}
				}

				boolean basegame = reqcoal.equals("Coalesced.bin");
				boolean balance = reqcoal.equals("ServerCoalesced.bin");
				ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "moddir", ME3TweaksUtils.coalFilenameToInternalName(reqcoal));

				//build descriptors
				if (basegame) {
					ModManager.debugLogger.writeMessage("Creating new mod, processing basegame coalesced pass");
					StringBuilder newsb = new StringBuilder();
					StringBuilder replacesb = new StringBuilder();
					//if (!basegameTLKOnly) {
					//coalesced
					newsb.append(reqcoal);
					replacesb.append(coalFileNameToDLCDir(reqcoal));
					//}

					//tlk, if they exist.
					String[] tlkFiles = ModManager.SUPPORTED_GAME_LANGUAGES;
					for (String tlkFilename : tlkFiles) {
						File basegameTLKFile = new File(compCoalDir + "\\BIOGame_" + tlkFilename + ".tlk");
						if (basegameTLKFile.exists()) {
							if (newsb.toString().length() > 0) {
								newsb.append(";");
								replacesb.append(";");
							}
							newsb.append("BIOGame_" + tlkFilename + ".tlk");
							replacesb.append("\\BIOGame\\CookedPCConsole\\BIOGame_" + tlkFilename + ".tlk");
							continue;
						}
					}
					if (newsb.toString().length() > 0) {
						newsb.append(";");
						replacesb.append(";");
					}
					newsb.append("PCConsoleTOC.bin");
					replacesb.append(ME3TweaksUtils.coalFileNameToDLCTOCDir(reqcoal));
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "newfiles", newsb.toString());
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "replacefiles", replacesb.toString());
				} else if (balance) {
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "newfiles", reqcoal + "");
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "replacefiles", coalFileNameToDLCDir(reqcoal));
				} else {
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "newfiles", reqcoal + ";PCConsoleTOC.bin");
					ini.put(ME3TweaksUtils.coalFilenameToHeaderName(reqcoal), "replacefiles",
							coalFileNameToDLCDir(reqcoal) + ";" + ME3TweaksUtils.coalFileNameToDLCTOCDir(reqcoal));
				}

				File compCoalSourceDir = new File(ModManager.getCompilingDir() + "coalesceds\\" + fileNameWithOutExt);
				try {
					FileUtils.deleteDirectory(compCoalSourceDir);
					ModManager.debugLogger.writeMessage("Deleted compiled coal directory: " + compCoalSourceDir);
				} catch (IOException e) {
					ModManager.debugLogger.writeMessage("IOException deleting compCoalSourceDir.");
					ModManager.debugLogger.writeException(e);
				}
			}

			//TLK Only, no coalesced
			if (languages.size() > 0 && !requiredCoals.contains("Coalesced.bin")) {
				File compCoalDir = new File(moddir.toString() + "\\" + ME3TweaksUtils.coalFilenameToInternalName("Coalesced.bin")); //MP4, PATCH2 folders in mod package
				compCoalDir.mkdirs();
				ini.put(ME3TweaksUtils.coalFilenameToHeaderName("Coalesced.bin"), "moddir", ME3TweaksUtils.coalFilenameToInternalName("Coalesced.bin"));

				//MOVE THE TLK FILES
				for (String tlkFilename : languages) {
					File compiledTLKFile = new File(ModManager.getCompilingDir() + "tlk\\" + "BIOGame_" + tlkFilename + ".tlk");
					if (!compiledTLKFile.exists()) {
						ModManager.debugLogger.writeMessage("TLK file " + compiledTLKFile + " is missing, might not have been selected for compilation. skipping.");
						continue;
					}
					File destTLKFile = new File(compCoalDir + "\\BIOGame_" + tlkFilename + ".tlk");
					if (compiledTLKFile.renameTo(destTLKFile)) {
						ModManager.debugLogger.writeMessage("Moved " + compiledTLKFile + " TLK to BASEGAME directory");
					} else {
						ModManager.debugLogger.writeMessage("Didn't move " + compiledTLKFile + " TLK to the BASEGAME directory. Could already exist.");
					}
				}

				//MOVE PCCONSOLETOC.bin
				File tocFile = new File(ModManager.getCompilingDir() + "toc\\" + ME3TweaksUtils.coalFilenameToInternalName("Coalesced.bin") + "\\PCConsoleTOC.bin");
				File destToc = new File(compCoalDir + "\\PCConsoleTOC.bin");
				destToc.delete();
				if (tocFile.renameTo(destToc)) {
					ModManager.debugLogger.writeMessage("Moved BASEGAME TOC to proper mod element directory");
				} else {
					ModManager.debugLogger.writeMessage("ERROR! Didn't move BASEGAME TOC to the proper mod element directory. Could already exist.");
				}
				StringBuilder newsb = new StringBuilder();
				StringBuilder replacesb = new StringBuilder();

				//tlk, if they exist.
				for (String tlkFilename : languages) {
					File basegameTLKFile = new File(compCoalDir + "\\BIOGame_" + tlkFilename + ".tlk");
					if (basegameTLKFile.exists()) {
						newsb.append("BIOGame_" + tlkFilename + ".tlk");
						replacesb.append("\\BIOGame\\CookedPCConsole\\BIOGame_" + tlkFilename + ".tlk");
						continue;
					}
				}
				newsb.append(";PCConsoleTOC.bin");
				replacesb.append(";" + ME3TweaksUtils.coalFileNameToDLCTOCDir("Coalesced.bin"));
				ini.put(ME3TweaksUtils.coalFilenameToHeaderName("Coalesced.bin"), "newfiles", newsb.toString());
				ini.put(ME3TweaksUtils.coalFilenameToHeaderName("Coalesced.bin"), "replacefiles", replacesb.toString());
			} //end tlk only basegame

			ModManager.debugLogger.writeMessage("Writing memory ini to disk.");
			ini.store();
			ModManager.debugLogger.writeMessage("Removing temporary directories:");
			try {
				FileUtils.deleteDirectory(new File(ModManager.getCompilingDir() + "tlk"));
				ModManager.debugLogger.writeMessage("Deleted tlk");
				FileUtils.deleteDirectory(new File(ModManager.getCompilingDir() + "toc"));
				ModManager.debugLogger.writeMessage("Deleted toc");
				FileUtils.deleteDirectory(new File(ModManager.getCompilingDir() + "coalesceds"));
				ModManager.debugLogger.writeMessage("Deleted coalesceds");
			} catch (IOException e) {
				ModManager.debugLogger.writeMessage("IOException deleting one of the tlk/toc/coalesced directories.");
				ModManager.debugLogger.writeException(e);
			}
		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			ModManager.debugLogger.writeMessage("IOException in main chunk of CreateCMMMod()! Setting error flag to true.");
			ModManager.debugLogger.writeException(e);
			error = true;
		}

		//TOC the mod
		ModManager.debugLogger.writeMessage("Loading moddesc for verification...");
		Mod newMod = new Mod(moddesc.toString());

		if (!newMod.isValidMod()) {
			//SOMETHING WENT WRONG!
			ModManager.debugLogger.writeMessage("Mod failed validation. Setting error flag to true.");
			error = true;
			JOptionPane.showMessageDialog(this, modName
					+ " was not successfully created.\nGenerate a diagnostics log from the help menu and search for errors.\nContact FemShep if you need help via the forums.",
					"Mod Not Created", JOptionPane.ERROR_MESSAGE);
		}
		/*
		 * File file = new File(DOWNLOADED_XML_FILENAME); file.delete();
		 * ModManager.debugLogger.writeMessage(
		 * "Deleted downloaded me3tweaks modinfo file");
		 */

		if (mod != null) {
			//its an update
			if (!mod.getModName().equals(modName)) {
				//mod on server has changed the name
				ModManager.debugLogger.writeMessage("Deleteing old mod directory as name has changed");
				try {
					FileUtils.deleteDirectory(new File(mod.getModPath()));
				} catch (IOException e) {
					JOptionPane.showMessageDialog(this, "The old version of this mod could not be deleted.\nBoth versions will remain.", "Could not delete old mod",
							JOptionPane.WARNING_MESSAGE);
					ModManager.debugLogger.writeError("Unable to delete old-named directory");
					ModManager.debugLogger.writeException(e);
				}
			}
		}

		if (!error) {
			//PROCESS MIXINS
			if (requiredMixinIds.size() > 0 || dynamicMixins.size() > 0) {
				currentOperationLabel.setText("Applying MixIns");
				ModManager.debugLogger.writeMessage("Mod delta recommends MixIns, running PatchLibraryWindow()");
				PatchLibraryWindow plw = new PatchLibraryWindow(this, requiredMixinIds, dynamicMixins, newMod);
				for (DynamicPatch dp : dynamicMixins) {
					FileUtils.deleteQuietly(dp.getOutputfile());
				}
			}
			finishModMaker(newMod);
		} else {
			ModManagerWindow.ACTIVE_WINDOW.submitJobCompletion(jobCode);
			dispose();
		}
	}

	public void finishModMaker(Mod newMod) {
		overallProgress.setValue(95);
		if (ModManager.AUTO_INJECT_KEYBINDS && hasKeybindsOverride()) {
			ModManager.debugLogger.writeMessage("Mod Manager has preference to auto install keybinds and keybinds override file is present.");
			new KeybindsInjectionWindow(ModManagerWindow.ACTIVE_WINDOW, newMod, true);
			overallProgress.setValue(98);
		}
		ModManager.debugLogger.writeMessage("Running AutoTOC on new mod: " + modName);
		new AutoTocWindow(newMod, AutoTocWindow.LOCALMOD_MODE, ModManagerWindow.GetBioGameDir());
		ModManagerWindow.ACTIVE_WINDOW.submitJobCompletion(jobCode);
		overallProgress.setValue(100);
		stepsCompleted++;
		ModManager.debugLogger.writeMessage("Mod successfully created:" + modName);
		ModManager.debugLogger.writeMessage("===========END OF MODMAKER========");
		//Mod Created!
		if (mod == null) {
			//updater supresses this window
			for (ModJob job : newMod.jobs) {
				if (job.getJobName().equals(ModTypeConstants.BINI)) {
					if (ModManager.checkIfASIBinkBypassIsInstalled(ModManagerWindow.GetBioGameDir())) {
						if (!ASIModWindow.IsASIModGroupInstalled(5)) {
							//loader installed, no balance changes replacer
							JOptionPane.showMessageDialog(this, "<html>" + modName
									+ " contains changes to the Balance Changes file.<br>For the mod to fully work you need to install the Balance Changes Replacer ASI from<br>the ASI Mod Management window, located at Mod Management > ASI Mod Manager.</html>",
									"Balance Changer Replacer ASI required", JOptionPane.WARNING_MESSAGE);
						}
					} else {
						JOptionPane.showMessageDialog(this, "<html><div style=\"width: 400px\">" + modName
								+ " contains changes to the Balance Changes file.<br>For the mod to fully work you need to install the ASI loader as well as the Balance Changes Replacer ASI from the ASI Mod Management window, located at Mod Management > ASI Mod Manager.</div></html>",
								"ASI Loader + Balance Changer Replacer ASI required", JOptionPane.WARNING_MESSAGE);
					}
					break;
				}
			}
			dispose();
			ModManagerWindow.ACTIVE_WINDOW.reloadModlist();
			ModManagerWindow.ACTIVE_WINDOW.highlightMod(newMod);
			JOptionPane.showMessageDialog(ModManagerWindow.ACTIVE_WINDOW, modName + " was successfully created!", "Mod Created", JOptionPane.INFORMATION_MESSAGE);
		}
		dispose();

	}

	private boolean hasKeybindsOverride() {
		File bioinputxml = new File(ModManager.getOverrideDir() + "bioinput.xml");
		return bioinputxml.exists();
	}

	class TOCDownloadWorker extends SwingWorker<Void, Integer> {
		private ArrayList<String> tocsToDownload;
		private JProgressBar progress;
		private int numtoc;

		public TOCDownloadWorker(ArrayList<String> tocsToDownload, JProgressBar progress) {
			progress.setIndeterminate(true);
			progress.setValue(0);
			this.tocsToDownload = new ArrayList<String>(tocsToDownload); //clone for downloading
			if (languages.size() > 0 && !this.tocsToDownload.contains("Coalesced.bin")) {
				this.tocsToDownload.add("Coalesced.bin");
			}
			this.tocsToDownload.remove("ServerCoalesced.bin");
			this.numtoc = this.tocsToDownload.size();
			this.progress = progress;
			if (numtoc > 0) {
				currentOperationLabel.setText("Downloading " + coalToTOCString(this.tocsToDownload.get(0)));
			}
			ModManager.debugLogger.writeMessage("============TOCDownloadWorker==========");

		}

		protected Void doInBackground() throws Exception {
			int tocsCompleted = 0;
			File tocDir = new File(ModManager.getCompilingDir() + "toc/");
			tocDir.mkdirs();
			for (String toc : tocsToDownload) {
				try {
					if (!ModManager.hasPristineTOC(toc, ME3TweaksUtils.FILENAME)) {
						ME3TweaksUtils.downloadPristineTOC(toc, ME3TweaksUtils.FILENAME);
					}
					File destTOC = new File(ModManager.getCompilingDir() + "toc/" + ME3TweaksUtils.coalFilenameToInternalName(toc) + "/PCConsoleTOC.bin"); //head should be same as standard folder
					FileUtils.copyFile(new File(ModManager.getPristineTOC(toc, ME3TweaksUtils.FILENAME)), destTOC);
					ModManager.debugLogger.writeMessage("Copied pristine TOC of COALESCED DLC(" + toc + ") to: " + destTOC.getAbsolutePath());
					tocsCompleted++;
					this.publish(tocsCompleted);
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					ModManager.debugLogger.writeException(e);
				} catch (Exception e) {
					ModManager.debugLogger.writeException(e);
				}
			}
			return null;
		}

		@Override
		protected void process(List<Integer> numCompleted) {

			progress.setIndeterminate(false);
			if (numtoc > numCompleted.get(0)) {
				currentOperationLabel.setText("Downloading " + ME3TweaksUtils.coalFilenameToInternalName(tocsToDownload.get(numCompleted.get(0))) + "/PCConsoleTOC.bin");
			}
			progress.setValue((int) (100 / (numtoc / (float) numCompleted.get(0))));
		}

		protected void done() {
			// TOCs downloaded
			try {
				get(); // this line can throw InterruptedException or ExecutionException
			} catch (ExecutionException e) {
				ModManager.debugLogger.writeMessage("Error occured in TOCDownloadWorker():");
				ModManager.debugLogger.writeException(e);
				JOptionPane.showMessageDialog(ModMakerCompilerWindow.this, "An error occured while trying to download the TOC files for the mod:\n" + e.getMessage()
						+ "\n\nYou should report this to FemShep via the Forums link in the help menu.", "Compiling Error", JOptionPane.ERROR_MESSAGE);
				error = true;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			if (error) {
				dispose();
				return;
			}

			ModManager.debugLogger.writeMessage("TOCs downloaded");
			stepsCompleted++;
			overallProgress.setValue((int) ((100 / (TOTAL_STEPS / stepsCompleted))));
			createCMMMod();
		}
	}

	public static String docToString(Document doc) {
		try {
			StringWriter sw = new StringWriter();
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

			transformer.transform(new DOMSource(doc), new StreamResult(sw));
			return sw.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

	public String coalToTOCString(String coalName) {
		switch (coalName) {
		case "Default_DLC_CON_MP1.bin":
			return "MP1 PCConsoleTOC.bin";
		case "Default_DLC_CON_MP2.bin":
			return "MP2 PCConsoleTOC.bin";
		case "Default_DLC_CON_MP3.bin":
			return "MP3 PCConsoleTOC.bin";
		case "Default_DLC_CON_MP4.bin":
			return "MP4 PCConsoleTOC.bin";
		case "Default_DLC_CON_MP5.bin":
			return "MP5 PCConsoleTOC.bin";
		case "Default_DLC_UPD_Patch01.bin":
			return "PATCH1 PCConsoleTOC.bin";
		case "Default_DLC_UPD_Patch02.bin":
			return "PATCH2 PCConsoleTOC.bin";
		case "Coalesced.bin":
			return "BASEGAME PCConsoleTOC.bin";
		default:
			return null;
		}
	}

	public JLabel getCurrentTaskLabel() {
		return currentOperationLabel;
	}

	class CoalescedCompilerTask implements Callable<Boolean> {
		private String coalescedFile;
		private CompilerWorker compilerWorker;
		private AtomicInteger COMPLETED;

		public CoalescedCompilerTask(String coalescedFile, AtomicInteger COMPLETED, CompilerWorker compilerWorker) {
			this.coalescedFile = coalescedFile;
			this.compilerWorker = compilerWorker;
			this.COMPLETED = COMPLETED;
		}

		@Override
		public Boolean call() throws Exception {
			String compilerdir = ModManager.getCompilingDir();
			String compilerPath = ModManager.getTankMasterCompilerDir() + "MassEffect3.Coalesce.exe";
			ArrayList<String> command = new ArrayList<>();
			command.add(compilerPath);
			command.add(compilerdir + "\\coalesceds\\" + FilenameUtils.removeExtension(coalescedFile) + "\\" + FilenameUtils.removeExtension(coalescedFile) + ".xml");
			command.add("--mode=ToBin");
			ProcessBuilder compileProcessBuilder = new ProcessBuilder(command);
			compilerWorker.publishProgress();
			boolean result = ModManager.runProcess(compileProcessBuilder, FilenameUtils.removeExtension(coalescedFile)).getReturnCode() == 0;
			if (result) {
				COMPLETED.incrementAndGet();
			}
			return result;
		}
	}

	class CoalescedDecompilerTask implements Callable<Boolean> {
		private String coalescedFile;
		private DecompilerWorker decompilerWorker;

		public CoalescedDecompilerTask(String coalescedFile, DecompilerWorker decompilerWorker) {
			this.coalescedFile = coalescedFile;
			this.decompilerWorker = decompilerWorker;
		}

		@Override
		public Boolean call() throws Exception {
			String compilerdir = ModManager.getCompilingDir();
			String compilerPath = ModManager.getTankMasterCompilerDir() + "MassEffect3.Coalesce.exe";
			ArrayList<String> command = new ArrayList<>();
			command.add(compilerPath);
			command.add(compilerdir + "\\coalesceds\\" + coalescedFile);
			ProcessBuilder compileProcessBuilder = new ProcessBuilder(command);
			decompilerWorker.publishProgress(decompilerWorker.COMPLETED.incrementAndGet());
			return ModManager.runProcess(compileProcessBuilder, FilenameUtils.removeExtension(coalescedFile)).getReturnCode() == 0;
		}
	}
}
