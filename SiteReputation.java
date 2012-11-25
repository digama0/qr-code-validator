import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SiteReputation
{
	String basicInfo;
	int responseCode = 0;
	String redirectURL;
	//This is the standard, as I recall.
	final static int MAX_REDIRECTS = 20;
	//Hooray for CSE webspace!
	final static String BLACKLIST_URL = "http://www.cse.ohio-state.edu/~powelle/blacklist.txt";
	
	public SiteReputation(String url_s){
		/**
		 * Gets whether a string is a URL, and whether it is valid http
		 */
		//First, check syntax.
		this.redirectURL = url_s;//this will be changed if it redirects.
		URL url = null;
		try{
			url= new URL(url_s);
		}
		catch (MalformedURLException e) {
			this.basicInfo = "This QR code isn't a website";
		}
		
		//Next, get the response code.
		HttpURLConnection conn = null;
		try{
			conn = (HttpURLConnection) url.openConnection();
		}
		catch (IOException e) {
			this.basicInfo =  "Unable to verify link.";
		}
		try {
			this.responseCode = conn.getResponseCode();
		} catch (IOException e) {
			this.basicInfo =  "Unable to verify link.";
		}
		//Now, with the response code, tell us about the link.
		if (this.responseCode == 404)
	    {
	        this.basicInfo = "Link doesn't work, page not found.";
	    }
	    else if (this.responseCode >= 400 && this.responseCode < 500)
	    {
	    	this.basicInfo = "Link doesn't work.";
	    }
	    else if (this.responseCode >= 500)
	    {
	    	this.basicInfo = "Servers down, link doesn't work.";
	    }
	    else if (this.responseCode == 200)
	    {
	    	this.basicInfo = "Link works.";
	    }
	    else if (this.responseCode > 200 && this.responseCode < 300)
	    {
	    	this.basicInfo =  "Link probably works, but is a bit odd.";
	    }
	    else if (this.responseCode == 305 || this.responseCode == 306)
	    {
	    	this.basicInfo =  "Proxy requested; possibly insecure.";
	    }
	    else if (this.responseCode >=300 && this.responseCode < 400)
	    {
	        this.basicInfo = "Link is a redirect.";
	        this.getRedirect(conn);
	    }
	}
	private void getRedirect(HttpURLConnection conn) {
		this.redirectURL = "";
		int i = 0;
		//keep checking redirects until we get tired of it.
		while (i < MAX_REDIRECTS){
			String redirect = conn.getHeaderField("Location");
			if (redirect != null) {
				this.redirectURL = redirect;
			}
			else {
				break;
			}
			//Make an object of it
			try{
				URL url= new URL(this.redirectURL);
				conn = (HttpURLConnection) url.openConnection();
			}
			catch (MalformedURLException e) {
				this.basicInfo =  "Broken redirect.";
				break;
			} catch (IOException e) {
				this.basicInfo =  "Broken redirect.";
				break;
			}
			//And now see what it does
			try {
				this.responseCode = conn.getResponseCode();
			} catch (IOException e) {
			    //This exception is thrown for go.osu.edu/lan and I'm not sure
			    //why. Some failed cert check, by the below.
			    //TODO
				//System.out.println(e.getMessage());
				this.basicInfo =  "Broken redirect.";
			}
			//If it's good, we're done here.
			if (this.responseCode >= 200 && this.responseCode < 300){
				break;
			}
			//If it's bad, say so, then we're done.
			else if (! (this.responseCode >=300 && this.responseCode < 400)){
				this.basicInfo =  "Broken redirect.";
				break;
			}
			//Otherwise, we're at it again.
		}
	}
	public static int getWOT(String url){
		url = "http://api.mywot.com/0.4/public_query2?url="+url;
		String s;
		try {
			s = StringFromURL(url);
		} catch (IOException e) {
			return -1; //Can't get a rating, so call it unrated.
		}
		//This is a terrible, quick & dirty way of parsing the xml.
		if (s.indexOf("name=\"0\"") < 0){
			//site is not rated.
			return -1;
		}
		
		int i = s.indexOf("name=\"0\"");
		i = s.indexOf("r=\"");
		i += 3; //i is now the start of the rating
		String rate = "";
		while (s.charAt(i) != '"') {
			rate += s.charAt(i);
			i++;
		}
		return Integer.parseInt(rate);
	}
	public static String getBlacklisted(String url) throws IOException{
		String s = StringFromURL(BLACKLIST_URL);
		for (String regex : s.split("\n")) {
		    String[] regexResponse = regex.split("\\s+", 2);
		    regex = regexResponse[0];
		    String response = regexResponse[1];
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(url);
			if (m.matches()){
				return response;
			}
		}
		return "No safety information.";
	}
	
	private static String StringFromURL(String url) throws IOException{
		URL urlo = null;
		try{
			urlo = new URL(url);
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		URLConnection conn = null;
		try{
			conn = urlo.openConnection();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		java.io.InputStream is = conn.getInputStream();
		
		StringBuilder data = new StringBuilder("");
		while (is.available() > 0) {
			data.append((char)is.read());
		}
		return data.toString();
	}
	
	public static void main(String[] args) throws IOException
	{
		SiteReputation test = new SiteReputation(args[0]);
		System.out.println(test.responseCode);
	    System.out.println(test.basicInfo);
	    System.out.println(test.redirectURL);
	    System.out.println(getWOT(test.redirectURL));
	    System.out.println(getBlacklisted(test.redirectURL));
	}
}
