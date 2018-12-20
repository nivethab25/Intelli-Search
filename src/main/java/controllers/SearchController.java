package controllers;

import java.io.IOException;
import java.util.LinkedHashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import entities.QueryProcessor;
import entities.AppContext;

/**
 * SearchController class to implement servlet for query search (/search)
 * request.
 *
 * @author Nivetha Babu
 */

@WebServlet("/search")
public class SearchController extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor.
	 */
	public SearchController() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// fetching query from html form
		String query = request.getParameter("query");
		System.out.println("\nEntered Query is: "+query);

		try {

			// path of component (like Vector Space Retrieval System, Page Ranker etc)
			// processing results
			String path = getServletContext().getRealPath("ComponentProcessingResults");
			
			// Query Processor instance to call query processing methods
			QueryProcessor queryProcessor = new QueryProcessor();

			// Process query to retrieve ranked list of relevant urls
			queryProcessor.buildInvertedIndex(path + "/invertedIndex");
			queryProcessor.computeDocumentLengths();
			queryProcessor.fetchPageRanks(path + "/pageRank");
			path = path.substring(0, path.indexOf("ComponentProcessingResults"));
			LinkedHashSet<String> urls = queryProcessor.processQuery(query, path + "stopwords.txt",
					path + "acronyms.txt");

			// Set success/failure of query processor
			boolean isSuccessful = urls.isEmpty() ? false : true;
			
			// Redirect to jsp view to show the ranked list of retrieved urls on success
			if (isSuccessful) {
				AppContext.relvUrls = urls;
				response.sendRedirect("results.jsp");
			}

			// Redirect to same page on failure
			else {
				System.out.println("\nNo relevant urls for query found! Redirecting to Search Engine's home page....");
				response.sendRedirect("index.html");
			}
		} catch (Exception e) {
			System.out.println("\n");
			e.printStackTrace();
		}
	}

}
