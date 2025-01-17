package org.example
import groovy.sql.Sql
import java.sql.Connection
import java.sql.DriverManager
class DatabaseService {
    static Connection connection
    static Sql sql
    static Connection getDBConnection() {
        if (connection == null) {
            String jdbcurl = "jdbc:oracle:thin:@//10.0.0.35:11521/clarity"
            String username = "niku"
            String password = "niku"
            try {
                println("Attempting to connect to the database...")
                connection = DriverManager.getConnection(jdbcurl, username, password)
                sql = new Sql(connection)
                println("Database connection successful.")
            } catch (Exception e) {
                println("Caught exception: ${e.message}")
                e.printStackTrace()
            }
        }
        return connection
    }
    static List<Map> getProjects(List<String> projectNamesFromJSON) {
        getDBConnection()
        def allProjects = []
        String query = """
        SELECT ID, CODE, NAME
        FROM INV_INVESTMENTS
        WHERE NAME IN (${projectNamesFromJSON.collect { "'${it}'" }.join(",")})
        """
        sql.eachRow(query) { row ->
            allProjects << [
                    id  : row.id,
                    code: row.code,
                    name: row.name
            ]
        }
        return allProjects
    }
    static List<Map> getTasks(Integer projectId, List<String> taskNamesFromJSON) {
        getDBConnection()  // Ensure the connection is established
        def allTasks = []
        def query = """
        SELECT PRID, PRNAME
        FROM PRTASK
        WHERE PRPROJECTID = ? AND PRNAME IN (${taskNamesFromJSON.collect { '?' }.join(', ')} )
        """
        sql.eachRow(query, [projectId, *taskNamesFromJSON]) { row ->
            allTasks << [
                    id  : row.prid,
                    name: row.prname
            ]
        }
        return allTasks
    }
    static String getProjectInternalId(String projectName) {
        getDBConnection()
        def query = "SELECT ID FROM INV_INVESTMENTS WHERE name = ?"
        def result = sql.firstRow(query, [projectName])
        return result?.ID ?: null
    }
    static Map getResourceDetails(String resourceCode) {
        getDBConnection()
        def query = "SELECT ID, UNIQUE_NAME FROM SRM_RESOURCES WHERE UNIQUE_NAME = ?"
        def resource = sql.firstRow(query, [resourceCode])
        return resource ? [id: resource.ID, code: resource.UNIQUE_NAME] : null
    }
}
