/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.util.Locale;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.dspace.utils.DSpace;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

public class AddonI18nUtil {
	private static final Logger log = Logger.getLogger(AddonI18nUtil.class);

	private static MessageSource messageSource;

	private static MessageSource getMessageSource() {
		if (AddonI18nUtil.messageSource == null) {
			DSpace dspace = new DSpace();
			AddonI18nUtil.messageSource = dspace.getServiceManager().getServiceByName("messageSource",
					MessageSource.class);
		}
		return AddonI18nUtil.messageSource;
	}

	public static String getMessage(String key, Object[] args, Context c) {
		return getMessage(key.trim(), args, c.getCurrentLocale());
	}

    /**
     * Get the i18n message string for a given key and context
     *
     * @param key String - name of the key to get the message for
     * @param c   Context having the desired Locale
     * @return message
     * String of the message
     */
    public static String getMessage(String key, Context c) {
        return getMessage(key.trim(), c.getCurrentLocale());
    }

    /**
     * Get the i18n message string for a given key and locale
     *
     * @param key    String - name of the key to get the message for
     * @param locale Locale, to get the message for
     * @return message
     * String of the message
     */
    public static String getMessage(String key, Locale locale) {
        return getMessage(key, locale, false);
    }

    public static String getMessage(String key, Locale locale, boolean throwExcIfNotFound) {
        return getMessage(key, null, locale, throwExcIfNotFound);
    }

	/**
	 * Get the appropriate localized version for the message string for a given key
	 * and parameters
	 *
	 * @param key
	 *            String - name of the key to get the message for
	 * @param args
	 *            Object[] - arguments for substitution
	 * @param locale
	 *            Locale - to get the message for
	 * @return
	 * @throws MissingResourceException
	 */
	public static String getMessage(String key, Object[] args, Locale locale) throws MissingResourceException {
		return getMessage(key, args, locale, false);
	}

	/**
	 * Get the appropriate localized version for the message string for a given key
	 * and parameters
	 *
	 * @param key
	 *            String - name of the key to get the message for
	 * @param args
	 *            Object[] - arguments for substitution
	 * @param locale
	 *            Locale - to get the message for
	 * @param throwExcIfNotFound
	 *            boolean - false if you want fail silent
	 * @return
	 * @throws MissingResourceException
	 */
	public static String getMessage(String key, Object[] args, Locale locale, boolean throwExcIfNotFound)
			throws MissingResourceException {
		String message = "";
		if (locale == null) {
			locale = I18nUtil.DEFAULTLOCALE;
		}

		try {
			message = getMessageSource().getMessage(key.trim(), args, locale);
		} catch (MissingResourceException | NoSuchMessageException e) {
			if (throwExcIfNotFound) {
				throw new MissingResourceException(e.getMessage(), messageSource.getClass().toString(), key);
			}
			log.error("'" + key + "' translation undefined in locale '" + locale.toString() + "'");
			return key;
		}
		return message;
	}

}
