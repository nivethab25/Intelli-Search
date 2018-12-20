package entities;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * VectorSpaceRS class to implement the Vector Space Retrieval System.This
 * program uses the Preprocessor and PorterStemmer classes to perform all the
 * text processing operations.
 *
 * @author Nivetha Babu
 */

public class VectorSpaceRS {
	// Inverted index to store each word alongwith its idf and set of all documents
	// and corresponding tf
	public HashMap<String, Pair<Double, HashMap<String, Double>>> invertedIndex = new HashMap<String, Pair<Double, HashMap<String, Double>>>();

	private Preprocessor preprocessor;
	private File collectionsFolder;
	private static boolean normailzeTF;
	private int totalNoOfDocs = 0;
	private double maxFreqInFile = 0.0;

	// Hashmap of document IDs and corresponding document vector lengths
	private HashMap<String, Double> docLengths = new HashMap<String, Double>();

	// Method to read each file in the collection and tokenize its title and text
	// and insert the tokens in inverted index
	public void buildInvertedIndex(String collection, String stopwordsFilename, boolean normailzeTf) {
		HashMap<String, Double> tokenFreqMap;
		collectionsFolder = new File(collection);
		normailzeTF = normailzeTf;
		for (final File file : collectionsFolder.listFiles()) {
			String url = "";
			maxFreqInFile = 0.0;
			preprocessor = new Preprocessor();
			++totalNoOfDocs;
			if (file.isDirectory()) {
				buildInvertedIndex(collection, stopwordsFilename, normailzeTF);
			} else {
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(file));
					String line;
					while ((line = br.readLine()) != null) {
						if (line.contains("|||||")) {
							String[] firstLine = line.replaceAll(" ", "").split("[|||||]");
							url = canonicalizeLink(firstLine[0]);
						}
						preprocessor.tokenize(line);

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
			tokenFreqMap = getTokensForIndexing(stopwordsFilename);
			maxFreqInFile = preprocessor.getMaxFreq();
			updateInvertedIndex(url.trim() + "|", maxFreqInFile, tokenFreqMap);
		}

	}

	// Method to compute the length of each document to be used to compute Cosine
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

	// Private method to remove stopwords, perform stemming and again remove any
	// remaining stopwords
	private HashMap<String, Double> getTokensForIndexing(String fileName) {
		preprocessor.removeStopWords(fileName);
		preprocessor.stem();
		preprocessor.removeStopWords(fileName);
		return preprocessor.tokenFreqMap;
	}

	// Private method to build the inverted index for each document that has been
	// read
	private void updateInvertedIndex(String docID, double maxFreq, HashMap<String, Double> tokenFreqMap) {
		for (Object token : tokenFreqMap.keySet()) {
			String s_token = (String) token;
			if (!invertedIndex.containsKey(s_token)) {
				HashMap<String, Double> map = new HashMap<String, Double>();
				if (normailzeTF)
					map.put(docID, tokenFreqMap.get(token) / maxFreq);
				else
					map.put(docID, tokenFreqMap.get(token));
				Pair<Double, HashMap<String, Double>> pair = new Pair<Double, HashMap<String, Double>>(new Double(1),
						map);
				invertedIndex.put(s_token, pair);
			} else {
				Pair<Double, HashMap<String, Double>> oldPair = (Pair<Double, HashMap<String, Double>>) invertedIndex
						.get(s_token);
				if (normailzeTF)
					oldPair.getValue().put(docID, tokenFreqMap.get(token) / maxFreq);
				else
					oldPair.getValue().put(docID, tokenFreqMap.get(token));
				Double newKey = oldPair.getKey() + 1;
				invertedIndex.put(s_token, new Pair<Double, HashMap<String, Double>>(newKey, oldPair.getValue()));

			}

		}
	}

	// Method to canonicalize a link
	public static String canonicalizeLink(String link) {

		if (link.contains("#"))
			link = link.substring(0, link.indexOf("#"));

		if (link.endsWith("/"))
			link = link.substring(0, link.length() - 1);

		return link;
	}

}
