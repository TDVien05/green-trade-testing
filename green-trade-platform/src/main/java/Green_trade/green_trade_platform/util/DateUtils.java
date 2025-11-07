package Green_trade.green_trade_platform.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class DateUtils {
    public LocalDate parseAndValidateDob(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Ngày sinh không được để trống.");
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        LocalDate dob;
        try {
            dob = LocalDate.parse(dobStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Định dạng ngày sinh không hợp lệ. Vui lòng nhập theo dạng dd-MM-yyyy.");
        }

        LocalDate today = LocalDate.now();

        if (dob.isAfter(today)) {
            throw new IllegalArgumentException("Ngày sinh không thể là ngày trong tương lai.");
        }

        int age = Period.between(dob, today).getYears();

        if (age > 100) {
            throw new IllegalArgumentException("Tuổi không hợp lệ (quá 100 tuổi).");
        }

        return dob;
    }
}
