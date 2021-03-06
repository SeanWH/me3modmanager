package com.me3tweaks.modmanager;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.me3tweaks.modmanager.utilities.ResourceUtils;

@SuppressWarnings("serial")
public class NetFrameworkMissingWindow extends JDialog {
	JLabel introLabel;
	JButton downloadButton;
	private static final String netPage = "https://www.microsoft.com/en-us/download/details.aspx?id=48130";

	public NetFrameworkMissingWindow(String text) {
		this.setTitle("No usable .NET Framework installed");
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		this.setResizable(false);
		this.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		setupWindow(text);
		this.setIconImages(ModManager.ICONS);
		this.pack();
		this.setLocationRelativeTo(null);
		this.setVisible(true);
	}

	private void setupWindow(String text) {
		JPanel updatePanel = new JPanel();
		updatePanel.setBorder(new EmptyBorder(5,5,5,5));
		updatePanel.setLayout(new BoxLayout(updatePanel, BoxLayout.Y_AXIS));
		introLabel = new JLabel("<html><div style=\"width:200px;\">"+text+"<br></div></html>");
		downloadButton = new JButton("Download .NET 4.6 from Microsoft");
		downloadButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					ResourceUtils.openWebpage(new URL(netPage));
				} catch (MalformedURLException e1) {
					ModManager.debugLogger.writeError("Invalid URL for .NET! This shouldn't happen...");
				}
			}
		});

		updatePanel.add(introLabel);
		updatePanel.add(Box.createRigidArea(new Dimension(5,5)));
		updatePanel.add(downloadButton);
		updatePanel.add(new JLabel("<html><div style=\"width:200px;\">If you are certain that this is installed, turn off this check in the Actions > Options menu.</div></html>"));
		this.getContentPane().add(updatePanel);
	}
}
