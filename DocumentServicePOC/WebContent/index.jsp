<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
		<title>Insert title here</title>
		<script src="js/jquery.min.js"></script>
		<script src="js/jquery-1.7.1.js"></script>
		<script>
			function myClick() {
				var deleteObject = {};
				var deleteObjectArray = [];
				for(var i=0;i<5;i++) {
				    deleteObject.CONTENT_ID = "98765432" + i;
				    deleteObject.CONTENT_NAME = "CONTENT NAME " + i;
				    deleteObject.CONTENT_TYPE = "CONTENT TYPE " + i;
				    deleteObjectArray.push(JSON.stringify(deleteObject));
				}
			    
				var json=[1,2,3,4];
				urlParameters = "?REPO_NAME=TEST_REPO&REPO_KEY=TEST_REPO_KEY&OPERATION_TYPE=DELETE_CONTENT";
				var documentServiceURL = "/DocumentServicePOC/DocumentOperations" + urlParameters;

				$.ajax({
				    url:documentServiceURL,
				    type:"POST",
				    dataType:'json',
				    data: {deleteData:deleteObjectArray},
				    success:function(data){
				        alert("SUCCESS");
				    },
					error: function(oData) {
						alert("ERROR");
					}
				});
			}
		</script>
	</head>
	<body>
		<h3>Document Service:</h3>
	    Enter Folder Path: <br />
	    <form action="DocumentOperations" method="GET">
	    	<input type="text" id="OPERATION_TYPE" name="OPERATION_TYPE" value="GETLIST" size="50"/>
	    	<br/>
	        <input type="text" id="FOLDER_PATH" name="FOLDER_PATH" value="POCFolder" size="50"/>
	        <br/>
	        <input type="button" value="Show content" onClick="javascript:myClick()"/>
	    </form>
	</body>
</html>