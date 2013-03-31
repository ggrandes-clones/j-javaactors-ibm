package com.ibm.actor.test;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.ibm.actor.AbstractActor;
import com.ibm.actor.Actor;
import com.ibm.actor.DefaultActorManager;
import com.ibm.actor.DefaultActorManager.ActorRunnable;
import com.ibm.actor.DefaultMessage;
import com.ibm.actor.Message;
import com.ibm.actor.logging.DefaultLogger;
import com.ibm.actor.utils.Utils;

/**
 * A GUI for running and visualizing Actors.
 * The implementation of this GUI is not discussed in this associated article. 
 * 
 * @author BFEIGENB
 *
 */
public class ActorDemo extends JPanel implements ChangeListener {
	// TODO: move to new package once imports/publics sorted out

	public static final int METER_HEIGHT = 50;
	public static final int METER_WIDTH = 300;
	public static final int VIEWPORT_WIDTH = 500;
	public static final int VIEWPORT_HEIGHT = 400;

	DefaultActorManager actorManager;

	protected JLabel messageLine, titleLine;
	// protected JTextField optionsField;
	protected JComboBox demosBox;
	protected JButton runButton, stopButton, nextButton, randomizeButton, addTaskButton, removeTaskButton;
	protected JSplitPane splitPane;
	protected JSlider stepSlider;
	protected JLabel stepLabel, threadCountLabel;
	protected JCheckBox transparentCheckBox;
	protected JTextArea logArea;
	protected JSpinner threadCounter;
	
	protected JRadioButton noSoundButton, soundWhenIdleButton, soundWhenBusyButton;

	protected Sequencer sequencer;
	protected Sequence sequence;
	protected Synthesizer synthesizer;
	protected Instrument instruments[];

	public Instrument[] getInstruments() {
		return instruments;
	}

	protected int currentChannel;

	public int getCurrentChannel() {
		return currentChannel;
	}

	public void setCurrentChannel(int currentChannel) {
		this.currentChannel = currentChannel;
	}

	public void startNote(int note, int attack) {
		if (isMidiOpen()) {
			midiChannels[currentChannel].noteOn(note, attack);
		}
	}

	public void stopNote(int note, int attack) {
		if (isMidiOpen()) {
			midiChannels[currentChannel].noteOff(note, attack);
		}
	}

	public void stopAllNotes() {
		if (isMidiOpen()) {
			midiChannels[currentChannel].allNotesOff();
		}
	}

	MidiChannel[] midiChannels;

	public MidiChannel[] getMidiChannels() {
		return midiChannels;
	}

	public boolean isMidiOpen() {
		return synthesizer != null && synthesizer.isOpen();
	}

	public void setupMidi() throws Exception {
		synthesizer = MidiSystem.getSynthesizer();
		synthesizer.open();
		midiChannels = synthesizer.getChannels();
		sequencer = MidiSystem.getSequencer();
		sequence = new Sequence(Sequence.PPQ, 10);
		Soundbank sb = synthesizer.getDefaultSoundbank();
		if (sb != null) {
			instruments = synthesizer.getDefaultSoundbank().getInstruments();
			for (int i = 0; i < instruments.length; i++) {
				System.out.printf("%4d: %s%n", i, instruments[i]);
			}
			if (instruments.length > 0) {
				synthesizer.loadInstrument(instruments[0]);
				midiChannels[currentChannel].programChange(0);
			}
			if (instruments.length > 14) {
				synthesizer.loadInstrument(instruments[14]);
				midiChannels[currentChannel].programChange(14);
			}
		}
	}

	public void shutdownMidi() {
		if (synthesizer != null) {
			synthesizer.close();
		}
		if (sequencer != null) {
			sequencer.close();
		}
		sequencer = null;
		synthesizer = null;
		instruments = null;
	}

	public void initUI(JFrame frame) {
		JPanel cp = (JPanel) frame.getContentPane();
		cp.setLayout(new BorderLayout(5, 5));

		splitPane = new JSplitPane();
		cp.add(splitPane, BorderLayout.CENTER);

		JPanel main = new JPanel(new BorderLayout(5, 5));
		splitPane.setLeftComponent(main);

		JPanel topPanel = new JPanel();
		main.add(topPanel, BorderLayout.NORTH);
		buildTopPanel(topPanel);

		JPanel bottomPanel = new JPanel();
		main.add(bottomPanel, BorderLayout.SOUTH);
		buildBottomPanel(bottomPanel);

		logArea = new JTextArea();
		logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		splitPane.setRightComponent(new JScrollPane(logArea));
		DefaultLogger.getDefaultInstance().setIncludeCaller(false);
		DefaultLogger.getDefaultInstance().setLogToFile(false);
		DefaultLogger.getDefaultInstance().setLogArea(logArea);
	}

