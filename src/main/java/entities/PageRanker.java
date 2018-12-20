package entities;

import javafx.util.Pair;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

/**
 * PageRanker class to implement the Page Rank algorithm for a collection.
 *
 * @author Nivetha Babu
 */

public class PageRanker {

	// Map to store each url and its adjacency list of urls alongwith its out-degree
	public HashMap<String, Pair<Integer, HashSet<String>>> webpagesGraph = new HashMap<String, Pair<Integer, HashSet<String>>>();

	// Map of urls and page rank scores
	HashMap<String, Double> webpageScoresMap = new HashMap<String, Double>();
	
	HashSet<String> urlsCrawled = new HashSet<String>();

	// Webpages collection
	private File collectionsFolder;

	// Method to read each webpage in the collection and tokenize its contents on
	// whitespace,
	// remove stopwords and apply Porter stemmer and remove stopwords again and
	// build a web graph of all the web pages in the collection
	public void buildWebpagesGraph(String collection) {
		HashSet<String> adjList = new HashSet<String>();
		String url = "";
		collectionsFolder = new File(collection);
		fetchUrlsSet();
		for (final File file : collectionsFolder.listFiles()) {
			adjList = new HashSet<String>();
			if (file.isDirectory()) {
				buildWebpagesGraph(file.getAbsolutePath());
			} else {
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(file));
					String line;
					while ((line = br.readLine()) != null) {
						String[] firstLine = null;
						if (line.contains("|||||")) {

							// fetch url and canonicalize it
							firstLine = line.replaceAll(" ", "").split("[|||||]");
							url = canonicalizeLink(firstLine[0]);
							int i = 1;
							line = "";
							while (i < firstLine.length) {
								line += firstLine[i];
								i++;
							}
						}

						// add all links to adjacency list
						adjList.addAll(extractAllLinks(url, line));
					}

				} catch (Exception e) {
					System.out.println(e.getMessage());

				} finally {
					try {
						br.close();
					} catch (IOException exc) {

					}
				}
			}

			webpagesGraph.put(url, new Pair<>(adjList.size(), adjList));

