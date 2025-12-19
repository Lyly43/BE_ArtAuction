package com.auctionaa.backend.Config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
public class MongoConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new StringToLocalDateConverter());
        converters.add(new DateToLocalDateConverter());
        converters.add(new LocalDateToStringConverter());
        converters.add(new LocalDateToDateConverter());
        return new MongoCustomConversions(converters);
    }

    /**
     * Converter để đọc LocalDate từ MongoDB (có thể là string ISO datetime hoặc date)
     * Xử lý trường hợp MongoDB lưu dạng: "1993-04-18T00:00:00.000Z"
     */
    @ReadingConverter
    public static class StringToLocalDateConverter implements Converter<String, LocalDate> {
        @Override
        public LocalDate convert(String source) {
            if (source == null || source.trim().isEmpty()) {
                return null;
            }
            
            try {
                // Thử parse ISO datetime string (ví dụ: "1993-04-18T00:00:00.000Z")
                if (source.contains("T")) {
                    // Parse ISO datetime và lấy phần date
                    Instant instant = Instant.parse(source);
                    return instant.atZone(ZoneId.systemDefault()).toLocalDate();
                } else {
                    // Parse date string đơn giản (ví dụ: "1993-04-18")
                    return LocalDate.parse(source, DateTimeFormatter.ISO_LOCAL_DATE);
                }
            } catch (DateTimeParseException e) {
                // Nếu không parse được, thử lấy 10 ký tự đầu (phần date)
                try {
                    if (source.length() >= 10) {
                        return LocalDate.parse(source.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Cannot parse date: " + source, ex);
                }
                throw new IllegalArgumentException("Cannot parse date: " + source, e);
            }
        }
    }

    /**
     * Converter để đọc LocalDate từ MongoDB Date object
     * Xử lý trường hợp MongoDB lưu dạng BSON Date
     */
    @ReadingConverter
    public static class DateToLocalDateConverter implements Converter<Date, LocalDate> {
        @Override
        public LocalDate convert(Date source) {
            if (source == null) {
                return null;
            }
            return source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }

    /**
     * Converter để ghi LocalDate vào MongoDB dưới dạng string
     */
    @WritingConverter
    public static class LocalDateToStringConverter implements Converter<LocalDate, String> {
        @Override
        public String convert(LocalDate source) {
            if (source == null) {
                return null;
            }
            return source.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    }

    /**
     * Converter để ghi LocalDate vào MongoDB dưới dạng Date object
     */
    @WritingConverter
    public static class LocalDateToDateConverter implements Converter<LocalDate, Date> {
        @Override
        public Date convert(LocalDate source) {
            if (source == null) {
                return null;
            }
            return Date.from(source.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
    }
}



