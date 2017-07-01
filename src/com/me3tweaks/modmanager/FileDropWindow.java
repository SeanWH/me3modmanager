package com.me3tweaks.modmanager;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.ArchUtils;

import com.me3tweaks.modmanager.ModManager.Lock;
import com.me3tweaks.modmanager.StarterKitWindow.StarterKitProgressDialog;
import com.me3tweaks.modmanager.modmaker.ModMakerCompilerWindow;
import com.me3tweaks.modmanager.modmaker.ModMakerEntryWindow;
import com.me3tweaks.modmanager.objects.ProcessResult;
import com.me3tweaks.modmanager.utilities.ResourceUtils;

public class FileDropWindow extends JDialog {
	File droppedFile;
	boolean show = true;
	private final Object lock = new Lock(); //threading wait() and notifyall();

	public FileDropWindow(JFrame parentFrame, File file) {
		droppedFile = file;
		setupWindow2();
		if (show) {
			setVisible(true);
		}
	}

	private void setupWindow2() {
		setTitle("Files drop task selector");
		setIconImages(ModManager.ICONS);

		if (droppedFile.isDirectory()) {
			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();

			c.gridy = 0;
			JLabel headerLabel = new JLabel("<html>You dropped a folder onto Mod Manager:<br>" + droppedFile
					+ "<br>Select what operation to perform on the contents of this folder.<br>Hover over each button to see a description.</html>");
			panel.add(headerLabel, c);

			JButton compileAllTLK = new JButton("Compile all TLK XML Manifests");

			JButton decompileAllTLK = new JButton("Decompile all TLK files");
			JButton compileAllCoalesced = new JButton("Compile all Coalesced manifest");
			JButton decompileAllCoalesced = new JButton("Decompile all Coalesced files");
			JButton decompressAllPcc = new JButton("Decompress all PCC files");
			JButton compressAllPcc = new JButton("Compress all PCC files");
			JButton sideloadAllModMaker = new JButton("Sideload all ModMaker XML files");

			compileAllTLK.setToolTipText("<html>Treats each .xml file in the folder as a TankMaster TLK manifest.<br>Will attempt to compile all of them.</html>");
			decompileAllTLK.setToolTipText("<html>Decompiles all TLK files using the TankMaster compiler tool included with Mod Manager.</html>");
			decompileAllCoalesced
					.setToolTipText("<html>Decompils all Coalesced.bin files (will use header info) using the TankMaster compiler tool included with Mod Manager.</html>");
			decompressAllPcc.setToolTipText("<html>Decompresses all PCC files to their uncompressed state</html>");
			compressAllPcc.setToolTipText("<html>Compresses all PCC files to their compressed state, using the game's method of compression</html>");
			sideloadAllModMaker.setToolTipText("<html>Sideload all XML files as ModMaker mods and compile them in batch mode</html>");

			compileAllTLK.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose COMPILE_TLK operation");
					new BatchWorker(droppedFile, BatchWorker.COMPILE_TLK, null).execute();
					dispose();
				}
			});

			decompileAllTLK.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose DECOMPILE_TLK operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPILE_TLK, null).execute();
					dispose();
				}
			});

			compileAllCoalesced.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose COMPILE_COAL operation");
					new BatchWorker(droppedFile, BatchWorker.COMPILE_COAL, null).execute();
					dispose();
				}
			});

			decompileAllCoalesced.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose DECOMPILE_COAL operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPILE_COAL, null).execute();
					dispose();
				}
			});

			decompressAllPcc.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose DECOMPRESS_PCC operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPRESS_PCC, null).execute();
					dispose();
				}
			});

			compressAllPcc.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose COMPRESS_PCC operation");
					new BatchWorker(droppedFile, BatchWorker.COMPRESS_PCC, null).execute();
					dispose();
				}
			});

			sideloadAllModMaker.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose SIDELOAD_MODMAKER operation");
					new BatchWorker(droppedFile, BatchWorker.SIDELOAD_MODMAKER, null).execute();
					dispose();
				}
			});

			c.gridy++;
			panel.add(compileAllTLK, c);
			c.gridy++;
			panel.add(decompileAllTLK, c);
			c.gridy++;
			panel.add(compileAllCoalesced, c);
			c.gridy++;
			panel.add(decompileAllCoalesced, c);
			panel.add(decompressAllPcc, c);
			c.gridy++;
			panel.add(compressAllPcc, c);
			c.gridy++;
			panel.add(sideloadAllModMaker, c);
			c.gridy++;
		}

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		String extension = FilenameUtils.getExtension(droppedFile.getAbsolutePath());
		switch (extension) {
		case "xml": {

			JButton compileAllTLK = new JButton("Compile as TLK manifest");
			JButton compileAllCoalesced = new JButton("Compile as coalesced manifest");
			JButton sideloadAllModMaker = new JButton("Sideload as ModMaker delta");

			JPanel xmlFilePanel = new JPanel();
			xmlFilePanel.setLayout(new BoxLayout(xmlFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder xmlBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "XML File Operations");
			xmlFilePanel.setBorder(xmlBorder);

			JPanel tlkXMLPanel = new JPanel();
			TitledBorder tlkManifestBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "XML - TLK Manifest File");
			tlkXMLPanel.setBorder(tlkManifestBorder);
			tlkXMLPanel.add(compileAllTLK);

			JPanel coalescedXMLPanel = new JPanel();
			TitledBorder coalescedManifestBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "XML - Coalesced Manifest File");
			coalescedXMLPanel.setBorder(coalescedManifestBorder);
			coalescedXMLPanel.add(compileAllCoalesced);

			JPanel modmakerXMLPanel = new JPanel();
			TitledBorder modmakerManifestBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "XML - ModMaker Mod Delta");
			modmakerXMLPanel.setBorder(modmakerManifestBorder);
			modmakerXMLPanel.add(sideloadAllModMaker);

			xmlFilePanel.add(coalescedXMLPanel);
			xmlFilePanel.add(tlkXMLPanel);
			xmlFilePanel.add(modmakerXMLPanel);

			panel.add(xmlFilePanel);
			break;
		}
		case "pcc": {
			JButton decompressAllPcc = new JButton("Decompress PCC file");
			JButton compressAllPcc = new JButton("Compress PCC file");
			JButton dumpGfxFiles = new JButton("Dump SWF/GFX GUI files");

			JPanel pccFilePanel = new JPanel();
			pccFilePanel.setLayout(new BoxLayout(pccFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder xmlBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "PCC File Operations");
			pccFilePanel.setBorder(xmlBorder);

			JPanel compressionPCCPanel = new JPanel();
			compressionPCCPanel.setLayout(new BoxLayout(compressionPCCPanel, BoxLayout.LINE_AXIS));
			TitledBorder compressionBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "PCC - Compression options");
			compressionPCCPanel.setBorder(compressionBorder);
			compressionPCCPanel.add(Box.createGlue());
			compressionPCCPanel.add(decompressAllPcc);
			compressionPCCPanel.add(Box.createGlue());
			compressionPCCPanel.add(compressAllPcc);
			compressionPCCPanel.add(Box.createGlue());

			JButton aipathfinding = new JButton("View AI Pathfinding in Map Pathfinding Viewer");
			if (ArchUtils.getProcessor().is64Bit()) {
				aipathfinding.setEnabled(false);
				aipathfinding.setToolTipText("Requires 64-bit Windows");
			}
			JButton dumppcc = new JButton("Dump PCC info with PCC Data Dumper");
			JPanel readPCCPanel = new JPanel();
			TitledBorder coalescedManifestBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "PCC - Data Viewers");
			readPCCPanel.setBorder(coalescedManifestBorder);
			readPCCPanel.add(Box.createGlue());
			readPCCPanel.add(aipathfinding);
			readPCCPanel.add(Box.createGlue());
			readPCCPanel.add(dumppcc);
			readPCCPanel.add(Box.createGlue());

			JPanel swfPCCPanel = new JPanel();
			swfPCCPanel.setLayout(new BoxLayout(swfPCCPanel, BoxLayout.LINE_AXIS));
			TitledBorder swfBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "PCC - SWF/GFX GUI options");
			swfPCCPanel.setBorder(swfBorder);

			swfPCCPanel.add(Box.createGlue());
			swfPCCPanel.add(dumpGfxFiles);
			swfPCCPanel.add(Box.createGlue());

			pccFilePanel.add(compressionPCCPanel);
			pccFilePanel.add(readPCCPanel);
			pccFilePanel.add(swfPCCPanel);

			panel.add(pccFilePanel);
			break;
		}
		case "bin": {
			JButton decompileCoalescedFile = new JButton("Decompile Coalesced File");

			JPanel binFilePanel = new JPanel();
			binFilePanel.setLayout(new BoxLayout(binFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder coalescedFilePanelBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "BIN File Operations");
			binFilePanel.setBorder(coalescedFilePanelBorder);

			JPanel coalescedPanel = new JPanel();
			TitledBorder coalescedBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "BIN - Coalesced File");
			coalescedPanel.setBorder(coalescedBorder);
			coalescedPanel.add(decompileCoalescedFile);

			binFilePanel.add(coalescedPanel);

			//Check if coalesced
			byte[] buffer = new byte[4];

			try (InputStream is = new FileInputStream(droppedFile.getAbsolutePath())) {
				if (is.read(buffer) != buffer.length) {
					// do something
					decompileCoalescedFile.setEnabled(false);
					decompileCoalescedFile.setToolTipText("Dropped file is not a coalesced file.");
					return;
				}
				int magic = ResourceUtils.byteArrayToInt(buffer);
				if (magic != ModManager.COALESCED_MAGIC_NUMBER) {
					//not a coalesced file
					decompileCoalescedFile.setEnabled(false);
					decompileCoalescedFile.setToolTipText("Dropped file is not a coalesced file.");
				}
				is.close();
			} catch (IOException e) {
				ModManager.debugLogger.writeErrorWithException("Error reading input binary file! (Coalesced Drop)", e);
			}
			panel.add(binFilePanel);
			break;
		}
		case "dlc": {
			JButton editMountButton = new JButton("Edit Mount.dlc in Mount Editor");

			JPanel mountFilePanel = new JPanel();
			mountFilePanel.setLayout(new BoxLayout(mountFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder mountFileBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Mount.dlc file operations");
			mountFilePanel.setBorder(mountFileBorder);

			JPanel mountDLCOperationsPanel = new JPanel();
			TitledBorder mountOperationsBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "DLC metadata file");
			mountDLCOperationsPanel.setBorder(mountOperationsBorder);
			mountDLCOperationsPanel.add(editMountButton);

			mountFilePanel.add(mountDLCOperationsPanel);

			panel.add(mountFilePanel);
			break;
		}
		case "asi": {
			JButton installASIButton = new JButton("Install ASI");
			installASIButton.setAlignmentX(CENTER_ALIGNMENT);
			JPanel asiFilePanel = new JPanel();
			asiFilePanel.setLayout(new BoxLayout(asiFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder asiBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "ASI Native Mod options");
			asiFilePanel.setBorder(asiBorder);

			JPanel asiInstallationPanel = new JPanel();
			TitledBorder coalescedBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "ASI - Native Mod");
			asiInstallationPanel.setBorder(coalescedBorder);
			asiInstallationPanel.setLayout(new BoxLayout(asiInstallationPanel, BoxLayout.PAGE_AXIS));
			asiInstallationPanel.setAlignmentX(CENTER_ALIGNMENT);
			JLabel warning = new JLabel("<html>Install ASI mods at your own risk, as they can execute native code.</html>");
			warning.setAlignmentX(CENTER_ALIGNMENT);

			asiInstallationPanel.add(warning);
			asiInstallationPanel.add(installASIButton);
			asiFilePanel.add(asiInstallationPanel);

			panel.add(asiFilePanel);
			break;
		}
		case "tlk": {
			JButton decompileTLKButton = new JButton("Decompile TLK");
			decompileTLKButton.setAlignmentX(CENTER_ALIGNMENT);
			JPanel tlkFilePanel = new JPanel();
			tlkFilePanel.setLayout(new BoxLayout(tlkFilePanel, BoxLayout.PAGE_AXIS));

			TitledBorder tlkFileBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "TLK file options");
			tlkFilePanel.setBorder(tlkFileBorder);

			JPanel tlkDecompilePanel = new JPanel();
			TitledBorder tlkLocalizeBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "TLK - String localization file");
			tlkDecompilePanel.setBorder(tlkLocalizeBorder);
			tlkDecompilePanel.setLayout(new BoxLayout(tlkDecompilePanel, BoxLayout.PAGE_AXIS));

			tlkDecompilePanel.add(decompileTLKButton);
			tlkFilePanel.add(tlkDecompilePanel);

			panel.add(tlkFilePanel);
			break;
		}
		case "txt": {
			JButton pathfindingButton = new JButton("Open file in Map Pathfinding Viewer");
			if (!ArchUtils.getProcessor().is64Bit()) {
				pathfindingButton.setEnabled(false);
				pathfindingButton.setToolTipText("Requires 64-bit Windows");
			}
			pathfindingButton.setAlignmentX(CENTER_ALIGNMENT);
			JPanel textFilePanel = new JPanel();
			textFilePanel.setLayout(new BoxLayout(textFilePanel, BoxLayout.PAGE_AXIS));
			TitledBorder textFileBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "TXT - Text file options");
			textFilePanel.setBorder(textFileBorder);

			JPanel pathfindingDumpPanel = new JPanel();
			TitledBorder pathfindingDumpBorder = BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "TXT - PCC or Pathfinding dump");
			pathfindingDumpPanel.setBorder(pathfindingDumpBorder);
			pathfindingDumpPanel.setLayout(new BoxLayout(pathfindingDumpPanel, BoxLayout.PAGE_AXIS));

			pathfindingDumpPanel.add(pathfindingButton);
			textFilePanel.add(pathfindingDumpPanel);

			panel.add(textFilePanel);
			break;
		}
		default: {
			JPanel failPanel = new JPanel();
			failPanel.add(new JLabel("<html><center>Unsupported Drag & Drop file extension: " + extension + "</center></html>", SwingConstants.CENTER));
			panel.add(failPanel);
		}
		}

		JPanel rootPanel = new JPanel(new BorderLayout());
		rootPanel.add(panel, BorderLayout.CENTER);
		JLabel others = new JLabel(
				"<html><center>Supported file drop types:<br> - .pcc (ME3 package file)<br> - .bin (Coalesced file)<br> - .tlk (Localization file)<br> - .txt (PCC data dump file)<br> - .xml (Multiple types)<br> - .dlc (Mount.dlc file)<br> - .asi (Runtime native mod)</center></html>",
				SwingConstants.CENTER);
		rootPanel.add(others, BorderLayout.SOUTH);
		rootPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(rootPanel);
		setMinimumSize(new Dimension(200, 200));
		pack();
		setLocationRelativeTo(ModManagerWindow.ACTIVE_WINDOW);
	}

	private void setupWindow() {
		setTitle("Batch Task Selector");
		setIconImages(ModManager.ICONS);

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		GridBagConstraints c = new GridBagConstraints();

		c.gridy = 0;
		if (droppedFile.isDirectory()) {
			JLabel headerLabel = new JLabel("<html>You dropped a folder onto Mod Manager:<br>" + droppedFile
					+ "<br>Select what operation to perform on the contents of this folder.<br>Hover over each button to see a description.</html>");
			panel.add(headerLabel, c);

			JButton compileAllTLK = new JButton("Compile all TLK XML Manifests");

			JButton decompileAllTLK = new JButton("Decompile all TLK files");
			JButton compileAllCoalesced = new JButton("Compile all Coalesced manifest");
			JButton decompileAllCoalesced = new JButton("Decompile all Coalesced files");
			JButton decompressAllPcc = new JButton("Decompress all PCC files");
			JButton compressAllPcc = new JButton("Compress all PCC files");
			JButton sideloadAllModMaker = new JButton("Sideload all ModMaker XML files");

			compileAllTLK.setToolTipText("<html>Treats each .xml file in the folder as a TankMaster TLK manifest.<br>Will attempt to compile all of them.</html>");
			decompileAllTLK.setToolTipText("<html>Decompiles all TLK files using the TankMaster compiler tool included with Mod Manager.</html>");
			decompileAllCoalesced
					.setToolTipText("<html>Decompils all Coalesced.bin files (will use header info) using the TankMaster compiler tool included with Mod Manager.</html>");
			decompressAllPcc.setToolTipText("<html>Decompresses all PCC files to their uncompressed state</html>");
			compressAllPcc.setToolTipText("<html>Compresses all PCC files to their compressed state, using the game's method of compression</html>");
			sideloadAllModMaker.setToolTipText("<html>Sideload all XML files as ModMaker mods and compile them in batch mode</html>");

			compileAllTLK.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose COMPILE_TLK operation");
					new BatchWorker(droppedFile, BatchWorker.COMPILE_TLK, null).execute();
					dispose();
				}
			});

			decompileAllTLK.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose DECOMPILE_TLK operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPILE_TLK, null).execute();
					dispose();
				}
			});

			compileAllCoalesced.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose COMPILE_COAL operation");
					new BatchWorker(droppedFile, BatchWorker.COMPILE_COAL, null).execute();
					dispose();
				}
			});

			decompileAllCoalesced.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					ModManager.debugLogger.writeMessage("User chose DECOMPILE_COAL operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPILE_COAL, null).execute();
					dispose();
				}
			});

			decompressAllPcc.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose DECOMPRESS_PCC operation");
					new BatchWorker(droppedFile, BatchWorker.DECOMPRESS_PCC, null).execute();
					dispose();
				}
			});

			compressAllPcc.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose COMPRESS_PCC operation");
					new BatchWorker(droppedFile, BatchWorker.COMPRESS_PCC, null).execute();
					dispose();
				}
			});

			sideloadAllModMaker.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					ModManager.debugLogger.writeMessage("User chose SIDELOAD_MODMAKER operation");
					new BatchWorker(droppedFile, BatchWorker.SIDELOAD_MODMAKER, null).execute();
					dispose();
				}
			});

			c.gridy++;
			panel.add(compileAllTLK, c);
			c.gridy++;
			panel.add(decompileAllTLK, c);
			c.gridy++;
			panel.add(compileAllCoalesced, c);
			c.gridy++;
			panel.add(decompileAllCoalesced, c);
			panel.add(decompressAllPcc, c);
			c.gridy++;
			panel.add(compressAllPcc, c);
			c.gridy++;
			panel.add(sideloadAllModMaker, c);
			c.gridy++;

		} else {
			String extension = FilenameUtils.getExtension(droppedFile.getAbsolutePath());
			switch (extension) {
			case "pcc": {
				JLabel headerLabel = new JLabel(
						"<html>You dropped an PCC file onto Mod Manager.<br>" + droppedFile + "<br>Select what operation to perform with this file.</html>");
				panel.add(headerLabel, c);

				JButton decompressPCC = new JButton("Decompress PCC");
				JButton compressPCC = new JButton("Compress PCC");

				decompressPCC.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						String name = FilenameUtils.getName(droppedFile.getAbsolutePath());
						ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Decompressing " + name);
						ProcessResult pr = ModManager.decompressPCC(droppedFile, droppedFile);
						if (pr.getReturnCode() == 0) {
							ModManager.debugLogger.writeMessage("Deompressed " + name);
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Decompressed " + name);
						} else {
							ModManager.debugLogger.writeMessage("Failed to decompress " + name + "(" + pr.getReturnCode() + ")");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Failed to decompress " + name + "(" + pr.getReturnCode() + ")");

						}
						dispose();
					}
				});

				compressPCC.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						String name = FilenameUtils.getName(droppedFile.getAbsolutePath());
						ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compressing " + name);
						ProcessResult pr = ModManager.compressPCC(droppedFile, droppedFile);
						if (pr.getReturnCode() == 0) {
							ModManager.debugLogger.writeMessage("Compressed " + name);
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compressed " + name);
						} else {
							ModManager.debugLogger.writeMessage("Failed to compressed " + name + "(" + pr.getReturnCode() + ")");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Failed to compress " + name + "(" + pr.getReturnCode() + ")");

						}
						dispose();
					}
				});

				c.gridy++;
				panel.add(decompressPCC, c);
				c.gridy++;

				panel.add(compressPCC, c);
				c.gridy++;

				break;
			}
			case "xml": {
				JLabel headerLabel = new JLabel(
						"<html>You dropped an XML file onto Mod Manager.<br>" + droppedFile + "<br>Select what operation to perform with this file.</html>");
				panel.add(headerLabel, c);

				JButton compileTLK = new JButton("Compile TLK (TLK Manifest (Tankmaster only))");
				JButton compileCoalesced = new JButton("Compile Coalesced (Coalesced Manifest)");
				JButton sideloadModMaker = new JButton("Sideload ModMaker mod (Mod Delta)");

				compileTLK.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						ProcessResult pr = TLKTool.compileTLK(droppedFile);
						if (pr.getReturnCode() == 0) {
							ModManager.debugLogger.writeMessage("Compiled dropped TLK manifest");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compiled TLK file");
						} else {
							ModManager.debugLogger.writeError("Error compiling dropped TLK manifest [" + pr.getReturnCode() + "]");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Error compiling TLK file [" + pr.getReturnCode() + "]");
						}
						dispose();
					}
				});

				compileCoalesced.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						ProcessResult pr = CoalescedWindow.compileCoalesced(droppedFile.getAbsolutePath());
						if (pr.getReturnCode() == 0) {
							ModManager.debugLogger.writeMessage("Compiled dropped coalesced manifest");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Compiled Coalesced file");
						} else {
							ModManager.debugLogger.writeError("Error compiling dropped Coalesced manifest [" + pr.getReturnCode() + "]");
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Error compiling Coalesced file [" + pr.getReturnCode() + "]");
						}
						dispose();
					}
				});
				sideloadModMaker.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						dispose();
						ModManager.debugLogger.writeMessage("Sideloading dropped ModMaker mod");
						ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Sideloading ModMaker mod...");
						new ModMakerCompilerWindow(droppedFile.getAbsolutePath(), ModMakerEntryWindow.getDefaultLanguages());
					}
				});

				c.gridy++;
				panel.add(compileTLK, c);
				c.gridy++;

				panel.add(compileCoalesced, c);
				c.gridy++;

				panel.add(sideloadModMaker, c);
				c.gridy++;
				break;
			}
			case "asi":
				//install ASI file
				if (!ModManagerWindow.validateBIOGameDir()) {
					JOptionPane.showMessageDialog(null, "The BioGame directory is not valid.\nASI mods can only be installed if the BIOGame directory is valid.",
							"Invalid BioGame Directory", JOptionPane.ERROR_MESSAGE);
					show = false;
					break;
				}

				JLabel headerLabel = new JLabel("<html>You dropped an ASI file onto Mod Manager.<br>" + droppedFile
						+ "<br>Be sure you trust the author before you install this<br>as it can run arbitrary code.</html>");
				panel.add(headerLabel, c);
				JButton compileTLK = new JButton("Install ASI Mod");
				compileTLK.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						String copyDest = ModManager.appendSlash(new File(ModManagerWindow.GetBioGameDir()).getParent()) + "Binaries/win32/asi/" + droppedFile.getName();
						try {
							FileUtils.copyFile(droppedFile, new File(copyDest));
							ModManager.debugLogger.writeMessage("Installed dropped ASI File: " + droppedFile + " to " + copyDest);
							ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Installed " + droppedFile.getName());
						} catch (IOException e1) {
							ModManager.debugLogger.writeErrorWithException("ASI Mod install failed " + copyDest + ":", e1);
							JOptionPane.showMessageDialog(null, "Unable to install ASI mod.\nYou may need to run Mod Manager as an administrator.", "Installation Failure",
									JOptionPane.ERROR_MESSAGE);
						}
						dispose();
					}
				});

				c.gridy++;
				panel.add(compileTLK, c);
				c.gridy++;

				break;
			}

		}
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(panel);
		pack();

	}

	static class BatchWorker extends SwingWorker<Void, String> {

		protected static final int COMPILE_TLK = 0;
		protected static final int DECOMPILE_TLK = 1;
		protected static final int COMPILE_COAL = 2;
		protected static final int DECOMPILE_COAL = 3;
		protected static final int DECOMPRESS_PCC = 4;
		protected static final int COMPRESS_PCC = 5;
		protected static final int SIDELOAD_MODMAKER = 6;

		private int operation;
		private File folder;
		public final Object lock = new Lock(); //threading wait() and notifyall();
		public boolean completed = false;
		private StarterKitProgressDialog dialog;

		public BatchWorker(File droppedFile, int operation, StarterKitProgressDialog dialog) {
			this.operation = operation;
			this.folder = droppedFile;
			this.dialog = dialog; //can be null.
		}

		@Override
		protected Void doInBackground() throws Exception {
			SuffixFileFilter suff = null;
			switch (operation) {
			case DECOMPILE_TLK:
				suff = new SuffixFileFilter(".tlk");
				break;
			case COMPRESS_PCC:
			case DECOMPRESS_PCC:
				suff = new SuffixFileFilter(".pcc");
				break;
			case SIDELOAD_MODMAKER:
			case COMPILE_TLK:
			case COMPILE_COAL:
				suff = new SuffixFileFilter(".xml");
				break;
			}
			if (suff == null) {
				return null;
			}
			Collection<File> files = FileUtils.listFiles(folder, suff, FalseFileFilter.INSTANCE);
			int processed = 0;
			for (File file : files) {
				processed++;
				switch (operation) {
				case COMPILE_TLK: {
					publish("Compiling " + FilenameUtils.getName(file.getAbsolutePath()));
					ProcessResult pr = TLKTool.compileTLK(file);
					break;
				}
				case DECOMPILE_TLK: {
					publish("Decompiling " + FilenameUtils.getName(file.getAbsolutePath()));
					ProcessResult pr = TLKTool.decompileTLK(file);
					break;
				}
				case COMPRESS_PCC: {
					publish("Compressing " + FilenameUtils.getName(file.getAbsolutePath()));
					ProcessResult pr = ModManager.compressPCC(file, file);
					break;
				}
				case DECOMPRESS_PCC: {
					publish("Decompressing " + FilenameUtils.getName(file.getAbsolutePath()));
					ProcessResult pr = ModManager.decompressPCC(file, file);
					break;
				}
				case SIDELOAD_MODMAKER:
					publish("Sideloading ModMaker mod [" + processed + "/" + files.size() + "]");
					ModMakerCompilerWindow mcw = new ModMakerCompilerWindow(file.getAbsolutePath(), ModMakerEntryWindow.getDefaultLanguages());
					while (mcw.isShowing()) {
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					break;
				case COMPILE_COAL: {
					publish("Compiling " + FilenameUtils.getBaseName(file.getAbsolutePath()));
					ProcessResult pr = CoalescedWindow.compileCoalesced(file.getAbsolutePath());
					break;
				}
				case DECOMPILE_COAL: {
					publish("Decompiling " + FilenameUtils.getBaseName(file.getAbsolutePath()));
					ProcessResult pr = CoalescedWindow.decompileCoalesced(file.getAbsolutePath());
					break;
				}
				}
			}
			return null;
		}

		@Override
		protected void process(List<String> chunks) {
			if (dialog != null) {
				dialog.infoLabel.setText(chunks.get(chunks.size() - 1));
			} else if (ModManagerWindow.ACTIVE_WINDOW != null)
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText(chunks.get(chunks.size() - 1));
		}

		@Override
		protected void done() {
			completed = true;
			synchronized (lock) {
				lock.notifyAll();
			}
			if (ModManagerWindow.ACTIVE_WINDOW != null)
				ModManagerWindow.ACTIVE_WINDOW.labelStatus.setText("Batch operation completed");
		}
	}
}