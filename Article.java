package application;

import java.util.List;

public class Article {
	// fields to store article properties
    private long id;
    private String title;
    private String level;
    private String description;
    private String content;
    private String privateNote; 
    private List<String> groupIdentifiers;
    private List<String> keywords;
    private List<String> links;
    
// constructor to initialize article object
    public Article(long id, String title, String level, String description, String content, String privateNote, List<String> groupIdentifiers, List<String> keywords, List<String> links) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.description = description;
        this.content = content;
        this.privateNote = privateNote;
        this.groupIdentifiers = groupIdentifiers;
        this.keywords = keywords;
        this.links = links;

   }
 // Getter for PrivateNote
   public String getPrivateNote() {
        return privateNote;
   }
// Setter for PrivateNote
   public void setPrivateNote(String privateNote) {
       this.privateNote = privateNote;
   }
// Getter for Article ID
   public long getId() {
       return id;
   }
// Getter for Article title
   public String getTitle() {
       return title;
   }
// Getter for Article level
   public String getLevel() {
       return level;
   }
// Getter for Article description
   public String getDescription() {
       return description;
   }
// Getter for Article content
   public String getContent() {
       return content;
   }
// Getter for group identifiers
   public List<String> getGroupIdentifiers() {
       return groupIdentifiers;
   }
// Getter for keywords
   public List<String> getKeywords() {
       return keywords;
   }
   
   // Getter for links
   public List<String>  getLinks() {
       return links;
   }

    // toString method to represent Article objet as a string
   //@Override
   public String toString() {
       return "Article{" +
              "id=" + id +
              ", title='" + title + '\'' +
              ", level='" + level + '\'' +
              ", description='" + description + '\'' +
              ", content='" + content + '\'' +
              ", groupIdentifiers=" + String.join(", ", groupIdentifiers) +
              ", keywords=" + String.join(", ", keywords) +
              ", links=" + String.join(", ", links) +
              '}';
   }
   
} 

		




