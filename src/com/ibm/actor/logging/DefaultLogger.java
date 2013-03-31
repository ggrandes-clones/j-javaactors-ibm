package com.ibm.actor.logging;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import javax.swing.JTextArea;

import com.ibm.actor.utils.Utils;

/**
 * Default Logger implementation. 
 * 
 * Intended only for tracing, not core to the Actor article. 
 * More functional than needed for this context. 
 * 
 * @author bfeigenb
 * 
 */
public class DefaultLogger implements Logger {
	private static final String MTP_PACKAGE_PREFIX = "com.ibm.haac.mtp.";

	private static final String myCname = DefaultLogger.class.getName();

	private static final String MTP_EMAIL_SUBJECT = "Mobile Topic Pods ERROR Notification";

	private static final String MTP_EMAIL = "mtp@us.ibm.com";

	private static final String SMTP_SERVER = "smtp.server";

	private static final String LOGGER_RESOURCE = "logger.properties";

	private static DefaultLogger instance;

	public static DefaultLogger getDefaultInstance() {
		if (instance == null) {
			instance = new DefaultLogger();
		}
		return instance;
	}

	protected static String getDefaultContext() {
		return null;
	}

	public DefaultLogger() {
		this(getDefaultContext());
	}

	public DefaultLogger(Object context) {
		this.context = context;
	}

	@Override
	public String toString() {
		return getClass().getName() + "[context=" + context
				+ ", includeStacktrace=" + includeStacktrace
				+ ", logToConsole=" + logToConsole + "]";
	}

	// private String dateTimePattern = "%1$tm-%<td-%<tY %<tH:%<tM:%<tS.%<tL";
	private String datePattern = "%1$tm-%<td-%<tY";
	private String timePattern = "%1$tH:%<tM:%<tS.%<tL";

	public String getDatePattern() {
		return timePattern;
	}

	public void setDatePattern(String datePattern) {
		this.timePattern = datePattern;
	}

	private Object context;

	public Object getContext() {
		return context;
	}

	/**
	 * Defines a context (used to qualify logging messages). Some
	 * implementations use the context to filter logging.
	 * 
	 * @param context
	 *            generally a String or a Class.
	 */
	public void setContext(Object context) {
		this.context = context;
	}

	protected boolean logToFile = true;

	public boolean isLogToFile() {
		return logToFile;
	}

	public void setLogToFile(boolean logToFile) {
		this.logToFile = logToFile;
	}

	private boolean includeThread = true;

	public boolean isIncludeThread() {
		return includeThread;
	}

	public void setIncludeThread(boolean includeThread) {
		this.includeThread = includeThread;
	}

	private boolean includeCaller = true;

	public boolean isIncludeCaller() {
		return includeCaller;
	}

	public void setIncludeCaller(boolean includeCaller) {
		this.includeCaller = includeCaller;
	}

	private boolean includeDate = true;

	public boolean isIncludeDate() {
		return includeDate;
	}

	public void setIncludeDate(boolean includeDate) {
		this.includeDate = includeDate;
	}

	private boolean includeContext = true;

	public boolean isIncludeContext() {
		return includeContext;
	}

	public void setIncludeContext(boolean includeContext) {
		this.includeContext = includeContext;
	}

	private LogLevel logLevel = LogLevel.INFO;

	public LogLevel getLogLevel() {
		return logLevel;
	}

	/**
	 * Controls what level of logging occurs.
	 * 
	 * @param logLevel
	 *            only this and higher levels will be recorded
	 * @see LogLevel
	 */
	public void setLogLevel(LogLevel logLevel) {
		this.logLevel = logLevel;
	}

	protected int callerFieldWidth = 60;

	public int getCallerFieldWidth() {
		return callerFieldWidth;
	}

	public void setCallerFieldWidth(int callerFieldWidth) {
		this.callerFieldWidth = callerFieldWidth;
	}

	protected int threadFieldWidth = 20;

	public int getThreadFieldWidth() {
		return threadFieldWidth;
	}

	public void setThreadFieldWidth(int threadFieldWidth) {
		this.threadFieldWidth = threadFieldWidth;
	}

	protected synchronized String log(LogLevel level, String message,
			Object... values) {
		String res = null;
		if (level.ordinal() >= logLevel.ordinal()) {
			Date now = new Date();
			String nowDate = Utils.safeFormat(datePattern, now);
			String nowTime = Utils.safeFormat(timePattern, now);
			PrintStream ps = getPrintStream(level);
			try {
				StringBuilder sb = new StringBuilder();
				if (includeDate) {
					sb.append(nowTime);
				}
				if (includeContext && context != null) {
					String text = context instanceof Class ? ((Class<?>) context)
							.getName() : context.toString();
					sb.append(Utils.safeFormat(" (%s)", text));
				}
				// sb.append(Utils.safeFormat(" %-10s ", level.toString()));
				sb.append(Utils.safeFormat(" %c ", level.toString().charAt(0)));
				if (includeThread) {
					String tname = Thread.currentThread().getName();
					if (!Utils.isEmpty(tname)) {
						if (tname.length() > threadFieldWidth) {
							tname = "<" + tname.substring(tname.length() - 19);
						}
						sb.append(Utils.safeFormat("[%-" + threadFieldWidth
								+ "s] ", tname));
					}
				}
				if (includeCaller) {
					StackTraceElement ste = getCaller();
					if (ste != null) {
						String cname = ste.getClassName();
						if (cname.startsWith(MTP_PACKAGE_PREFIX)) {
							cname = "<."
									+ cname.substring(MTP_PACKAGE_PREFIX
											.length());
						}
						String calledFrom = Utils.safeFormat("%s#%s:%d", cname,
								ste.getMethodName(), ste.getLineNumber());
						int length = calledFrom.length();
						if (length > callerFieldWidth) {
							calledFrom = "..."
									+ calledFrom.substring(length
											- callerFieldWidth + 3, length);
						}
						sb.append(Utils.safeFormat("(%-" + callerFieldWidth
								+ "s)", calledFrom));
					}
				}
				if (includeThread || includeCaller) {
					sb.append(Utils.safeFormat("- "));
				}
				sb.append(Utils.isEmpty(values) ? message : Utils.safeFormat(
						message, values));
				sb.append('\n');

				String text = sb.toString();
				res = text;
				if (logToConsole) {
					syncPrint(ps, text);
				}

				if (logArea != null) {
					logArea.append(text);
					logArea.setCaretPosition(logArea.getText().length());
				}
			} catch (Exception e) {
				syncPrint(ps, Utils.safeFormat("log exception %s, %s: %s%n",
						level, message, e));
			}
		}
		return res;
	}