			// update adjacency lists of all connected urls
			for (String link : adjList) {
				if (webpagesGraph.containsKey(link)) {
					Pair<Integer, HashSet<String>> newPair;
					HashSet<String> newList = webpagesGraph.get(link).getValue();
					newList.add(url);
					newPair = new Pair<>(newList.size(), newList);
					webpagesGraph.put(link, newPair);
				} else
					webpagesGraph.put(link, new Pair<Integer, HashSet<String>>(0, new HashSet<String>()));
			}
		}

		// Update out-degree of pages that are sinks
		for (Entry<String, Pair<Integer, HashSet<String>>> entry : webpagesGraph.entrySet()) {
			if (entry.getValue().getKey() == 0)
				webpagesGraph.put(entry.getKey(), new Pair<>(webpagesGraph.size(), entry.getValue().getValue()));
		}
	}
	
	private void fetchUrlsSet() {
		for (final File file : collectionsFolder.listFiles()) {
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(file));
					String line;
					while ((line = br.readLine()) != null) {
						String[] firstLine = null;
						if (line.contains("|||||")) {

							// fetch url and canonicalize it
							firstLine = line.replaceAll(" ", "").split("[|||||]");
							String url = canonicalizeLink(firstLine[0]);
							urlsCrawled.add(url);
							break;
						}
					}
				}
				catch (Exception e) {
					System.out.println(e.getMessage());

				} finally {
					try {
						br.close();
					} catch (IOException exc) {

					}
				}
		}
	}

	// Method to extract all links from a webpage
	public HashSet<String> extractAllLinks(String webUrl, String htmlLine) {
		String temp1 = htmlLine, temp2 = htmlLine;
		HashSet<String> links = new HashSet<String>();

		// fecth links in the href of tags
		while (!temp1.isEmpty() && temp1.contains("href")) {
			int start = temp1.indexOf("href=\"") + 6;
			int end = temp1.indexOf("\"", start);
			if (end == -1 || start == end)
				temp1 = temp1.substring(start);
			else if (start > 0 && end < temp1.length() && start < end) {
				String link = temp1.substring(start, end);

				// complete relative urls
				link = checkNCompleteRelativeUrls(webUrl, link);

				// check for UIC domain
				if (isUICdomain(link) && isHtml(link)) {
					String canURL = canonicalizeLink(link);
					if(urlsCrawled.contains(canURL))
					links.add(canURL);
				}
				temp1 = temp1.substring(end + 1);
			}
		}

		// fetch links in <frame>
		while (!temp2.isEmpty() && temp2.contains("frame src")) {
			int start = temp2.indexOf("frame src=\"") + 5;
			int end = temp2.indexOf("\"", start);
			if (end == -1 || start == end)
				temp2 = temp2.substring(start);
			else if (start > 0 && end < temp2.length() && start < end) {
				String link = temp2.substring(start, end);

				// complete relative urls
				link = checkNCompleteRelativeUrls(webUrl, link);

				// check for UIC domain
				if (isUICdomain(link) && isHtml(link)) {
					String canURL = canonicalizeLink(link);
					if(urlsCrawled.contains(canURL))
					links.add(canURL);
				}
				temp2 = temp2.substring(end + 1);
			}
		}
		return links;

	}

	// Method to check if input url is of UIC domain
	public static boolean isUICdomain(String link) {
		if (link.matches("^(https?)://[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*uic[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*"))
			return true;
		return false;
	}

	// Method to check if input url is a html page
	public static boolean isHtml(String link) {
		String lastPartOfURl = link.substring(link.lastIndexOf("/") + 1);
		if (!lastPartOfURl.contains(".") || (lastPartOfURl.contains(".") && (lastPartOfURl.endsWith("html")
				|| lastPartOfURl.endsWith("htm") || lastPartOfURl.endsWith("shtml") || lastPartOfURl.endsWith("edu"))))
			return true;
		return false;
	}

	// Method to check and complete relative urls
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

	// Method to canonicalize a link
	public static String canonicalizeLink(String link) {

		if (link.contains("#"))
			link = link.substring(0, link.indexOf("#"));

		if (link.endsWith("/"))
			link = link.substring(0, link.length() - 1);

		return link;
	}

	// Method to run PageRank algorithm on the web graph
	public void runPageRankAlgorithm() {

		int noOfWebpagesInGraph = webpagesGraph.size();

		// Initialize PageRank scores
		for (Object url : webpagesGraph.keySet()) {
			webpageScoresMap.put((String) url, Double.valueOf(1.0 / noOfWebpagesInGraph));
		}

		HashMap<String, Double> newWebpageScoresMap = new HashMap<String, Double>();

		// Iteratively computing PageRank scores for each word in the document until
		// convergence or 10 iterations
		for (int i = 0; i < 10; i++) {
			newWebpageScoresMap = new HashMap<String, Double>();
			Double dampingFactor = 0.85, pi = Double.valueOf(1.0 / noOfWebpagesInGraph);
			for (Entry<String, Pair<Integer, HashSet<String>>> webpageNadjList : webpagesGraph.entrySet()) {
				Double sumOfEachTerm = 0.0;
				for (String link : webpageNadjList.getValue().getValue()) {
					Integer out = webpagesGraph.get(link).getKey();
					Double scoreOfWebPage = webpageScoresMap.get(link);
					sumOfEachTerm += scoreOfWebPage / out;
				}

				Double pageRankScore = dampingFactor * sumOfEachTerm + (1.0 - dampingFactor) * pi;
				newWebpageScoresMap.put(webpageNadjList.getKey(), pageRankScore);
			}

		}
		webpageScoresMap = newWebpageScoresMap;

	}

}
