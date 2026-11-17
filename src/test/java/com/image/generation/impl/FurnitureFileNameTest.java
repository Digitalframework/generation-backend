package com.image.generation.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FurnitureFileNameTest {

    @Test
    void readsNameAndMeasurements() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_1_180_90_40.png");

        assertThat(parsed.name()).isEqualTo("Hoeffner_1");
        assertThat(parsed.widthCm()).isEqualByComparingTo("180");
        assertThat(parsed.heightCm()).isEqualByComparingTo("90");
        assertThat(parsed.depthCm()).isEqualByComparingTo("40");
    }

    @Test
    void keepsUnderscoresInTheNameAndUsesTheLastGroupAsMeasurements() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_Sofa_2x3_Sitzer_200_85_95.png");

        assertThat(parsed.name()).isEqualTo("Hoeffner_Sofa_2x3_Sitzer");
        assertThat(parsed.widthCm()).isEqualByComparingTo("200");
    }

    @Test
    void acceptsDecimalsWithCommaOrDotAndScalesToTwoDigits() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_1_180,5_90.25_40.png");

        assertThat(parsed.widthCm()).isEqualTo(new BigDecimal("180.50"));
        assertThat(parsed.heightCm()).isEqualTo(new BigDecimal("90.25"));
        assertThat(parsed.depthCm()).isEqualTo(new BigDecimal("40.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Hoeffner_1_180_90_40.PNG",   // upper case extension
            "Hoeffner_1_180 _ 90 _ 40.png", // spaces around the separators
            "Hoeffner_1_180_90_40cm.png", // trailing unit
            "Hoeffner_1-180_90_40.png"    // dash instead of underscore
    })
    void toleratesTheUsualSpellingVariants(String fileName) {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(fileName);

        assertThat(parsed.name()).isEqualTo("Hoeffner_1");
        assertThat(parsed.widthCm()).isEqualByComparingTo("180");
        assertThat(parsed.heightCm()).isEqualByComparingTo("90");
        assertThat(parsed.depthCm()).isEqualByComparingTo("40");
    }

    @Test
    void takesTheWholeNameAndNoMeasurementsWhenTheFileNameStatesNone() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_1.jpg");

        assertThat(parsed.name()).isEqualTo("Hoeffner_1");
        assertThat(parsed.widthCm()).isNull();
        assertThat(parsed.heightCm()).isNull();
        assertThat(parsed.depthCm()).isNull();
    }

    /**
     * Anything short of all three is no measurement at all: the remains are part of the
     * name rather than two thirds of a size.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Hoeffner_1_180x90.png",    // only two of them
            "Hoeffner_1_axbxc.png",     // not numbers
            "Hoeffner_1_180x90x40.png" // old x format is not an underscore dimension group
    })
    void keepsAnUnreadableGroupAsPartOfTheName(String fileName) {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(fileName);

        assertThat(parsed.name()).isEqualTo(fileName.substring(0, fileName.lastIndexOf('.')));
        assertThat(parsed.widthCm()).isNull();
        assertThat(parsed.heightCm()).isNull();
        assertThat(parsed.depthCm()).isNull();
    }

    /**
     * A group that is plainly meant as measurements but states an impossible one is still
     * an error — silently dropping it would store the picture without the numbers the
     * uploader believed they had given.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Hoeffner_1_180_90_0.png",      // zero is not a size
            "Hoeffner_1_180_90_999999.png"  // beyond what the column holds
    })
    void rejectsMeasurementsItCanReadButCannotStore(String fileName) {
        assertThatThrownBy(() -> FurnitureFileNameAndDimensionParser.parse(fileName))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"sofa_180_20_3.png", "180_20_3.jpg"})
    void readsRequestedUnderscoreFormatWithOrWithoutAName(String fileName) {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(fileName);
        assertThat(parsed.widthCm()).isEqualByComparingTo("180");
        assertThat(parsed.heightCm()).isEqualByComparingTo("20");
        assertThat(parsed.depthCm()).isEqualByComparingTo("3");
    }

    @Test
    void rejectsAnEmptyName() {
        assertThatThrownBy(() -> FurnitureFileNameAndDimensionParser.parse("   .png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1 to 128 characters");
    }
}
