package net.modfest.util;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.apache.commons.text.matcher.StringMatcher;
import org.apache.commons.text.matcher.StringMatcherFactory;

import java.util.Map;

public class StringTemplate {
	private static final StringMatcher MATCHER_PREFIX = StringMatcherFactory.INSTANCE.stringMatcher("{");

	public static String format(String template, String key, String value) {
		return format(template, StringLookupFactory.INSTANCE.functionStringLookup(s -> key.equals(s) ? value : null));
	}

	public static String format(String template, String k1, String v1, String k2, String v2) {
		return format(template, StringLookupFactory.INSTANCE.functionStringLookup(s -> {
			if (k1.equals(s)) return v1;
			if (k2.equals(s)) return v2;
			return null;
		}));
	}

	public static String format(String template, String k1, String v1, String k2, String v2, String k3, String v3) {
		return format(template, StringLookupFactory.INSTANCE.functionStringLookup(s -> {
			if (k1.equals(s)) return v1;
			if (k2.equals(s)) return v2;
			if (k3.equals(s)) return v3;
			return null;
		}));
	}

	public static String format(String template, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4) {
		return format(template, StringLookupFactory.INSTANCE.functionStringLookup(s -> {
			if (k1.equals(s)) return v1;
			if (k2.equals(s)) return v2;
			if (k3.equals(s)) return v3;
			if (k4.equals(s)) return v4;
			return null;
		}));
	}

	public static String format(String template, String k1, String v1, String k2, String v2, String k3, String v3, String k4, String v4, String k5, String v5) {
		return format(template, StringLookupFactory.INSTANCE.functionStringLookup(s -> {
			if (k1.equals(s)) return v1;
			if (k2.equals(s)) return v2;
			if (k3.equals(s)) return v3;
			if (k4.equals(s)) return v4;
			if (k5.equals(s)) return v5;
			return null;
		}));
	}

	public static String format(String template, Map<String, String> variables) {
		return format(template, StringLookupFactory.INSTANCE.mapStringLookup(variables));
	}

	public static String format(String template, StringLookup lookup) {
		return new StringSubstitutor(lookup, MATCHER_PREFIX, StringSubstitutor.DEFAULT_SUFFIX, '\\').replace(template);
	}
}
