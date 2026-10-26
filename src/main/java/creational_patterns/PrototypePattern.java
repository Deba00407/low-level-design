package creational_patterns;

import lombok.Getter;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

interface Prototype<T> {
    T copy();
}

// ---------- Parts of a document ----------

@Getter
final class Section implements Prototype<Section> {
    private final String heading;   // immutable -> shared
    private final String style;     // immutable -> shared
    private String text;            // reassignable field

    Section(String heading, String text, String style) {
        this.heading = Objects.requireNonNull(heading, "heading must not be null");
        this.text = Objects.requireNonNull(text, "text must not be null");
        this.style = Objects.requireNonNull(style, "style must not be null");
    }

    // Copy constructor: all fields are immutable types, so sharing them is safe
    private Section(Section other) {
        this.heading = other.heading;
        this.text = other.text;
        this.style = other.style;
    }

    void setText(String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    @Override
    public Section copy() {
        return new Section(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Section s
                && heading.equals(s.heading) && text.equals(s.text) && style.equals(s.style);
    }

    @Override
    public int hashCode() {
        return Objects.hash(heading, text, style);
    }
}

final class Metadata implements Prototype<Metadata> {
    private final List<String> tags;
    private final LocalDate created;   // immutable type -> shared
    private String author;

    Metadata(String author, LocalDate created) {
        this.author = Objects.requireNonNull(author, "author must not be null");
        this.created = Objects.requireNonNull(created, "created must not be null");
        this.tags = new ArrayList<>();
    }

    private Metadata(Metadata other) {
        this.author = other.author;
        this.created = other.created;
        this.tags = new ArrayList<>(other.tags);   // Strings are immutable, so a new list is enough
    }

    String getAuthor() {
        return author;
    }

    void setAuthor(String author) {
        this.author = Objects.requireNonNull(author, "author must not be null");
    }

    LocalDate getCreated() {
        return created;
    }

    List<String> getTags() {
        return Collections.unmodifiableList(tags);
    }

    void addTag(String tag) {
        tags.add(Objects.requireNonNull(tag, "tag must not be null"));
    }

    @Override
    public Metadata copy() {
        return new Metadata(this);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Metadata m
                && author.equals(m.author) && created.equals(m.created) && tags.equals(m.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, created, tags);
    }
}

// ---------- Simulated expensive step ----------

final class LayoutLoader {
    static final AtomicInteger LOAD_COUNT = new AtomicInteger();

    private LayoutLoader() {
    }

    static String load(String fileName) {
        LOAD_COUNT.incrementAndGet();
        // pretend this reads and parses a file from disk (~200 ms)
        return "parsed-layout:" + fileName;
    }
}

// ---------- The prototype ----------

@Getter
class Document implements Prototype<Document>, Cloneable {

    private final String title;      // immutable, fine as final
    private final String type;       // immutable, fine as final
    private final String layout;     // parsed result, immutable String -> shared by all copies

    // NOT final: clone() must be able to replace them with deep copies
    private List<Section> sections;
    private Metadata metadata;
    private Map<String, String> placeholders;

    Document(String title, String type, String layout, Metadata metadata) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.layout = Objects.requireNonNull(layout, "layout must not be null");
        this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
        this.sections = new LinkedList<>();
        this.placeholders = new HashMap<>();
    }

    List<Section> getSections() {
        return Collections.unmodifiableList(sections);
    }

    void addSection(Section s) {
        sections.add(Objects.requireNonNull(s, "section must not be null"));
    }

    void setPlaceholder(String key, String value) {
        placeholders.put(Objects.requireNonNull(key, "key must not be null"),
                Objects.requireNonNull(value, "value must not be null"));
    }

    String getPlaceholder(String key) {
        return placeholders.get(key);
    }

    Map<String, String> getPlaceholders() {
        return Collections.unmodifiableMap(placeholders);
    }

    @Override
    public Document copy() {
        try {
            // super.clone() uses the runtime class, so an Invoice stays an Invoice.
            // It copies primitives and references; references are shared until replaced below.
            Document clone = (Document) super.clone();

            clone.metadata = this.metadata.copy();

            clone.sections = new LinkedList<>();
            for (Section s : this.sections) {
                clone.sections.add(s.copy());   // copy each Section, not just the list
            }

            clone.placeholders = new HashMap<>(this.placeholders);   // String -> String: safe
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Cannot happen: Document implements Cloneable", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document d = (Document) o;
        return title.equals(d.title) && type.equals(d.type) && layout.equals(d.layout)
                && sections.equals(d.sections) && metadata.equals(d.metadata)
                && placeholders.equals(d.placeholders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, type, layout, sections, metadata, placeholders);
    }
}

final class Invoice extends Document {
    private double taxRate;   // primitive: clone() copies it automatically

    Invoice(String title, String layout, Metadata metadata, double taxRate) {
        super(title, "invoice", layout, metadata);
        this.taxRate = taxRate;
    }

    double getTaxRate() {
        return taxRate;
    }

    void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    @Override
    public Invoice copy() {                 // covariant return: no cast needed by callers
        return (Invoice) super.copy();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && Double.compare(taxRate, ((Invoice) o).taxRate) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Double.hashCode(taxRate);
    }
}

// ---------- Registry ----------

final class TemplateRegistry {
    private final Map<String, Document> templates = new ConcurrentHashMap<>();

    void register(String name, Document template) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(template, "template must not be null");
        templates.put(name, template.copy());   // copy IN: caller's original can't alter the registry
    }

    Document create(String name) {
        Objects.requireNonNull(name, "name must not be null");
        Document prototype = templates.get(name);
        if (prototype == null) {
            throw new IllegalArgumentException("No template registered with name: " + name);
        }
        return prototype.copy();                // copy OUT: stored prototype never leaves the registry
    }
}

// ---------- Deliberately shallow version for test 12 ----------

final class ShallowDocument implements Cloneable {
    final List<Section> sections = new ArrayList<>();
    final Metadata metadata;
    final Map<String, String> placeholders = new HashMap<>();

