<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>Web Search Results</title>
<meta name="viewport" content="width=device-width, initial-scale=1.0">

<link href="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css" rel="stylesheet">   
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js"></script>
<link rel="stylesheet" href="http://cdn.datatables.net/1.10.2/css/jquery.dataTables.min.css">
<script type="text/javascript" src="http://cdn.datatables.net/1.10.2/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" src="http://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/js/bootstrap.min.js"></script>

</head>

<body>
	<%@ page import="entities.AppContext"%>
	<%@ page import="java.util.*"%>

	<form method="post"
		style="background-image: url('images/inner2.png'); background-repeat:no-repeat; background-size: cover; position: absolute; top: 0; left: 0; min-width: 100%; min-height: 100%;  width: 100%; height: auto;">
		<br> <br> <br> <br>
		<div class="container">
	<div class="row">
      <div class="table-responsive">
        <table class="table table-hover" id="myTable">
				<thead>
					<tr>
						<th>Relevant Pages in UIC domain</th>
						</tr>
				</thead>
				<tbody id="myTableBody">
					<%
						try {
							
							for (String url: AppContext.relvUrls) {
							
					%>
					<tr>
						<td><a href = <%=url%>><%=url%></a><br></td>
						</tr>
					<%
						}
								
						
			} catch (Exception e) {
				e.printStackTrace();
			}
		%>
		</tbody>
			</table>
			</div>
      </div>
		</div>
		<div>
			<input type="button" style="margin-left: 15%; margin-top: 2%"
				onClick="window.location.href= 'index.html'"
				value="Return to Home" />
		</div>
		
	</form>
</body>
<script>
$(document).ready(function(){
    $('#myTable').dataTable({
    	  "ordering": false
    } );
});
</script>
</html>