package entities;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QueryProcessor class to process the given query fetch ranked list of relevant
 * urls based on cosine similarity measure, page rank scores, acronym expansion,
 * query-(url name) similarity and home page or authority relevance to query.
 *
 * @author Nivetha Babu
 */

public class QueryProcessor {

	// Hashmap to store inveretd index built by Vector Space Retrieval System
	public HashMap<String, Pair<Double, HashMap<String, Double>>> invertedIndex = new HashMap<String, Pair<Double, HashMap<String, Double>>>();

	// Hashmap to stores urls and page rank scores computed by the Page Ranker
	LinkedHashMap<String, Double> webpageScoresMap = new LinkedHashMap<String, Double>();

	// Set to store retrieved ranked relevant urls
	LinkedHashSet<String> relvDocs = new LinkedHashSet<String>();

	// Preprocessor intsance
	Preprocessor preprocessor;

	boolean normailzeTF = true;
	private int totalNoOfDocs = 0;

	// HashSet to store unique urls to avoid duplicates
	HashSet<String> uniqUrls = new HashSet<String>();

	// Hashmap of common acronyms in UIC Domain
	HashMap<String, String> acronymMap = new HashMap<String, String>();

	// Hashmap of urls and corresponding webpage vector lengths
	private HashMap<String, Double> docLengths = new HashMap<String, Double>();

