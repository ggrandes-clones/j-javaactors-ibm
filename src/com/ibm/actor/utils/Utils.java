package com.ibm.actor.utils;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.NodeList;

import com.ibm.actor.logging.DefaultLogger;
import com.ibm.actor.logging.Logger;

/**
 * Assorted utility functions (sort of a kitchen sink). 
 * Some methods used but core to Actor implementation.
 * Additional unused methods may be included and should be ignored.  
 * 
 * @author bfeigenb
 * 
 */
public class Utils {
	public static final String _OUT_QUALIFIER = "_out.";

	public static final String UTF8_ENCODING_NAME = "UTF-8";

	public static final String MIME_WILDCARD_SUFFIX = "/*";
	public static DateFormat hhmmsstttTimeFormat = new SimpleDateFormat(
			"hh-mm-ss.SSS");
	public static DateFormat yyyymmddDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd");
	public static SimpleDateFormat dashOnlyDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'hh-mm-ss-SSS");
	public static DateFormat standardFullDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	public static DateFormat dateTimeInstance = DateFormat.getDateTimeInstance(
			DateFormat.SHORT, DateFormat.MEDIUM);

	public static final String FORM_URLENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded";

	/** Set to enable debugging. */
	public static final String MTP_DEBUG_PROPERTY = "com.ibm.haac.mtp.debug";

	private static final int LOG_RETENSION_LENGTH = 1 * 1000 * 1000;

	public static final Logger logger = DefaultLogger.getDefaultInstance();

	/** File to log connection into. Placed in current temp directory. */
	public static final String HTTP_CONNECTIONS_LOG_FILE = "connections.log";

	public static final int SECOND_MILLIS = 1000;
	public static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
	public static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final int DAY_MILLIS = 24 * HOUR_MILLIS;

	protected static final int CHAR_BUFFER_LENGTH = 1024;

	/** US.IBM.COM suffix. */
	public static final String US_IBM_COM = "us.ibm.com";

	/** Carriage Return character as a string. */
	public static final String CR = "\r";
	/** Line Feed character as a string. */
	public static final String LF = "\n";
	/** Form Feed character as a string. */
	public static final String FF = "\f";
	/** Tab character as a string. */
	public static final String TAB = "\t";
	/** Tab character as a string. */
	public static final String SPACE = " ";

	public static String capitalizeFirst(String s) {
		if (!isEmpty(s)) {
			s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
		}
		return s;
	}

	public static boolean isNull(Object o) {
		return o == null;
	}

	/** Test if array is null or empty. */
	public static boolean isEmpty(byte[] ba) {
		return ba == null || ba.length == 0;
	}

	/** Test if array is null or empty. */
	public static boolean isEmpty(Object[] oa) {
		return oa == null || oa.length == 0;
	}

	/** Test if string is null or empty. */
	public static boolean isEmpty(CharSequence s) {
		return s == null || s.length() == 0;
	}

	/** Test if string is null or empty. */
	public static boolean isEmpty(Appendable s) {
		int length = -1;
		if (s != null) {
			Method m = null;
			Class<? extends Appendable> sclass = s.getClass();
			try {
				m = sclass.getMethod("size");
			} catch (NoSuchMethodException e1) {
				try {
					m = sclass.getMethod("length");
				} catch (NoSuchMethodException e2) {
					try {
						m = sclass.getMethod("getLength");
					} catch (NoSuchMethodException e3) {
					}
				}
			}
			if (m != null) {
				try {
					length = (Integer) m.invoke(s);
				} catch (Exception e) {
				}
			}
		}
		return s == null || length == 0;
	}

	/** Test if node list is null or empty. */
	public static boolean isEmpty(NodeList nl) {
		return nl == null || nl.getLength() == 0;
	}

	/** Test if map is null or empty. */
	public static boolean isEmpty(Map<?, ?> m) {
		return m == null || m.size() == 0;
	}

