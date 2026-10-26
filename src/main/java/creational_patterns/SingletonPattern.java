package creational_patterns;

// A single instance is shared across all usages.

// Example: A basic singleton configuration loader class, that lazy loads the configuration file only when the resource is requested.

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

interface Loader {
    String getParam(String key, String defaultValue);

    void setParam(String key, String value);

    void reload();
}

class ConfigurationLoader implements Loader {
    static final AtomicInteger LOAD_COUNT = new AtomicInteger(0);
    private static final String DEFAULT_CONFIG_FILE_PATH = "src/main/java/creational_patterns/config.properties";
    private static volatile ConfigurationLoader instance;
    private final String filePath;
    private Map<String, String> configMap = null;

    private ConfigurationLoader(String filePath) {
        this.filePath = Objects.requireNonNull(filePath);
        this.configMap = readFile(filePath);
    }

    public static Loader getInstance() {
        ConfigurationLoader loader = instance;
        if (loader == null) {
            synchronized (ConfigurationLoader.class) {
                loader = instance;
                if (loader == null) {
                    instance = loader = new ConfigurationLoader(DEFAULT_CONFIG_FILE_PATH);
                }
            }
        }

        return loader;
    }

    @Override
    public void reload() {
        this.configMap = readFile(this.filePath);
    }

    public Map<String, String> readFile(String filePath) {
        Map<String, String> configs = new ConcurrentHashMap<>();

        Properties props = new Properties();

        try (InputStream inputStream = Files.newInputStream(Path.of(filePath))) {
            props.load(inputStream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load config file: " + filePath, e);
        }

        for (String key : props.stringPropertyNames()) {
            configs.put(key, props.getProperty(key));
        }

        System.out.println("Config file loaded successfully.");

        LOAD_COUNT.getAndIncrement();

        return configs;
    }

    @Override
    public String getParam(String key, String defaultValue) {
        return configMap.getOrDefault(key, defaultValue);
    }

    @Override
    public void setParam(String key, String value) {
        configMap.put(key, value);
    }
}

public class SingletonPattern {

    private static void testThreadSafety() throws Exception {
        int threads = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        Set<Integer> ids = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();                      // all threads released together
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                ids.add(System.identityHashCode(ConfigurationLoader.getInstance()));
            });
        }

        ready.await();
        go.countDown();
        pool.shutdown();
        boolean terminationResult = pool.awaitTermination(5, TimeUnit.SECONDS);

        if (!terminationResult) {
            System.err.println("FAIL: Threads did not terminate within the specified time.");
            return;
        }

        System.out.println(
                ids.size() == 1
                        ? "PASS: one instance across " + threads + " threads."
                        : "FAIL: " + ids.size() + " instances created."
        );

        System.out.println(ConfigurationLoader.LOAD_COUNT.get() == 1
                ? "PASS: file loaded exactly once."
                : "FAIL: file loaded " + ConfigurationLoader.LOAD_COUNT.get() + " times.");
    }

    static void main() {
        try {
            testThreadSafety();
        } catch (Exception e) {
            throw new RuntimeException("Failed to test thread safety: " + e.getMessage());
        }
    }
}
