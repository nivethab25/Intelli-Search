# Intelli-Search
An Intelligent Web Search for uic.edu domain

This web search engine is built of several information retrieval components 
that contribute towards its functionality and intelligence. Each of these 
components processes the webpages’ URLs and provides them to the other components 
for further refining of the search results. The main components are: Web Crawler, 
Vector Space Retrieval System and Page Ranker.
Apart from these main components this search engine also involves other minor 
intelligent components that further refine its search results. 
They are: acronym expansion, query- (URL name) similarity and home page or authority relevance to query.
The search engine also uses other text preprocessing components like stopword eliminator, 
Porter Stemmer and URL processing components like link canonicalizer, URL filter, domain identifier, html page identifier.
Since this web search engine is built as a dynamic Maven- based web project using Java 8, 
it can be run using simple Maven commands in the terminal (or shell) on any 
operating system and doesn’t require any particular IDE supporting dynamic web projects
that involves setting up local servers like Apache Tomcat.
web search engine for query “graduate assistantships”
All the application settings and library dependencies have been included in the pom.xml file 
contained in the project’s root directory which makes it platform and IDE independent and hence adds to its portability.
