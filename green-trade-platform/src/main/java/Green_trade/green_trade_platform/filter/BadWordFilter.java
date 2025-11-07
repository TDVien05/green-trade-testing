package Green_trade.green_trade_platform.filter;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class BadWordFilter {
    // 🚫 List of Vietnamese bad words (expand as needed)
    private static final List<String> BAD_WORDS = Arrays.asList(
            "địt", "dit", "cặc", "cac", "lồn", "lon", "buồi", "buoi",
            "đụ", "du", "đm", "dm", "đéo", "deo", "đĩ", "di",
            "mẹ mày", "me may", "khốn", "súc vật", "thằng chó", "thang cho"
    );

    // ✅ Allow common electrical terms to prevent false positives
    private static final Set<String> WHITELIST = new HashSet<>(Arrays.asList(
            "điện", "dây điện", "ổ cắm", "điện áp", "sạc", "pin", "đèn", "công tắc",
            "bóng đèn", "quạt", "máy lạnh", "máy giặt", "lò vi sóng", "ổ cắm điện"
    ));

    private String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}", "");
        normalized = normalized.replaceAll("đ", "d").replaceAll("Đ", "D");
        return normalized.toLowerCase();
    }

    public boolean containsBadWord(String text) {
        if (text == null || text.isEmpty()) return false;

        String normalized = normalize(text);

        // Skip if the feedback mainly contains technical terms
        for (String safe : WHITELIST) {
            if (normalized.contains(normalize(safe))) {
                // Found safe electrical term, keep checking other parts
            }
        }

        // Only match full bad words or exact phrases
        for (String bad : BAD_WORDS) {
            String regex = "\\b" + Pattern.quote(bad) + "\\b";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(normalized).find()) {
                return true;
            }
        }

        return false;
    }

    public String censorBadWords(String text) {
        if (text == null || text.isEmpty()) return text;
        String censored = text;
        for (String bad : BAD_WORDS) {
            String regex = "(?i)\\b" + Pattern.quote(bad) + "\\b";
            censored = censored.replaceAll(regex, "***");
        }
        return censored;
    }
}
