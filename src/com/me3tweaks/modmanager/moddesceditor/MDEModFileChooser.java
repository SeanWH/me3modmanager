package com.me3tweaks.modmanager.moddesceditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.me3tweaks.modmanager.ModManager;
import com.me3tweaks.modmanager.objects.ModJob;

public class MDEModFileChooser extends JDialog {
	public static final int OPTIONTYPE_ADDONLY = 0;
	public static final int OPTIONTYPE_SELECTONLY = 1;

	private String selectedFile;

	public String getSelectedFile() {
		return selectedFile;
	}

	public void setSelectedFile(String selectedFile) {
		this.selectedFile = selectedFile;
	}

	public MDEModFileChooser(ModDescEditorWindow callingWindow, String currentOption, int optionType, ModJob job) {
		setupWindow(callingWindow, currentOption, optionType, job);
		setVisible(true);
	}

	public void setupWindow(ModDescEditorWindow callingWindow, String currentOption, int optionType, ModJob job) {
		JPanel contentPanel = new JPanel(new BorderLayout());

		DefaultListModel<String> model = new DefaultListModel<String>();

		if (optionType == OPTIONTYPE_SELECTONLY) {
			//add files
			for (String file : job.getFilesToReplaceTargets()) {
				model.addElement(file);
			}
		} else {
			//add files
			for (String file : job.getDestFolders()) {
				model.addElement("/" + file);
				model.addElement("/" + file + "/CookedPCConsole/");
			}
		}

		JList<String> mainFileList = new JList<String>(model);
		JScrollPane listScroller = new JScrollPane(mainFileList);
		JTextField itemField = new JTextField(currentOption == null ? "Select file from the list" : currentOption);

		mainFileList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				if (!e.getValueIsAdjusting()) {
					itemField.setText(mainFileList.getSelectedValue());
				}
			}
		});
		contentPanel.add(listScroller, BorderLayout.CENTER);

		JPanel bottomPanel = new JPanel(new BorderLayout());
		JButton setFile = new JButton("Set File");
		setFile.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				selectedFile = itemField.getText();
				dispose();
			}
		});
		JLabel label = new JLabel("Type in a file path to install the extra file to.\nEntry must start with one of the options listed above.");
		bottomPanel.add(label, BorderLayout.NORTH);
		bottomPanel.add(itemField, BorderLayout.CENTER);
		bottomPanel.add(setFile, BorderLayout.EAST);

		if (optionType == OPTIONTYPE_SELECTONLY) {
			label.setVisible(false);
			itemField.setEnabled(false);
		}
		contentPanel.add(bottomPanel, BorderLayout.SOUTH);
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(contentPanel);

		setMinimumSize(new Dimension(500, 400));
		setIconImages(ModManager.ICONS);
		setTitle(optionType == OPTIONTYPE_SELECTONLY ? "Select in-game modification target" : "Select additional file installation target");
		setModalityType(ModalityType.APPLICATION_MODAL);
		pack();
		setLocationRelativeTo(callingWindow);
	}
}
