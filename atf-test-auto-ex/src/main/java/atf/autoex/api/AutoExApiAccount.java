package atf.autoex.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static atf.autoex.api.AutoExApiFormField.ADDRESS1;
import static atf.autoex.api.AutoExApiFormField.ADDRESS2;
import static atf.autoex.api.AutoExApiFormField.BIRTH_DATE;
import static atf.autoex.api.AutoExApiFormField.BIRTH_MONTH;
import static atf.autoex.api.AutoExApiFormField.BIRTH_YEAR;
import static atf.autoex.api.AutoExApiFormField.CITY;
import static atf.autoex.api.AutoExApiFormField.COMPANY;
import static atf.autoex.api.AutoExApiFormField.COUNTRY;
import static atf.autoex.api.AutoExApiFormField.EMAIL;
import static atf.autoex.api.AutoExApiFormField.FIRST_NAME;
import static atf.autoex.api.AutoExApiFormField.LAST_NAME;
import static atf.autoex.api.AutoExApiFormField.MOBILE_NUMBER;
import static atf.autoex.api.AutoExApiFormField.NAME;
import static atf.autoex.api.AutoExApiFormField.PASSWORD;
import static atf.autoex.api.AutoExApiFormField.STATE;
import static atf.autoex.api.AutoExApiFormField.TITLE;
import static atf.autoex.api.AutoExApiFormField.ZIPCODE;

/** Typed form fields for the documented create/update account API contract. */
public record AutoExApiAccount(String name, String email, String password, String title,
                               String birthDate, String birthMonth, String birthYear,
                               String firstName, String lastName, String company,
                               String address1, String address2, String country,
                               String zipcode, String state, String city, String mobileNumber) {
    public AutoExApiAccount {
        name = required(name, NAME);
        email = required(email, EMAIL);
        password = required(password, PASSWORD);
        title = required(title, TITLE);
        birthDate = required(birthDate, BIRTH_DATE);
        birthMonth = required(birthMonth, BIRTH_MONTH);
        birthYear = required(birthYear, BIRTH_YEAR);
        firstName = required(firstName, FIRST_NAME);
        lastName = required(lastName, LAST_NAME);
        company = required(company, COMPANY);
        address1 = required(address1, ADDRESS1);
        address2 = Objects.requireNonNullElse(address2, "");
        country = required(country, COUNTRY);
        zipcode = required(zipcode, ZIPCODE);
        state = required(state, STATE);
        city = required(city, CITY);
        mobileNumber = required(mobileNumber, MOBILE_NUMBER);
    }

    public Map<String, String> formFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(NAME.value(), name);
        fields.put(EMAIL.value(), email);
        fields.put(PASSWORD.value(), password);
        fields.put(TITLE.value(), title);
        fields.put(BIRTH_DATE.value(), birthDate);
        fields.put(BIRTH_MONTH.value(), birthMonth);
        fields.put(BIRTH_YEAR.value(), birthYear);
        fields.put(FIRST_NAME.value(), firstName);
        fields.put(LAST_NAME.value(), lastName);
        fields.put(COMPANY.value(), company);
        fields.put(ADDRESS1.value(), address1);
        fields.put(ADDRESS2.value(), address2);
        fields.put(COUNTRY.value(), country);
        fields.put(ZIPCODE.value(), zipcode);
        fields.put(STATE.value(), state);
        fields.put(CITY.value(), city);
        fields.put(MOBILE_NUMBER.value(), mobileNumber);
        return Map.copyOf(fields);
    }

    @Override
    public String toString() {
        return "AutoExApiAccount[email=" + email + ", name=" + name + ", credentials=<redacted>]";
    }

    private static String required(String value, AutoExApiFormField field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field.value() + " is required");
        return value.trim();
    }
}
