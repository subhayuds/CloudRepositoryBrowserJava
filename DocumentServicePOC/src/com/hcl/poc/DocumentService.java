package com.hcl.poc;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sap.ecm.api.ServiceException;
import com.sap.security.um.user.PersistenceException;
import com.sap.security.um.user.User;
import com.sap.security.um.user.UserProvider;

/**
 * Servlet implementation class DocumentService
 */
@WebServlet(description = "Document Service", urlPatterns = { "/DocumentService" })
public class DocumentService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// upload settings
    private static final int MEMORY_THRESHOLD   = 1024 * 1024 * 3;  // 3MB
    private static final int MAX_FILE_SIZE      = 1024 * 1024 * 40; // 40MB
    private static final int MAX_REQUEST_SIZE   = 1024 * 1024 * 50; // 50MB
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DocumentService() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.err.println("Inisde GET");
		String operationType = "";
		String documentID = "";
		JSONObject userData = new JSONObject();
		JSONObject jsonObject = new JSONObject();
		JSONObject errorJson = new JSONObject();
		
		try {
			JSONObject repositorySpec = new JSONObject((request.getParameterValues("repoDetails[]"))[0]);
			CMISHelper.REPO_NAME = repositorySpec.getString("REPO_NAME");
			System.err.println("Repository: " + CMISHelper.REPO_NAME);
			CMISHelper.REPO_PRIVATEKEY = repositorySpec.getString("REPO_KEY");
			System.err.println("Repository Key: " + CMISHelper.REPO_PRIVATEKEY);
			CMISHelper.REPO_APPFOLDER = repositorySpec.getString("FOLDER_PATH");
			System.err.println("FOLDER PATH: " + CMISHelper.REPO_APPFOLDER);
			operationType = repositorySpec.getString("OPERATION_TYPE");
			System.err.println("OPERATION TYPE: " + operationType);
			
			errorJson.put("ERROR_MSG", "");
		} catch (JSONException e) {
			System.err.println(e);
		}

		CMISHelper cmisHelper = new CMISHelper();
		
		switch(operationType) {
		case "GET_LIST":
			cmisHelper = new CMISHelper();
			List<DocumentDTO> documentList = null;
			try {
				documentList = cmisHelper.getDocumentsList();
			} catch (CmisObjectNotFoundException | ServiceException | NamingException ex) {
				System.err.println(ex);
				errorJson = this.handleException(ex);
			} catch (Exception ex) {
				System.err.println(ex);
				errorJson = this.handleException(ex);
			}
			
			jsonObject = this.convertDocumentListToJSON(documentList);
			try {
				jsonObject.put("error", errorJson);
			} catch (JSONException ex) {
				System.err.println(ex);
			}
		
			response.setContentType("application/json");
			response.getWriter().write(jsonObject.toString());
			
			break;
		
		case "DOWNLOAD_DOC":
			try {
				documentID = userData.getString("DOC_ID");
				userData = new JSONObject((request.getParameterValues("userData[]"))[0]);
				cmisHelper.streamOutDocument(response, documentID);
			} catch (CmisObjectNotFoundException | NamingException ex) {
				errorJson = this.handleException(ex);
				response.setContentType("application/json");
				response.getWriter().write(jsonObject.toString());
			} catch (Exception ex) {
				errorJson = this.handleException(ex);
				response.setContentType("application/json");
				response.getWriter().write(jsonObject.toString());
			}
			break;
			
		default:
			
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.err.println("Inisde POST");
		String operationType = "";
		String folderPath = "";
		String folderName = "";
		String parentID = "";
		CMISHelper cmisHelper = new CMISHelper();
		JSONObject repositorySpec = new JSONObject();
		JSONObject userData = new JSONObject();
		JSONObject jsonObject = new JSONObject();
		JSONObject errorJson = new JSONObject();
		
		try {
			if (ServletFileUpload .isMultipartContent(request)) {
				operationType = request.getParameter("OPERATION_TYPE");
				folderPath = request.getParameter("FOLDER_PATH");
			} else {
				repositorySpec = new JSONObject((request.getParameterValues("repoDetails[]"))[0]);
				operationType = repositorySpec.getString("OPERATION_TYPE");
				folderPath = repositorySpec.getString("FOLDER_PATH");
				parentID = repositorySpec.getString("PARENT_ID");
				System.err.println("OPERATION TYPE: " + operationType);
			}
		
			errorJson.put("ERROR_MSG", "");
		} catch (JSONException ex) {
			System.err.println(ex);
		}
		
		switch(operationType) {
		case "CREATE_FOLDER":
			try {
				userData = new JSONObject((request.getParameterValues("userData[]"))[0]);
				folderName = userData.getString("FOLDER_NAME");
				cmisHelper.createFolder(folderName, parentID);
			} catch (CmisObjectNotFoundException | ServiceException | NamingException ex) {
				System.err.println(ex);
				errorJson = this.handleException(ex);
			} catch (Exception ex) {
				System.err.println(ex);
				errorJson = this.handleException(ex);
			}
			
			try {
				jsonObject.put("error", errorJson);
			} catch (JSONException ex) {
				System.err.println(ex);
			}
			
			response.setContentType("application/json");
			response.getWriter().write(jsonObject.toString());
			
			break;
			
		case "UPLOAD_FILE":
			// checks if the request actually contains upload file
	        if (!ServletFileUpload .isMultipartContent(request)) {
	            // if not, we stop here
	            System.err.println("Error: Form must has enctype=multipart/form-data.");
	            return;
	        }
	        System.err.println("Here 2");
	        // configures upload settings
	        DiskFileItemFactory factory = new DiskFileItemFactory();
	        // sets memory threshold - beyond which files are stored in disk
	        factory.setSizeThreshold(MEMORY_THRESHOLD);
	        // sets temporary location to store files
	        factory.setRepository(new File(System.getProperty("java.io.tmpdir")));
	 
	        ServletFileUpload  upload = new ServletFileUpload(factory);
	        System.err.println("Here 3");
	        // sets maximum size of upload file
	        upload.setFileSizeMax(MAX_FILE_SIZE);
	         
	        // sets maximum size of request (include file + form data)
	        upload.setSizeMax(MAX_REQUEST_SIZE);
	        
	        // parses the request's content to extract file data
            List<FileItem> formItems;
			try {
				formItems = upload.parseRequest(request);
				if (formItems != null && formItems.size() > 0) {
	                // iterates over form's fields
	                for (FileItem item : formItems) {
	                    if (!item.isFormField()) {
	                    	CMISHelper.REPO_APPFOLDER = folderPath;
	                    	cmisHelper.addDocument(item,this.getLoggedInUser());
	                    }
	                }
	            }
			} catch (FileUploadException | CmisObjectNotFoundException | ServiceException | NamingException ex) {
				errorJson = this.handleException(ex);
			} catch(Exception ex) {
				System.err.println(ex);
				errorJson = this.handleException(ex);
			}
			
			try {
				jsonObject.put("error", errorJson);
			} catch (JSONException ex) {
				System.err.println(ex);
			}
			
			response.setContentType("application/json");
			response.getWriter().write(jsonObject.toString());

			break;
		
		case "DELETE_CONTENT":
			System.err.println("Inside case DELETE_CONTENT");
			System.err.println(request.getParameterValues("deleteData[]"));
			if(request.getParameterValues("deleteData[]") != null) {
				System.err.println("Length of JSOn Array: " + (request.getParameterValues("deleteData[]")).length);
				String jsonData[] = request.getParameterValues("deleteData[]");
				JSONObject deleteJsonObject = null;
				for(int i=0;i<jsonData.length;i++) {
					try {
						deleteJsonObject = new JSONObject(jsonData[i]);
						if(deleteJsonObject.getString("CONTENT_TYPE").equalsIgnoreCase("FOLDER")) {
							cmisHelper.deleteFolder(deleteJsonObject.getString("CONTENT_PATH") + "/" + deleteJsonObject.getString("CONTENT_NAME"));
						} else if(deleteJsonObject.getString("CONTENT_TYPE").equalsIgnoreCase("FILE")) {
							cmisHelper.deleteDocument(deleteJsonObject.getString("CONTENT_ID"));
						}
					} catch (CmisObjectNotFoundException | CmisConstraintException | ServiceException | JSONException | NamingException ex) {
						System.err.println(ex);
						errorJson = this.handleException(ex);
					} catch (Exception ex) {
						System.err.println(ex);
						errorJson = this.handleException(ex);
					}
				}
			} else
				System.err.println("NULL JSON Data");
			
			try {
				jsonObject.put("error", errorJson);
			} catch (JSONException ex) {
				System.err.println(ex);
			}
			
			response.setContentType("application/json");
			response.getWriter().write(jsonObject.toString());
				
			break;
			
		default:
			
		}
	}
	
	/**
	 * 
	 * @param documentList
	 * @return
	 */
	private JSONObject convertDocumentListToJSON(List<DocumentDTO> documentList) {
		JSONObject jsonObject = null;
		JSONObject returnJSON = null;
		JSONArray docJSONArray = new JSONArray();
		
		try {
			if(documentList == null) {
				returnJSON = new JSONObject().put("results", new JSONArray());
			} else {
				Iterator<DocumentDTO> iterator = documentList.listIterator();

				while(iterator.hasNext()) {
					DocumentDTO documentDTO = iterator.next();
					jsonObject = new JSONObject();
					jsonObject.put("FILENAME", documentDTO.getFilename());
					jsonObject.put("ID", documentDTO.getId());
					if(documentDTO.isFolder()) { 
						jsonObject.put("TYPE", "FOLDER");
						jsonObject.put("SIZE","");
					} else {
						jsonObject.put("TYPE", "FILE");
						jsonObject.put("SIZE", documentDTO.getFileLength());
					}
					jsonObject.put("MIMETYPE", documentDTO.getMimeType());
					jsonObject.put("SIZE_UNIT", documentDTO.getFileLengthUnit());
					jsonObject.put("CREATED_ON", documentDTO.getCreationDate());
					if(documentDTO.getAuthorName().contains("anonymous")) jsonObject.put("CREATED_BY", "System");
					else jsonObject.put("CREATED_BY", documentDTO.getAuthorName());
					jsonObject.put("CREATED_BY_MAIL", documentDTO.getAuthorEmail());
					jsonObject.put("DOWNLOAD_LINK", documentDTO.getDownloadLink());
					jsonObject.put("STREAM", documentDTO.getStream());
					
					docJSONArray.put(jsonObject);
				}
				
				returnJSON = new JSONObject().put("results",docJSONArray);
			}
		} catch (JSONException ex) {
			System.err.println(ex);
		} 
		
		return returnJSON;
	}
	
	/**
	 * 
	 * @return
	 */
	private User getLoggedInUser() {
		User user = null;
		try {
			InitialContext ctx = new InitialContext();
			UserProvider userProvider = (UserProvider) ctx.lookup("java:comp/env/user/Provider");
			
		     user = userProvider.getCurrentUser();
		} catch (PersistenceException ex) {
			System.err.println(ex);
		} catch (NamingException ex) {
			System.err.println(ex);
		} catch(Exception ex) {
			System.err.println(ex);
		}
		
		return user;
	}
	
	/**
	 * 
	 * @param ex
	 * @return
	 */
	private JSONObject handleException(Exception ex) {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("ERROR_MSG", ex.getMessage());
		} catch (JSONException e) {
			System.err.println(e);
		}
		
		return jsonObject;
	}
}