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
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_1_180x90x40.png");

        assertThat(parsed.name()).isEqualTo("Hoeffner_1");
        assertThat(parsed.widthCm()).isEqualByComparingTo("180");
        assertThat(parsed.heightCm()).isEqualByComparingTo("90");
        assertThat(parsed.depthCm()).isEqualByComparingTo("40");
    }

    @Test
    void keepsUnderscoresInTheNameAndUsesTheLastGroupAsMeasurements() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_Sofa_2x3_Sitzer_200x85x95.png");

        assertThat(parsed.name()).isEqualTo("Hoeffner_Sofa_2x3_Sitzer");
        assertThat(parsed.widthCm()).isEqualByComparingTo("200");
    }

    @Test
    void acceptsDecimalsWithCommaOrDotAndScalesToTwoDigits() {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse("Hoeffner_1_180,5x90.25x40.png");

        assertThat(parsed.widthCm()).isEqualTo(new BigDecimal("180.50"));
        assertThat(parsed.heightCm()).isEqualTo(new BigDecimal("90.25"));
        assertThat(parsed.depthCm()).isEqualTo(new BigDecimal("40.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Hoeffner_1_180X90X40.PNG",   // upper case x and extension
            "Hoeffner_1_180 x 90 x 40.png", // spaces around the separators
            "Hoeffner_1_180x90x40cm.png", // trailing unit
            "Hoeffner_1-180x90x40.png"    // dash instead of underscore
    })
    void toleratesTheUsualSpellingVariants(String fileName) {
        FurnitureFileNameAndDimensionParser parsed = FurnitureFileNameAndDimensionParser.parse(fileName);

        assertThat(parsed.name()).isEqualTo("Hoeffner_1");
        assertThat(parsed.widthCm()).isEqualByComparingTo("180");
        assertThat(parsed.heightCm()).isEqualByComparingTo("90");
        assertThat(parsed.depthCm()).isEqualByComparingTo("40");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Hoeffner_1.png",           // no measurements at all
            "Hoeffner_1_180x90.png",    // only two of them
            "180x90x40.png",            // no name
            "Hoeffner_1_180x90x0.png",  // zero is not a size
            "Hoeffner_1_axbxc.png"
    })
    void rejectsFileNamesItCannotRead(String fileName) {
        assertThatThrownBy(() -> FurnitureFileNameAndDimensionParser.parse(fileName))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
