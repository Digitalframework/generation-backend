package com.image.generation.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The upload file name carries the name and the measurements of the furniture:
 * {@code Hoeffner_1_180x90x40.png} means name {@code Hoeffner_1}, 180 cm wide,
 * 90 cm high and 40 cm deep — width x height x depth, in centimetres.
 */
public record FurnitureFileNameAndDimensionParser(String name, BigDecimal widthCm, BigDecimal heightCm, BigDecimal depthCm) {

    static final String EXPECTED_FORMAT = "<name>_<width>x<height>x<depth>.png, e.g. Hoeffner_1_180x90x40.png";

    private static final int MAX_NAME_LENGTH = 128;
    private static final BigDecimal MAX_CM = new BigDecimal("99999.99");

    /**
     * A greedy name group so the *last* WxHxD group in the file name wins.
     */
    private static final Pattern PATTERN = Pattern.compile(
            "^(?<name>.+)[_-](?<width>\\d+(?:[.,]\\d+)?)\\s*[xX×]\\s*(?<height>\\d+(?:[.,]\\d+)?)\\s*[xX×]\\s*(?<depth>\\d+(?:[.,]\\d+)?)\\s*(?:cm)?$");

    public static FurnitureFileNameAndDimensionParser parse(String fileName) {
        String baseName = stripExtension(fileName);

        Matcher matcher = PATTERN.matcher(baseName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Cannot read name and measurements from '%s'; expected %s".formatted(fileName, EXPECTED_FORMAT));
        }

        String name = matcher.group("name").trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "The name in '%s' must be 1 to %d characters".formatted(fileName, MAX_NAME_LENGTH));
        }

        return new FurnitureFileNameAndDimensionParser(
                name,
                centimetres(matcher.group("width"), "width", fileName),
                centimetres(matcher.group("height"), "height", fileName),
                centimetres(matcher.group("depth"), "depth", fileName));
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