	// Method to fetch inverted index built by the VectorSpaceRS class
	public void buildInvertedIndex(String invertedIndexFolder) {
		File folder = new File(invertedIndexFolder);
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				buildInvertedIndex(file.getName());
			} else {
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(file));
					String line;
					while ((line = br.readLine()) != null) {
						if (line.contains("=")) {
							int indexOfEq = line.indexOf("=");
							String token = line.substring(0, indexOfEq);
							String idf = line.substring(indexOfEq + 1, line.indexOf("{") - 1);
							HashMap<String, Double> map = new HashMap<String, Double>();
							String entries = line.substring(line.indexOf("{") + 1, line.length() - 2);
							if (entries.contains(",")) {
								String[] hashMapEntries = entries.split(",");
								for (String keyValue : hashMapEntries) {
									if (keyValue.contains("|=")) {
										int indexOfSep = keyValue.indexOf("|=");
										String url = keyValue.substring(0, indexOfSep).trim();
										String tf = keyValue.substring(indexOfSep + 2);
										map.put(url, Double.valueOf(tf));
										uniqUrls.add(url);
									}
								}
							} else {
								if (entries.contains("|=")) {
									int indexOfSep = entries.indexOf("|=");
									String url = entries.substring(0, indexOfSep).trim();
									String tf = entries.substring(indexOfSep + 2);
									map.put(url, Double.valueOf(tf));
									uniqUrls.add(url);
								}
							}
							invertedIndex.put(token,
									new Pair<Double, HashMap<String, Double>>(Double.valueOf(idf), map));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();

				}
				finally {
					if(br!=null)
						try {
							br.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		}
		totalNoOfDocs = uniqUrls.size();
	}

	// Method to fetch urls and page rank scores computed by the PageRanker class
	public void fetchPageRanks(String pageRanksFolder) {
		File folder = new File(pageRanksFolder);
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				fetchPageRanks(file.getName());
			} else {
				BufferedReader br = null;
				try {
					if (file.getName().endsWith(".txt")) {
						br = new BufferedReader(new FileReader(file));
						String line;
						while ((line = br.readLine()) != null) {
							if (line.contains("=")) {
								int indexOfEq = line.lastIndexOf("=");
								String url = line.substring(0, indexOfEq);
								String score = line.substring(indexOfEq + 1);

								webpageScoresMap.put(url, Double.valueOf(score));
							}
						}
					}

				} catch (Exception e) {
					System.out.println(e.getMessage());

				}
				finally {
					if(br!=null)
						try {
							br.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			}
		}
		webpageScoresMap = rankDocuments(webpageScoresMap);
	}

	// Method to compute the length of each webpage to be used to compute Cosine
	// similarity
	public void computeDocumentLengths() {
		for (Object token : invertedIndex.keySet()) {
			Pair<Double, HashMap<String, Double>> pair = (Pair<Double, HashMap<String, Double>>) invertedIndex
					.get(token);
			Double idf = Math.log((double) totalNoOfDocs / pair.getKey()) / Math.log(2);
			invertedIndex.put((String) token, new Pair<Double, HashMap<String, Double>>(idf, pair.getValue()));
			HashMap<String, Double> docFreMap = pair.getValue();
			for (String docID : docFreMap.keySet())
				docLengths.put(docID,
						(double) docLengths.getOrDefault(docID, 0.0) + Math.pow(docFreMap.get(docID) * idf, 2));
		}
		for (Object docID : docLengths.keySet()) {
			double lenSqr = (double) docLengths.get(docID);
			docLengths.put((String) docID, Math.sqrt(lenSqr));
		}
	}

	// Method to process given query using stopwords file and acronyms file
	public LinkedHashSet<String> processQuery(String query, String stopwordsFile, String acronymsFile) {
		try {

			// Build acronyms map
			buildAcronymMap(acronymsFile);

			// Preprocess query to perform exactly the same text operations as those
			// performed on the webpages in the collection
			Pair<Pair<Double, HashMap<String, Double>>, String> result = preprocessQuery(query, stopwordsFile);
			query = result.getValue();
			HashMap<String, Double> queryWordsFreq = (HashMap<String, Double>) result.getKey().getValue();
			Double maxFreqInQuery = (Double) result.getKey().getKey();

			// Compute the length of the query to be used to compute Cosine similarity
			Pair<Double, HashMap<String, Double>> result1 = computeQueryLen(queryWordsFreq, maxFreqInQuery);
			Double queryLen = (Double) result1.getKey();
			queryWordsFreq = (HashMap<String, Double>) result1.getValue();

			// Retrieve webpages relevant to the query using Cosine similarity measure,
			// page rank scores, acronym expansion, query-(url name) similarity and
			// home page or authority relevance to query.
			relvDocs = retrieveRelevantDocs(queryWordsFreq, queryLen, query);

			// Print retrieved ranked urls to the console output for debugging
			if(!relvDocs.isEmpty()) {
				System.out.println("\nSearch Engine retrieved "+relvDocs.size() +" urls relevant to the query.");
				
				System.out.println("\nTop 20 of the retrieved urls relevant to the query : "	
				+ "\n----------------------------------------------------");
				int ct =1;
				Iterator<String> itr = relvDocs.iterator();
				while (ct < 21 && itr.hasNext()) {
					System.out.println(itr.next());
					ct++;
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return relvDocs;
	}

	// Private method to remove stopwords, perform stemming and again remove any
	// remaining stopwords
	private HashMap<String, Double> getTokensForIndexing(String fileName) {
		preprocessor.removeStopWords(fileName);
		preprocessor.stem();
		preprocessor.removeStopWords(fileName);
		return (HashMap<String, Double>) preprocessor.tokenFreqMap;
	}

	// Private method to perform all the text preprocessing operations on the query
	// that were done on the webpages in the collection
	private Pair<Pair<Double, HashMap<String, Double>>, String> preprocessQuery(String query, String stopwordsFile) {
		HashMap<String, Double> queryWordsFreq;
		Double maxFreqInQuery;
		preprocessor = new Preprocessor();
		for (String queryWord : query.toLowerCase().split(" ")) {
			if (queryWord.equals("uic") && query.length() > 5) {
				query = query.replaceAll("uic", " ");
				continue;
			}
			if (acronymMap.containsKey(queryWord)) {
				query += " " + expandAcronym(queryWord);
			}
		}
		preprocessor.tokenize(query);
		queryWordsFreq = getTokensForIndexing(stopwordsFile);
		maxFreqInQuery = preprocessor.getMaxFreq();
		return new Pair<Pair<Double, HashMap<String, Double>>, String>(
				new Pair<Double, HashMap<String, Double>>(maxFreqInQuery, queryWordsFreq), query);

	}

	// Method to expand given acronym
	private String expandAcronym(String acronym) {
		return acronymMap.getOrDefault(acronym, "");
	}

	// Method to build acronym map from file
	private void buildAcronymMap(String acronymsFile) {
		acronymMap = new HashMap<String, String>();
		File acronFile = new File(acronymsFile);
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(acronFile));
			String line;
			while ((line = br.readLine()) != null) {
				if (line.contains("=")) {
					String[] keyValue = line.split("=");
					acronymMap.put(keyValue[0], keyValue[1]);
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());

		}
		finally {
			if(br!=null)
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

	}

	// Private method to compute the length of the query to be used to compute
	// Cosine similarity
	private Pair<Double, HashMap<String, Double>> computeQueryLen(HashMap<String, Double> queryWordsFreq,
			Double maxFeq) {
		Double length = 0.0;
		for (String word : queryWordsFreq.keySet()) {
			Double tf;
			if (normailzeTF) {
				tf = queryWordsFreq.get(word) / maxFeq;
				queryWordsFreq.put(word, tf);
			} else
				tf = queryWordsFreq.get(word);
			Pair<Double, HashMap<String, Double>> indexPair = (Pair<Double, HashMap<String, Double>>) invertedIndex
					.getOrDefault(word, new Pair<Double, HashMap<String, Double>>(0.0, new HashMap<>()));
			length += Math.pow((double) indexPair.getKey() * tf, 2);
		}
		return new Pair<>(Math.sqrt(length), queryWordsFreq);
	}

	// Private method to retrieve relevant documents for a query based on cosine
	// similarity measure,
	// page rank scores, acronym expansion, query-(url name) similarity and
	// home page or hub relevance to query.
	private LinkedHashSet<String> retrieveRelevantDocs(HashMap<String, Double> queryWordsFreq, Double queryLen,
			String query) {
		LinkedHashSet<String> relvDocs = new LinkedHashSet<String>();

		// Compute Cosine similarity of each webpage containing atleast one query word
		LinkedHashMap<String, Double> docIDCosSimMap = computeCosSim(queryWordsFreq, queryLen);

		// Rank the retrieved documents in descending order of Cosine Similarity measure
		docIDCosSimMap = rankDocuments(docIDCosSimMap);

		// Perform acronym expansion
		LinkedList<String> urls = new LinkedList<String>();
		for (String queryWord : query.split(" ")) {
			if (acronymMap.containsKey(queryWord)) {
				Iterator<String> itr = docIDCosSimMap.keySet().iterator();
				while (itr.hasNext()) {
					String url = itr.next();
					String regx1 = "^(https?)://[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*[^a-z^A-Z]+(" + queryWord + ")";
					String regx2 = "^(https?)://(" + queryWord + ")[^a-z^A-Z]+[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*";
					String regx3 = "^(https?)://[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*[^a-z^A-Z]+(" + queryWord
							+ ")[^a-z^A-Z]+[a-zA-Z0-9+&@#/%?=~_|!:,.;-]*";

					if (url.matches(regx1) || url.matches(regx2) || url.matches(regx3))
						urls.add(url);

				}
			}

		}

		// Add relevant urls after acronym expansion
		if (!urls.isEmpty()) {
			Collections.sort(urls, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					return Integer.valueOf(o1.length()).compareTo(o2.length());
				}
			});

			relvDocs = new LinkedHashSet<String>();

			Iterator<String> itr = urls.iterator();
			while (relvDocs.size() <= 5 && itr.hasNext())
				relvDocs.add(itr.next());
		}

		// Add relevant urls based on cosine similarity measure
		Iterator<String> itr = docIDCosSimMap.keySet().iterator();
		while (relvDocs.size() <= 5 && itr.hasNext())
			relvDocs.add(itr.next());

		// Add relevant urls based on page rank scores
		LinkedHashMap<String, Double> urlnScoreMap = new LinkedHashMap<String, Double>();
		for (String url : docIDCosSimMap.keySet()) {
			if (webpageScoresMap.containsKey(url))
				urlnScoreMap.put(url, webpageScoresMap.get(url));

		}

		urlnScoreMap = rankDocuments(urlnScoreMap);
		relvDocs.addAll(urlnScoreMap.keySet());
		return relvDocs;
	}

	// Private method to compute the cosine similarity of webpages containing
	// atleast one query word
	private LinkedHashMap<String, Double> computeCosSim(HashMap<String, Double> queryWordsFreq, Double queryLen) {
		LinkedHashMap<String, Double> docIDCosSimMap = new LinkedHashMap<String, Double>();
		for (String word : queryWordsFreq.keySet()) {
			if (invertedIndex.containsKey(word)) {
				Pair<Double, HashMap<String, Double>> idfDocIdTfPairs = (Pair<Double, HashMap<String, Double>>) invertedIndex
						.get(word);
				Double idf = (double) idfDocIdTfPairs.getKey();
				HashMap<String, Double> docIdTfMap = (HashMap<String, Double>) idfDocIdTfPairs.getValue();
				for (Object docID : docIdTfMap.keySet()) {
					docIDCosSimMap.put((String) docID, (double) docIDCosSimMap.getOrDefault(docID, 0.0)
							+ (double) docIdTfMap.get(docID) * idf * queryWordsFreq.get(word) * idf);
				}

			}
		}

		for (Object docID : docIDCosSimMap.keySet()) {
			docIDCosSimMap.put((String) docID,
					(double) docIDCosSimMap.get(docID) / (queryLen * (double) docLengths.get(docID)));
		}
		return docIDCosSimMap;
	}

	// Method to sort the docIDCosSimMap in descending order of Cosine Similarity
	private LinkedHashMap<String, Double> rankDocuments(LinkedHashMap<String, Double> docIDCosSimMap) {
		List<Map.Entry<String, Double>> entries = new LinkedList<>(docIDCosSimMap.entrySet());
		entries.sort((e1, e2) -> -e1.getValue().compareTo(e2.getValue()));
		docIDCosSimMap = entries.stream()
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
		return docIDCosSimMap;
	}

}
