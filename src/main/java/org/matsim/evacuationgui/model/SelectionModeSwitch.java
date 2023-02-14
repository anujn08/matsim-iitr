package org.matsim.evacuationgui.model;

import org.matsim.evacuationgui.control.Controller;
import org.matsim.evacuationgui.model.Constants.SelectionMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SelectionModeSwitch extends JPanel implements ActionListener {

	private Controller controller;
	private JButton modeCircleBtn;
	private JButton modePolyBtn;

	public SelectionModeSwitch(Controller controller) {

		this.controller = controller;

		this.setPreferredSize(new Dimension(150, 120));
		// this.setBackground(new Color(200,150,200));

		this.setLayout(new FlowLayout());

		modeCircleBtn = new JButton(controller.getLocale().btCircular());
		modeCircleBtn.setActionCommand(SelectionMode.CIRCLE.toString());
		modeCircleBtn.setBorder(BorderFactory.createLoweredBevelBorder());
		modeCircleBtn.setPreferredSize(new Dimension(95, 30));

		modePolyBtn = new JButton(controller.getLocale().btPolygon());
		modePolyBtn.setActionCommand(SelectionMode.POLYGONAL.toString());
		modePolyBtn.setPreferredSize(new Dimension(95, 30));

		modeCircleBtn.addActionListener(this);
		modePolyBtn.addActionListener(this);

		this.setBorder(BorderFactory.createTitledBorder(controller.getLocale()
				.labelSelectionMode()));

		this.add(modeCircleBtn);
		this.add(modePolyBtn);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// circle selected
		if (e.getActionCommand().equals(SelectionMode.CIRCLE.toString())) {
			modeCircleBtn.setBorder(BorderFactory.createLoweredBevelBorder());
			modePolyBtn.setBorder(null);
			this.controller.setSelectionMode(SelectionMode.CIRCLE);
		}
		// polygon selected
		else if (e.getActionCommand()
				.equals(SelectionMode.POLYGONAL.toString())) {
			modePolyBtn.setBorder(BorderFactory.createLoweredBevelBorder());
			modeCircleBtn.setBorder(null);
			this.controller.setSelectionMode(SelectionMode.POLYGONAL);
		}

		this.controller.setInSelection(false);

	}

}
