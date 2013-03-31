package com.ibm.actor.test;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

/**
 * A GUI panel that shows an image as its contents. 
 * 
 * @author BFEIGENB
 *
 */
public class ImageView extends JPanel {
	// TODO: move to new package

	public ImageView() {
		setLayout(null);
	}

	public ImageView(BufferedImage image) {
		this();
		this.image = image;
	}

	protected BufferedImage image;

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		//System.out.printf("paintComponent: %s%n", this);
		Graphics2D g2d = (Graphics2D) g;
		if (image != null) {
			g2d.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		} else {
			g2d.setColor(Color.LIGHT_GRAY.brighter());
			g2d.fillRect(0, 0, getWidth(), getHeight());
			g2d.setColor(Color.RED);
			g2d.drawString("No image yet!", getWidth() / 10, getHeight() / 2);
		}
	}
}
