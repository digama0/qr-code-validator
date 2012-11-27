package edu.osu.cse.security.qrcodevalidator;

import java.io.*;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

public class SiteReputation {
    public ErrorCode basicInfo;
    public int responseCode = 0;
    public String originalURL, redirectURL;
    private int wot = -2;
    private String blacklisted = null;
    // This is the standard, as I recall.
    final static int MAX_REDIRECTS = 20;
    // Hooray for CSE webspace!
    final static String BLACKLIST_URL = "http://www.cse.ohio-state.edu/~powelle/blacklist.txt";

    public SiteReputation(final String url_s) {
        /**
         * Gets whether a string is a URL, and whether it is valid http
         */
        // First, check syntax.
        redirectURL = originalURL = url_s;// this will be changed if it
                                          // redirects.
        URL url = null;
        try {
            url = new URL(url_s);
        } catch (final MalformedURLException e) {
            basicInfo = ErrorCode.NOT_SITE;
            return;
        }

        // Next, get the response code.
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setInstanceFollowRedirects(false);
        } catch (final IOException e) {
            basicInfo = ErrorCode.CANT_VERIFY;
            return;
        }
        try {
            responseCode = conn.getResponseCode();
        } catch (final IOException e) {
            basicInfo = ErrorCode.CANT_VERIFY;
        }
        // Now, with the response code, tell us about the link.
        if (responseCode == 404) {
            basicInfo = ErrorCode.NOT_FOUND;
        }
        else if (responseCode >= 400 && responseCode < 500) {
            basicInfo = ErrorCode.BROKEN;
        }
        else if (responseCode >= 500) {
            basicInfo = ErrorCode.SERVER_DOWN;
        }
        else if (responseCode == 200) {
            basicInfo = ErrorCode.SUCCESS;
            getRedirect(conn);
        }
        else if (responseCode > 200 && responseCode < 300) {
            basicInfo = ErrorCode.WTF;
            getRedirect(conn);
        }
        else if (responseCode == 305 || responseCode == 306) {
            basicInfo = ErrorCode.PROXY;
        }
        else if (responseCode >= 300 && responseCode < 400) {
            basicInfo = ErrorCode.REDIRECT;
            getRedirect(conn);
        }
    }
    private void getRedirect(HttpURLConnection conn) {
        final int i = 0;
        // keep checking redirects until we get tired of it.
        while (i < MAX_REDIRECTS) {
            final String redirect = conn.getHeaderField("Location");
            if (redirect != null) {
                basicInfo = ErrorCode.REDIRECT;
                redirectURL = redirect;
            }
            // Make an object of it
            try {
                final URL url = new URL(redirectURL);
                conn = (HttpURLConnection)url.openConnection();
                conn.setInstanceFollowRedirects(false);
            } catch (final MalformedURLException e) {
                basicInfo = ErrorCode.BROKEN_REDIRECT;
                break;
            } catch (final IOException e) {
                basicInfo = ErrorCode.BROKEN_REDIRECT;
                break;
            }
            // And now see what it does
            try {
                responseCode = conn.getResponseCode();
            } catch (final SSLException e) {
                basicInfo = ErrorCode.BROKEN_CERTS;
                break;
            } catch (final IOException e) {
                basicInfo = ErrorCode.BROKEN_REDIRECT;
                break;
            }
            // If it's good, we're done here.
            if (responseCode >= 200 && responseCode < 300) {
                try {
                    // Look for http-equiv="refresh" redirects.
                    String data = stringFromConn(conn);
                    String p = "<meta\\s+http-equiv=\"refresh\"\\s+content=\"\\d+;URL=([^\"]*)\"";
                    // Special-case v.gd, which uses a weird redirect mechanism.
                    if (redirectURL.matches("https?://v\\.gd/.+"))
                        p = "<a href=\"([^\"]*)\" class=\"biglink\">";
                    Matcher m = Pattern.compile(p).matcher(data);
                    if (m.find()) {
                        basicInfo = ErrorCode.REDIRECT;
                        redirectURL = m.group(1);
                    }
                    else {
                        // No more redirects
                        break;
                    }
                } catch (IOException e) {
                    basicInfo = ErrorCode.BROKEN_REDIRECT;
                    break;
                }
            }
            // If it's bad, say so, then we're done.
            else if (!(responseCode >= 300 && responseCode < 400)) {
                basicInfo = ErrorCode.BROKEN_REDIRECT;
                break;
            }
            // Otherwise, we're at it again.
        }
    }
    public int getWot() {
        if (wot == -2) {
            if (basicInfo == ErrorCode.NOT_SITE) return wot = -1;
            String url = "http://api.mywot.com/0.4/public_query2?url="
                + redirectURL;
            String s;
            try {
                s = stringFromUrl(url);
            } catch (final IOException e) {
                return wot = -1; // Can't get a rating, so call it unrated.
            }
            // This is a terrible, quick & dirty way of parsing the xml.
            if (s.indexOf("name=\"0\"") < 0) // site is not rated.
                return wot = -1;

            int i = s.indexOf("name=\"0\"");
            i = s.indexOf("r=\"");
            i += 3; // i is now the start of the rating
            String rate = "";
            while (s.charAt(i) != '"') {
                rate += s.charAt(i);
                i++;
            }
            wot = Integer.parseInt(rate);
        }
        return wot;
    }
    public String getBlacklisted() {
        if (blacklisted == null) {
            try {
                if (basicInfo == ErrorCode.NOT_SITE) return blacklisted = "";
                final String s = stringFromUrl(BLACKLIST_URL);
                for (String regex : s.split("\n")) {
                    final String[] regexResponse = regex.split("\\s+", 2);
                    regex = regexResponse[0];
                    final String response = regexResponse[1];
                    final Pattern p = Pattern.compile(regex);
                    final Matcher m = p.matcher(redirectURL);
                    if (m.matches())
                        return blacklisted = response;
                }
                blacklisted = "No safety information.";
            } catch (IOException e) {
                blacklisted = "Unable to reach blacklist site: "
                    + e.getMessage();
            }
        }
        return blacklisted;
    }

    private static String stringFromUrl(final String url) throws IOException {
        URL urlo = null;
        try {
            urlo = new URL(url);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection conn = null;
        try {
            conn = urlo.openConnection();
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return stringFromConn(conn);
    }

    private static String stringFromConn(final URLConnection conn)
        throws IOException
    {
        final java.io.InputStream is = conn.getInputStream();

        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void main(final String[] args) throws IOException {
        String s;
        if (args.length == 0) {
            System.out.print("Enter a URL: ");
            s = new BufferedReader(new InputStreamReader(System.in)).readLine();
        }
        else
            s = args[0];
        final SiteReputation test = new SiteReputation(s);
        System.out.println(test.responseCode);
        System.out.println(test.basicInfo.verbose);
        System.out.println(test.redirectURL);
        System.out.println(test.getWot());
        System.out.println(test.getBlacklisted());
    }

    public enum ErrorCode {
        NOT_SITE("This QR code isn't a website"),
        CANT_VERIFY("Unable to verify link."),
        NOT_FOUND("Link doesn't work, page not found."),
        BROKEN("Link doesn't work."),
        SERVER_DOWN("Servers down, link doesn't work."),
        SUCCESS("Link works."),
        WTF("Link probably works, but is a bit odd."),
        PROXY("Proxy requested; possibly insecure."),
        REDIRECT("Link is a redirect."),
        BROKEN_REDIRECT("Broken redirect."),
        BROKEN_CERTS("Unable to verify certificate (possible security threat).");

        public String verbose;

        private ErrorCode(String n) {
            verbose = n;
        }
    }
}