	protected JTextArea logArea;

	public JTextArea getLogArea() {
		return logArea;
	}

	public void setLogArea(JTextArea logArea) {
		this.logArea = logArea;
	}

	protected static StackTraceElement getCaller() {
		StackTraceElement res = null;
		StackTraceElement[] stea = Thread.currentThread().getStackTrace();
		for (int i = 0; res == null && i < stea.length; i++) {
			StackTraceElement ste = stea[i];
			if (ste.getClassName().equals(myCname)) {
				continue;
			}
			if (ste.getClassName().endsWith("Utils")) {
				continue;
			}
			if (ste.getMethodName().indexOf("getStackTrace") == 0) {
				continue;
			}
			res = ste;
		}
		return res;
	}

	protected void syncPrint(PrintStream ps, String text) {
		synchronized (ps) {
			ps.print(text);
			ps.flush();
		}
	}

	@Override
	public void info(String message, Object... values) {
		log(LogLevel.INFO, message, values);
	}

	@Override
	public void trace(String message, Object... values) {
		log(LogLevel.TRACE, message, values);
	}

	@Override
	public void warning(String message, Object... values) {
		log(LogLevel.WARNING, message, values);
	}

	protected String logSevere(LogLevel level, String message, Object... values) {
		String res = null;
		try {
			if (lastIsThrowable(values)) {
				res = log(level, Utils.safeFormat(message, values) + ": %s",
						(Throwable) values[values.length - 1]);
				String trace = logStackTrace(level,
						(Throwable) values[values.length - 1]);
				if (!Utils.isEmpty(trace)) {
					res += "\n\n" + trace;
				}
			} else {
				res = log(level, message, values);
			}
		} catch (Exception e) {
			System.out.printf("logSevere exception %s, %s: %s%n", level,
					message, e);
		}
		return res;
	}

	@Override
	public void error(String message, Object... values) {
		logSevere(LogLevel.ERROR, message, values);
	}

	protected String notifyRecepients;

	public String getNotifyRecepients() {
		return notifyRecepients;
	}

	public void setNotifyRecepients(String notifyRecepients) {
		this.notifyRecepients = notifyRecepients;
	}

	@Override
	public void notify(String message, Object... values) {
		String msg = logSevere(LogLevel.NOTIFY, message, values);
	}

	protected boolean lastIsThrowable(Object... values) {
		return values != null && values.length > 0
				&& values[values.length - 1] instanceof Throwable;
	}

	protected String logStackTrace(LogLevel level, Throwable t) {
		String res = null;
		if (t != null && includeStacktrace) {
			StringWriter sw = new StringWriter();
			t.printStackTrace(new PrintWriter(sw));
			String text = sw.toString();
			res = text;
			if (logToConsole) {
				getPrintStream(level).print(text);
			}
		}
		return res;
	}

	protected PrintStream getPrintStream(LogLevel level) {
		// PrintStream out = level == LogLevel.NOTIFY ? System.err : System.out;
		PrintStream out = System.out;
		return out;
	}

	private boolean includeStacktrace = true;

	public boolean getIncludeStacktrace() {
		return includeStacktrace;
	}

	/**
	 * Determines if ERROR or NOTIFY level logging with an Exception also
	 * includes the stack trace.
	 * 
	 * @param includeStacktrace
	 */
	public void setIncludeStacktrace(boolean includeStacktrace) {
		this.includeStacktrace = includeStacktrace;
	}

	private boolean logToConsole = true;
	/*
	 * // test case public static void main(String[] args) { DefaultLogger lu =
	 * new DefaultLogger(); lu.setIncludeStacktrace(true); lu.setFileLogger(new
	 * FileLogger("/temp/log"));
	 * 
	 * lu.info("info message: %s", "sub"); lu.warning("warn message: %s",
	 * "sub"); lu.error("error message: %s", "sub");
	 * lu.notify("notify message: %s", "sub");
	 * 
	 * Exception e = new Exception("test exception"); lu.error("error message",
	 * e); lu.notify("notify message", e);
	 * 
	 * lu.setContext(DefaultLogger.class); lu.info("info message: %s", "sub"); }
	 */
}
