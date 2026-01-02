package application;
import java.util.List;
//class for special access article
public class SpecialAccessArticle {
	private long specialGroupArticleId;
	private long articleId;
	private String groupName;
	private String title;
	private String level;
	private String description;
	private String content;
	private String privateNote;
	private List<String> keywords;
	private List<String> links;
	

	//will be used later in future
	public SpecialAccessArticle(
			long specialGroupArticleId,
			long articleId,
			String groupName,
			String title,
			String level,
			String description,
			String content,
			String privateNote,
			List<String> keywords,
			List<String> links) {
		this.specialGroupArticleId = specialGroupArticleId;
		this.articleId = articleId;
		this.groupName = groupName;
		this.title = title;
		this.level = level;
		this.description = description;
		this.content = content;
		this.privateNote = privateNote;
		this.keywords = keywords;
		this.links = links;
		
	}
	
	//getter setter methods
	public long getSpecialGroupArticleId() {
		return specialGroupArticleId;
	}
	
	public void setSpecialGroupArticleId(long specialGroupArticlrId) {
		this.specialGroupArticleId = specialGroupArticleId;
	}
	
	public long getArticle() {
		return articleId;
	}
	
	public void setArticleId(long articleId) {
		this.articleId = articleId;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
		
	}
	
	public String getTitle() {
		return title;
		
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getLevel() {
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getContent() {
		return content;
		
	}
	public void setContent(String privateNote) {
		this.privateNote = privateNote;
		
	}
	public List<String> getKeywords() {
		return keywords;
	}
	
	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}
	
	public List<String> getLinks() {
		return links;
	}
	
	public void setLinks(List<String> links) {
		this.links = links;
	}
	
	//ovverriding 
	@Override
	public String toString() {
		return "SpecialAccessArticle {" +
	           "specialGroupArticleId=" + specialGroupArticleId +
	           ", articleId=" + articleId +
	           ", groupName='" + groupName + '\'' +
	           ", title='" + title + '\'' +
	           ", level='" + level + '\'' +
	           ", description='" + description + '\'' +
	           ", content='" + content + '\'' +
	           ", privateNote='" + privateNote + '\'' +
	           ", keywords=" + keywords +
	           ", links=" + links +
	           '}';
	}
}