package com.sebbcontt.subscriptions.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Cleans messy bank descriptions so the same merchant always gets the same name. */
public final class MerchantNormalizer {

    private static final Pattern PREFIXES = Pattern.compile(
            "^(SQ \\*|TST\\*|TST \\*|SP \\*|PAYPAL \\*|PP\\*|POS DEBIT|DEBIT CARD PURCHASE|RECURRING PAYMENT|PURCHASE AUTHORIZED ON \\d{1,2}/\\d{1,2})\\s*");
    private static final Pattern NOT_LETTERS_OR_DIGITS = Pattern.compile("[^A-Z0-9 ]");
    private static final Set<String> STATES = Set.of(
            "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "FL", "GA", "HI", "ID", "IL", "IN", "IA", "KS", "KY",
            "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO", "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND",
            "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY", "DC");

    private MerchantNormalizer() {
    }

    /** "NETFLIX.COM 866-579-7172 CA" becomes "NETFLIX". Returns "" if nothing is left. */
    public static String normalize(String description) {
        if (description == null) {
            return "";
        }
        String text = description.toUpperCase(Locale.ROOT).trim();
        text = PREFIXES.matcher(text).replaceFirst("");
        text = text.replace(".COM", "");
        text = NOT_LETTERS_OR_DIGITS.matcher(text).replaceAll(" ");

        List<String> words = new ArrayList<>();
        for (String word : text.split("\\s+")) {
            if (!word.isEmpty() && word.chars().noneMatch(Character::isDigit)) {
                words.add(word);
            }
        }
        if (words.size() > 1 && STATES.contains(words.get(words.size() - 1))) {
            words.remove(words.size() - 1);
        }
        return String.join(" ", words);
    }

    /** "BLUE BOTTLE COFFEE" becomes "Blue Bottle Coffee". */
    public static String toDisplayName(String normalized) {
        StringBuilder out = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(word.charAt(0)).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }
}
