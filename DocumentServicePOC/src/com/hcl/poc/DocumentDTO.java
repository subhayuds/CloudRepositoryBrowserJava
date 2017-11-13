package com.hcl.poc;

public class DocumentDTO {
	public boolean isFolder = false;
	public String authorName = "";
    public String authorEmail = "";
    public String id = "";
    public long fileLength = 0;
    public String fileLengthUnit = ""; 
	public String creationDate = "";
    public String filename = "";
    public String downloadLink = "";
    public String stream = "";
    public String mimeType = "";
    
	public boolean isFolder() {
		return isFolder;
	}

	public void setFolder(boolean isFolder) {
		this.isFolder = isFolder;
	}

	public String getAuthorName() {
		return authorName;
	}
	
	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}
	
	public String getAuthorEmail() {
		return authorEmail;
	}
	
	public void setAuthorEmail(String authorEmail) {
		this.authorEmail = authorEmail;
	}
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public long getFileLength() {
		return fileLength;
	}
	
	public void setFileLength(long fileLength) {
		this.fileLength = fileLength;
	}
	
	public String getFileLengthUnit() {
		return fileLengthUnit;
	}

	public void setFileLengthUnit(String fileLengthUnit) {
		this.fileLengthUnit = fileLengthUnit;
	}
	
	public String getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	
	public String getFilename() {
		return filename;
	}
	
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public String getDownloadLink() {
		return downloadLink;
	}
	
	public void setDownloadLink(String downloadLink) {
		this.downloadLink = downloadLink;
	}
	
	public String getStream() {
		return stream;
	}
	
	public void setStream(String stream) {
		this.stream = stream;
	}
	
	public String getMimeType() {
		return mimeType;
	}
	
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
}