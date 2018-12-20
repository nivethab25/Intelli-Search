package entities;

import javafx.util.Pair;
import org.jsoup.Jsoup;

import java.net.*;
import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * WebCrawler class to crawl webpages in the UIC domain. The start url for the
 * crwaler is "https://www.cs.uic.edu". The crawler is capable of crawling 80000
 * unique html pages in the UIC domain
 * 
 * @author Nivetha Babu
 */

public class WebCrawler {

	// Queue to store the urls to be crawled
	static Queue<String> crawledUrls = new LinkedList<String>();

	// Hashset to store unique urls to avoid duplication
	static HashSet<String> uniqueUrls = new HashSet<String>();

	// Map of url and web content
	static HashMap<String, String> urlContentMap = new HashMap<String, String>();

	public static void main(String[] args) throws Exception {

		String startUrl = "https://www.cs.uic.edu", url = null;
		uniqueUrls.add(startUrl.substring(startUrl.indexOf("://") + 3));
		File urlnPage = new File(
				"./src/main/webapp/ComponentProcessingResults/crawledPages/" + startUrl.replaceAll("[^a-zA-Z0-9.]", "") + ".txt");
		File urlnContent = new File(
				"./src/main/webapp/ComponentProcessingResults/urlContents/" + startUrl.replaceAll("[^a-zA-Z0-9.]", "") + ".txt");
		urlnPage.getParentFile().mkdirs();
		urlnPage.createNewFile();
		urlnContent.getParentFile().mkdirs();
		urlnContent.createNewFile();
		FileOutputStream out = new FileOutputStream(urlnPage);
		PrintWriter pw = new PrintWriter(out);
		FileOutputStream out1 = new FileOutputStream(urlnContent);
		PrintWriter pw1 = new PrintWriter(out1);
		Pair<String, String> webpageNcontent = processWebpage(startUrl);
		pw.println(startUrl + "|||||" + webpageNcontent.getKey());
		pw.close();
		out.close();
		pw1.println(startUrl + "|||||" + webpageNcontent.getValue());
		pw1.close();
		out1.close();
		urlContentMap.put(startUrl, webpageNcontent.getValue());

		// Crawl until 80000 unique webpages have been processed or queue empty
		while (!crawledUrls.isEmpty() && urlContentMap.size() <= 8000) {
			url = crawledUrls.peek();

			// Check and process if html page
			if (isHtml(url)) {
				webpageNcontent = processWebpage(crawledUrls.remove());

				// Try https protocol if webpage has been moved
				if (webpageNcontent.getKey().contains("301 Moved")
						|| webpageNcontent.getKey().contains("document has moved")
						|| webpageNcontent.getKey().contains("moved to")) {
					url = url.replaceFirst("://", "s://").replaceFirst("ss://", "s://");
					webpageNcontent = processWebpage(url);
				}

				if (webpageNcontent.getKey().contains("html") || webpageNcontent.getKey().contains("HTML")) {
					urlContentMap.put(url, webpageNcontent.getValue());
					urlnPage = new File("./src/main/webapp/ComponentProcessingResults/crawledPages/"
							+ url.replaceAll("[^a-zA-Z0-9.]", "") + ".txt");
					urlnPage.getParentFile().mkdirs();
					urlnPage.createNewFile();
					out = new FileOutputStream(urlnPage);
					pw = new PrintWriter(out);
					pw.println(url + "|||||" + webpageNcontent.getKey());
					pw.close();
					out.close();

					urlnContent = new File(
							"./src/main/webapp/ComponentProcessingResults/urlContents/" + url.replaceAll("[^a-zA-Z0-9.]", "") + ".txt");
					urlnContent.getParentFile().mkdirs();
					urlnContent.createNewFile();
					out1 = new FileOutputStream(urlnContent);
					pw1 = new PrintWriter(out1);
					pw1.println(url + "|||||" + webpageNcontent.getValue());
					pw1.close();
					out1.close();
				}
			}

			else
				crawledUrls.remove();
		}
	}

	// Method to process a webpage to extract all links
	public static Pair<String, String> processWebpage(String webUrl) {
		String webpage = "";
		String content = "";
		String inputLine;
		BufferedReader in = null;
		URLConnection conn = null;
		CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
		try {
			URL url = new URL(webUrl);
			conn = (HttpURLConnection) url.openConnection();
			conn.connect();

			in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			while ((inputLine = in.readLine()) != null) {
				if (inputLine.contains("href=") || inputLine.contains("frame src=\""))
					extractAllLinks(webUrl, inputLine);
				webpage += inputLine;
			}
			content += extractContent(webpage);

		} catch (Exception ex) {

		} finally {
			try {
				in.close();
				((HttpURLConnection) conn).disconnect();
			} catch (Exception ex) {
			}
		}
		return new Pair<String, String>(webpage, content);

	}

