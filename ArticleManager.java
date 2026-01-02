0package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.utilColleections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.PriorityQueue;

//This is the main function
public class ArticleManager{
    private long nextId = 1;
    private PriorityQueue<Long> availableIds = new PriorityQueue<>();
    private static final String INSERT_ARTICAL_SQL = "INSERT INTO articles (title, level, description, content, private_note) VALUES (?,?,?,?,?)";
    private static final String INSERT_GROUP_SQL = "INSERT INTO article_groups (article_id, group_identifier) VALUES (?,?)";
    private static final String INSERT_KEYWORD_SQL = "INSERT INTO article_keywords (article_id, keyword) VALUES (?,?)";
    private static final String INSERT_LINK_SQL = "INSERT INTO article_links (article_id, link) VALUES (?,?)";
    private static final String SELECT_ALL_ARTICLES_SQL ="""
        SELECT a.*,
                GROUP_CONCAT(DISTINCT ag.group_identifier) AS `groups`,
                GROUP_CONCAT(DISTINCT ag.keyword) AS keywords,
                GROUP_CONCAT(DISTINCT ag.link) AS links
        FROM articles a
        LEFT JOIN article_groups ag ON a.id = ag.article_id
        LEFT JOIN article_keywords ak ON a.id = ak.article_id
        LEFT JOIN article_links al ON a.id = al.article_id
        GROUP BY a.id;
        """;
        
