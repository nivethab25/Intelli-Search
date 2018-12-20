package entities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Preprocessor class to perform all the text processing operations like
 * tokenization, stopword removal and stemming with the help of PorterStemmer
 * class.
 *
 * @author Nivetha Babu
 */

public class Preprocessor {

	public HashMap<String, Double> tokenFreqMap = new HashMap<String, Double>();
	private LinkedList<String> stopList = new LinkedList<String>();

	// Method to tokenize on whitespace after removing punctuations, numbers, words
	// with 1 or 2 characters in length and converting to lowercase
	public void tokenize(String line) {
		String[] tokens;
		if (!line.matches("\\<.*?>")) {
			tokens = line.replaceAll("\\p{P}", "").replaceAll("\\d", "").toLowerCase().split("\\s+");
			for (String token : tokens) {
				if (token.length() > 2)
					tokenFreqMap.put(token, ((Double) tokenFreqMap.getOrDefault(token, 0.0)) + 1);
			}
		}
	}

	// Method to stem the given string using the PorterStemmer
	public void stem() {
		PorterStemmer stemmer = new PorterStemmer();
		HashMap<String, Double> reducedTokenFreqMap = new HashMap<String, Double>();
		for (Object token : tokenFreqMap.keySet()) {
			String s_token = (String) token;
			String stem = stemmer.stripAffixes(s_token);
			if (stem.length() > 2) {
				if (!reducedTokenFreqMap.containsKey(stem))
					reducedTokenFreqMap.put(stem, tokenFreqMap.getOrDefault(s_token, (double) 0));
				else
					reducedTokenFreqMap.put(stem, (double) tokenFreqMap.getOrDefault(s_token, (double) 0)
							+ (double) reducedTokenFreqMap.get(stem));
			}
		}
		tokenFreqMap = reducedTokenFreqMap;
	}

	// Method to remove stopwords
	public void removeStopWords(String stopwordsFile) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(stopwordsFile)));
			String line;
			while ((line = br.readLine()) != null) {
				((LinkedList<String>) stopList).addLast(line);
			}

			for (int i = 0; i < stopList.size(); i++) {
				String stopword = (String) stopList.get(i);
				if (tokenFreqMap.containsKey(stopword)) {
					tokenFreqMap.remove(stopword);
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

	// Method to return the frequency of the most frequent word in the current
	// document
	public double getMaxFreq() {
		return (double) Collections.max(tokenFreqMap.values());
	}

}