    ShallowDocument(Metadata metadata) {
        this.metadata = metadata;
    }

    ShallowDocument shallowCopy() {
        try {
            return (ShallowDocument) super.clone();
        }   // shares every reference
        catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}

// ---------- Demo and tests ----------

public class PrototypePattern {

    private static int passed = 0, failed = 0;

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

    private static Document buildInvoiceTemplate() {
        String layout = LayoutLoader.load("invoice-layout.xml");      // the slow part
        Metadata meta = new Metadata("Finance Team", LocalDate.of(2026, 1, 1));
        meta.addTag("invoice");

        Invoice invoice = new Invoice("Invoice", layout, meta, 0.18);
        invoice.addSection(new Section("Header", "Invoice for {{customerName}}", "bold-16"));
        invoice.addSection(new Section("Body", "Amount due: {{amount}}", "regular-12"));
        invoice.addSection(new Section("Footer", "Thank you", "italic-10"));
        return invoice;
    }

    static void main() {
        LayoutLoader.LOAD_COUNT.set(0);
        TemplateRegistry registry = new TemplateRegistry();
        Document original = buildInvoiceTemplate();
        registry.register("invoice", original);

        // 1, 2: different object, equal contents
        Document copy = original.copy();
        check("1: copy is a different object", copy != original);
        check("2: copy has equal contents", copy.equals(original));

        // 3: section mutation
        copy.getSections().get(0).setText("CHANGED");
        check("3: section change does not affect original",
                original.getSections().get(0).getText().equals("Invoice for {{customerName}}"));

        // 4: tag mutation
        copy.getMetadata().addTag("urgent");
        check("4: tag added to copy not in original",
                !original.getMetadata().getTags().contains("urgent"));

        // 5: placeholder mutation
        copy.setPlaceholder("customerName", "Alice");
        check("5: placeholder on copy not in original", original.getPlaceholder("customerName") == null);

        // 6: registry stays pristine
        Document fromRegistry = registry.create("invoice");
        fromRegistry.setPlaceholder("customerName", "Bob");
        fromRegistry.getMetadata().addTag("x");
        fromRegistry.getSections().get(1).setText("hacked");
        Document next = registry.create("invoice");
        check("6: next create() is pristine",
                next.getPlaceholder("customerName") == null
                        && !next.getMetadata().getTags().contains("x")
                        && next.getSections().get(1).getText().equals("Amount due: {{amount}}"));

        // 7: unknown name
        check("7: create(\"nope\") throws IllegalArgumentException",
                throwsType(IllegalArgumentException.class, () -> registry.create("nope")));

        // 8: expensive setup happens once
        for (int i = 0; i < 1000; i++) registry.create("invoice");
        check("8: layout loaded exactly once (count=" + LayoutLoader.LOAD_COUNT.get() + ")",
                LayoutLoader.LOAD_COUNT.get() == 1);

        // 9: subclass-safe copy through a Document reference
        Document asDocument = original;
        Document invoiceCopy = asDocument.copy();
        check("9: copy through Document reference is an Invoice",
                invoiceCopy instanceof Invoice inv && inv.getTaxRate() == 0.18);

        // 10: registry stores its own copy
        Document mine = buildInvoiceTemplate();
        registry.register("mine", mine);
        mine.getSections().get(0).setText("mutated after register");
        mine.getMetadata().addTag("late");
        check("10: mutating the registered original does not affect the registry",
                registry.create("mine").getSections().get(0).getText().equals("Invoice for {{customerName}}")
                        && !registry.create("mine").getMetadata().getTags().contains("late"));

        // 11: null arguments
        check("11a: register(null, doc) throws NPE",
                throwsType(NullPointerException.class, () -> registry.register(null, original)));
        check("11b: register(\"x\", null) throws NPE",
                throwsType(NullPointerException.class, () -> registry.register("x", null)));

        // 12: shallow copy fails tests 3 to 5
        ShallowDocument shallow = new ShallowDocument(new Metadata("A", LocalDate.now()));
        shallow.sections.add(new Section("H", "original text", "s"));
        ShallowDocument shallowCopy = shallow.shallowCopy();
        shallowCopy.sections.get(0).setText("changed via shallow copy");
        shallowCopy.metadata.addTag("leak");
        shallowCopy.placeholders.put("k", "v");
        check("12: shallow copy leaks section change into original (expected to leak)",
                shallow.sections.get(0).getText().equals("changed via shallow copy"));
        check("12: shallow copy leaks tag into original (expected to leak)",
                shallow.metadata.getTags().contains("leak"));
        check("12: shallow copy leaks placeholder into original (expected to leak)",
                shallow.placeholders.containsKey("k"));

        System.out.println();
        System.out.println("Passed: " + passed + ", Failed: " + failed);
    }
}