        //This query helps retrieve articles by a specific group
        private static final String SELECT_ARTICLES_BY_GROUP_SQL = """
        SELECT a.*,
                GROUP_CONCAT(DISTINCT ag.group_identifier) AS `groups`,
                GROUP_CONCAT(DISTINCT ag.keyword) AS keywords,
                GROUP_CONCAT(DISTINCT ag.link) AS links
        FROM articles a
        LEFT JOIN article_groups ag ON a.id = ag.article_id
        LEFT JOIN article_keywords ak ON a.id = ak.article_id
        LEFT JOIN article_links al ON a.id = al.article_id
        WHERE ag.group_identifier = ?
        GROUP BY a.id;
        """;
        //This query helps retrieve articles by the same title etc.
        private static final String CHECK_BASE_DUPLICATE_SQL = """
            SELECT a.id
            FROM articles a
            LEFT JOIN article_groups ag ON a.id = ag.article_id
            LEFT JOIN article_keywords ak ON a.id = ak.article_id
            LEFT JOIN article_links al ON a.id = al.article_id\
            WHERE a.title = ? AND a.level = ? AND a.description = ? AND a.content = ? AND a.private_note = ?
            GROUP BY a.id;
        """;
        private static final String DELETE_ARTICLE_SQL = "DELETE FROM articles WHERE id = ?";

//This functios helps to adds a new article.
    public boolean addArticle(
            String title, String level, String description,
            String content, String privateNote,
            List<String> groups, List<String> keywords, List<String> links){

        long articleId = generateArticleId();

        try (Connection connection = DatabaseConnection.getConnection()){
            connection.SetAutoCommit(false);

            if (generateArticleId(articleId) != null){
                connection.rollback();
                return false;
            }
            if (isDuplicateArticle(connection,title, level, description, content, privateNote, groups, keywords, links)){
                connection.rollbacl();
                return false;
            }

            String insertArticleSql = """
                INSERT INTO articles (id, title, level, description, content, private_note)
                VALUES (?,?,?,?,?,?)
            """;

            try(PreparedStatement articleStatement = connection.PreparedStatement(insertArticleSql)){
                articleStatement.setLong(1, article_id);
                articleStatement.setLong(2, title);
                articleStatement.setLong(3, level);
                articleStatement.setLong(4, description);
                articleStatement.setLong(5, content);
                articleStatement.setLong(6, privateNote);

                int affectedRows = articleStatement.executeUpdate();
                if (affectedRows ==0){
                    throw new SQLException("Creating article failed, no rows affected.");
                }

                insertAssociations(connection, INSERT_GROUP_SQL, articleId, groups);
                insertAssociations(connection, INSERT_KEYWORD_SQL, articleId, keywords);
                insertAssociations(connection, INSERT_LINK_SQL, articleId, links);

                return true;
            }
        } catch (SQLException e){
            try{
                DatabaseConnection.getConnection().rollback();
            } catch (SQLException rollbackEx){
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return false;
        }

    }

    //This function generates a new article id 
    public synchronized long generateArticleId(){
        try(Connection connection=DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs stmt.executeQuery("SELECT MAX(id) FROM articles")){
            
            if(rs.next()){
                long maxId = rs.getLong(1);
                return maxId + 1;
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return nextId++;
    }

    //This function checks whether a duplicate article is added
    public boolean isDuplicateArticle(Connection connection String title, String level, String description, String content, String privateNote, List<String> groups, List<String> links) throws SQLException{
        try(preparedStatement checkDuplicateStatement = connection.preparedStatement(CHECK_BASE_DUPLICATE_SQL)){

            checkDuplicateStatement.setString(1, title);
            checkDuplicateStatement.setString(2, level);
            checkDuplicateStatement.setString(3, description);
            checkDuplicateStatement.setString(4, content);
            checkDuplicateStatement.setString(5, privateNote);

            try (ResultSet rs = checkDuplicateStatement.executeQuery()){
                while (rs.next()){
                    long articleId = rs.getLong("id");
                    if (isExactMatch(connection, articleId, groups, keywords, links)){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private boolean isExactMatch(Connection connection, long articleId, List<String> groups, List<String> keywords, List<String> links ) throws SQLException{
        List<String> existingGroups = getAssociations(connection, "SELECT group_identifier FROM article_groups WHERE article_id = ?", articleId);
        List<String> existingKeywords = getAssociations(connection, "SELECT keyword FROM article_keywords WHERE article_id = ?", articleId);
        List<String> existingLinks = getAssociations(connection, "SELECT link FROM article_groups WHERE article_id = ?", articleId);

        return existingGroups.equals(groups) && existingKeywords.equals(keywords) && existLinks.equals(links);
        
    }

    //This functionn inserts association
    private void insertAssociations(Connection connection, String sql, long articleId, List<String> items) throws SQLException{
        try (PreparedStatement statement = connection.preparedStatement(sql)){
            for (String item : items){
                statement.setLong(1, articleId);
                statement.setString(2, item);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }


    //This function helps to list the articles by group
    public List<Article> listArticlesByGroups(list<List<String>> groupFilters){
        List<Article> articles = new ArrayList<>();

        if (groupFilters == null || groupFilters.isEmpty()){
            return listAllArticles();
        }

        String queryBuilder = new StringBuilder("""
            SELECT a.*,
                GROUP_CONCAT(DISTINCT ag.group_identifier) AS `groups`,
                GROUP_CONCAT(DISTINCT ag.keyword) AS keywords,
                GROUP_CONCAT(DISTINCT ag.link) AS links
            FROM articles a
            LEFT JOIN article_groups ag ON a.id = ag.article_id
            LEFT JOIN article_keywords ak ON a.id = ak.article_id
            LEFT JOIN article_links al ON a.id = al.article_id
            WHERE
        """);

        List<String> orConditions = new ArrayList<>();

        for (List<String> andgroup : groupfiletrs){
            if(andGroup.size()==1) {
                orConditions.add("ad.group_identifier = ?");
            } else {
                String andCondition = "a.id IN (SELECT article_id FROM article_groups WHERE group_identifier IN (" +
                        String.join(",", Collections.nCopies(andGroup.size(), "?")) +
                        ") GROUP BY article_id HAVING COUNT(DISTINCT group_identifier) = ?)";
                orConditions.add("(" + andCondition + ")");
            }
        }

        queryBuilder.append(String,join(" OR ", orConditions));
        queryBuilder.APPEND("GROUP BY a,id");

        try(Connection connection - DatabaseConnection.getConnection();
            PreparedStatement stmt = connection.preparedStatement(queryBuilder.toString())){

                int index = 1;
                for (List<String> andGroup : groupFiletrs){
                    for (String group : andGroup){
                        stmt.setstring(index++, group);

                    }
                    if (andGroup.size()1){
                        stmt.setInt(index++, andGroup.size());
                    }
                }

                try (ResultSet rs = stmt.executeQuery()){
                    while(rs.next()){
                        articles.add(extractArticleFromResultSet(rs));
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
            return articles;
    }

    //This function helps to list the articles by group
    public List<Article> listAllArticles(){
        List<Article> articles = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement preparedStatement = Connection.preparedStatement(SELECT_ALL_ARTICLES_SQL);
             ResultSet resultSet preparedStatement.executeQuery()){

            while(resultSet.next()){
                Article article = extractArticleFromResultSet(resultSet);
                articles.add(article);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return articles;
    }


    //This function deletes an article
    public boolean deleteArticle(long articleId){
        try (Connection connection = DatabaseConnection.getConnection()){

            String deleteSql = "DELECT FROM articles WHERE id =?";
            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteSql)){
                delteStmt.setLong(1, articleId);
                int rowsAffected = delete.executeUpdate();

                if (rowsAffected>0){
                    resetAutoIncrement();
                    return true;
                }

            }

        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    //This functiona auto increments the id
    private void resetAutoIncrement(){
        try(Connection connection = DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement()){


            String resetSql = "ALTER TABLE articles AUTO_INCREMENT = 1";
            stmt.executeUpdate(resetSql);    
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    //This function marks the ids
    private void markIdAsAvailable(long id){
        availableIds.add(id);
    }

    //This function helps to extract Article 
    private extractArticleFromResultSet(ResultSet resultSet) throws SQLException{
        List<String> groups = resultSet.getString("groups") != null
                ? Arrays.asList(resultSet.getString("groups").split(","))
                : new ArrayList<>();
        
        List<String> keywords = resultSet.getString("keywords") != null
                ? Arrays.asList(resultSet.getString("keywords").split(","))
                : new ArrayList<>();

        List<String> links = resultSet.getString("links") != null
                ? Arrays.asList(resultSet.getString("links").split(","))
                : new ArrayList<>();
        
        return new Artcle(
                resultSet.getLong("id"),
                resultSet.getLong("title"),
                resultSet.getLong("level"),
                resultSet.getLong("description"),
                resultSet.getLong("content"),
                resultSet.getLong("private_note"),
                groups,
                keywords,
                links
        
        );
    }

    //This function delets articles by group 
    public boolean deleteArticlesByGroup(String group) {
        String sql = """
            DELETE a FROM articles a
            JOIN article_groups ag ON a.id = ag.article_id
            WHERE ag.group_identifier = ?
        """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement = STMT = CONN.prepareStatement(Sql)){
            
            stmt.setString(1, group);
            return stmt.executeUpdate() > 0;
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    //This function helps backup articles 
    public void backupAllArticles(String fileName){

        List<Article> articles = listAllArticles();
        backupArticles(fileName, articles);
    }

    //This functions helps to backup group articles
    public void backupGroupArticles(String groupNames, String fileName){
        List<String> groups = Arrays.asList(groupNames.split("&"));

        List<Article> articles = listArticlesByGroups(Collections.singletonList(groups));
        backupArticles(fileName, articles);
    }


    //This functions helps to backup group articles
    private void backupArticles(String fileName, List<Article> articles){
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))){
            writer.println("ID,Title,Level,Description,Content,PrivateNote,Groups,Keywords,Links");

            for (Article article : articles){
                String group = String.join(";", article.getGroupIdentifiers());
                String keywords = String.join(";", article.getKeywords());
                String links = String.join(";", article.getLinks());

                writer.printf("%d,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    article.getId(),
                    article.getTitle(),
                    article.getLevel(),
                    article.getDescription(),
                    article.getContent(),
                    article.getPrivateNote(),
                    groups,
                    keyword,
                    links
                );
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    //This function helps restore Articles from backup
    public void restoreArticlesFromBackup(String fileName, boolean replaceExisting){
        List<Article> articles = readArticlesFromCSV(fileName);

        try (Connection connection = DatabaseConnection.getConnection()){
            if (replaceExisting){
                deleteAllArticles(connection);
            }

            for (Article article : articles) {
                if (replaceExisting || !doesArticleExist(connection, article.getId())){
                    addArticleWithId(connection, article);
                }
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    //Checks if the article exist 
    private boolean doesArticleExist(Connection connection,long article_id) throws SQLException{
        String sql = "SELECT id FROM articles WHERE id = ?";
        try (PreparedStatement stmt = connection.preparedStatement(sql)){
            stmt.setLong(1, articleId);
            try (ResultSet rs = stmt.executeQuery()){
                return rs.next();
            }
        }
    }


    //this functions adds an article with id
    private void addArticleWithId(Connection connection, Article article) throws SQLException{
        String sql = """
            INSERT INTO articles (id, title, level, description, content, private_note)
            VALUES (?,?,?,?,?,?)
        """;

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setLong(1, article.getId());
            stmt.setLong(2, article.getTitle());
            stmt.setLong(3, article.getLevel());
            stmt.setLong(4, article.getDescription());
            stmt.setLong(5, article.getContent());
            stmt.setLong(6, article.getPrivateNote());
            stmt.executeUpdate();

            insertAssociations(connection, INSERT_GROUP_SQL, article.getId, article.getGroupIdentifiers());
            insertAssociations(connection, INSERT_KEYWORD_SQL, article.getId, article.getKeywords());
            insertAssociations(connection, INSERT_LINK_SQL, article.getId, article.getLinks());
        }

    }


    //This function delets all articles
    private void deleteAllArticles(Connection connection) throws SQLException{
        try(Statement stmt = connection.createStatement()){
            stmt.executeUpdate("DELETE FROM articles");
            stmt.executeUpdate("ALTER TABLE articles AUTO_INCREMENT = 1");
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    private List<String> getAssociations( Connection connection, String query, long articleId) throws SQLException{
        List<String> items = new ArrayList<>();
        try(PreparedStatement stmt = connection.prepareStatement(query)){
            stmt.setLong(1,articleId);
            try (ResultSet rs = stmt.executeQuery()){
                while(rs.next()){
                    items.add(rs.getString(1));
                }
            }
        }
        return items;
    }


    //Reads article form a CSV file
    private List<Article> readArticlesFromCSV(String fileName){
        List<Article> articles = new ArrayList<>();

        try(BufferedReader reader = new BufferedReader(new FileReader(fileName))){
            String line = reader.readLine();

            while((line = reader.readLine())!=null) {
                String[] fields = line.split(",", -1);

                long id = Long.parseLong(fields[0]);
                String title = fields[1];
                String level = fields[2];
                String description = fields[3];
                String content = fields[4];
                String privateNote = fields[5];
                List<String> groups = Arrays.asList(fields[6].split(";"));
                List<String> keywords = Arrays.asList(fields[7].split(";"));
                List<String> links = Arrays.asList(fields[8].split(";"));

                Article article = new Article(id,title, description, content, privateNote, groups, keywords, links);
                articles.add(article);
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return articles;
    }

    //Deletes all articles
    private void deleteAllArticles() {
        try(Connection connection = DatabaseConnection.getConnection();
             Statement stmt = connection.createStatement()){
            stmt.executeUpdate("DELETE FROM articles");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Updates an article by id
    public boolean updateArticleById(
			long articleId, String title, String level, String description, String content, String privateNote,
			List<String> groups, List<String> keywords, List<String> links) {
		try (Connection conn = DatabaseConnection.getConnection()) {
			if (isDuplicateArticleOnUpdate(conn, articleId, title, level, description, content, privateNote, groups, keywords, links)) {
				return false;
			}
			
			String updateSql = """
					UPDATE articles
					SET title = ?, level = ?, description = ?, content = ?, private_note = ?
					WHERE id = ?
					""";
			
			try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
				stmt.setString(1, title);
				stmt.setString(2, level);
				stmt.setString(3, description);
				stmt.setString(4, content);
				stmt.setString(5, privateNote);
				stmt.setLong(6, articleId);
				
				int rowsUpdated = stmt.executeUpdate();
				
				if (rowsUpdated > 0) {
					clearAssociationsForArticle(conn, articleId);
					insertAssociations(conn, INSERT_GROUP_SQL, articleId, groups);
					insertAssociations(conn, INSERT_KEYWORD_SQL, articleId, keywords);
					insertAssociations(conn, INSERT_LINK_SQL, articleId, links);
					return true;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

    //Checks if an article is duplicating while updating an article
    public boolean isDuplicateArticleOnUpdate(
			Connection connection, long articleId, String title, String level, String description, String content, String privateNote,
			List<String> groups, List<String> keywords, List<String> links) throws SQLException {
		
		String duplicateCheckSql = """
			SELECT a.id
			FROM articles a
			LEFT JOIN article_groups ag ON a.id = ag.article_id
			LEFT JOIN article_keywords ak ON a.id = ak.article_id
			LEFT JOIN article_links al ON a.id = al.articel_id
			WHERE a.title = ? AND a.level = ? AND a.description = ?
					AND a.content = ? AND a.private_note = ? AND a.id != ?
			GROUP BY a.id
		""";
		
		try (PreparedStatement stmt = connection.preparedStatement(duplicateCheckSql)) {
			stmt.setString(1, title);
			stmt.setString(2, level);
			stmt.setString(3, description);
			stmt.setString(4, content);
			stmt.setString(5, privateNote);
			stmt.setLong(6, articleId);
			
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					long foundId = rs.getLong("id");
					
					if (isExactMatch(connection, foundId, groups, keywords, links)) {
						return true;
					}
				}
			}
		}
		return false;
	}

    private void clearAssociationsForArticle(Connection conn, long articleId) throws SQLException {
		String deleteGroupsSql = "DELETE FROM article_groups WHERE article_id = ?";
		String deleteKeywordsSql = "DELETE FROM article_keywords WHERE article_id = ?";
		String deleteLinksSql = "DELETE FROM article_links WHERE article_id = ?";
		
		try (PreparedStatement deleteGroupsStmt = conn.PreparedStatement(deleteGroupsSql);
			 PreparedStatement deleteKeywordsStmt = conn.PreparedStatement(deleteKeyowrdsSql);
			 PreparedStatement deleteLinksStmt = conn.PreparedStatement(deleteLinksSql)) {
			
			deleteGroupsStmt.setLong(1, articleId);
			deleteGroupsStmt.executeUpdate();
			
			deleteKeywordsStmt.setLong(1, articleId);
			deleteKeywordsStmt.executeUpdate();
			
			deleteLinksSql.setLong(1, articleId);
			deleteLinksSql.executeUpdate();
		}
	}

    //Updates an article by their group 
    public List<String> updateArticlesByGroup(String group, String newTitle, String newLevel,
		   List<String> newGroups, List<String> newKeywords) {
		List<String> duplicates = new ArrayLists<>();
		
		String selectSql = """
			SELECT a.id, a.title, a.level
			FROM articles a
			JOIN article_groups ag ON a.id = ag.article_id
			WHERE ag.group_identifier = ?
		""";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
			
			selectStmt.setString(1,group);
			
			try (ResultSet rs = selectStmt.executeQuery()) {
				while (rs.next()) {
					long articleId = rs.getLong("id");
					
					String title = (newTitle == null || newTitle.isEmpty()) ? rs.getString("title") : newTitle;
					String level = (newLevel == null || newLevel.isEmpty()) ? rs.getString("level") : newLevel;
					
					if (isDuplicateArticleForGroupUpdate(conn, articleId, title, level, newGroups, newKeywords)) {
						duplicates.add("ID: " + articleId + " (Title: " + title + ")");
					} else {
						String updateSql = """
							UPDATE articles
							SET title = ?, level = ?
							WHERE id = ?
						""";
						
						try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
							updateStmt.setString(1, title);
							updateStmt.setString(2, level);
							updateStmt.setLong(3, articleId);
							updateStmt.executeUpdate();
						}
						
						clearAndUpdateGroupAssociations(conn, articleId, newGroups, newKeywords);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return  duplicates;
	}

    //This function clears and updates group Associations 
    private void clearAndUpdateGroupAssociations(Connection conn, long articleId,
			List<String> newGroups, List<String> newKeywords) throws SQLException {
		String deleteGroupsSql = "DELETE FROM article_groups WHERE article_id = ?";
		String deleteKeywordsSql = "DELETE FROM article_keywords WHERE article_id = ?";
		
		try (PreparedStatement deleteGroupsStmt = conn.prepareStatement(deleteGroupsSql);
			 PreparedStatement deleteKeywordsStmt = conn.prepareStatement(deleteKeywordsSql)) {
			
			deleteGroupsStmt.setLong(1, articleId);
			deleteGroupsStmt.executeUpdate();
			
			deleteKeywordsStmt.setLong(1, articleId);
			deleteKeywordsStmt.executeUpdate();
		}
			
			insertAssociations(conn, INSERT_GROUP_SQL, articleId, newGroups);
			insertAssociations(conn, INSERT_KEYWORD_SQL, articleId, newKeywords);				
	}

    //This function fetches article by their id
    public Article getArticleById(long articleId) {
		String sql = """
			SELECT a.*,
					GROUP_CONCAT(DISTINCT ag.group_identifier) AS 'groups',
					GROUP_CONCAT(DISTINCT ak.keyowrd) AS keywords,
					GROUP_CONCAT(DISTINCT al.link) AS links
			FROM articles a
			LEFT JOIN article_groups ag ON a.id = ag.article_id
			LEFT JOIN article_keyowrds ak ON a.id = ak.article_id
			LEFT JOIN article_links al ON a.id = al.article_id
			WHERE a.id = ?
			GROUP BY a.id;
		""";
		
		try (Connection conn = DatabaseConnection.getConnection();
			 PreparedStatement stmt = conn.prepareStatement(sql)) {
			
			stmt.setLong(1, articleId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return extractArticleFromResultSet(rs);
				}
			}
		} catch (SQLExecution e) {
			e.printStackTrace();
		}
		return null;
	}

    //This function checks if there is a duplicate article while updating with the group
    private boolean isDuplicateArticleForGroupUpdate(
			Connection conn, long articleId, String title, String level,
			List<String> groups, List<String> keywords) throws SQLException {
		
		String duplicateCheckSql = """
			SELECT a.id
			FROM articles a
			JOIN article_groups ag ON a.id = ag.article_id
			JOIN article_keywords ak ON a.id = ak.article_id
			WHERE a.title = ? AND a.level = ? AND a.id != ?
			GROUP BY a.id
			HAVING
				COUNT(DISTINCT ag.group_identifier) = ?
				AND COUNT(DISTINCT ak.keyword) = ?;
		""";
		
		try (PreparedStatement stmt = conn.prepareStatement(duplicateCheckSql)) {
			stmt.setString(1, title);
			stmt.setString(2, level);
			stmt.setLong(3, articleId);
			stmt.setInt(4, groups.size());
			stmt.setInt(5, keywords.size());
			
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return true;
				}
			}
		}
		return false;
	}


    //This function searches article by their Title 
    public List<Article> searchArticelsByTitle(String title) {
		List<Article> articles = new ArrayList<>();
		String Sql = """
			SELECT a.*,
					GROUP_CONCAT(DISTINCT ag.group_identifier) AS 'groups',
					GROUP_CONCAT(DISTINCT ak.keyword) AS keywords,
					GROUP_CONCAT(DISTINCT al.link) AS links
			FROM articles a
			LEFT JOIN article_groups ag ON a.id = ag.article_id
			LEFT JOIN article_keywords ak ON a.id = ak.article_id
			LEFT JOIN article_links al ON a.id = al.article_id
			WHERE a.title LIKE ?
			GROUP BY a.id;
		""";
		
		try (Connection connection = DatabaseConnection.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, "%" + title + "%");
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					articles.add(extractArticleFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return articles;
	}


//  This function searches article by their level
    public List<Article> searchArticlesByLevel(String level) {
		List<Article> articles = new ArrayList<>();
		String sql = """
			SELECT a.*,
					GROUP_CONCAT(DISTINCT ag.group_identifier) AS 'groups',
					GROUP_CONCAT(DISTINCT ak.keyword) AS keywords,
					GROUP_CONCAT(DISTINCT al.link) AS links
			FROM articles a
			LEFT JOIN article_groups ag ON a.id = ag.article_id
			LEFT JOIN article_keywords ak ON a.id = ak.article_id
			LEFT JOIN article_links al ON a.id = al.article_id
			WHERE a.level = ?
			GROUP BY a.id;
		""";
		
		try (Connection connection = DatabaseConnection.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, level);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					articles.add(extractArticleFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return articles;
	}

    //This function search article by group 
    public List<Article> searchArticlesByGroup(String group) {
		List<Article> articles = new ArrayList<>();
		String sql = """
				SELECT a.*,
					   GROUP_CONCAT(DISTINCT ag.group_identifier) AS 'groups',
					   GROUP_CONCAT(DISTINCT ak.keyword) AS keywords,
					   GROUP_CONCAT(DISTINCT al.link) AS links
				FROM articles a
				LEFT JOIN article_groups ag ON a.id = ag.article_id
				LEFT JOIN article_keywords ak ON a.id = ak.article_id
				LEFT JOIN article_links al ON a.id = al.article_id
				WHERE a.group_identifier = ?
				GROUP BY a.id;
				""";
		
		try (Connection connection = DatabaseConnection.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(sql)) {
			stmt.setString(1, group);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					articles.add(extractArticleFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return articles;
	}

    //This functon lists articles by keywords
    public List<Article> listArticlesByKeywords(List<List<String>> keywordFilters) {
		List<Article> articles = new ArrayList<>();
		
		if (keywordFilters == null || keywordFilters.isempty()) {
			return listAllArticles();
		}
		
		StringBuilder queryBuilder = new StringBuilder("""
			SELECT a.*,
					GROUP_CONCAT(DISTINCT ag.group_identifier) AS 'groups',
					GROUP_CONCAT(DISTINCT ak.keyword) AS keywords,
					GROUP_CONCAT(DISTINCT al.link) AS links
			FROM articles a
			LEFT JOIN article_keywords ak ON a.id = ak.article_id
			LEFT JOIN article_groups ag ON a.id = ag.article_id				
			LEFT JOIN article_links al ON a.id = al.article_id
			WHERE
		""");
		
		List<String> orConditions = new ArrayList<>();
		
		for (List<String> andKeywords : keywordFilters) {
			if (andKeywords.size() == 1) {
				orConditions.add("ak.keyword = ?");
			} else {
				String andCondition = "a.id IN (SELECT article_id FROM article_keywords WHERE keyword IN (" +
						String.join(",", Collections.nCopies(andKeywords.size(), "?")) +
						") GROUP BY article_id HAVING COUNT(DISTINCT keyword) = ?)";
				orCondtions.add("(" + andCondition + ")");
			}
		}
		
		queryBuilder.append(String.join(" OR ", orConditions));
		queryBuilder.append(" GROUP BY a.id");
		
		try (Connection connection = DatabaseConnection.getConnection();
			 PreparedStatement stmt = connection.prepareStatement(queryBuilder.toString())) {
			int index = 1;
			for (List<String> andKeywords : keywordFilters) {
				for (String keyword : andKeywords) {
					stmt.setString(index++, keyword);
				}
				if (andKeywords.size() > 1) {
					stmt.setInt(index++, andKeywords.size());
				}
			}
			
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					articles.add(extractArticleFromResultSet(rs));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return articles;
	}


    //This function lost SpecialAccess Groups 
    public List<String> listSpecialAccessGroups(){
        List<String> groupNames = new ArrayList<>();
        String sql = "SELECT group_name FROM special_access_groups";

        try (Connection connection = DatabaseConnection.getConnecion();
             PreparedStatement stmt = connection.prepareStatement(sql)
             ResultSet rs = stmt.executeQuery()){

            while (rs.next()) {
                groupNames.add(rs.getString("group_name"));
            }

        } catch (SQLException e){
            e.printStacktrace();
        }
        return groupNames;
    }

    //This function gets the article by Title
    public Article getArticleByTitle(String title){
        String sql = "SELECT * FROM articles WHERE title = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)){
            
            stmt.setString(1, title);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()){
                return extractArticleFromResultSet(rs);
            }

        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // This function lists the article by their group name
    public List<Article> listArticlesByGroupName(String groupName){
        List<Article> articles = new ArrayList<>();
        String sql = """
            SELECT a.*
            FROM articles a
            JOIN article_groups ag ON a.id = ag.article_id
            WHERE ag.group_identifier = ?
        """;
        try(Connection connection = DatabaseConnection.getConnecion();
             PreparedStatement stmt = connection.prepareStatement(sql)){

            
            stmt.setString(1, groupName);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()){
                articles.add(extractArticleFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return articles;
    }

    public boolean addArticlesByGroupFiltersToSpecialAccessGroup(String groupName, Article article){
       String sql = """
            INSERT INTO special_acess_group_articles (article_id, group_name, encrypted_content, title, level, description, private_note, keywords, links)
            VALUES (?,?,?,?,?,?,?,?,?)
        """;

        try (Connection connection = DatabaseConnection.getConnecion();
             PreparedStatement stmt = connection.prepareStatement(sql)){
            
            String encryptedContent = encryptContent(article.getContent());

            stmt.setLong(1, article.getId());
            stmt.setString(2, groupName);
            stmt.setString(3, encryptedContent);
            stmt.setString(4, article.getTitle());
            stmt.setString(5, article.getLevel());
            stmt.setString(6, article.detDescription());
            stmt.setString(7, article.getPrivateNote());
            stmt.setString(8, article.getKeywords() != null ? String.join(",", article.getKeywords()) : null);
            stmt.setString(9, article.getLinks() != null ? String.join(",", article.getLinks()) : null);
            
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0; 
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } 
    }


//This function add Group to special Access
private void addGroupToSpecialAccessGroups(String groupName) {
	String sql = "INSERT IGNORE INTO special_access_groups (group_name) VALUES (?)";
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
		stmt.setString(1,groupName);
		stmt.executeUpdate();
	} catch (SQLException e) {
		e.printStackTrace();
	}
}


//tis function chceks if the special access group name is valid 
private boolean isSpecialAccessGroupNameValid(String groupName) {
	String checkSql = "SELECT 1 FROM special_access_group WHERE group_name = ?";
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(checkSql)) {
		
		stmt.setString(1, groupName);
		try (ResultSet rs = stmt.executeQuery()) {
			return rs.next(); 
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	return false;
}


//Thsi function checks if the group name is valid 
private boolean isGroupNameValid(String groupName) {
	String checkSql = "SELECT 1 FROM article_groups WHERE group_identifier = ?";
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(checkSql)) {
		
		
		stmt.setString(1, groupName);
		try (ResultSet rs = stmt.executeQuery()) {
			return rs.next();
		}
	} catch (SQLException e) {
		e.printStackTrace();
		
	}
	return false;
}


//This function chceks if the content is encrypted
private String encryptContent(String content) {
	int shift = 3;
	StringBuilder encrypted = new StringBuilder();
	
	for (char c : content.toCharArray()) {
		encrypted.append((char) (c + shift));
	}
	
	return encrypted.toString();
}

//This functions checks if the content is decrypted
private String decryptContent(String encryptedContent) {
	int shift = 3;
	StringBuilder decrypted = new StringBuilder();
	
	for (char c : encryptedContent.toCharArray()) {
		decrypted.append((char) (c - shift));
	}
	return decrypted.toString();
}

public List<SpecialAccessArticle> getSpecialAccessGroupArticles(String groupName) {
	String sql = """
			SELECT s.special_group_article_id, s.article_id, s.title, s.level, s,description,
			       s.encrypted_content, s.private_note, s.keywords, s.links
			FROM special_access_group_articles s
			WHERE s.group_name = ?
	""";
	
	List<SpecialAccessArticle> specailArticles = new ArrayList<>();
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
		
		stmt.setString(1, groupName);
		ResultSet rs = stmt.executeQuery();
		
		while (rs.next()) {
			String decryptedContent = decryptContent(rs.getString("encrypted_content"));
			
			
			SpecialAccessArticle specialArticle = new SpecialAccessArticle(
				rs.getLong("special_group_article_id"),
				rs.getLong("article_id"),
				groupName,
				rs.getString("title"),
				rs.getString("level"),
				rs.getString("description"),
				decryptedContent,
				rs.getString("private_note"),
				rs.getString("keywords") != null ? Arrays.asList(rs.getString("keywords").split(",")) : new ArrayList<>(),
				rs.getString("links") != null ? Arrays.asList(rs.getString("links").split(",")) : new ArrayList<>()
			);
	
		    specialArticles.add(specialArticle);
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	return specialArticles;
			
	
}

//This function lists the special access articles
public List<SpecialAccessArticle> getSpecialAccessArticles() {
	List<SpecialAccessArticle> specialArticles = new ArrayList<>();
	
	String sql = """
			SELECT special_group_article_id, article_id, group_name, title, level, description, encrypted_content, private_note, keywords, links
			FROM specail_access_group_articles
	""";
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql);
		 ResultSet rs = stmt.executeQuery()) {
		
		while (rs.next()) {
			long specialGroupArticleId = rs.getLong("special_group_article_id");
			long articleID = rs.getLong("article_id");
			String groupName = rs.getString("group_name");
			String title = rs.getString("title");
			String level = rs.getString("level");
			String description = rs.getString("description");
			
			String decryptedContent = "";
			try {
				decryptedContent = decryptContent(rs.getString("encrypted_content"));
			} catch (Exception e) {
				System.out.println("Failed to decrypt content for article ID: " + articleId);
				e.printStackTrace();
			}
			
			String privateNote = rs.getString("private_note");
			List<String> keywords = rs.getString("keywords") != null
					? Arrays.asList(rs.getString("keywords").split(","))
					: new ArrayList<>();
			List<String> links = rs.getString("links") != null
					? Array.asList(rs.getString("links").split)(","))
                    : new ArrayList<>();
            
            SpecialAccessArticle specialArticle = new SpecialAccessArticle(
            		specialGroupArticleId,
            		articleId,
            		groupName,
            		title,
            		level,
            		description,
            		decryptedContent,
            		privateNote,
            		keywords,
            		links
            		
             );
            
             specialArticles.add(specialArticle);
                    
  
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	return secialArticles;
}



//This function adds article 
public boolean addArticlesByGroupFiltersToSpecialAccessGroup(List<List<String>> groupFilters, String specialAccessGroupName) {
	List<Article> articlesToAdd = listArticlesByGroups(groupFilters);
	
	if (articlesToAdd.isEmpty()) {
		System.out.println("No articles found for the specified group filters.");
		return false;
	}
	
	String insertSql = """
			INSERT INTO special_access_group_articles (article_id, group_name, encrypted_content)
			VALUES (?, ?, ?)
			""";
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(insertSql)) {
		
		for (Article article : articlesToAdd) {
			String encryptedContent = encryptContent(article.getContent());
			
			
			stmt.setLong(1, article.getId());
			stmt.setString(2, specialAccessGroupName);
			stmt.setString(3, encryptedContent);
			stmt.addBatch();
		}
		
		int[] results = stmt.executeBatch();
		return results.length == articlesToAdd.size();
		
	} catch (SQLException e) {
		e.printStackTrace();
	}
	return false;
}

//This function shows the available articles
public List<String> listAvailableArticleGroups() {
	List<String> articleGroups = new ArrayList<>();
	String sql = "SELECT DISTINCT group_identifier FROM article_groups";
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql);
		 ResultSet rs = stmt.executeQuery()) {
		
		while (rs.next()) {
			articleGroups.add(rs.getString("group_identifier"));
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	return articlesGroups;
}

//This function adds article by a specific criteria 
public boolean addArticleToSpecialAccessGroupByCriteria(List<List<String>> groupFilters, Long articlesId) {
	List<Article> articlesToAdd = new ArrayList<>();
	
	try (Connection connection = DatabaseConnection.getConnection()) {
		
		if (groupFilters != null && !groupFilters.isEmpty()) {
			articlesToAdd = listArticlesByGroups(groupFilters);
			if (articlesToAdd.isEmpty()) {
				System.out.println("No articles found for the specified group filters.");
				return false;
			}
		} else if (articleId != null) {
			Article article = getArticleById(articleId);
			if (article != null) {
				articlesToAdd.add(artilce);
			} else {
				System.out.println("No article found with ID: " + articleId);
				return false;
			}
		}
		
		for (Article article : articlesToAdd) {
			String encryptedContent = encryptContent(artcile.getContent());
			String groupName = article.getGroupIdentifiers().isEmpty() ? "default" :article.getGroupIdentifiers().get(0);
			
			
			String insertSql = """
					INSERT INFO special_access_group_articles (article_id, encrypted_content, group_name, title, level, description, content, private_note, eywords, links)
					VALUES (?, ?, ?, ?, ?, ?, 'encrypted', ?, ?, ?)
				
			""";
			
			try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
				stmt.setLong(1, article.getId());
				stmt.setString(2, encryptedContent);
				stmt.setString(3, groupName);
				stmt.setString(4, article.getTitle());
				stmt.setString(5, article.getLevel());
				stmt.setString(6, article.getDescription());
				stmt.setString(7, article.getPrivateNote());
				stmt.setSring(8, String.join(",", article.getKeywords()));
				stmt.setSring(9, String.join(",", article.getLinks()));
				
				int rowsInserted = stmt.executeUpdate();
				if (rowsInserted > 0) {
					System.out.println("Article added to special access group with ID: " + artcle.getId());
					
				} else {
					System.out.println("Failed to add article with ID: " + article.getId() + "to special access group.");
					return false;
				}
			}
			
			if (!deleteArticle(article.getId())) {
				System.out.println("Failed to delete article from general articles table with ID: " + article.getId());
				return false;
			}
		}
	} catch (SQLException e) {
		e.printStackTrace();
		return false;
	}
	
	return true;
}


//This function transfers articles to the special access groups 
public boolean transferArticlesToSpecialAccessGroup(String groupName, Long articleId, String specialAccessGroupNmae) {
	List<Article> articlesToAdd = new ArrayList<>();
	
	try (Connection connection = DatabasrConnection.getConnection()) {
		if (groupName != null && !groupName.isEmpty()) {
			if(isGroupNameValid(groupName)) {
			   articlesToAdd = listArticleByGroupName(groupName);
			   if (articlesToAdd.isEmpty()) {
				   System.out.println("No artcles found for group: " + groupName);
				   return false;
				   
			   }
			} else {
				System.out.println("Group name does not exist: " + groupName);
				return false;
			}
		} else if (artilceId != null) {
			Article article = getArticleById(artileId);
			if (article != null) {
				articlesToAdd.add(article);
			} else {
				System.out.println("No article found with ID: " + articleId);
				return false;
			}
		}
		
		if (!isSpecialAccessGroupNameValid(specialAccessGroupName)) {
			addGroupToSpecialAccessGroups(specialAccessGroupName);
		}
		
		String insertSql = """
				INSERT INTO special_access_group_articles (article_id, group_name, encrypted_content, title, level, description, keywords, links, private_note, created_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";
		
		
		try (PreparedStatement stmt = connection.prepareStatement(insertSql)) {
			for (Article article : articlesToAdd) {
				String encryptedContent = encryptContent(article.getContent());
				
				stmt.setLong(1, article.getId());
				stmt.setString(2, specialAccessGroupName);
				stmt.setString(3, encryptedContent);
				stmt.setString(4, article.getTitle());
				stmt.setString(5, article.getLevel());
				stmt.setString(6, article.getDescription());
				stmt.setString(7, Strint.join(",", article.getKeywords()));
				stmt.setString(8, String.join(",", article.getLinks()));
				stmt.setString(9, article.getPrivateNote());
				stmt.addBatch();
			}
			
			int[] results = stmt.executeBatch();
			return results.length == articlesToAdd.size();
		}
	} catch (SQLExecution e) {
		e.printStackTrace();
		return false;
	}
}

//This function adds Students help message 
public boolean addStudentHelpMessage(long studentId, String messageType, String messageText) {
	String sql = "INSERT INTO student_help_message (student_id, message_type, message_text) VALUES (?, ?, ?)";
	
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
		
		stmt.setLong(1, studentId);
		stmt.setString(2, messagType);
		stmt.setString(3, messageText);
		
		int rowsAffected = stmt.executeUpdate();
		return rowsAffected > 0;
	} catch (SQLException e) {
		e.printStackTrace();
		return false;
	}
}

//This function helps with student search request 
public boolean addStudentSearchRequest(long studentId, String searchQuery) {
	String sql = "INSERT INTO student_seach_requests (student_id, search_query) VALUES (?, ?)";
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
		
		stmt.setLong(1, studentId);
		stmt.setString(2, searchQuery);
		
		int rowsffected = stmt.executeUpdate();
		return rowsAffected > 0;
	} catch (SQLException e) {
		e.printStackTrace();
		return false;
	}
}

public List<Map<String, Object>> getHelpMessagesByStudentId(long stuentId) {
	String sql = "SELECT message_id, message_type, message_text, created_at FROM student_help_messages WHERE student_id = ?";
	List<Map<String, Object>> messages = new ArrayList<>();
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
		
		stmt.setLong(1, studentId);
		ResultSet rs = stmt.executeQuery();
		
		while (rs.next()) {
			Map<String, Object> message = new HashMap<>();
			message.put("message_id", rs.getLong("message_id"));
			message.put("message_type", rs.getString("message_type"));
			message.put("message_text", rs.getString("message_text"));
			message.put("created_at", rs.getTimestamp("created_at"));
			message.add(message);
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	return messages;
}

//Tis function searche sthe reqest id request
public List<Map<String, Object>> getSearchRequestByStudentId(long studentId) {
	String sql = "SELECT request_id, search_query, created_at FROM student_search_requests WHERE student_id = ?";
	List<Map<String, Object>> requests = new ArrayList<>();
	
	try (Connection connection = DatabaseConnection.getConnection();
		 PreparedStatement stmt = connection.prepareStatement(sql)) {
			 
		stmt.setLong(1, studentId);
		ResultSet rs = stmt.executeQuery();
		
		while (rs.next()) {
			Map<String, Object> request = new HashMap<>();
			request.put("request_id", rs.getLong("request_id"));
			request.put("search_query", rs.getString("search_query"));
			request.put("created_at", rs.getTimestamp("created_at"));
			requests.add(request);
		 }
	 } catch (SQLException e) {
			 e.printStackTrace();
	 }
	 return requests;
}

//Tis function searches articles by their description
    public List<Article> searchArticlesByDescription(String description) {
	List<Article> articles = new ArrayList<>();
	String sql = """
			SELECT a.*,
			       GROUP_CONCAT(DISTINCT ag.group_identifier) AS groups,
			       GROUP_CONCAT(DISTINCT ak.keyword) AS keywords,
			       GROUP_CONCAT(DISTINCT al.link) AS links
			FROM articles a
			LEFT JOIN article_groups ag ON a.id = ag.article_id
			LEFT JOIN article_keywords ak ON a.id = ak.article_id
			LEFT_JOIN article_links al ON a.id = al.article_id
			WHERE a.description LIKE ?
			GROUP BY a.id;
	""";
	
	try (Connection connection = DatabaseConnection.getConnection();
		 preparedStatement stmt = connection.prepareStatement(sql)) {
		stmt.setString(1, "%" + description + "%");
		try (ResultSet rs = stmt.executeQuery()) {
			while (rs.next()) {
				articles.add(extractArticleRromResultSet(rs));
			}
		}
	} catch (SQLException e) {
		e.printStackTrace();
	}
	
	return articles;

}
	//This function gets all students messages
    public List<Map<String, Object>> getAllStudentMessages() {
        String sql = "SELECT student_id, message_id, message_type, message_text, created_at FROM student_help_messages";
        List<Map<String, Object>> messages = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){
                while (rs.next()) {
                    Map<String, Object> message = new HashMap<>();
                    message.put("student_id", rs.getLong("student_id"));
                    message.put("message_id", rs.getLong("message_id"));
                    message.put("message_type", rs.getString("message_type"));
                    message.put("message_text", rs.getString("message_text"));
                    message.put("created_at", rs.getTimestamp("created_at"));
                    messages.add(message);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            return messages;
    }


    //Tis function updates special access by article id
    public boolean updateSpecialAccessArticleById( long specialGroupArticleId, String title, String level,
    String description, String content, String groupName, List<String> keywords, List<String> links) {
        try (Connection conn = DatabaseConnection.getConnection()){
            if (isDuplicateSpecialAccessArticle(conn, specialGroupArticleId, title, level, description, content,
            groupName, keywords, links)) {
                return false;
            }

            String updateSql = """ UPDATE special_access_group_articles SET titlr = ?, level = ?,
            description = ?, content = ?, group_name = ? WHERE special_group_article_id = ? """;

            try (PreparedStatement stmt = conn.preparedStatement(updateSql)) {
                stmt.setString(1, title);
                stmt.setString(2, level);
                stmt.setString(3, description);
                stmt.setString(4, content);
                stmt.setString(5, groupName);
                stmt.setString(6, specialGroupArticleId);

                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    clearAssociationsForSpecialAccessArticle(conn, specialGroupArticleId);
                    inserAssociations(conn, INSERT_KEYWORD_SQL, specialGroupArticleId, keywords);
                    insertAssociations(conn, INSERT_LINK_SQL, specialGroupArticleId, links);

                    return true;
                }
            }    
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    //Tis function clears association for special access
    private void clearAssociationsForSpecialAccessArticle(Connection conn, long specialGroupArticleId) throws SQLException {
        String deleteKeywordsSql = "DELETE FROM special_access_keywords WHERE article_id = ?";
        String deleteLinksSql = "DELETE FROM special_access_links WHERE article_id = ?";

        try (PreparedStatement deleteKeywordsStmt = conn.prepareStatement(deleteKeywordsSql);
            PreparedStatement deleteLinkStmt = conn.prepareStatement(deleteLinksSql)) {
                deleteKeywordsStmt.setLong (1, specialGroupArticleId);
                deleteKeywordsStmt.executeUpdate();

                deleteLinksStmt.setLong(1, specialGroupArticleId);
                deleteLinksStmt.executeUpdate();
            }
    }


    //This function checks duplicate in the special access article 
    public boolean isDuplicateSpecialAccessArticle( Connection conn, long specialGroupArticleId, String title, String level,
    String description, String content, String groupName, List<String> keywords, List<String> links) throws SQLException {
        String duplicateCheckSql = """ SELECT special_group_srticle_id FROM special_access_group_articles WHERE title = ?
        AND level = ? AND description = ? AND content = ? AND group_name = ? AND special_group_article_id != ? """;

        try (PreparedStatement stmt = conn. prepareStatement(duplicateCheckSql)) {
            stmt.setString(1, title);
            stmt.setString(2, level);
            stmt.setString(3, description);
            stmt.setString(4, content);
            stmt.setString(5, groupName);
            stmt.setLong(6, specialGroupArticleId);

            try (ResultSet rs = stmt.executeQuery()){
                while (rs.next()) {
                    long foundId = rs.getLong("special_group_article_id");

                    if (isExactMatch(conn, foundId, keywords, links)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    //Tis function checks if there is a match
    private boolean isExactmatch(Connection conn, long specialGroupArticleId, List<String> keywords, 
    List<String> links) throws SQLException {
        List<String> existingKeywords = getAssociations(conn, "SELECT keyword FROM special_access_keywords 
        WHERE article_id = ?", specialGroupArticleId);

        List<String> existingLinks = getAssociations(conn, "SELECT link FROM special_access_links 
        WHERE article_id = ?", specialGroupArticleId);

        return existingKeywords.equals(keywords) && existing Links.equals(links);
    }

    //This function checks for special access article by their id
    public SpecialAccessArticle getSpecialAccessArticleId(long articleId) {
        String sql = """SELECT special_group_article_id, article_id, group_name, title, level, description,
        encrypted_content, private_note, keywords, links WHERE special_group_article_id = ? """;

        try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, articleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String decryptedContent = decryptContent(rs.getSytring("encrypted_content"));

                    return new SpecialAccessArticle(
                        rs.getLong("special_group_article_id").
                        rs.getLong("article_id"),
                        rs.getString("group_name"),
                        rs.getString("title"),
                        rs.getString("level"),
                        rs.getString("description"),
                        decrytedContent,
                        rs.getString("private_note"),
                        rs.getString("keywords") != null ?
                        Arrays.asList(rs.getString("keywords").split(",")) : new ArrayList<>(),
                        rs.getString("links") != null ?
                        Arrays.asList(rs.getString("links").split(",")) : new ArrayList<>() 
                    );
                }
            }

        } catch (SQLExeption e) {
            e.printStackTrace();
        }

        return null;
    }


    //This function delete the special access articles by their id
    public boolean deleteSpecialAccessArticleBySpecialId(long specialGroupArticleId) {
        String deleteSql = "DELETE FROM special_access_group_articles WHERE special_group_article_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setLong(1, specialGroupArticleId);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    public SpecialAccessArticle getSpecialAccessArticleBySpecialId(long specialGroupArticleID) {
        String sql = """SELECT special_group_article_id, article_id, group_name, title, level, description, 
        encrypted_content, private_note, keywords, links FROM special_access_group_srticles 
        WHERE special_group_article_id = ?""";

        try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, specialGroupArticleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String decryptedContent = decryptContent(rs.getString("encrypted_content"));

                    return new SpecialAccessArticle(
                        rs.getLong("special_group_article_id"),
                        rs.getLong("article_id"),
                        rs.getString("group_name"),
                        rs.getString("title"),
                        rs.getString("level"),
                        rs.getString("description"),
                        decryptedContent,
                        rs.getString("private_note"),
                        rs.getString("keywords") != null ?
                        Arrays.asList(rs.getString("keywords").split(",")) : new ArrayList<>(), 
                        rs.getString("links") != null ? Arrays.asList(rs.getString("links").split(",")) : new ArrayList<>()
                        
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    //This function deletes special articles  by the group
    public boolean deleteSpecialAccessArticleByGroup(String groupname) {
        String deleteSql = "DELETE FROM special_access_group_articles WHERE group_name = ?";

        try(Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(deleteSql)) {
            stmt.setString(1, groupName);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStacktrace();
            return false;
        }
    }

    //This function helps backup all special access articles
    public void backupAllSpecialAccessArticles(String fileName) {
        List<SpecialAccessArticle> specialArticles = listAllSpecialAccessArticles();
        backupSpecialAccessArticles(fileName, specialArticles);
    }

    //This function helps backup all special access articles by group
    public void backupSpecialAccessArticlesByGroup(String groupname, String fileName) {
        List<SpecialAccessArticle> specialArticles = getSpecialAccessGroupArticles(groupname);
        backupSpecialAccessArticles(fileName, specialArticles);
    }

    //This function helps backup all special access articles
    private void backupSpecialAccessArticles(String fileName, List<SpecialAccessArticle> specialArticles) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("SpecialGroupArticleID, ArticleID, GroupName, Title, Level, Description, Content,
            PrivateNote, Keywords, Links");

            for (SpecialAccessArticle article : specialArticles) {
                String keywords = String.join(";", article.getKeywords());
                String links = String.joint(";", srticle.getLinks());

                writer.printf("%d, %d, %s, %s, %s, %s, %s, %s, %s, %s%n",
                    article.getSpecialGroupArticleId(),
                    article.getArticleId(),
                    article.getGroupName(),
                    article.getTitle(),
                    article.getLevel(),
                    article.getDescription(),
                    article.getContent(),
                    article.getPrivateNote(),
                    keywords,
                    links
                    );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<SpecialAccessArticle> listAllSpecialAccessArticles() {
        String sql = """SELECT special_group_article_id, article_id, group_name, title, level, description, 
        encrypted_content, private_note, keywords, links FROM special_access_grouparticles """;

        List<SpecialAccessArticle> specialArticles = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
        PreparedStatement stmt = connection.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String decryptedContent = decryptContent(rs.getString("encrypted_content"));

                SpecialAccessArticle article = new SpecialAccessArticle(
                    rs.getLong("special_group_article_id"),
                    rs.getLong("article_id"),
                    rs.getString("group_name"),
                    rs.getString("title"),
                    rs.getString("level"),
                    rs.getString("description"),
                    decryptedContent, 
                    rs.getString("private_note"),
                    rs.getString("keywords") != null ? Arrays.asList(rs.getString("keywords").split(";")) : new ArrayList<>()

            
                );

                specialArticles.ass(article);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return specialArticles;
    }


    //This function restores all the backup articles
    public void restoreSpecialAccessArticlesFromBackup(String fileName, boolean replaceExisting) {
        List<SpecialAccessArticle> articles = readSpecialAccessArticlesFromCSV(fileName);

        try (Connection connection = DtabaseConnection.getConnection()) {
            if (ReplaceExisting) {
                deleteAllSpecialAccessArticles(connection);
            }

            for (SpecialAccessArticle article  : articles) {
                if(replecaeExisting || !doesSpecialAccessArticleExist(connection, article.getSpecialGroupArticleId())) {
                    addSpecialAccessArticleWithId(connection, article);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Tis function reads the articles from CSV for backup 
    private List<SpecialAccessArticle> readSpecialAccessArticlesFromCSV(String fileName) {
        List<SpecialAccessArticle> articles = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                SpecialAccessArticle article = new SpecialAccessArticle(Long.parseLong(fields[0]),
                long.parseLong(fields[1]),
                fields[2],
                fields[3],
                fields[4],
                fields[5],
                fields[6],
                fields[7],
                fields[8] != null ? Arrays.asList(fields[8].split(";")) : new ArrayList<>(),
                fields[9] != null ? Arraya.asList(fields[9].split(";")) : new ArrayList<>()
                );

                articles.add(article);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return articles;
    }


    //This function deletes all special access articles 
    private void deleteAllSpecialAccessArticles(Connection connection) throws SQLException {
        String sql = "DELETE FROM special_access_group_articles";
        try(PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }


    //This function checks if special access articles exist 
    private boolean doesSpecialAccessArticleExist(Connection connection, long specialGroupArticleId) throws SQLException{
        String sql = "SELECT special_group_article_id FROM special_access_group_articles WHERE apecial_group_article_id = ?";
        try (PreparedStatement stmt - connection.prepareStatement(sql)){
            stmt.setLong(1, specialGroupArticleId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
        
    }


    //This function adds special access article with ids
    private void addSpecialAccessArticleWithId(Connection connection, SpecialAccessArticle article) throws SQLException {
        String sql = """INSERT INTO special_access_group_articles (special_group_article_id, article_id,
        group_name, title, level, description, encrypted_content, private_note, keywords, links) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) """;

        try (PreparedStatement stmt = connection.prepareStatemnt(sql)){
            stmt.setLong(1, article.getSpecialGroupArticleId());
            stmt.setLong(2, article.getArticleId());
            stmt.setString(3, article.getGroupName());
            stmt.setString(4, article.getTitle());
            stmt.setString(5, article.getLevel());
            stmt.setString(6, article.getDescrption());
            stmt.setString(7, encryptContent(article.getContent()));
            stmt.setString(8, article.getPrivateNote());
            stmt.setString(9, String.join(";", article.getKeywords()));
            stmt.setString(10, String.join(";", article.getLinks()));

            stmt.executeUpdate();
        }
    }

	public List <Map<String, Object>> listSpecialAccessUsers() {
    String sql = """
    SELECT
      id, user_id, user_type, role, username, email, first_name, middle_name, last_name, preferred_name, 
      expertise_level, is_setup_complete
    FROM special_access_group_users;
    """;

    List<Map<String, Object>> users = new ArrayList<>();

    try (Connection connection = DatabaseConnection.getConnection();
         PreparedStatement stmt = connection.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("user_id", rs.getInt("user_id"));
                user.put("user_type", rs.getString("user_type"));
                user.put("role", rs.getString("role"));
                user.put("username", rs.getString("username"));
                user.put("email", rs.getString("email"));
                user.put("first_name", rs.getString("first_name"));
                user.put("middle_name", rs.getString("middle_name"));
                user.put("last_name", rs.getString("last_name"));
                user.put("preferred_name", rs.getString("preferred_name"));
                user.put("expertise_level", rs.getString("expertise_level"));
                user.put("is_setup_complete", rs.getBoolean("is_setup_complete"));
                users.add(user);
            }
         } catch (SQLException e) {
            e.printStackTrace();
         } 

         return users;

}

public boolean updateSpecialAccessRole(int userId, String role) {
    String sql = """
      UPDADATE special_access_group_users
      SET role = ?
      WHERE user_id = ?
      """;

      try (Connection connection = DatabaseConnection.getConnection();
           PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, role);
            stmt.setInt(2,userId);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
           } catch (SQLException e) {
            e.printStackTrace();
           }

           return false;
}

    //Tis function helps delete articles while filtering 
    public boolean deletaArticlesByFilter(Strig title, String level, List<List<String>> froupFilters,
    List<List<String>> krywordFilters) {
        StringBuilder query = new StringBuilder("DELETE a FORM articles a ");
        List<String> conditions = new ArrayList<>();
        List<Object> parameters = new Arraylist<>();

        if (keywordFiletrs != null && !keywordFilters.isEmpty()) {
            query.append("JOIN article_keywords ak ON a.id = ak.article_id ");
        } 
        if (groupFilters != null && !groupFilters.isEmpty()) {
            query.append("JOIN article_groups ag ON a.id = ag.article_id");
        }
        if (title != null && !title.isEmpty()) {
            conditions.add("a.title LIKE ?");
            parameters.add("%" +title + "%");
        }
        if (level != null && !level.isEmpty()) {
            conditions.add("a.level = ?");
            parameters.add(level);
        }
        if (groupFilters != null && !groupFilters.isEmpty()) {
            conditions.add(generateFilterCondition("ag.group_identifier", groupFilters));
            parameters.assAll(flattenFilterParameters(groupFilters));
        }
        if (keywordFilters != null && !keywordFilters.isEmpty()) {
            conditions.add(generateFilterCondition("ak.keyword", keywordFilters));
            parameters.addAll(flattenFilterParameters(keywordFilters));
        }

    if (!conditions.isEmpty()) {
        query.append("WHERE ").append(String.join(" AND", conditions));
    }
    try (Connection connection = DatabaseConnection.getConnection();
    PreparedStatement stmt = connection.prepareDtatement(query.toString())) {
        for (int i = 0, i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }
        int rowsAffected = stmt.executeUpdate();
        return rowsAffected > 0;
    } catch (SQLException e) {
        e.printStackTrace();
        return false;
    }
    }

    //This function helps filtering 
    private String generateFilterCondition(String columnName, List<List<String>> filters) {
        List<String> orConditions = new ArrayList<>();
        for (List<String> andGroup : filters) {
            List<String> andConditions = new ArrayList();
            for (String value : andGroup) {
            andConditions.add(columnName + " = ?");
        }
        orConditions.add("(" +String.join(" AND ", andConditions) + ")");
        }
    return String.join(" OR ", orConditions);
    }
    private List<Object> flattenFilterParameters(List<List<String>> filters) {
        List<Object> flattened = new ArrayList<>();
        for (List<String> filterGroup : filters) {
            flattened.addAll(filterGroup);
        }
        return flattened;
    }
	

	


}

        

            
