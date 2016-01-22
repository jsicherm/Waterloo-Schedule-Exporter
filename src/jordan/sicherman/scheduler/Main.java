package jordan.sicherman.scheduler;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.DefaultCaret;

public class Main {

	private static File exportFile;

	public static JTextArea input;
	private static boolean valid;

	private static Action wrapper = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (!valid)
				valid = true;
			else
				System.exit(0);
		}
	};

	public static void createFrame() {
		JFrame frame = new JFrame("Waterloo Schedule Exporter");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setOpaque(true);
		input = new JTextArea(13, 46);
		input.setWrapStyleWord(true);
		input.setEditable(true);
		input.append("Paste the list view of your class schedule here." + System.lineSeparator()
				+ System.lineSeparator() + "1. Log into Quest" + System.lineSeparator()
				+ "2. Navigate to the Enroll tab and view your class schedule for the desired term."
				+ System.lineSeparator() + "3. Press ctrl+A followed by ctrl+C" + System.lineSeparator()
				+ "4. Click on this textbox and then press ctrl+A followed by ctrl+V" + System.lineSeparator()
				+ "5. Press Enter.");
		Object actionKey = input.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("ENTER"));
		input.getActionMap().put(actionKey, wrapper);
		JScrollPane scroller = new JScrollPane(input);
		scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		DefaultCaret caret = (DefaultCaret) input.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		panel.add(scroller);
		frame.getContentPane().add(BorderLayout.CENTER, panel);
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		frame.setResizable(true);
	}

	public static void main(String[] args) {
		createFrame();
		while (!valid)
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		String everything = input.getText();
		exportFile = new File(
				System.getProperty("user.home") + File.separator + "Desktop" + File.separator + "Calendar.ical");
		createICAL();

		List<String> classes = new ArrayList<String>();

		StringBuilder sb = new StringBuilder();

		boolean started = false;
		int toSkip = 0;

		long start = System.currentTimeMillis();
		int failCount = 0;

		for (String line : everything.split("\n"))
			if (line != null && !"Printer Friendly Page".equals(line)) {
				if (started) {
					String app = beautify(line.trim());
					if (!app.isEmpty())
						if (app.startsWith("Exam Information")) {
							classes.add(sb.toString());
							sb = new StringBuilder();
						} else {
							if (app.startsWith("Enrolled"))
								toSkip = 3;
							else if (app.startsWith("Degree Requirement"))
								toSkip += 1;
							if (toSkip > 0)
								toSkip--;
							else {
								sb.append(app);
								sb.append(System.lineSeparator());
							}
						}
				}

				started = started || line.endsWith("University of Waterloo");
			}

		for (String s : classes) {
			String name = s.substring(0, s.indexOf(" - "));
			String description = s.substring(s.indexOf(" - ") + 3,
					s.indexOf(System.lineSeparator(), s.indexOf(" - ") + 3));
			String section = null, component = null, dates = null, room = null, prof = null, range = null;
			for (String l : s.substring(s.indexOf(description) + description.length() + 2)
					.split(System.lineSeparator())) {
				if (l.matches("\\d{4}")) {
					section = null;
					component = null;
					continue;
				}

				if (section == null)
					section = l;
				else if (component == null)
					component = l;
				else if (dates == null)
					dates = l;
				else if (room == null)
					room = l;
				else if (prof == null)
					prof = l;
				else if (range == null)
					range = l;
				if (range != null) {
					try {
						failCount += processClass(name, section, component, description, room, prof, dates, range);
					} catch (Exception exc) {
						reportError(exc);
						try {
							close();
						} catch (Exception e) {
							reportError(e);
						}
						return;
					}
					dates = null;
					room = null;
					prof = null;
					range = null;
				}
			}
		}

		long time = System.currentTimeMillis() - start;

		try {
			close();
		} catch (Exception exc) {
			reportError(exc);
			return;
		}

		if (classes.size() == 0)
			input.setText("It appears that your schedule was malformatted. " + degrade());
		else
			input.setText("Successfully exported " + (classes.size() - failCount)
					+ " class components to Calendar.ical on your desktop (" + time + " ms)."
					+ (failCount > 0 ? "\n" + failCount
							+ " class(es) were not added because the details have not yet been finalized." : "")
					+ "\n\n" + degradeAgain());
		input.setEditable(false);
	}

	private static String degrade() {
		switch (new Random().nextInt(11)) {
		case 0:
			return "Try harder this time.";
		case 1:
			return "Were the instructions not clear enough?";
		case 2:
			return "I thought you went to Waterloo?";
		case 3:
			return "You must be in arts or something.";
		case 4:
			return "Thanks for trying.";
		case 5:
			return "Go on, give it another go.";
		case 6:
			return "But you were so close.";
		case 7:
			return "Are you sure you're in university?";
		case 8:
			return "Awkward.";
		case 9:
			return "Maybe it was my fault.";
		default:
			return "I even bet that you would get it right.";
		}
	}

	private static String degradeAgain() {
		switch (new Random().nextInt(11)) {
		case 0:
			return "Press the any key.";
		case 1:
			return "Thanks for playing.";
		case 2:
			return "Your calendar will be so happy.";
		case 3:
			return "That's all you're taking?";
		case 4:
			return "I'm glad I'm not taking your classes.";
		case 5:
			return "See you next term!";
		case 6:
			return "Enjoy!";
		case 7:
			return "I'm surprised it worked!";
		case 8:
			return "I also made you some coffee while you were waiting.";
		case 9:
			return "Sorry I took so long!";
		default:
			return "Have a nice term!";
		}
	}

	private static String degradeSelf() {
		switch (new Random().nextInt(11)) {
		case 0:
			return "I'm sorry!";
		case 1:
			return "Well it was worth a shot.";
		case 2:
			return "I should have known that would happen.";
		case 3:
			return "Oh well.";
		case 4:
			return "Maybe next year.";
		case 5:
			return "Your schedule must be funny looking.";
		case 6:
			return "Nobody's perfect.";
		case 7:
			return "Well shit.";
		case 8:
			return "Maybe if you try running it again...";
		case 9:
			return "This probably isn't my fault...";
		default:
			return "Oops.";
		}
	}

	private static void reportError(Exception exc) {
		input.setText("Unexpected Error: " + exc.getMessage() + "\n\n" + degradeSelf());
		input.setEditable(false);
		exc.printStackTrace();
	}

	private static void createICAL() {
		try {
			exportFile.createNewFile();
		} catch (IOException exc) {
			reportError(exc);
		}

		try {
			writer = new BufferedWriter(new FileWriter(exportFile));
			writer.write("BEGIN:VCALENDAR" + System.lineSeparator() + "VERSION:2.0" + System.lineSeparator()
					+ "PRODID:-//Jordan Sicherman//Waterloo Class Schedule//EN" + System.lineSeparator()
					+ "CALSCALE:GREGORIAN" + System.lineSeparator() + "METHOD:PUBLISH" + System.lineSeparator()
					+ "X-WR-TIMEZONE:America/Toronto" + System.lineSeparator()
					+ "X-WR-CALDESC:A class schedule for a student at the University of Waterloo. Generated automatically by Jordan Sicherman."
					+ System.lineSeparator() + System.lineSeparator());

			writer.write("BEGIN:VTIMEZONE" + System.lineSeparator() + "TZID:America/Toronto" + System.lineSeparator()
					+ "X-LIC-LOCATION:America/Toronto" + System.lineSeparator() + "BEGIN:DAYLIGHT"
					+ System.lineSeparator() + "TZOFFSETFROM:-0500" + System.lineSeparator() + "TZOFFSETTO:-0400"
					+ System.lineSeparator() + "TZNAME:EDT" + System.lineSeparator() + "DTSTART:19700308T020000"
					+ System.lineSeparator() + "RRULE:FREQ=YEARLY;BYMONTH=3;BYDAY=2SU" + System.lineSeparator()
					+ "END:DAYLIGHT" + System.lineSeparator() + "BEGIN:STANDARD" + System.lineSeparator()
					+ "TZOFFSETFROM:-0400" + System.lineSeparator() + "TZOFFSETTO:-0500" + System.lineSeparator()
					+ "TZNAME:EST" + System.lineSeparator() + "DTSTART:19701101T020000" + System.lineSeparator()
					+ "RRULE:FREQ=YEARLY;BYMONTH=11;BYDAY=1SU" + System.lineSeparator() + "END:STANDARD"
					+ System.lineSeparator() + "END:VTIMEZONE" + System.lineSeparator() + System.lineSeparator());
		} catch (IOException exc) {
			reportError(exc);
		}
	}

	private static void close() throws Exception {
		writer.write("END:VCALENDAR");
		writer.flush();
		writer.close();
	}

	private static BufferedWriter writer;

	private static int processClass(String name, String section, String component, String description, String room,
			String prof, String dates, String range) throws Exception {
		if ("TBA".equals(dates))
			return 1;
		room = room.replaceAll("  ", " ");

		String days = fetchDates(dates.split(" ")[0]);
		String timeSlot = dates.split(" ")[1];
		int sHour = Integer.parseInt(timeSlot.substring(0, timeSlot.indexOf(":")));
		sHour += timeSlot.charAt(timeSlot.indexOf(":") + 3) == 'P' ? sHour < 12 ? 12 : 0 : 0;
		String sMin = timeSlot.substring(timeSlot.indexOf(":") + 1, timeSlot.indexOf(":") + 3);

		timeSlot = dates.split(" - ")[1];
		int eHour = Integer.parseInt(timeSlot.substring(0, timeSlot.indexOf(":")));
		eHour += timeSlot.charAt(timeSlot.indexOf(":") + 3) == 'P' ? eHour < 12 ? 12 : 0 : 0;
		String eMin = timeSlot.substring(timeSlot.indexOf(":") + 1, timeSlot.indexOf(":") + 3);

		String dateSlot = range.split(" - ")[0];
		int sMon = Integer.parseInt(dateSlot.substring(0, 2));
		int sDay = Integer.parseInt(dateSlot.substring(3, 5));
		int sYear = Integer.parseInt(dateSlot.substring(6, 10));

		dateSlot = range.split(" - ")[1];
		int eMon = Integer.parseInt(dateSlot.substring(0, 2));
		int eDay = Integer.parseInt(dateSlot.substring(3, 5));
		int eYear = Integer.parseInt(dateSlot.substring(6, 10));

//		sHour += 3;
//		eHour += 3;

		writer.write("BEGIN:VEVENT" + System.lineSeparator() + "UID:" + UUID.randomUUID().toString().replaceAll("-", "")
				+ System.lineSeparator());

		writer.write("SUMMARY:" + name + " " + component + " in " + room + System.lineSeparator() + "LOCATION:" + room
				+ System.lineSeparator());
		writer.write("DESCRIPTION:" + name + "-" + section + ": " + description + " (" + component + ") in " + room
				+ " with " + prof + System.lineSeparator());

		writer.write("DTSTART;TZID=America/Toronto:" + sYear + "" + (sMon < 10 ? "0" : "") + sMon + ""
				+ (sDay < 10 ? "0" : "") + sDay + "T" + (sHour < 10 ? "0" : "") + sHour + sMin + "00"
				+ System.lineSeparator());
		writer.write(
				"DTEND;TZID=America/Toronto:" + sYear + "" + (sMon < 10 ? "0" : "") + sMon + "" + (sDay < 10 ? "0" : "")
						+ sDay + "T" + (eHour < 10 ? "0" : "") + eHour + eMin + "00" + System.lineSeparator());
		if (sMon != eMon || sDay != eDay || sYear != eYear)
			writer.write("RRULE:FREQ=WEEKLY;UNTIL=" + eYear + "" + (eMon < 10 ? "0" : "") + eMon + ""
					+ (eDay < 10 ? "0" : "") + eDay + "T" + (eHour < 10 ? "0" : "") + eHour + eMin + "00"
					+ ";WKST=SU;BYDAY=" + days + System.lineSeparator());

		writer.write("BEGIN:VALARM" + System.lineSeparator() + "ACTION:DISPLAY" + System.lineSeparator()
				+ "DESCRIPTION:This is an event reminder" + System.lineSeparator() + "TRIGGER:-P0DT0H25M0S"
				+ System.lineSeparator() + "END:VALARM" + System.lineSeparator());

		writer.write("END:VEVENT" + System.lineSeparator() + System.lineSeparator());
		return 0;
	}

	private static String fetchDates(String format) throws Exception {
		StringBuilder process = new StringBuilder();
		boolean maybeThursday = false;
		for (String s : format.split(""))
			switch (s) {
			case "M":
				if (maybeThursday) {
					maybeThursday = false;
					process.append("TU,");
				}
				process.append("MO,");
				break;
			case "W":
				if (maybeThursday) {
					maybeThursday = false;
					process.append("TU,");
				}
				process.append("WE,");
				break;
			case "F":
				if (maybeThursday) {
					maybeThursday = false;
					process.append("TU,");
				}
				process.append("FR,");
				break;
			case "S":
				if (maybeThursday) {
					maybeThursday = false;
					process.append("TU,");
				}
				process.append("SA,");
				break;
			default:
				if (!maybeThursday)
					maybeThursday = true;
				else if ("h".equals(s)) {
					process.append("TH,");
					maybeThursday = false;
					break;
				} else {
					process.append("TU,");
					break;
				}
				break;
			}
		if (maybeThursday)
			process.append("TU,");
		return process.substring(0, process.length() - 1);
	}

	private static final String[] spaceReplace = new String[] { "\r", "\t" };
	private static final String[] removeReplace = new String[] { "Status[ ]+Units[ ]+Grading[ ]+Grade",
			"Numeric Grading Basis",
			"Class Nbr[ ]+Section[ ]+Component[ ]+Days & Times[ ]+Room[ ]+Instructor[ ]+Start/End Date",
			"Degree Requirement, Not in Avg, Not in Fail Count (Taken)", "Credit / Non-Credit Basis",
			"Status[ ]+Units[ ]+Grading Grade[ ]+Requirement Designation", "Enrolled" };

	private static String beautify(String line) {
		for (String s : spaceReplace)
			line = line.replaceAll(s, " ");
		for (String s : removeReplace)
			line = line.replaceAll(s, "");
		return line;
	}
}