	protected void buildTopPanel(JPanel inputs) {
		inputs.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagLayout gbl = new GridBagLayout();
		inputs.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(1, 2, 1, 2);

		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		titleLine = new JLabel("Nothing selected");
		titleLine.setHorizontalAlignment(JLabel.CENTER);
		inputs.add(titleLine, gbc);

		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		// optionsField = new JTextField(50);
		// optionsField.setText("pc");
		// inputs.add(optionsField, gbc);
		String[] testNames = DefaultActorTest.getTestNames();
		String[] xtestNames = new String[testNames.length + 1];
		System.arraycopy(testNames, 0, xtestNames, 0, testNames.length);
		xtestNames[testNames.length] = "All demos in sequence";
		demosBox = new JComboBox(xtestNames);
		inputs.add(demosBox, gbc);
		demosBox.setSelectedIndex(demosBox.getItemCount() - 1);

		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		final int maxSends = 50;
		sendRateBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, maxSends);
		sendRateBar.setBackground(Color.LIGHT_GRAY.brighter());
		sendRateBar.setStringPainted(true);
		sendRateBar.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = sendRateBar.getValue();
				sendRateBar.setString(String
						.format(value > maxSends - 1 ? (">=" + maxSends + " mps") : "%d mps", value));
			}
		});
		inputs.add(sendRateBar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		final int maxThreads = 50; // actorManager.getThreads().length * 2;
		dispatchRateBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, maxThreads);
		dispatchRateBar.setBackground(Color.LIGHT_GRAY.brighter());
		dispatchRateBar.setStringPainted(true);
		dispatchRateBar.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int value = dispatchRateBar.getValue();
				dispatchRateBar.setString(String.format(value > maxThreads - 1 ? (">=" + maxThreads + " cps")
						: "%d dps", value));
			}
		});
		inputs.add(dispatchRateBar, gbc);

		gbc.gridx = 0;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.BOTH;
		trendLineView = new ImageView();
		trendLineView.setPreferredSize(new Dimension(METER_WIDTH, METER_HEIGHT));
		inputs.add(trendLineView, gbc);

		gbc.gridx = 1;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.30;
		stepSlider = new JSlider(4, 9, 8);
		stepSlider.setMajorTickSpacing(1);
		stepSlider.setPaintLabels(true);
		stepSlider.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				stepLabel.setText(formatSteps());
			}
		});
		inputs.add(stepSlider, gbc);

		gbc.gridx = 2;
		gbc.gridy = 4;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		stepLabel = new JLabel("", JLabel.RIGHT);
		stepLabel.setText(formatSteps());
		inputs.add(stepLabel, gbc);

		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		runButton = new JButton("Start");
		inputs.add(runButton, gbc);
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doStart();
				doRun();
			}
		});

		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		stopButton = new JButton("Stop");
		stopButton.setEnabled(false);
		inputs.add(stopButton, gbc);
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				doStop();
				messageLine.setText("Stopped");
				stopButton.setEnabled(false);
				runButton.setEnabled(true);
			}
		});

		gbc.gridx = 1;
		gbc.gridy = 2;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		randomizeButton = new JButton("Redistribute");
		inputs.add(randomizeButton, gbc);
		randomizeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (actorManager != null) {
					actorManager.randomizeActors();
				}
			}
		});

		gbc.gridx = 1;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		addTaskButton = new JButton("Add Task");
		inputs.add(addTaskButton, gbc);
		addTaskButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (actorManager != null) {
					Thread t = actorManager.addThread("new" + taskId++);
					t.start();
				}
			}
		});

		gbc.gridx = 2;
		gbc.gridy = 3;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		removeTaskButton = new JButton("Remove Task");
		inputs.add(removeTaskButton, gbc);
		removeTaskButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (actorManager != null) {
					Thread[] ta = actorManager.getThreads();
					for (Thread t : ta) {
						if (t.getName().startsWith("new")) {
							actorManager.removeThread(t.getName());
							break;
						}
					}
				}
			}
		});


		gbc.gridx = 1;
		gbc.gridy = 5;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.30;
		transparentCheckBox = new JCheckBox("Transparent Actors");
		transparentCheckBox.setSelected(true);
		transparentCheckBox.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				isTransparentActors = transparentCheckBox.isSelected();
			}
		});
		inputs.add(transparentCheckBox, gbc);


		
		gbc.gridx = 0;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		//gbc.weightx = 0.15;
		ButtonGroup bg = new ButtonGroup();
		JPanel soundPanel = new JPanel();
		noSoundButton = new JRadioButton("No Sound");
		soundPanel.add(noSoundButton);
		bg.add(noSoundButton);
		soundWhenIdleButton = new JRadioButton("Idle");
		soundPanel.add(soundWhenIdleButton);
		bg.add(soundWhenIdleButton);
		soundWhenBusyButton = new JRadioButton("Busy");
		soundPanel.add(soundWhenBusyButton);
		bg.add(soundWhenBusyButton);
		noSoundButton.setSelected(true);
		inputs.add(soundPanel, gbc);
		
		gbc.gridx = 1;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		SpinnerNumberModel model = new SpinnerNumberModel(10, 2, 25, 1);
		threadCounter = new JSpinner(model);
		threadCounter.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				threadCount = (Integer) threadCounter.getValue();
			}
		});
		inputs.add(threadCounter, gbc);

		gbc.gridx = 2;
		gbc.gridy = 6;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 0.15;
		threadCountLabel = new JLabel("Thread Count", JLabel.RIGHT);
		inputs.add(threadCountLabel, gbc);

	}

	protected int threadCount = 10;

	protected int taskId;

	protected boolean isTransparentActors = true;

	protected JProgressBar sendRateBar, dispatchRateBar;

	private String formatSteps() {
		return String.format("%d steps", (int) Math.pow(2, stepSlider.getValue()));
	}

	protected ImageView trendLineView;

	protected ImageView threadHistoryView;

	protected ImageView mainActorView;
	protected ImageView[] subActorViews;
	protected final int subActorViewCount = 5;

	protected void buildBottomPanel(JPanel views) {
		views.setLayout(new BorderLayout(5, 5));

		JPanel mp = new JPanel(new BorderLayout(5, 5));
		views.add(mp, BorderLayout.CENTER);

		threadHistoryView = new ImageView();
		threadHistoryView.setPreferredSize(new Dimension(VIEWPORT_WIDTH, VIEWPORT_HEIGHT / 5));
		mp.add(threadHistoryView, BorderLayout.NORTH);

		mainActorView = new ImageView();
		mainActorView.setPreferredSize(new Dimension(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
		mp.add(mainActorView, BorderLayout.CENTER);

		JPanel gp = new JPanel(new GridLayout(0, subActorViewCount, 5, 5));
		mp.add(gp, BorderLayout.SOUTH);
		subActorViews = new ImageView[subActorViewCount];
		for (int i = 0; i < subActorViewCount; i++) {
			ImageView av = new ImageView();
			subActorViews[i] = av;
			av.setPreferredSize(new Dimension((VIEWPORT_WIDTH - subActorViewCount * 5) / subActorViewCount,
					VIEWPORT_HEIGHT / 5));
			gp.add(av);
		}

		messageLine = new JLabel(" ");
		views.add(messageLine, BorderLayout.SOUTH);
	}

	protected String reduceName(String name) {
		return name.toLowerCase().replaceAll("\\s+", "");
	}

	protected void doRun() {
		runButton.setEnabled(false);
		// nextButton.setEnabled(true);
		stopButton.setEnabled(true);
		logArea.setText("");
		logArea.setCaretPosition(0);
		// final String[] args = optionsField.getText().split("\\s+");
		int selectedIndex = demosBox.getSelectedIndex();
		if (selectedIndex >= 0) {
			// mySize = getSize();

			setTitle((String) demosBox.getSelectedItem());
			final String args = selectedIndex < demosBox.getItemCount() - 1 ? reduceName(DefaultActorTest
					.getTestNames()[selectedIndex]) : "*";
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						if ("*".equals(args)) {
							for (String name : DefaultActorTest.getTestNames()) {
								doStart();
								setTitle(name);
								runTest(reduceName(name));
								doStop();
								try {
									Thread.sleep(10 * 1000);
								} catch (InterruptedException e) {
									break;
								}
							}
						} else {
							doStart();
							runTest(args);
							doStop();
						}
					} finally {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// TODO: make multiple runs possible
								// runButton.setEnabled(true);
								stopButton.setEnabled(false);
								runButton.setEnabled(true);
								messageLine.setText("Done");
							}
						});
					}
				}
			});
			t.setDaemon(true);
			t.start();
		}
	}

	protected void doStart() {
		if (updater != null) {
			updater.interrupt();
		}
		updater = new Thread(new UpdateRunnable());
		updating = true;
		updater.setDaemon(true);
		updater.start();
	}

	protected void doNext() {
		// TODO: finish
		doStop();
		runButton.setEnabled(false);
		// nextButton.setEnabled(true);
		stopButton.setEnabled(true);
		logArea.setText("");
		logArea.setCaretPosition(0);
		// final String[] args = optionsField.getText().split("\\s+");
		int selectedIndex = demosBox.getSelectedIndex();
		if (selectedIndex >= 0) {
			selectedIndex++;
			// mySize = getSize();

			setTitle((String) demosBox.getSelectedItem());
			final String args = selectedIndex < demosBox.getItemCount() - 1 ? reduceName(DefaultActorTest
					.getTestNames()[selectedIndex]) : "*";
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					try {
						if ("*".equals(args)) {
							for (String name : DefaultActorTest.getTestNames()) {
								doStart();
								setTitle(name);
								runTest(reduceName(name));
								doStop();
								try {
									Thread.sleep(10 * 1000);
								} catch (InterruptedException e) {
									break;
								}
							}
						} else {
							doStart();
							runTest(args);
							doStop();
						}
					} finally {
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								// TODO: make multiple runs possible
								// runButton.setEnabled(true);
								stopButton.setEnabled(false);
								runButton.setEnabled(true);
								messageLine.setText("Done");
							}
						});
					}
				}
			});
			t.setDaemon(true);
			t.start();
		}
	}

	protected void doStop() {
		test.terminateRun();
		updating = false;
	}

	public String getMessage() {
		final String[] res = new String[1];
		if (messageLine != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					res[0] = messageLine.getText();
				}
			});
		}
		return res[0];
	}

	public void setMessage(final String message) {
		if (messageLine != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					messageLine.setText(message != null ? message : "");
				}
			});
		}
	}

	public String getTitle() {
		final String[] res = new String[1];
		if (titleLine != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					res[0] = titleLine.getText();
				}
			});
		}
		return res[0];
	}

	public void setTitle(final String message) {
		if (titleLine != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					titleLine.setText(message != null ? message : "");
				}
			});
		}
	}

	// volatile Dimension mySize;

	volatile boolean updating;

	public static final int TIMES_PER_SECOND = 10;

	protected class UpdateRunnable implements Runnable {
		boolean inRun;
		@Override
		public void run() {
			if (inRun) {
				return;
			}
			inRun = true;
			actorColorMap.clear();
			int xiteration = 0;
			while (updating) {
				try {
					Utils.sleep(1000 / TIMES_PER_SECOND);
					if (xiteration % TIMES_PER_SECOND == 0) {
						updateRates();
					}
					if (xiteration % (5 * TIMES_PER_SECOND) == 0) {
						renderSnapshots();
					}

					ImageView avMain = mainActorView;
					BufferedImage bi = new BufferedImage(avMain.getWidth(), avMain.getHeight(),
							BufferedImage.TYPE_INT_ARGB);
					Graphics g = bi.getGraphics();
					renderTo(g, bi.getWidth(), bi.getHeight());
					avMain.setImage(bi);
					// System.out.printf("repaint: %s%n", renderImage1);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							// System.out.printf("**** repaint%n");
							mainActorView.repaint();
							for (int i = 0; i < subActorViews.length; i++) {
								subActorViews[i].repaint();
							}
							threadHistoryView.repaint();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
				xiteration++;
			}
			updateRates();
			inRun = false;
		}

		protected void renderSnapshots() {
			// System.out.printf("**** UpdateRunnable run: %d %n",
			// xiteration);
			int last = subActorViews.length - 1;
			for (int i = 0; i < last; i++) {
				ImageView av = subActorViews[i];
				ImageView avNext = subActorViews[i + 1];
				BufferedImage bi = avNext.getImage();
				addTimeStamp(-(last - i + 1) * 5, bi);
				av.setImage(bi);
			}
			ImageView avLast = subActorViews[last];
			BufferedImage bi = new BufferedImage(avLast.getWidth(), avLast.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics g = bi.getGraphics();
			renderTo(g, bi.getWidth(), bi.getHeight());
			addTimeStamp(-5, bi);
			avLast.setImage(bi);
		}

		protected void updateRates() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					int sendPerSecondCount = -1, dispatchPerSecondCount = -1;
					if (actorManager != null) {
						if (sendRateBar != null) {
							sendPerSecondCount = actorManager.getSendPerSecondCount();
							sendRateBar.setValue(sendPerSecondCount);
						}
						if (dispatchRateBar != null) {
							dispatchPerSecondCount = actorManager.getDispatchPerSecondCount();
							dispatchRateBar.setValue(dispatchPerSecondCount);
						}
						// System.out.printf("rates: sendPerSecondCount=%d, dispatchPerSecondCount=%d%n",
						// sendPerSecondCount, dispatchPerSecondCount);
						if (trendLineView != null) {
							trendLineView.setImage(renderTrendLine(actorManager.getTrendValue(),
									actorManager.getMaxTrendValue()));
						}
					}
				}
			});
		}

		protected void addTimeStamp(int delta, BufferedImage bi) {
			if (bi != null) {
				Graphics g = bi.getGraphics();
				g.setColor(Color.CYAN);
				g.fillRect(0, 0, 20, 15);
				g.setColor(Color.BLACK);
				g.drawString(String.format("%d", delta), 0, 14);
			}
		}
	}

	protected BufferedImage trendLineImage;

	public BufferedImage getTrendLineImage() {
		return trendLineImage;
	}

	protected Thread updater;

	protected static Random rand = new Random();

	// TOOD: need more contrast in colors.
	// may need to manually define the colors
	protected static List<Color> colors = new ArrayList<Color>();
	static {
		/*
		 * for (int r = 0; r < 3; r++) { for (int g = 0; g < 3; g++) { for (int
		 * b = 0; b < 3; b++) { // don't get too close to white Color c = new
		 * Color(128 + rand.nextInt(100), 128 + rand.nextInt(100), 128 +
		 * rand.nextInt(100)); if (!colors.contains(c)) { colors.add(c); } } } }
		 */
		int r = 0, g = 0, b = 0;
		for (int i = 0; colors.size() < 64 && i < 1000; i++) {
			Color c = new Color(Math.min(128, (150 + r) % 256), Math.min(128, (128 + g) % 256), Math.min(128,
					(128 + b) % 256)).brighter();
			int dist = Integer.MAX_VALUE;
			for (Color xc : colors) {
				int xdist = getColorDistance(xc, c);
				if (dist > xdist) {
					dist = xdist;
				}
			}
			if (dist > 25) {
				// Utils.logger.trace("adding color %d: %s", colors.size(), c);
				colors.add(c);
			} else {
				// Utils.logger.trace("skipping color %d: %s", colors.size(),
				// c);
			}
			r += 81;
			g += 11;
			b += 59;
		}
	}

	protected static int getColorDistance(Color c1, Color c2) {
		int r = Math.abs(c1.getRed() - c2.getRed());
		int g = Math.abs(c1.getGreen() - c2.getGreen());
		int b = Math.abs(c1.getBlue() - c2.getBlue());
		return r + g + b;
	}

	protected Map<String, Color> actorColorMap = new HashMap<String, Color>();

	int errorY;

	protected void initThreadHistory(Thread[] threads) {
		for (Thread t : threads) {
			String name = t.getName();
			ThreadHistory th = threadHistoryMap.get(name);
			if (th == null) {
				th = new ThreadHistory();
				threadHistoryMap.put(name, th);
			}
		}
	}

	protected void renderTo(Graphics g, int width, int height) {
		// System.out.printf("renderTo: %dx%d%n", width, height);
		Graphics2D g2d = (Graphics2D) g;
		errorY = 0;

		if (actorManager != null) {
			Font f = g2d.getFont();
			try {
				if (width < 200 || height < 200) {
					g2d.setFont(new Font(f.getFontName(), f.getSize(), f.getSize() / 2));
				}
				Set<Actor> selfSends = new HashSet<Actor>();
				AbstractActor[] actors = actorManager.getActors();
				Thread[] threads = actorManager.getThreads();
				initThreadHistory(threads);

				int base = Math.min(width, height);
				g2d.setBackground(Color.LIGHT_GRAY.brighter());
				g2d.fillRect(0, 0, width, height);

				int tdiam = base / 20;
				int tradius = (int) (base * 0.85) / 2;

				int adiam = base / 15;
				int aradius = (int) (base * 0.7) / 2;

				// TODO: move positioning from here to setSize();
				Map<String, AbstractActor> actorMap = new HashMap<String, AbstractActor>();
				Map<String, Integer> actorXMap = new HashMap<String, Integer>();
				Map<String, Integer> actorYMap = new HashMap<String, Integer>();
				Map<String, Integer> actorIdMap = new HashMap<String, Integer>();

				double angle = 0;

				int count = 0, acount = 0;
				for (AbstractActor aa : actors) {
					String cname = aa.getClass().getName();
					actorIdMap.put(aa.getName(), acount++);
					if (!actorColorMap.containsKey(cname)) {
						actorColorMap.put(cname, colors.get(rand.nextInt(colors.size())));
					}
					angle = renderMessageConnections(g2d, actors, height, width, aradius, adiam, actorMap, actorXMap,
							actorYMap, angle, aa, colors.get(count++ % colors.size()), selfSends);
				}

				renderActors(g2d, selfSends, actors, adiam, actorXMap, actorYMap, actorIdMap);

				renderHistories(width, height, g2d, threads, tdiam, tradius, actorIdMap);
			} catch (Exception e) {
				paintError("renderTo", g2d, e);
			} finally {
				g2d.setFont(f);
			}
		}
	}

	protected void renderHistories(int width, int height, Graphics2D g2d, Thread[] threads, int tdiam, int tradius,
			Map<String, Integer> actorIdMap) {
		double angle;
		List<ThreadHistory> histories = new ArrayList<ThreadHistory>();
		angle = 0;
		for (Thread t : threads) {
			String name = t.getName();
			angle = renderThread(g2d, threads, height, width, tdiam, tradius, angle, t, actorIdMap);
			ThreadHistory th = threadHistoryMap.get(name);
			if (th != null) {
				th.setLast(name);
				histories.add(th);
			}
		}
		renderHistory(histories);
	}

	protected void renderActors(Graphics2D g2d, Set<Actor> selfSends, AbstractActor[] actors, int adiam,
			Map<String, Integer> actorXMap, Map<String, Integer> actorYMap, Map<String, Integer> actorIdMap) {
		int acount;
		acount = 0;
		for (AbstractActor aa : actors) {
			renderActor(g2d, adiam, actorXMap, actorYMap, actorColorMap, actorIdMap, aa, selfSends, acount++,
					actors.length);
		}
	}

	protected void renderHistory(List<ThreadHistory> histories) {
		int hvwidth = threadHistoryView.getWidth();
		int hvheight = threadHistoryView.getHeight();
		if (hvwidth > 0 && hvheight > 0) {
			BufferedImage bi = new BufferedImage(hvwidth, hvheight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D biG2d = (Graphics2D) bi.getGraphics();
			biG2d.setColor(Color.LIGHT_GRAY.brighter());
			biG2d.fillRect(0, 0, hvwidth, hvheight);
			int hsize = histories.size();
			if (hsize > 0) {
				int noteStart = 30;
				int noteStep = (60 + hsize - 1) / hsize;
				int cwidth = (hvwidth + hsize - 1) / hsize;
				int pstart = 0;
				stopAllNotes();
				for (int i = 0; i < hsize; i++) {
					biG2d.setColor(Color.YELLOW);
					biG2d.fillRect(cwidth * i, 0, cwidth * (i + 1), hvheight);
					ThreadHistory threadHistory = histories.get(i);
					int max = threadHistory.getMaxCount();
					int cur = threadHistory.getRunningCount();
					String name = threadHistory.name;
					int start = (int) (hvheight * (double) (max - cur) / max);
					//System.out.printf("Start=%d, height=%d%n", start, hvheight);
					boolean busySelected = soundWhenBusyButton.isSelected();
					boolean idleSelected = soundWhenIdleButton.isSelected();
					if ((start <= hvheight / 5) && busySelected) {
						//System.out.printf("Start note busy%n");
						startNote(noteStart + i * noteStep, (500 * (max - cur)) / max);
					} else if ((start >= 4 * hvheight / 5 && start < hvheight) && idleSelected) {
						//System.out.printf("Start note idle%n");
						startNote(noteStart + i * noteStep, (500 * (max - cur)) / max);
					}
					biG2d.setColor(Color.GREEN);
					biG2d.fillRect(cwidth * i, start, cwidth * (i + 1), hvheight);
					biG2d.setColor(name != null && name.startsWith("new") ? Color.CYAN : Color.BLACK);
					biG2d.drawRect(cwidth * i, 0, cwidth * (i + 1), hvheight);
					
					biG2d.setColor(Color.MAGENTA);
					biG2d.setStroke(new BasicStroke(1));
					if (busySelected) {
						biG2d.drawLine(cwidth * i, hvheight / 5, cwidth * (i + 1), hvheight / 5);
					}
					if (idleSelected) {
						biG2d.drawLine(cwidth * i, 4 * hvheight / 5, cwidth * (i + 1), 4 * hvheight / 5);
					}
					
					if (i > 0) {
						biG2d.setColor(Color.RED);
						biG2d.setStroke(new BasicStroke(2));
						biG2d.drawLine(cwidth * (i - 1) + cwidth / 2, pstart, cwidth * i + cwidth / 2, start);
					}
					pstart = start;
				}
			} else {
				biG2d.setColor(Color.LIGHT_GRAY.brighter());
				biG2d.fillRect(0, 0, hvwidth, hvheight);
			}
			threadHistoryView.setImage(bi);
		}
	}

	protected class ThreadHistory {
		// public Thread thread;
		public boolean[] running;
		public String name;

		public ThreadHistory() {
			running = new boolean[TIMES_PER_SECOND];
		}

		public void setLast(boolean f) {
			for (int i = 0; i < running.length - 1; i++) {
				running[i] = running[i + 1];
			}
			running[running.length - 1] = f;
		}

		public void setLast(String name) {
			this.name = name;
			ActorRunnable ar = actorManager.getRunnable(name);
			setLast(ar != null && ar.hasThread);
		}

		public int getMaxCount() {
			return running.length;

		}

		public int getRunningCount() {
			int count = 0;
			for (int i = 0; i < running.length; i++) {
				if (running[i]) {
					count++;
				}
			}
			return count;
		}
	}

	protected Map<String, ThreadHistory> threadHistoryMap = new HashMap<String, ThreadHistory>();

	protected int safeGetInt(Map<String, Integer> map, String key) {
		Integer i = map.get(key);
		return i != null ? i : -1;
	}

	protected double renderMessageConnections(Graphics2D g2d, AbstractActor[] actors, int height, int width,
			int aradius, int adiam, Map<String, AbstractActor> actorMap, Map<String, Integer> actorXMap,
			Map<String, Integer> actorYMap, double angle, AbstractActor aa, Color color, Set<Actor> selfSends) {
		String actorName = aa.getName();
		actorMap.put(actorName, aa);
		int x = width / 2 + (int) (Math.cos(angle) * aradius);
		actorXMap.put(actorName, x);
		int y = height / 2 + (int) (Math.sin(angle) * aradius);
		actorYMap.put(actorName, y);
		angle += 2 * Math.PI / actors.length;

		Message[] sent = actorManager.getAndClearSentMessages(aa);
		if (sent != null && sent.length > 0) {
			for (Message m : sent) {
				Actor from = m.getSource(), to = aa;
				try {
					if (from != null) {
						if (from != to) {
							// System.out.printf("sent: %s to %s%n", from, to);
							String fromName = from.getName();
							int fx = safeGetInt(actorXMap, fromName), fy = safeGetInt(actorYMap, fromName);
							if (fx >= 0 && fy >= 0) {
								String toName = to.getName();
								int tx = safeGetInt(actorXMap, toName), ty = safeGetInt(actorYMap, toName);
								if (tx >= 0 && ty >= 0) {
									g2d.setColor(Color.RED);
									// g2d.setStroke(new BasicStroke(1,
									// BasicStroke.CAP_BUTT,
									// BasicStroke.JOIN_MITER, 10,
									// new float[] { 5, 2, 3, 2 }, 1));
									g2d.setStroke(new BasicStroke(4));
									g2d.drawLine(fx, fy, tx, ty);
									// System.out.printf("drawSend: (d,%d) -> (%d,%d)%n",
									// fx, fy, tx, ty);
								}
							}
						} else {
							selfSends.add(aa);
						}
					}
				} catch (Exception e) {
					paintError("send", g2d, e);
				}
			}
		}

		Map<String, Integer> fromToCounts = new HashMap<String, Integer>();
		DefaultMessage[] messages = aa.getMessages();
		for (DefaultMessage m : messages) {
			Actor from = m != null ? m.getSource() : null, to = aa;
			if (from != null) {
				try {
					String fromName = from.getName();
					int fx = safeGetInt(actorXMap, fromName), fy = safeGetInt(actorYMap, fromName);
					if (fx >= 0 && fy >= 0) {
						String toName = to.getName();
						int tx = safeGetInt(actorXMap, toName), ty = safeGetInt(actorYMap, toName);
						if (tx >= 0 && ty >= 0) {
							g2d.setColor(color);
							g2d.setStroke(new BasicStroke(2));
							g2d.drawLine(fx, fy, tx, ty);
							int dx = tx - fx, dy = ty - fy;
							int count = getCount(fromToCounts, fromName, toName);
							int markerWidth = (int) (adiam * (0.1 + 0.1 * count));
							int xx = fx + (int) (dx * 0.85) - markerWidth / 2, xy = fy + (int) (dy * 0.85)
									- markerWidth / 2;
							g2d.fillOval(xx, xy, markerWidth, markerWidth);
						}
					}
				} catch (Exception e) {
					paintError("pending", g2d, e);
				}
			}
		}
		return angle;
	}

	protected int getCount(Map<String, Integer> fromToCounts, String fromName, String toName) {
		String fromToName = fromName + toName;
		Integer count = fromToCounts.get(fromToName);
		if (count == null) {
			count = 0;
		}
		count++;
		fromToCounts.put(fromToName, count);
		return count;
	}

	protected double renderThread(Graphics2D g2d, Thread[] threads, int height, int width, int tdiam, int tradius,
			double angle, Thread t, Map<String, Integer> actorIdMap) {
		try {
			int x = width / 2 + (int) (Math.cos(angle) * tradius);
			int y = height / 2 + (int) (Math.sin(angle) * tradius);
			ActorRunnable ar = actorManager.getRunnable(t.getName());
			if (ar != null) {
				g2d.setColor(ar.hasThread ? Color.GREEN.darker() : Color.YELLOW);
				g2d.fillRect(x - tdiam / 2, y - tdiam / 2, tdiam, tdiam);
				Actor a = ar.actor;
				if (ar.hasThread && a != null) {
					int theight = tdiam / 2;
					Color c = actorColorMap.get(a.getClass().getName());
					if (c != null) {
						g2d.setColor(c);
						g2d.fillOval(x - theight / 2, y - theight / 2, theight, theight);
					}
					Integer count = actorIdMap.get(a.getName());
					if (count != null) {
						g2d.setColor(Color.BLACK);
						g2d.drawString(Integer.toString(count), x - tdiam / 2, y
								+ (y > height / 2 ? tdiam + 10 : -tdiam - 10));
					}
				}
			}
			angle += 2 * Math.PI / threads.length;
		} catch (Exception e) {
			paintError("thread", g2d, e);
		}
		return angle;
	}

	protected void renderActor(Graphics2D g2d, int adiam, Map<String, Integer> actorXMap,
			Map<String, Integer> actorYMap, Map<String, Color> actorColorMap, Map<String, Integer> actorIdMap,
			AbstractActor aa, Set<Actor> selfSends, int index, int total) {
		try {
			String name = aa.getName();
			int x = safeGetInt(actorXMap, name), y = safeGetInt(actorYMap, name);
			if (x >= 0 && y >= 0) {
				Color acolor = actorColorMap.get(aa.getClass().getName());
				if (acolor == null) {
					acolor = Color.RED;
				}
				Color xacolor = aa.getHasThread() ? acolor.darker() : acolor;
				g2d.setColor(xacolor);
				if (selfSends.contains(aa)) {
					// System.out.printf("renderActor: self %s%n", aa);
					g2d.drawOval(x - adiam / 2, y - adiam / 2, adiam, adiam);
				} else {
					Color c = isTransparentActors ? new Color(xacolor.getRed(), xacolor.getGreen(), xacolor.getBlue(),
							150) : xacolor;
					g2d.setColor(c);
					// System.out.printf("renderActor: other %s%n", aa);
					g2d.fillOval(x - adiam / 2, y - adiam / 2, adiam, adiam);
					if (adiam > 20) {
						g2d.setColor(Color.BLACK);
						g2d.drawOval(x - adiam / 2, y - adiam / 2, adiam, adiam);
					}
				}
				// g2d.setFont(font);
				// g2d.setColor(Color.BLACK);
				// if (name.length() > 10) {
				// name = name.substring(name.length() - 10);
				// }
				// g2d.drawString(name, x - adiam / 2, y);
				if (index == total - 1) {
					Integer count = actorIdMap.get(name);
					if (count != null) {
						g2d.setColor(Color.BLACK);
						g2d.drawString(Integer.toString(count), x - 10, y + 5);
					}
				}
			}
		} catch (Exception e) {
			paintError("actor", g2d, e);
		}
	}

	protected BufferedImage renderTrendLine(int trendValue, int maxTrendValue) {
		BufferedImage bi = new BufferedImage(METER_WIDTH, METER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int width = bi.getWidth();
		int halfWidth = width / 2;
		int height = bi.getHeight();
		// System.out.printf("renderTrendLine %dx%d: %d, %d%n", width, height,
		// trendValue, maxTrendValue);

		Graphics2D g2d = (Graphics2D) bi.getGraphics();
		// g2d.scale(1, 1);
		g2d.setColor(Color.LIGHT_GRAY.brighter());
		g2d.fillRect(0, 0, width, height);

		int atv = Math.abs(trendValue);
		Color c = Color.GREEN;
		if (atv > maxTrendValue / 3) {
			c = Color.YELLOW;
			if (atv > maxTrendValue * 2 / 3) {
				c = Color.ORANGE;
			}
			if (atv > maxTrendValue) {
				c = Color.RED;
			}
		}
		g2d.setColor(c);
		int tlv = Math.min(atv, maxTrendValue);
		int xl = halfWidth * tlv / maxTrendValue;
		int textX = 0;
		if (trendValue < 0) {
			g2d.fillRect(halfWidth - xl, 0, xl, height);
			textX = halfWidth + 5;
		} else {
			g2d.fillRect(halfWidth, 0, xl, height);
			textX = 5;
		}

		g2d.setColor(Color.BLACK);
		g2d.setFont(new Font("monospace", Font.BOLD, 24));
		g2d.drawString(String.format("%d", trendValue), textX, height / 2);
		g2d.setStroke(new BasicStroke(2));
		g2d.drawLine(halfWidth, 1, halfWidth, height - 2);

		g2d.setColor(Color.DARK_GRAY);
		g2d.drawRect(1, 1, width - 1, height - 2);
		return bi;
	}

	volatile BufferedImage renderImageMain;

	protected void paintError(String from, Graphics2D g2d, Exception e) {
		g2d.setColor(Color.RED);
		errorY += 50;
		g2d.drawString(e.toString(), 25, errorY);
		System.out.printf("paint exception %s: %s%n", from, e);
		e.printStackTrace(System.out);
	}

	Font font;

	public void postShow() {
		Dimension d = getSize();
		double posn = d.width >= 800 ? 0.33 : (d.width >= 400 ? 0.40 : 0.50);
		splitPane.setDividerLocation(posn);
		font = new Font("Courier New", Font.PLAIN, 10);
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		setMessage(String.format("Count down to halt: %d", test.getStepCount()));
	}

	DefaultActorTest test;

	public DefaultActorTest getTest() {
		return test;
	}

	public void setActorTest(DefaultActorTest test) {
		this.test = test;
	}

	public void runTest(String args) {
		actorManager = new DefaultActorManager();
		test = new DefaultActorTest();
		test.setActorManager(actorManager);
		// am.detachAllActors();
		if (!isMidiOpen()) {
			try {
				setupMidi();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		test.addChangeListener(this);
		int value = (int) Math.pow(2, stepSlider.getValue());
		test.run((args + " -sc:" + value + " -tc:" + threadCount).split("\\s+"));
		test.removeChangeListener(this);
		// am.detachAllActors();
		if (isMidiOpen()) {
			try {
				shutdownMidi();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {

		ActorDemo av = new ActorDemo();

		JFrame frame = new JFrame("ActorDemo v1.0");
		// frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}

			@Override
			public void windowClosed(WindowEvent e) {
				System.exit(0);
			}
		});
		frame.setSize(1000, 850);
		av.initUI(frame);

		frame.setVisible(true);
		av.postShow();
		// av.runTest(args);

		Utils.logger.trace("Done");
	}
}
