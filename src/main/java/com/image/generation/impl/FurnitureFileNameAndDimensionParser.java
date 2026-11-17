package com.image.generation.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The upload file name carries the name of the furniture and, if they are known, its
 * measurements: {@code Hoeffner_1_180_90_40.png} means name {@code Hoeffner_1},
 * 180 cm wide, 90 cm high and 40 cm deep — width_height_depth, in centimetres.
 * <p>
 * The measurements are optional. A file name that does not end in such a group —
 * {@code Hoeffner_1.png} — is taken as a name on its own, and the three values come
 * back null, which is what the columns now allow.
 * <p>
 * A group that <em>is</em> there but states no usable size ({@code ..._180_90_0.png})
 * is still refused rather than quietly dropped: the file name shows the uploader meant
 * to give measurements, so the right answer is to say what is wrong with them, not to
 * store a picture missing the numbers they thought they had provided.
 */
public record FurnitureFileNameAndDimensionParser(String name, BigDecimal widthCm, BigDecimal heightCm, BigDecimal depthCm) {

    private static final int MAX_NAME_LENGTH = 128;
    private static final BigDecimal MAX_CM = new BigDecimal("99999.99");

    /**
     * A greedy name group so the *last* width_height_depth group in the file name wins.
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^(?:(?<name>.+)[_-])?(?<width>\\d+(?:[.,]\\d+)?)\\s*_\\s*(?<height>\\d+(?:[.,]\\d+)?)\\s*_\\s*(?<depth>\\d+(?:[.,]\\d+)?)\\s*(?:cm)?$");

    public static FurnitureFileNameAndDimensionParser parse(String fileName) {
        String baseName = stripExtension(fileName);

        Matcher matcher = PATTERN.matcher(baseName);
        if (!matcher.matches()) {
            return new FurnitureFileNameAndDimensionParser(name(baseName, fileName), null, null, null);
        }

        return new FurnitureFileNameAndDimensionParser(
                name(matcher.group("name") == null ? baseName : matcher.group("name"), fileName),
                centimetres(matcher.group("width"), "width", fileName),
                centimetres(matcher.group("height"), "height", fileName),
                centimetres(matcher.group("depth"), "depth", fileName));
    }

    private static String name(String candidate, String fileName) {
        String name = candidate.trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The name in '%s' must be 1 to %d characters".formatted(fileName, MAX_NAME_LENGTH));
        }
        return name;
    }

    private static BigDecimal centimetres(String value, String what, String fileName) {
        BigDecimal cm = new BigDecimal(value.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
        if (cm.signum() <= 0 || cm.compareTo(MAX_CM) > 0) {
            throw new IllegalArgumentException(
                    "The %s in '%s' must be between 0.01 and %s cm".formatted(what, fileName, MAX_CM));
        }
        return cm;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