	/** Test if collection is null or empty. */
	public static boolean isEmpty(Collection<?> c) {
		return c == null || c.size() == 0;
	}

	public static boolean isEmptyOrNull(String s) {
		return isEmpty(s) || s.trim().equalsIgnoreCase("null");
	}

	/** Make null string have some value. */
	public static String fixNull(String s) {
		return s != null ? s : "Unknown";
	}

	public static String formatString(String s) {
		return !isEmpty(s) ? s : "";
	}

	/** Truncate a text string to 60 characters. Replace newlines with '~'. */
	public static String truncateText(String text) {
		return truncateText(text, 60);
	}

	/** Truncate a text string to length characters. Replace newlines with '~'. */
	public static String truncateText(String text, int length) {
		if (!isEmpty(text)) {
			text = truncate(text, length);
			text = text.replace('\r', '~').replace('\n', '~');
		}
		return text;
	}

	/** Truncate a text string to 100 characters. */
	public static String truncate(Object s) {
		return s != null ? truncate(s.toString(), 100) : "null";
	}

	/** Remove repeated whitespace (including newlines) */
	public static String removeMultipleSpaces(String s) {
		return removeMultipleSpaces(s, ' ');
	}

	/** Remove repeated whitespace (including newlines) */
	public static String removeMultipleSpaces(String s, char nlReplace) {
		if (!isEmpty(s)) {
			// s = s.replace('\n', nlReplace);
			s = s.replaceAll("\\s\\s+", " ");
		}
		return s;
	}

	/** Truncate a text string to size characters. */
	public static String truncate(String s, int size) {
		if (!isEmpty(s)) {
			s = removeMultipleSpaces(s);
			if (s.length() > size) {
				int leadLength = size / 2;
				int tailLength = size / 2;
				s = s.substring(0, leadLength) + " ... "
						+ s.substring(s.length() - tailLength);
			}
		}
		return s;
	}

	/** Remove lead text before "&lt;HTML" start. */
	public static String trimDoctype(String contents) {
		if (!isEmpty(contents)) {
			int posn = contents.toLowerCase().indexOf("<html");
			if (posn > 0) {
				contents = contents.substring(posn);
			}
		}
		return contents;
	}

	/** Remove content after a ';'. */
	public static String stripAfterSemicolon(String type) {
		return stripAfter(type, ';');
	}

	/** Remove content after a delimiter character. */
	public static String stripAfter(String type, char c) {
		if (!isEmpty(type)) {
			int posn = type.indexOf(c);
			if (posn > 0) {
				type = type.substring(0, posn);
			}
		}
		return type;
	}

	public static String[] splitAndTrim(String s) {
		return splitAndTrim(s, ",|;");
	}

	public static String[] splitAndTrim(String s, String delim) {
		String[] sa = null;
		if (s != null) {
			sa = s.split(delim);
			for (int i = 0; i < sa.length; i++) {
				sa[i] = sa[i].trim();
			}
		}
		return sa != null ? sa : new String[0];
	}

	/** Test if all strings are not empty/null. */
	public static boolean areAllNotEmpty(String... values) {
		boolean res = true;
		for (String v : values) {
			res &= !isEmpty(v);
		}
		return res;
	}

	/** Test if any strings are not empty/null. */
	public static boolean areAnyNotEmpty(String... values) {
		boolean res = false;
		for (String v : values) {
			res |= !isEmpty(v);
		}
		return res;
	}

	/** Trim a potentially null string. */
	public static String safeTrim(String s) {
		return s != null ? s.trim() : s;
	}

	/**
	 * Convert a name to camelCase format.
	 * 
	 * @param name
	 * @return
	 */
	public static String camelCaseName(String name) {
		return isEmpty(name) ? "" : Character.toLowerCase(name.charAt(0))
				+ name.substring(1);
	}

	/**
	 * Does the string look like JSON.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isJsonArray(String s) {
		return isJson(s, '[', ']');
	}

	/**
	 * Does the string look like JSON.
	 * 
	 * @param s
	 * @return
	 */
	public static boolean isJsonObject(String s) {
		return isJson(s, '{', '}');
	}

