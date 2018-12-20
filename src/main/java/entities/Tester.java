package entities;

import javafx.util.Pair;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.*;
import java.util.Map.*;

/**
 * Tester program for the VectorSpaceRS,
 * PageRanker and
 * QueryProcessor classes. The top 20 results of search 
 * are displayed on the console output
 * 
 * @author Nivetha Babu
 */

public class Tester {

	public static void main(String args[]) {

//		VectorSpaceRS vectorRS = new VectorSpaceRS();
//		String collectionFolder, stopwordsFile;
//		Scanner sc = new Scanner(System.in);
//
//		collectionFolder = "./src/main/webapp/ComponentProcessingResults/urlContents";
//
//		stopwordsFile = "./src/main/webapp/stopwords.txt";
//
//		System.out.println(
//				"\nThe IR System is now processing the given text document collection to build inverted index.....");
//
//		System.out.println("\nDo you want to normalize the term frequencies in the inverted index?(y/n) : ");
//		boolean normailzeTF = (sc.nextLine().equalsIgnoreCase("Y")) ? true : false;
//		// Implement an indexing scheme based on the vector space model using tf-idf
//		// weighting
//		vectorRS.buildInvertedIndex(collectionFolder, stopwordsFile, normailzeTF);
//
//		// Compute lengths of each document in the collection
//		vectorRS.computeDocumentLengths();
//		PrintWriter pw = null;
//
//		try {
//
//			File invertedIndexFile = new File("./src/main/webapp/ComponentProcessingResults/invertedIndex/" + "hashmap.txt");
//
//			invertedIndexFile.getParentFile().mkdirs();
//			invertedIndexFile.createNewFile();
//			FileOutputStream out = new FileOutputStream(invertedIndexFile);
//			pw = new PrintWriter(out);
//
//			for (Object entry : vectorRS.invertedIndex.entrySet()) {
//				pw.println(entry);
//			}
//
//		} catch (Exception ex) {
//		} finally {
//			if (sc != null)
//				sc.close();
//			if (pw != null)
//				pw.close();
//		}
//	}
//
//		PageRanker pageRanker = new PageRanker();
//		String collectionFolder;
//
//		collectionFolder = "./src/main/webapp/ComponentProcessingResults/crawledPages";
//
//		System.out.println("\nProcessing the given webpage collection to build web graph.....");
//
//		// Load each webpage into a web graph
//		pageRanker.buildWebpagesGraph(collectionFolder);
//
//		// Q.2 Run PageRank Algorithm on web graph and
//		System.out.println("\nNow running PageRank algorithm on each webpage .....");
//		pageRanker.runPageRankAlgorithm();
//		PrintWriter pw = null;
//		try {
//
//			File pageRankFile = new File("./src/main/webapp/ComponentProcessingResults/pageRank/" + "pageRankScores.txt");
//
//			pageRankFile.getParentFile().mkdirs();
//			pageRankFile.createNewFile();
//			FileOutputStream out = new FileOutputStream(pageRankFile);
//			pw = new PrintWriter(out);
//
//			for (Object entry : pageRanker.webpageScoresMap.entrySet()) {
//				pw.println(entry);
//			}
//
//		} catch (Exception ex) {
//		} finally {
//			if (pw != null)
//				pw.close();
//		}
//	}

	Scanner sc = new Scanner(System.in);
	QueryProcessor queryProcessor = new QueryProcessor();
	queryProcessor.buildInvertedIndex("./src/main/webapp/ComponentProcessingResults/invertedIndex");
	queryProcessor.computeDocumentLengths();
	queryProcessor.fetchPageRanks("./src/main/webapp//ComponentProcessingResults/pageRank");
	System.out.println("Enter your query: ");
	String query = sc.nextLine();
	queryProcessor.processQuery(query,"./src/main/webapp/stopwords.txt", "./src/main/webapp/acronyms.txt");
	sc.close();
}

}
