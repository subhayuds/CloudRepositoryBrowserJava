package com.hcl.poc;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisBaseException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisConstraintException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisNameConstraintViolationException;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

import com.sap.ecm.api.EcmService;
import com.sap.ecm.api.RepositoryAlreadyExistsException;
import com.sap.ecm.api.RepositoryOptions;
import com.sap.ecm.api.RepositoryOptions.Visibility;
import com.sap.ecm.api.ServiceException;
import com.sap.security.um.user.User;

public class CMISHelper {
    public static Session cmisSession = null;
    // The folder name for the repository of all documents of your application
    public static String REPO_APPFOLDER = "POCFolder";
    // This is the unique name of that repository
    // REMEMBER: ON THE TRIAL ACCOUNT THE QUOTA FOR DOC SERVICE REPOS IS 3!
    // SO MAKE USE OF THEM WISELY AND REMEMBER THE  REPO_NAME!!!
    public static String REPO_NAME = "POCRepository";
    // This is the secret key that your application uses to access the repository.
    // DON'T FORGET THIS KEY!
    public static String REPO_PRIVATEKEY = "POCRepository_key";
    // Maximum size of a file (in Bytes) that you can upload at a time to your repository
    //static private int MAX_FILE_SIZE = 10000000;
    // The prefix for the download URL of a file
    private static String SERVLET_DOWNLOAD_PREFIX = "?OPERATION_TYPE=DOWNLOAD_DOC&DOCID=";
    // The directory to store the uploaded files temporarily
    public static String TEMP_UPLOAD_DIR = "uploads";

	/**
     * Adds a document to the folder of your application
     *
     * @param file to be imported
	 * @throws NamingException 
	 * @throws ServiceException 
	 * @throws CmisObjectNotFoundException 
     */
    public void addDocument(FileItem item, User user) throws IOException, CmisObjectNotFoundException, CmisNameConstraintViolationException, ServiceException, NamingException, Exception {
        // create a new file in the root folder
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, item.getName());
        if(user != null) properties.put(PropertyIds.CREATED_BY, user.getName());
        properties.put(PropertyIds.CREATION_DATE, System.currentTimeMillis());
        // Try to identify the mime type of the file
        String mimeType = URLConnection.guessContentTypeFromName(item.getName());
        if (mimeType == null || mimeType.length() == 0) {
            mimeType = "application/octet-stream";
        }
        
        InputStream stream = item.getInputStream();
        Session session = getRepositorySession();
        ContentStream contentStream = session.getObjectFactory().createContentStream(item.getName(), item.getSize(), mimeType, stream);
        
