package edu.osu.cse.security.qrcodevalidator;

import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SiteReputation {
    String basicInfo;
    int responseCode = 0;
    String redirectURL;
    // This is the standard, as I recall.
    final static int MAX_REDIRECTS = 20;
    // Hooray for CSE webspace!
    final static String BLACKLIST_URL = "http://www.cse.ohio-state.edu/~powelle/blacklist.txt";

    public SiteReputation(final String url_s) {
        /**
         * Gets whether a string is a URL, and whether it is valid http
         */
        // First, check syntax.
        redirectURL = url_s;// this will be changed if it redirects.
        URL url = null;
        try {
            url = new URL(url_s);
        } catch (final MalformedURLException e) {
            basicInfo = "This QR code isn't a website";
        }

        // Next, get the response code.
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
        } catch (final IOException e) {
            basicInfo = "Unable to verify link.";
        }
        try {
            responseCode = conn.getResponseCode();
        } catch (final IOException e) {
            basicInfo = "Unable to verify link.";
        }
        // Now, with the response code, tell us about the link.
        if (responseCode == 404) {
            basicInfo = "Link doesn't work, page not found.";
        }
        else if (responseCode >= 400 && responseCode < 500) {
            basicInfo = "Link doesn't work.";
        }
        else if (responseCode >= 500) {
            basicInfo = "Servers down, link doesn't work.";
        }
        else if (responseCode == 200) {
            basicInfo = "Link works.";
        }
        else if (responseCode > 200 && responseCode < 300) {
            basicInfo = "Link probably works, but is a bit odd.";
        }
        else if (responseCode == 305 || responseCode == 306) {
            basicInfo = "Proxy requested; possibly insecure.";
        }
        else if (responseCode >= 300 && responseCode < 400) {
            basicInfo = "Link is a redirect.";
            getRedirect(conn);
        }
    }
    private void getRedirect(HttpURLConnection conn) {
        redirectURL = "";
        final int i = 0;
        // keep checking redirects until we get tired of it.
        while (i < MAX_REDIRECTS) {
            final String redirect = conn.getHeaderField("Location");
            if (redirect != null) {
                redirectURL = redirect;
            }
            else {
                break;
            }
            // Make an object of it
            try {
                final URL url = new URL(redirectURL);
                conn = (HttpURLConnection)url.openConnection();
            } catch (final MalformedURLException e) {
                basicInfo = "Broken redirect.";
                break;
            } catch (final IOException e) {
                basicInfo = "Broken redirect.";
                break;
            }
            // And now see what it does
            try {
                responseCode = conn.getResponseCode();
            } catch (final IOException e) {
                // This exception is thrown for go.osu.edu/lan and I'm not sure
                // why. Some failed cert check, by the below.
                // TODO
                // System.out.println(e.getMessage());
                basicInfo = "Broken redirect.";
            }
            // If it's good, we're done here.
            if (responseCode >= 200 && responseCode < 300) {
                break;
            }
            // If it's bad, say so, then we're done.
            else if (!(responseCode >= 300 && responseCode < 400)) {
                basicInfo = "Broken redirect.";
                break;
            }
            // Otherwise, we're at it again.
        }
    }
    public static int getWOT(String url) {
        url = "http://api.mywot.com/0.4/public_query2?url=" + url;
        String s;
        try {
            s = StringFromURL(url);
        } catch (final IOException e) {
            return -1; // Can't get a rating, so call it unrated.
        }
        // This is a terrible, quick & dirty way of parsing the xml.
        if (s.indexOf("name=\"0\"") < 0) // site is not rated.
        return -1;

        int i = s.indexOf("name=\"0\"");
        i = s.indexOf("r=\"");
        i += 3; // i is now the start of the rating
        String rate = "";
        while (s.charAt(i) != '"') {
            rate += s.charAt(i);
            i++;
        }
        return Integer.parseInt(rate);
    }
    public static String getBlacklisted(final String url) throws IOException {
        final String s = StringFromURL(BLACKLIST_URL);
        for (String regex : s.split("\n")) {
            final String[] regexResponse = regex.split("\\s+", 2);
            regex = regexResponse[0];
            final String response = regexResponse[1];
            final Pattern p = Pattern.compile(regex);
            final Matcher m = p.matcher(url);
            if (m.matches()) return response;
        }
        return "No safety information.";
    }

    private static String StringFromURL(final String url) throws IOException {
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
        final java.io.InputStream is = conn.getInputStream();

        final StringBuilder data = new StringBuilder("");
        while (is.available() > 0) {
            data.append((char)is.read());
        }
        return data.toString();
    }

    public static void main(final String[] args) throws IOException {
        final SiteReputation test = new SiteReputation(args[0]);
        System.out.println(test.responseCode);
        System.out.println(test.basicInfo);
        System.out.println(test.redirectURL);
        System.out.println(getWOT(test.redirectURL));
        System.out.println(getBlacklisted(test.redirectURL));
    }
}
