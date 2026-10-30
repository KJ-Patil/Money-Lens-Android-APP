package com.example.moneylens;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CountryTest {

    private static Country india() {
        return Country.getByIso("IN");
    }

    @Test
    public void digitsOnlyStripsFormatting() {
        assertEquals("919876543210", Country.digitsOnly("+91 98765-43210"));
        assertEquals("9876543210", Country.digitsOnly("98765-43210"));
        assertEquals("", Country.digitsOnly(null));
        assertEquals("", Country.digitsOnly("abc"));
    }

    @Test
    public void validatesIndianTenDigitNumber() {
        assertNull(india().validatePhone("9876543210"));
        assertNull(india().validatePhone("98765 43210"));
    }

    @Test
    public void rejectsWrongLengthForCountry() {
        assertNotNull(india().validatePhone("98765"));       // too short
        assertNotNull(india().validatePhone("987654321012")); // too long
        assertNotNull(india().validatePhone(""));             // empty
    }

    @Test
    public void buildsE164() {
        assertEquals("+919876543210", india().toFullNumber("98765 43210"));
        assertEquals("+14155552671", Country.getByIso("US").toFullNumber("(415) 555-2671"));
    }

    @Test
    public void lookupHelpers() {
        assertEquals("IN", india().isoCode);
        assertNull(Country.getByIso("ZZ"));
        assertEquals("IN", Country.getByCurrency("INR").isoCode);
        assertEquals("US", Country.getByCurrency("USD").isoCode);
        assertNull(Country.getByCurrency("XXX"));
    }

    @Test
    public void rejectsElevenDigitsForIndia() {
        // Specifically verifies that an 11-digit number without trunk stripping is rejected for India
        String error = india().validatePhone("98765432101");
        assertNotNull(error);
        assertEquals("Enter a valid 10-digit number for India.", error);
    }

    @Test
    public void parsePhoneStripsTrunkZeroAndDetectsCountries() {
        // 11-digit Indian number starting with 0 trunk prefix (e.g. 09876543210 -> 9876543210)
        Country.ParsedPhone parsedTrunk = Country.parsePhone("09876543210", india());
        assertEquals("IN", parsedTrunk.country.isoCode);
        assertEquals("9876543210", parsedTrunk.nationalNumber);
        assertNull(parsedTrunk.country.validatePhone(parsedTrunk.nationalNumber));

        // 12-digit Indian number starting with 91
        Country.ParsedPhone parsed91 = Country.parsePhone("919876543210", india());
        assertEquals("IN", parsed91.country.isoCode);
        assertEquals("9876543210", parsed91.nationalNumber);

        // International format with '+'
        Country.ParsedPhone parsedPlus91 = Country.parsePhone("+91 98765 43210", null);
        assertEquals("IN", parsedPlus91.country.isoCode);
        assertEquals("9876543210", parsedPlus91.nationalNumber);

        // US number with '+'
        Country.ParsedPhone parsedUS = Country.parsePhone("+1 (415) 555-2671", null);
        assertEquals("US", parsedUS.country.isoCode);
        assertEquals("4155552671", parsedUS.nationalNumber);

        // UK number with '+'
        Country.ParsedPhone parsedUK = Country.parsePhone("+44 7911 123456", null);
        assertEquals("GB", parsedUK.country.isoCode);
        assertEquals("7911123456", parsedUK.nationalNumber);
    }

    @Test
    public void dialCodeLookup() {
        assertEquals("IN", Country.getByDialCode("+91").isoCode);
        assertEquals("IN", Country.getByDialCode("91").isoCode);
        assertEquals("US", Country.getByDialCode("+1").isoCode);
        assertEquals("GB", Country.getByDialCode("+44").isoCode);
        assertNull(Country.getByDialCode("+9999"));
    }

    @Test
    public void loosePhoneAcceptsDomesticAndForeign() {
        assertNull(Country.validateLoosePhone("9876543210"));      // Indian 10-digit
        assertNull(Country.validateLoosePhone("+1 415 555 2671")); // US with code
        assertNotNull(Country.validateLoosePhone("1234"));         // too short
        assertNotNull(Country.validateLoosePhone(""));             // empty
    }
}
