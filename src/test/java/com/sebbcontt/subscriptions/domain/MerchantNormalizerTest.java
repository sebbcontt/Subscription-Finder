package com.sebbcontt.subscriptions.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MerchantNormalizerTest {

    @Test
    void removesPhoneNumbersDotComAndState() {
        assertEquals("NETFLIX", MerchantNormalizer.normalize("NETFLIX.COM 866-579-7172 CA"));
    }

    @Test
    void removesPaymentProcessorPrefixAndStoreNumber() {
        assertEquals("BLUE BOTTLE COFFEE", MerchantNormalizer.normalize("SQ *BLUE BOTTLE COFFEE 4421"));
    }

    @Test
    void sameMerchantWithDifferentReferenceNumbersMatches() {
        assertEquals(
                MerchantNormalizer.normalize("SPOTIFY USA 8841 NY"),
                MerchantNormalizer.normalize("SPOTIFY USA 3310 NY"));
    }

    @Test
    void keepsAStateLookingWordWhenItIsTheOnlyWord() {
        assertEquals("OH", MerchantNormalizer.normalize("OH"));
    }

    @Test
    void handlesNullAndBlank() {
        assertEquals("", MerchantNormalizer.normalize(null));
        assertEquals("", MerchantNormalizer.normalize("   "));
    }

    @Test
    void makesAFriendlyDisplayName() {
        assertEquals("Blue Bottle Coffee", MerchantNormalizer.toDisplayName("BLUE BOTTLE COFFEE"));
    }
}
