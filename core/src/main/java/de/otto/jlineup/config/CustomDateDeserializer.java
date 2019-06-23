package de.otto.jlineup.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CustomDateDeserializer extends JsonDeserializer<Date> {

    private static final String COOKIE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String COOKIE_TIME_FORMAT_3 = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final String COOKIE_TIME_FORMAT_2 = "yyyy-MM-dd";

    private static final String[] DATE_FORMATS = new String[] {
            COOKIE_TIME_FORMAT,
            COOKIE_TIME_FORMAT_2,
            COOKIE_TIME_FORMAT_3
    };

    @Override
    public Date deserialize(JsonParser paramJsonParser, DeserializationContext paramDeserializationContext)
            throws IOException, JsonProcessingException {
        if (paramJsonParser == null || "".equals(paramJsonParser.getText()))
            return null;
        String date = paramJsonParser.getText();

        for (String format : DATE_FORMATS) {
            try {
                return new SimpleDateFormat(format, Locale.US).parse(date);
            } catch (ParseException e) {
                //This page was left blank intentionally
            }
        }
        System.err.println("Could not parse " + date);
        return null;
    }
}
