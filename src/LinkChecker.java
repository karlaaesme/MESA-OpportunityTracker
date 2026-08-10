import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs with no user interaction — meant to be triggered on a schedule (GitHub Actions).
 * It never changes Status itself; it only sets Flagged/FlagNote so a staff member
 * can quickly review and decide.
 *
 * Usage: java LinkChecker [path-to-data.csv]
 */
public class LinkChecker {

    // phrases that commonly show up on a closed/expired application page
    private static final String[] CLOSED_SIGNALS = {
            "no longer accepting", "applications are closed", "applications closed",
            "position has been filled", "no longer available", "opportunity has expired",
            "deadline has passed", "registration is closed", "registration closed"
    };

    // phrases that suggest a closed opportunity has announced when it comes back
    private static final Pattern REOPEN_SIGNAL = Pattern.compile(
            "reopens|reopen|opens again|opening in|check back|next cycle|will reopen|" +
            "opens on|opens for|application cycle opens|opens in|opening soon|coming soon|" +
            "application period|application window|application dates|applications open|" +
            "accepting applications|apply beginning|applications will open|applications begin",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FULL_DATE = Pattern.compile(
            "(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{1,2}),?\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MONTH_YEAR = Pattern.compile(
            "(January|February|March|April|May|June|July|August|September|October|November|December)\\s+(\\d{4})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern NUMERIC_DATE = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");

    private static final Map<String, Integer> MONTHS = new HashMap<>();
    static {
        String[] names = {"january","february","march","april","may","june","july",
                "august","september","october","november","december"};
        for (int i = 0; i < names.length; i++) MONTHS.put(names[i], i + 1);
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "docs/data.csv";

        OpportunityManager manager = new OpportunityManager();
        manager.loadFile(path);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        int flaggedCount = 0;
        for (Opportunity opp : manager.getAll()) {
            if ("Close".equalsIgnoreCase(opp.getStatus())) {
                // closed opportunities: look for a reopening announcement instead
                String reopenDate = checkReopen(client, opp.getLink());
                if (reopenDate != null) {
                    opp.setSuggestedReopenDate(reopenDate);
                    opp.setFlag(true, "Possible reopening detected on the site (around " + reopenDate + "). Please verify and update Status/DueDate.");
                    flaggedCount++;
                    System.out.println("Reopen suggestion: " + opp.getName() + " — " + reopenDate);
                }
                continue;
            }

            List<String> reasons = new ArrayList<>();

            if (opp.getDeadline().isBefore(LocalDate.now())) {
                reasons.add("Due date has passed but status is still \"" + opp.getStatus() + "\".");
            } else {
                reasons.addAll(checkLink(client, opp.getLink()));
            }

            if (!reasons.isEmpty()) {
                opp.setFlag(true, String.join(" ", reasons));
                flaggedCount++;
                System.out.println("Flagged: " + opp.getName() + " — " + opp.getFlagNote());
            }
        }

        manager.saveFile(path);
        System.out.println("Check complete. " + flaggedCount + " opportunity(ies) flagged for review.");
    }

    /**
     * Fetches a closed opportunity's page and looks for a nearby date after phrases
     * like "reopens" / "check back" / "opens on". Returns an ISO date string
     * (yyyy-MM-dd) if a clear, parseable date is found near such a phrase, else null.
     * This is pattern matching, not language understanding — it only catches
     * clearly-worded cases and is meant purely as a suggestion for staff to confirm.
     */
    private static String checkReopen(HttpClient client, String link) {
        if (link == null || !(link.startsWith("http://") || link.startsWith("https://"))) {
            return null;
        }
        String body;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "OpTrack-LinkChecker/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) return null;
            body = stripHtml(response.body());
        } catch (Exception e) {
            return null; // unreachable link — nothing to suggest
        }

        Matcher sig = REOPEN_SIGNAL.matcher(body);
        while (sig.find()) {
            int start = sig.end();
            int end = Math.min(body.length(), start + 150);
            String window = body.substring(start, end);

            String date = tryFullDate(window);
            if (date == null) date = tryMonthYear(window);
            if (date == null) date = tryNumericDate(window);
            if (date != null) return date;
        }
        return null;
    }

    private static String tryFullDate(String window) {
        Matcher m = FULL_DATE.matcher(window);
        if (!m.find()) return null;
        Integer month = MONTHS.get(m.group(1).toLowerCase(Locale.ROOT));
        if (month == null) return null;
        try {
            return LocalDate.of(Integer.parseInt(m.group(3)), month, Integer.parseInt(m.group(2))).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String tryMonthYear(String window) {
        Matcher m = MONTH_YEAR.matcher(window);
        if (!m.find()) return null;
        Integer month = MONTHS.get(m.group(1).toLowerCase(Locale.ROOT));
        if (month == null) return null;
        try {
            return LocalDate.of(Integer.parseInt(m.group(2)), month, 1).toString();
        } catch (Exception e) {
            return null;
        }
    }

    private static String tryNumericDate(String window) {
        Matcher m = NUMERIC_DATE.matcher(window);
        if (!m.find()) return null;
        try {
            int mo = Integer.parseInt(m.group(1));
            int da = Integer.parseInt(m.group(2));
            int yr = Integer.parseInt(m.group(3));
            return LocalDate.of(yr, mo, da).toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Strips tags/scripts/styles so "Application Period</div><div>Aug 19, 2026" reads as plain adjacent text. */
    private static String stripHtml(String html) {
        if (html == null) return "";
        String text = html.replaceAll("(?is)<script.*?</script>", " ")
                           .replaceAll("(?is)<style.*?</style>", " ")
                           .replaceAll("(?s)<[^>]+>", " ")
                           .replace("&nbsp;", " ")
                           .replace("&amp;", "&");
        return text.replaceAll("\\s+", " ");
    }

    private static List<String> checkLink(HttpClient client, String link) {
        List<String> reasons = new ArrayList<>();

        if (link == null || !(link.startsWith("http://") || link.startsWith("https://"))) {
            reasons.add("Link doesn't look like a valid URL — please verify.");
            return reasons;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(link))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "OpTrack-LinkChecker/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                reasons.add("Link returned HTTP " + response.statusCode() + " — may be broken or removed.");
                return reasons;
            }

            String bodyLower = stripHtml(response.body()).toLowerCase(Locale.ROOT);
            for (String signal : CLOSED_SIGNALS) {
                if (bodyLower.contains(signal)) {
                    reasons.add("Page text suggests this may be closed (found phrase: \"" + signal + "\").");
                    break;
                }
            }
        } catch (Exception e) {
            reasons.add("Could not reach the link — may be broken. (" + e.getClass().getSimpleName() + ")");
        }

        return reasons;
    }
}