	// Method to extract the content of a webpage
	public static String extractContent(String string) {
		String content = Jsoup.parse(string).text();
		while (content.contains("</div>"))
			content = Jsoup.parse(content).text();

		if (content.contains("You may use these HTML tags and attributes:"))
			content = content.substring(0, content.indexOf("You may use these HTML tags and attributes:") - 1)
					+ content.substring(content.indexOf("Copyright © 2012 All rights reserved.") + 37);

		return content;
	}

	// Method to extract all links in a webpage
	public static void extractAllLinks(String webUrl, String htmlLine) {
		String temp1 = htmlLine, temp2 = htmlLine;

		// Extract links in the href of tags
		while (!temp1.isEmpty() && temp1.contains("href")) {
			int start = temp1.indexOf("href=\"") + 6;
			int end = temp1.indexOf("\"", start);
			if (end == -1 || start == end)
				temp1 = temp1.substring(start);
			else if (start > 0 && end < temp1.length() && start < end) {
				String link = temp1.substring(start, end);

				// Check and complete relative urls
				link = checkNCompleteRelativeUrls(webUrl, link);

				// Check if UIC domain
				if (isUICdomain(link) && isHtml(link)) {

					// Canonicalize link
					String canURL = canonicalizeLink(link), uniqUrl = null;
					uniqUrl = canURL.substring(canURL.indexOf("://") + 3);
					if (!uniqueUrls.contains(uniqUrl)) {
						uniqueUrls.add(uniqUrl);
						crawledUrls.add(canURL);
					}
				}
				temp1 = temp1.substring(end + 1);
			}
		}

		// Extract links in the <frame> tags
		while (!temp2.isEmpty() && temp2.contains("frame src")) {
			int start = temp2.indexOf("frame src=\"") + 5;
			int end = temp2.indexOf("\"", start);
			if (end == -1 || start == end)
				temp2 = temp2.substring(start);
			else if (start > 0 && end < temp2.length() && start < end) {
				String link = temp2.substring(start, end);

				// Check and complete relative urls
				link = checkNCompleteRelativeUrls(webUrl, link);

				// Check if UIC domain
				if (isUICdomain(link) && isHtml(link)) {

					// Canonicalize link
					String canURL = canonicalizeLink(link), uniqUrl = null;
					uniqUrl = canURL.substring(canURL.indexOf("://") + 3);
					if (!uniqueUrls.contains(uniqUrl)) {
						uniqueUrls.add(uniqUrl);
						crawledUrls.add(canURL);
					}
				}
				temp2 = temp2.substring(end + 1);
			}
		}

	}

	// Check if url is of UIC Domain
	public static boolean isUICdomain(String link) {
		if (link.matches("^(https?)://[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*uic.edu[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*"))
			return true;
		return false;
	}

	// Check and complete relative urls
	public static String checkNCompleteRelativeUrls(String parentUrl, String link) {
		if (!link.matches("^(https?)://[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*")) {
			if (link.contains("../")) {
				String[] splitUrl = link.split("/");
				for (String part : splitUrl) {
					if (part.equals("..")) {
						link = link.substring(link.indexOf("..") + 2);
						parentUrl = parentUrl.substring(0, parentUrl.lastIndexOf("/"));
					}
				}
			}

			if (link.contains("/"))
				link = link.substring(link.indexOf("/") + 1);
			return parentUrl + "/" + link;
		}
		return link;
	}

	// Check if url is a html page
	public static boolean isHtml(String link) {
		String lastPartOfURl = link.substring(link.lastIndexOf("/") + 1);
		if (!lastPartOfURl.contains(".") || (lastPartOfURl.contains(".") && (lastPartOfURl.endsWith("html")
				|| lastPartOfURl.endsWith("htm") || lastPartOfURl.endsWith("shtml") || lastPartOfURl.endsWith("edu"))))
			return true;
		return false;
	}

	// Canonicalize url
	public static String canonicalizeLink(String link) {

		if (link.contains("#"))
			link = link.substring(0, link.indexOf("#"));

		if (link.endsWith("/"))
			link = link.substring(0, link.length() - 1);

		return link;
	}
}
