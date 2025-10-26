package io.github.samu04072013;

// LicenseManager.java
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.io.IOException;
import org.jsoup.Jsoup;
import org.jsoup.Connection;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.jsoup.Jsoup;

public class LicenseManager {

    private final String licenseUrl;

    public LicenseManager(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }

    // Normalize email, compute Base64 -> MD5
    private String md5OfBase64(String email) throws Exception {
        String norm = email.trim().toLowerCase(Locale.ROOT);
        String b64 = Base64.getEncoder().encodeToString(norm.getBytes(StandardCharsets.UTF_8));

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(b64.getBytes(StandardCharsets.UTF_8));

        // Convert to lowercase hex
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    // Fetch remote file and read hashes into a set
    private Set<String> fetchRemoteHashes() throws Exception {
        String body = Jsoup.connect(licenseUrl).ignoreContentType(true).timeout(5000).execute().body();
        String[] lines = body.split("\\r?\\n");
        Set<String> hashes = new HashSet<>();
        for (String line : lines) {
            String s = line.trim();
            if (!s.isEmpty()) hashes.add(s.toLowerCase());
        }
        return hashes;
    }

    // Public check method
    public boolean isAllowed(String email) {
        try {
            String hash = md5OfBase64(email);
            Set<String> allowed = fetchRemoteHashes();
            return allowed.contains(hash);
        } catch (Exception e) {
            System.err.println("License check failed: " + e);
            return false;
        }
    }

    // Quick test
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter email to check: ");
        String email = sc.nextLine();

        LicenseManager lm = new LicenseManager("http://samu04072013.github.io/wiki-scraper/passkkeys");
        boolean ok = lm.isAllowed(email);
        System.out.println("Allowed? " + ok);
    }
}
