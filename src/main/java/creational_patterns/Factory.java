package creational_patterns;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Objects;
import java.util.regex.Pattern;

@FunctionalInterface
interface NotificationService {
    void send(String recipient, String message);
}

enum NotificationChannels{
    EMAIL, SMS, PUSH
}

class EmailNotificationService implements NotificationService {
    @Override
    public void send(String recipient, String message) {
        String outgoingMessage = """
               Subject: Sending email to %s
               Body: %s
               """.formatted(recipient, message);

        System.out.println(outgoingMessage);
    }
}

class SMSNotificationService implements NotificationService {
    @Override
    public void send(String recipient, String message) {
        // keep only the first 160 characters
        String truncatedMsg = message.substring(0, Math.min(message.length(), 160));
        System.out.println("Sending message via SMS to " + recipient + ":");
        System.out.println(truncatedMsg);
    }
}

class PushNotificationService implements NotificationService {

    private static final Pattern PATTERN = Pattern.compile("^[A-Za-z0-9_-]{32,64}$");

    private static void validateToken(String token){
        if(token == null || token.isEmpty()){
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        if(!PATTERN.matcher(token).matches()){
            throw new IllegalArgumentException(
                    "Invalid device token format: expected 32-64 chars of [A-Za-z0-9_-]");
        }
    }

    @Override
    public void send(String token, String message) {
        validateToken(token);

        System.out.println("Sending message via Push NotificationService to " + token + ":");
        System.out.println(message);
    }
}

class NotificationFactory{
    public static NotificationService getInstance(NotificationChannels type){
        NotificationService instance = null;

        switch (type){
            case SMS -> instance = new SMSNotificationService();
            case EMAIL -> instance = new EmailNotificationService();
            case PUSH -> instance = new PushNotificationService();
        }

        return Objects.requireNonNull(instance);
    }
}


// Test code for the NotificationFactory

class NotificationFactoryTest {

    private static int passed = 0;
    private static int failed = 0;

    // getters
    public static int getPassed() {
        return passed;
    }

    public static int getFailed() {
        return failed;
    }

    // ---------- tiny test helpers ----------

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS: " + name);
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static boolean throwsType(Class<? extends Throwable> type, Runnable r) {
        try {
            r.run();
            return false;
        } catch (Throwable t) {
            return type.isInstance(t);
        }
    }

    /**
     * Runs the action, captures everything it prints, and returns it.
     */
    private static String captureOutput(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setOut(new PrintStream(buffer));
        try {
            action.run();
        } catch (RuntimeException e) {
            // swallow here; tests that expect exceptions use throwsType separately
        } finally {
            System.setOut(original);
        }
        return buffer.toString();
    }

    // ---------- tests ----------

    static void testFactoryReturnsCorrectTypes() {
        check("EMAIL -> EmailNotificationService",
                NotificationFactory.getInstance(NotificationChannels.EMAIL)
                        instanceof EmailNotificationService);
        check("SMS -> SMSNotificationService",
                NotificationFactory.getInstance(NotificationChannels.SMS)
                        instanceof SMSNotificationService);
        check("PUSH -> PushNotificationService",
                NotificationFactory.getInstance(NotificationChannels.PUSH)
                        instanceof PushNotificationService);

        boolean allNonNull = true;
        for (NotificationChannels ch : NotificationChannels.values()) {
            if (NotificationFactory.getInstance(ch) == null) allNonNull = false;
        }
        check("every channel returns non-null", allNonNull);
    }

    static void testNullChannel() {
        check("getInstance(null) throws NullPointerException",
                throwsType(NullPointerException.class,
                        () -> NotificationFactory.getInstance(null)));
    }

    static void testEmail() {
        String out = captureOutput(() ->
                NotificationFactory.getInstance(NotificationChannels.EMAIL)
                        .send("alice@example.com", "Your order has shipped!"));

        check("email output has Subject", out.contains("Subject:"));
        check("email output has recipient", out.contains("alice@example.com"));
        check("email output has body", out.contains("Your order has shipped!"));
    }

    static void testSmsTruncation() {
        NotificationService sms = NotificationFactory.getInstance(NotificationChannels.SMS);

        String longOut = captureOutput(() -> sms.send("+91-9876543210", "x".repeat(300)));
        check("SMS 300 chars -> exactly 160 x's",
                longOut.contains("x".repeat(160)) && !longOut.contains("x".repeat(161)));

        String exactOut = captureOutput(() -> sms.send("+91-1", "y".repeat(160)));
        check("SMS exactly 160 is not cut", exactOut.contains("y".repeat(160)));

        String shortOut = captureOutput(() -> sms.send("+91-1", "Your OTP is 482913"));
        check("SMS short message unchanged", shortOut.contains("Your OTP is 482913"));
    }

    static void testPushValidation() {
        NotificationService push = NotificationFactory.getInstance(NotificationChannels.PUSH);

        // valid + boundaries
        check("push: valid 40-char token passes",
                captureOutput(() -> push.send("a".repeat(40), "Shipped!")).contains("Shipped!"));
        check("push: 32 chars passes",
                !throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(32), "hi")));
        check("push: 64 chars passes",
                !throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(64), "hi")));
        check("push: dash and underscore allowed",
                !throwsType(IllegalArgumentException.class,
                        () -> push.send("abc-def_ghi-jkl_mno-pqr_stu-vwx_yz01", "hi")));

        // invalid
        check("push: 31 chars rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(31), "hi")));
        check("push: 65 chars rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(65), "hi")));
        check("push: null rejected",
                throwsType(IllegalArgumentException.class, () -> push.send(null, "hi")));
        check("push: empty rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("", "hi")));
        check("push: spaces rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(20) + " " + "b".repeat(20), "hi")));
        check("push: '@' rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(40) + "@", "hi")));
        check("push: ':' rejected",
                throwsType(IllegalArgumentException.class, () -> push.send("a".repeat(20) + ":" + "b".repeat(20), "hi")));

        // nothing sent on failure
        String out = captureOutput(() -> push.send("short", "hi"));
        check("push: invalid token prints nothing", out.isEmpty());
    }
}

public class Factory {
    static void main() {
        NotificationFactoryTest.testFactoryReturnsCorrectTypes();
        NotificationFactoryTest.testNullChannel();
        NotificationFactoryTest.testEmail();
        NotificationFactoryTest.testSmsTruncation();
        NotificationFactoryTest.testPushValidation();

        System.out.println();
        System.out.println("Passed: " + NotificationFactoryTest.getPassed() +", Failed: " + NotificationFactoryTest.getFailed());
        if (NotificationFactoryTest.getFailed() > 0) System.exit(1);
    }
}
