package io.github.samu04072013;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class WikiScraperGUI {

    private boolean premium;
    private Map<String, Map<String, Integer>> reportData = new HashMap<>();
    private int topWordsCount = 5; // default for graphs

    public WikiScraperGUI(boolean premium) {
        this.premium = premium;
    }

    public void start() {
        if (!premium) {
            runConsoleMode();
        } else {
            runGuiMode();
        }
    }

    // --- Console mode for free users ---
    private void runConsoleMode() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Free version (console mode). Top 3 words only.");

        while (true) {
            System.out.println("\nOptions:");
            System.out.println("1) Scrape a single page");
            System.out.println("0) Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "0":
                    return;
                case "1":
                    System.out.print("Enter Wikipedia page name: ");
                    String page = sc.nextLine().trim();
                    Map<String, Integer> counts = fetchWordCounts(page);
                    if (counts != null) {
                        reportData.put(page, counts);
                        printTopWords(counts, 3);
                    }
                    break;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    // --- GUI mode for premium users ---
    private void runGuiMode() {
        JFrame frame = new JFrame("Wiki Scraper - Premium");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel topPanel = new JPanel();
        JTextField pageField = new JTextField(20);
        JButton scrapeBtn = new JButton("Scrape Page");
        topPanel.add(new JLabel("Wikipedia Page:"));
        topPanel.add(pageField);
        topPanel.add(scrapeBtn);

        // Slider for top N words
        JPanel sliderPanel = new JPanel();
        JSlider topWordsSlider = new JSlider(1, 20, topWordsCount);
        topWordsSlider.setMajorTickSpacing(1);
        topWordsSlider.setPaintLabels(true);
        topWordsSlider.setPaintTicks(true);
        sliderPanel.add(new JLabel("Top words:"));
        sliderPanel.add(topWordsSlider);

        JPanel chartPanelHolder = new JPanel(new BorderLayout());

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(sliderPanel, BorderLayout.SOUTH);
        mainPanel.add(chartPanelHolder, BorderLayout.CENTER);

        scrapeBtn.addActionListener(e -> {
            String page = pageField.getText().trim();
            if (!page.isEmpty()) {
                Map<String, Integer> counts = fetchWordCounts(page);
                if (counts != null) {
                    reportData.put(page, counts);
                    displayChart(chartPanelHolder, page, counts, topWordsCount);
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to fetch page.");
                }
            }
        });

        topWordsSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                topWordsCount = topWordsSlider.getValue();
                // If a page is already scraped, refresh chart
                if (!reportData.isEmpty()) {
                    Map.Entry<String, Map<String, Integer>> entry = reportData.entrySet().iterator().next();
                    displayChart(chartPanelHolder, entry.getKey(), entry.getValue(), topWordsCount);
                }
            }
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    // --- Fetch word counts ---
    private Map<String, Integer> fetchWordCounts(String pageName) {
        try {
            String url = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(pageName, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url).get();
            Element content = doc.selectFirst("#mw-content-text");
            if (content == null) return null;

            String text = content.text();
            Map<String, Integer> counts = new HashMap<>();
            String[] words = text.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
            for (String word : words) {
                if (word.length() < 3) continue;
                counts.put(word, counts.getOrDefault(word, 0) + 1);
            }
            return counts;
        } catch (IOException e) {
            return null;
        }
    }

    private void printTopWords(Map<String, Integer> counts, int topN) {
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
    }

    // --- Display chart ---
    private void displayChart(JPanel panel, String pageName, Map<String, Integer> counts, int topN) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        counts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(topN)
                .forEach(e -> dataset.addValue(e.getValue(), "Count", e.getKey()));

        JFreeChart chart = ChartFactory.createBarChart(
                "Top " + topN + " words - " + pageName,
                "Word",
                "Count",
                dataset
        );

        panel.removeAll();
        panel.add(new ChartPanel(chart), BorderLayout.CENTER);
        panel.revalidate();
        panel.repaint();
    }

    // --- Main ---
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter your email: ");
        String email = sc.nextLine().trim();

        LicenseManager lm = new LicenseManager("http://samu04072013.github.io/wiki-scraper/passkkeys");
        boolean premium = lm.isAllowed(email);

        if (premium) {
            System.out.println("✅ Premium features unlocked! Launching GUI...");
        } else {
            System.out.println("🔒 Free version only. Console mode.");
        }

        WikiScraperGUI app = new WikiScraperGUI(premium);
        app.start();
    }
}