	private static boolean isJson(String s, char b, char e) {
		boolean res = false;
		if (s != null) {
			s = s.trim();
			if (s.length() >= 2) {
				res = s.charAt(0) == e && s.charAt(s.length() - 1) == e;
			}
		}
		return res;
	}

	/** Test objects for null or (if a string) "null". */
	public static boolean areAllNull(Object[] oa) {
		boolean res = true;
		if (oa != null) {
			for (Object o : oa) {
				res = o == null || "null".equals(o);
			}
		}
		return res;
	}

	public static String toList(Object[] oa) {
		return oa != null ? Arrays.asList(oa).toString() : null;
	}

	public static String defValue(String text, String def) {
		if (isEmpty(text)) {
			text = def;
		}
		return text;
	}

	public static void printf(String format, Object... args) {
		synchronized (System.out) {
			System.out.printf(format, args);
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ForToString {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ForEquals {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	public @interface ForCompareTo {
		String value() default "";
	}

	@Override
	public String toString() {
		return toString(this);
	}

	/** Reflectively implement toString() using ForToString annotations. */
	public String toString(Object target) {
		Map<String, Object> xfields = getMineAndParentFields(ForToString.class,
				target);
		return formatToString(xfields, target);
	}

	public static boolean useShortClassName = true;

	protected String formatToString(Map<String, Object> xfields, Object target) {
		StringBuilder sb = new StringBuilder();
		String cname = useShortClassName ? target.getClass().getSimpleName()
				: target.getClass().getName();
		if (isEmpty(cname)) {
			cname = target.getClass().getName();
		}
		sb.append(cname);
		sb.append('[');
		if (!isEmpty(xfields)) {
			for (String key : xfields.keySet()) {
				if (sb.charAt(sb.length() - 1) != '[') {
					sb.append(',');
				}
				Object v = xfields.get(key);
				sb.append(key);
				sb.append('=');
				if (v != null) {
					if (v.getClass().isArray()) {
						Object[] va = new Object[] { v };
						v = Arrays.deepToString(va);
					}
				}
				sb.append(v);
			}
		}
		sb.append(']');
		sb.append('@');
		sb.append(Integer.toHexString(System.identityHashCode(this))
				.toUpperCase());
		return sb.toString();
	}

	@Override
	/** Reflectively implement equals() using ForEquals annotations. */
	public boolean equals(Object that) {
		boolean res = that != null;
		if (res) {
			res = this.getClass().isAssignableFrom(that.getClass());
		}
		if (res) {
			res = testEquals(that);
		}
		return res;
	}

	protected boolean testEquals(Object that) {
		boolean res = true;
		Map<String, Object> fields1 = getMineAndParentFields(ForEquals.class,
				this);
		Map<String, Object> fields2 = getMineAndParentFields(ForEquals.class,
				that);
		if (!isEmpty(fields1) && fields2.size() >= fields1.size()) {
			for (String key : fields1.keySet()) {
				Object v1 = fields1.get(key);
				Object v2 = fields2.get(key);
				if (v1 != null) {
					if (v2 != null) {
						res &= v1.equals(v2);
					} else {
						res = false;
					}
				} else {
					if (v2 != null) {
						res = false;
					} else {
						// res &= true;
					}
				}
				if (!res) {
					break;
				}
			}
		} else {
			res = super.equals(that);
		}
		return res;
	}

	@SuppressWarnings("rawtypes")
	/** Reflectively implement compareTo() using ForCompareTo annotations. */
	public int compareToOther(Object that) {
		int res = that != null ? (this == that ? 0 : 1) : -1;
		if (that != null) {
			if (this instanceof Comparable && that instanceof Comparable
					&& this.getClass().isAssignableFrom(that.getClass())) {
				res = testCompareTo((Comparable) this, (Comparable) that);
			}
		}
		return res;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected int testCompareTo(Comparable o1, Comparable o2) {
		int res = 0;
		Map<String, Object> fields1 = getMineAndParentFields(
				ForCompareTo.class, o1);
		Map<String, Object> fields2 = getMineAndParentFields(
				ForCompareTo.class, o2);
		if (!isEmpty(fields1) && fields2.size() >= fields1.size()) {
			for (String key : fields1.keySet()) {
				Object v1 = fields1.get(key);
				Object v2 = fields2.get(key);
				if (v1 != null) {
					if (v2 != null) {
						if (v1 instanceof Comparable
								&& v2 instanceof Comparable) {
							res = ((Comparable) v1).compareTo((Comparable) v2);
						} else {
							throw new IllegalArgumentException(
									safeFormat(
											"both arguments must be Comparables: (%s vs. %s)",
											v1.getClass(), v2.getClass()));
						}
					} else {
						res = 1;
					}
				} else {
					if (v2 != null) {
						res = -1;
					} else {
						// res = 0;
					}
				}
				if (res != 0) {
					break;
				}
			}
		} else {
			res = o1.compareTo(o2);
		}
		return res;
	}

	@Override
	/** Reflectively implement hashCode() using ForEquals annotations. */
	public int hashCode() {
		int res = 31;
		Map<String, Object> fields = getMineAndParentFields(ForEquals.class,
				this);
		if (!isEmpty(fields)) {
			for (String key : fields.keySet()) {
				Object v = fields.get(key);
				if (v != null) {
					res ^= v.hashCode();
				}
			}
		} else {
			res = super.hashCode();
		}
		return res;
	}

	protected Map<String, Object> getMineAndParentFields(
			Class<? extends Annotation> targetAnnoation, Object target) {
		Map<String, Object> fields = new TreeMap<String, Object>();
		Class<? extends Object> clazz = target.getClass();
		// getFieldValues(clazz, clazz.getFields(), fields);
		while (clazz != null) {
			getFieldValues(targetAnnoation, clazz, clazz.getDeclaredFields(),
					fields, target);
			clazz = clazz.getSuperclass();
		}
		return fields;
	}

	protected void getFieldValues(Class<? extends Annotation> targetAnnoation,
			Class<? extends Object> clazz, Field[] fields,
			Map<String, Object> xfields, Object target) {
		for (Field field : fields) {
			// printf("getFieldValues: class=%s, field=%s%n", clazz, field);
			if (!Modifier.isStatic(field.getModifiers())) {
				getFieldValue(targetAnnoation, xfields, field, target);
			}
		}
	}

	protected void getFieldValue(Class<? extends Annotation> targetAnnoation,
			Map<String, Object> xfields, Field field, Object target) {
		Annotation anno = field.getAnnotation(targetAnnoation);
		if (anno != null) {
			try {
				boolean acc = field.isAccessible();
				try {
					field.setAccessible(true);
					Object o = field.get(target);
					String name = null;
					Method value = anno.annotationType().getMethod("value");
					if (value != null) {
						name = (String) value.invoke(anno);
					}
					if (isEmpty(name)) {
						name = field.getName();
					}
					xfields.put(name, o);
				} finally {
					field.setAccessible(acc);
				}
			} catch (Exception e) {
				e.printStackTrace(); // temp
			}
		}
	}

	/** Safely implement sleep(). */
	public static void sleep(long millis) {
		if (millis >= 0) {
			try {
				Thread.sleep(millis);
			} catch (InterruptedException e) {
				// e.printStackTrace(System.out);
			}
		}
	}

	/** Remove matching " or ' from a string. */
	public String trimQuotes(String s) {
		if (!isEmpty(s)) {
			if (s.startsWith("\"") && s.endsWith("\"") || s.startsWith("'")
					&& s.endsWith("'")) {
				s = s.substring(1, s.length() - 1);
			}
		}
		return s;
	}

	public static String safeFormat(String format, Object... args) {
		return String.format(format, args);
	}
}