        try {
            Folder yourFolder = getFolderOfYourApp(REPO_APPFOLDER);
            yourFolder.createDocument(properties, contentStream, VersioningState.NONE);
        } catch (CmisNameConstraintViolationException ex) {
            throw ex;
        }
        stream.close();
    }
    
    /**
     *
     * @return a list of all documents that are in the folder of your repository
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    public List<DocumentDTO> getDocumentsList() throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
        // Get the repository folder of your app
    	System.err.println("Folder Path: " + REPO_APPFOLDER);
        Folder yourFolder = getFolderOfYourApp(REPO_APPFOLDER);
        ItemIterable<CmisObject> children = yourFolder.getChildren();
        System.err.println("Folder content size: " + yourFolder.getChildren());
        List<Document> docs = new ArrayList<Document>();
        List<Folder> folders = new ArrayList<Folder>();
        
        for (CmisObject o : children) {
            // Only add the object if it is a Document
            if (o instanceof Document) {
                Document doc = (Document) o;
                docs.add(doc);
            } else if(o instanceof Folder) {
            	Folder fol = (Folder) o;
                folders.add(fol);
            }
        }
        
        if (docs.size() > 0 || folders.size() > 0) {
        	System.err.println("Folder or Docs List size > 0");
            return convertCmisDocsToMyDocsDTOs(docs,folders);
        } else {
        	System.err.println("Both List size 0");
            return null;
        }
    }
    
    /**
     * This method deletes a folder via it's name
     * 
     * @param folderName
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    public void deleteFolder(String folderName) throws CmisObjectNotFoundException, CmisConstraintException, ServiceException, NamingException, Exception {
        // Get the session
        Session session = getRepositorySession();
        if (session == null) {
            throw new IllegalArgumentException("Session must be set, but is null!");
        }
        Folder folder = getFolderOfYourApp(folderName);
        folder.delete(true);
    }
    
    /**
     * This method deletes a file via it's document id
     *
     * @param documentId the document id of the file in the CMIS system
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    public void deleteDocument(String documentId) throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
        // Get the session
        Session session = getRepositorySession();
        if (session == null) {
            throw new IllegalArgumentException("Session must be set, but is null!");
        }
        Document doc = getDocumentById(documentId);
        doc.delete(true);
    }
    
    /**
     *
     * @param documentId the document id of the file in the CMIS system
     * @return The document as a CMIS Document object
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    private Document getDocumentById(String documentId) throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
        Session session = getRepositorySession();
        if (session == null) {
            throw new IllegalArgumentException("Session must be set, but is null!");
        }
        if (documentId == null) {
            return null;
        }
        try {
            Document doc = (Document) session.getObject(documentId);
            return doc;
        } catch (CmisObjectNotFoundException onfe) {
            return null;
            // throw new Exception("Document doesn't exist!", onfe);
        } catch (CmisBaseException cbe) {
            // throw new Exception("Could not retrieve the document:" +
            // cbe.getMessage(), cbe);
            return null;
        }
    }
    
    /**
     * @param documentId the document id of the file in the CMIS system
     * @return the content of the file
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    private ContentStream getDocumentStreamById(String documentId) throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
        ContentStream contentStream = null;
        Document doc = getDocumentById(documentId);
        if (doc != null) {
            contentStream = doc.getContentStream();
        }
        return contentStream;
    }
    
    /**
     * Initializes the document service repository
     *
     * @param uniqueName the unique name for the repository. Use a unique name with package semantics e.g. com.sap.foo.myrepo1
     * @param secretKey the secret key only known to your app.. Should be at least 10 chars long.
     * @param folder the one folder where all your documents will be stored
     * @return the corresponding CMIS session
     * @throws ServiceException, CmisObjectNotFoundException, NamingException 
     * @throws ServiceException, NamingException 
     */
    private Session getRepositorySession() throws ServiceException, CmisObjectNotFoundException, NamingException, Exception {
        try {
			// Only connect to the repository if a session hasn't been opened, yet
			if ((cmisSession == null) || ((cmisSession != null) && !(cmisSession.getRepositoryInfo().getName().equalsIgnoreCase(CMISHelper.REPO_NAME)))) {
			    InitialContext ctx = new InitialContext();
			    String lookupName = "java:comp/env/" + "EcmService";
			    EcmService ecmSvc = (EcmService) ctx.lookup(lookupName);
			    cmisSession = ecmSvc.connect(REPO_NAME, REPO_PRIVATEKEY);
			}
			
			return cmisSession;
		} catch (CmisObjectNotFoundException | NamingException ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		}
    }
    
    public void createRepository() throws RepositoryAlreadyExistsException, ServiceException, NamingException, Exception {
    	try {
			RepositoryOptions options = new RepositoryOptions();
			options.setUniqueName(REPO_NAME);
			options.setRepositoryKey(REPO_PRIVATEKEY);
			options.setVisibility(Visibility.PROTECTED);
			InitialContext ctx = new InitialContext();
			String lookupName = "java:comp/env/" + "EcmService";
			EcmService ecmSvc = (EcmService) ctx.lookup(lookupName);
			ecmSvc.createRepository(options);
		} catch (ServiceException | NamingException ex) {
			throw ex;
		} catch(Exception ex) {
			throw ex;
		}
    }
    
    /**
     * Creates a folder. If the folder already exists nothing is done
     *
     * @param session the CMIS session
     * @param rootFolder the root folder of your repository
     * @param folderName the name of the folder to be created
     * @return returns true if the folder has been created and returns false if the folder already exists
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    private Folder getFolderOfYourApp(String folderName) throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
    	System.err.println("Folder Path to retrieve data: " + folderName);
        Session session = getRepositorySession();
        Folder rootFolder = session.getRootFolder();
        Folder appFolder = null;
        try {
            session.getObjectByPath("/" + folderName);
            appFolder = (Folder) session.getObjectByPath("/" + folderName);
            System.err.println("Found Folder: " + appFolder.getId() + " *** " + appFolder.getName());
        } catch (CmisObjectNotFoundException e) {
        	System.err.println("Error in getFolderOfYourApp: " + e);
            // Create the folder if it doesn't exist, yet
            Map<String, String> newFolderProps = new HashMap<String, String>();
            newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
            newFolderProps.put(PropertyIds.NAME, folderName);
            appFolder = rootFolder.createFolder(newFolderProps);
        }
        return appFolder;
    }
    
    /**
     * Creates new folder under the parent ID
     * 
     * @param folderName
     * @param parentID
     * @throws NamingException 
     * @throws ServiceException 
     * @throws CmisObjectNotFoundException 
     */
    public void createFolder(String folderName,String parentID) throws CmisObjectNotFoundException, ServiceException, NamingException, Exception {
    	System.err.println("Folder Name: " + folderName);
        Session session = getRepositorySession();
        Folder rootFolder = session.getRootFolder();
        
    	Map<String, String> newFolderProps = new HashMap<String, String>();
        newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        newFolderProps.put(PropertyIds.NAME, folderName);
        if(!parentID.equals("")) newFolderProps.put(PropertyIds.PARENT_ID, folderName);
        rootFolder.createFolder(newFolderProps);
    }
    
    /**
     *
     * @param docs the CMIS documents that you want to convert
     * @return the representation of the documents as a List of MyDocsDTO
     */
    private List<DocumentDTO> convertCmisDocsToMyDocsDTOs(List<Document> docs,List<Folder> folders) {
        List<DocumentDTO> result = null;
        
        if(folders != null & folders.size() > 0) {
        	result = new ArrayList<DocumentDTO>();
            for (Folder folder : folders) {
                DocumentDTO pc = convertCmisDocToMyDocsDTO(folder);
                result.add(pc);
            }
        }
        
        if (docs != null) {
        	if(result == null) result = new ArrayList<DocumentDTO>();
            for (Document doc : docs) {
                DocumentDTO pc = convertCmisDocToMyDocsDTO(doc);
                result.add(pc);
            }
        }
        
        return result;
    }
    
    /**
     * 
     * @param folder folder the CMIS folder that you want to convert
     * @return the MyDocsDTO representation of the folders
     */
   private DocumentDTO convertCmisDocToMyDocsDTO(Folder folder) {
       DocumentDTO result = null;
       if (folder != null) {
           result = new DocumentDTO();
           result.setFolder(true);
           result.setFilename(folder.getName());
           result.setMimeType("");
           result.setAuthorName(folder.getCreatedBy());
           result.setDownloadLink("");
           result.setId(folder.getId());
           result.setCreationDate(new SimpleDateFormat("dd-MM-yyyy hh:mm").format(folder.getCreationDate().getTime()));
           result.setFileLength(0);
           // List<String> mydocProperties = doc.getPropertyValue("sap:tags");
       }
       return result;
   }
    
    /**
     *
     * @param doc the CMIS document that you want to convert
     * @return the MyDocsDTO representation of the document
     */
    private DocumentDTO convertCmisDocToMyDocsDTO(Document doc) {
        DocumentDTO result = null;
        if (doc != null) {
            result = new DocumentDTO();
            result.setFilename(doc.getName());
            result.setMimeType(doc.getContentStreamMimeType());
            result.setAuthorName(doc.getCreatedBy());
            result.setDownloadLink(SERVLET_DOWNLOAD_PREFIX + doc.getId());
            result.setId(doc.getId());
            result.setCreationDate(new SimpleDateFormat("dd-MM-yyyy hh:mm").format(doc.getCreationDate().getTime()));
            if(doc.getContentStreamLength() < 1024) {
            	result.setFileLength(doc.getContentStreamLength());
            	result.setFileLengthUnit("Bytes");
            } else if(doc.getContentStreamLength() < (1024*1024)) {
            	result.setFileLength(doc.getContentStreamLength() / (1024));
            	result.setFileLengthUnit("KB");
            } else if(doc.getContentStreamLength() < (1024*1024*1024)) {
            	result.setFileLength(doc.getContentStreamLength() / (1024*1024*1024));
            	result.setFileLengthUnit("MB");
            } else if(doc.getContentStreamLength() < (1024*1024*1024*1024)) {
            	result.setFileLength(doc.getContentStreamLength() / (1024*1024*1024*1024));
            	result.setFileLengthUnit("GB");
            }
            // List<String> mydocProperties = doc.getPropertyValue("sap:tags");
        }
        return result;
    }
    
    /**
     * Enables the upload of a file from the client computer to the server
     *
     * @param realPathOfApp The physical path of the application on the server
     * @param request The HttpServletRquest
     * @return A file object is stored on the server in the TEMP_UPLOAD_DR
     */
    public File uploadDocument(String realPathOfApp, HttpServletRequest request) {
        File uploadedFile = null;
        boolean isMultipart = ServletFileUpload.isMultipartContent(request);
        String uploadPath = realPathOfApp + File.separator + TEMP_UPLOAD_DIR;
        File path = new File(uploadPath);
        // If the path doesn't exist, yet, create it
        if (!path.exists()) {
            path.mkdir();
        }
        if (isMultipart) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<?> items = upload.parseRequest(request);
                Iterator<?> iterator = items.iterator();
                while (iterator.hasNext()) {
                    FileItem item = (FileItem) iterator.next();
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        uploadedFile = new File(path + File.separator + fileName);
                        item.write(uploadedFile);
                    }
                }
            } catch (FileUploadException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return uploadedFile;
    }
    
    /**
     *
     * @param response The HttpServletResponse
     * @param docId The document id of the document in your repository
     * @throws NamingException 
     * @throws IOException 
     */
    public void streamOutDocument(HttpServletResponse response, String docId) throws CmisObjectNotFoundException, NamingException, IOException, Exception {
        try {
			ContentStream docStream = getDocumentStreamById(docId);
			if (docStream != null) {
			    System.out.println("Download Mime Type: " + docStream.getMimeType());
			    response.setContentType("application/octet-stream");
				response.setHeader("Content-Disposition","attachment; filename=\"" + docStream.getFileName() + "\"");
			    IOUtils.copy(docStream.getStream(), response.getOutputStream());
			    IOUtils.closeQuietly(docStream.getStream());
			} else {
			    // If the document doesn't have any stream return "file-not-found" status code in http responce
			    response.setStatus(404);
			}
		} catch (CmisObjectNotFoundException e) {
			throw e;
		} catch (ServiceException e) {
			throw e;
		} catch (NamingException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
    }
    
    /**
     * 
     * @param response
     * @param docId
     */
    public void copyDocument(String docId, User user) {
        try {
            ContentStream docStream = getDocumentStreamById(docId);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
            properties.put(PropertyIds.NAME, docStream.getFileName());
            if(user != null) properties.put(PropertyIds.CREATED_BY, user.getName());
            properties.put(PropertyIds.CREATION_DATE, System.currentTimeMillis());
            // Try to identify the mime type of the file
            String mimeType = URLConnection.guessContentTypeFromName(docStream.getFileName());
            if (mimeType == null || mimeType.length() == 0) {
                mimeType = "application/octet-stream";
            }
            
            Session session = getRepositorySession();
            ContentStream contentStream = session.getObjectFactory().createContentStream(docStream.getFileName(), docStream.getLength(), mimeType, docStream.getStream());
            this.deleteDocument(docId);
            
            try {
                Folder yourFolder = getFolderOfYourApp(REPO_APPFOLDER);
                yourFolder.createDocument(properties, contentStream, VersioningState.NONE);
            } catch (CmisNameConstraintViolationException e) {
                // Document exists already, nothing to do
            }
            
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}