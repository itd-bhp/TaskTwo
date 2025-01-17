package org.example

class DataInsertion {
    static void main(String[] args) {
        ReadJson parsing = new ReadJson()
        try {
            // Step 1: Read data from JSON files
            List<Map> projectsData = parsing.projectJson()
            println("Projects Data: ${projectsData}")
            List<Map> tasksData = parsing.taskJson()
            println("Tasks Data: ${tasksData}")
            List<Map> resourcesData = parsing.resourceJson()
            println("Resources Data: ${resourcesData}")
            List<Map> assignmentsData = parsing.assignJson()
            println("Assignments Data: ${assignmentsData}")

            // Step 2: Post projects and tasks to Clarity using the ClarityService
            postProjectsAndTasksToClarity(projectsData, tasksData)

            // Step 3: Filter projects and tasks from the database using DatabaseService
            List<String> projectNamesFromJSON = projectsData.collect { it.project_name }
            List<String> taskNamesFromJSON = tasksData.collect { it.name }
            List<Map> filteredProjects = DatabaseService.getProjects(projectNamesFromJSON)
            println "Filtered Projects from Database: ${filteredProjects}"

            List<Map> filteredTasks = []
            filteredProjects.each { project ->
                def projectId = (project.id as Integer)
                List<Map> tasksForProject = DatabaseService.getTasks(projectId, taskNamesFromJSON)
                filteredTasks.addAll(tasksForProject)
                println "Filtered Tasks for Project ${project.name} from Database: ${tasksForProject}"
            }

            // Step 4: Generate Resources XOG XML using the new ResourceXOGGenerator
            String resourcesXML = ResourceXOGGenerator.generateResourcesXML(resourcesData)
            println "Generated Resources XOG XML:\n${resourcesXML}"
            XOGUtil.saveToFile("resources_xog.xml", resourcesXML)

            // Step 5: Generate Assignments XOG XML using the new AssignmentXOGGenerator
            String assignmentsXML = AssignmentXOGGenerator.generateAssignmentXOGXML(resourcesData, assignmentsData, tasksData, projectsData, filteredProjects)
            println "Generated Assignments XOG XML:\n${assignmentsXML}"
            XOGUtil.saveToFile("assignments_xog.xml", assignmentsXML)

            // Step 6: Post the generated XML data to Clarity
            String xmlData = new File("assignments_xog.xml").text
            ClarityService.postTeamsWithResources(xmlData)
            println("Posted resources as teams to Clarity PPM successfully.")
        } catch (Exception e) {
            println("Error reading JSON files: ${e.message}")
            e.printStackTrace()
        }
    }

    static void postProjectsAndTasksToClarity(List<Map> projectsData, List<Map> tasksData) {
        try {
            ClarityService.postProjectsWithTasks(projectsData, tasksData)
            println("Posted projects and tasks to Clarity PPM successfully.")
        } catch (Exception e) {
            println("Error posting projects and tasks to Clarity PPM: ${e.message}")
            e.printStackTrace()
        }
    }
}
