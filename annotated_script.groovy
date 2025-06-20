# [1]: Imports the ComponentAccessor to access Jira components
import com.atlassian.jira.component.ComponentAccessor
# [2]: Imports the ProjectRoleManager to manage project roles
import com.atlassian.jira.security.roles.ProjectRoleManager
# [3]: Imports the ProjectManager for project operations
import com.atlassian.jira.project.ProjectManager
# [4]: Static import for ISSUE_TYPE constant
import static com.atlassian.jira.issue.IssueFieldConstants.ISSUE_TYPE
# [5]: Imports Logger for logging
import org.apache.log4j.Logger
# [6]: Blank line for readability

# [7]: Creates a logger for the script
def log = Logger.getLogger("com.onresolve.scriptrunner.runner.Script")
# [8]: Blank line for readability

# [9]: Gets the project role manager component
def projectRoleManager = ComponentAccessor.getComponent(ProjectRoleManager)
# [10]: Gets the project manager component
def projectManager = ComponentAccessor.getComponent(ProjectManager)
# [11]: Retrieves the currently logged in user
def user = ComponentAccessor.jiraAuthenticationContext.getLoggedInUser()
# [12]: Gets the Issue Type field from the context
def issueTypeField = getFieldById(ISSUE_TYPE)
# [13]: Blank line for readability

# [14]: Checks if the Issue Type field was found
if (!issueTypeField) {
    # [15]: Logs a warning if the Issue Type field is missing
    log.warn("❌ Не удалось получить поле Issue Type. Завершение скрипта.")
    # [16]: Stops the script when Issue Type field is missing
    return
}
# [17]: Blank line for readability

# [18]: Comment describing the next section: getting the project
// 🔹 Получаем проект
# [19]: Retrieves the project from the issue context
def project = issueContext?.projectObject
# [20]: Checks if a project was found
if (!project) {
    # [21]: Logs a warning when the project can't be determined
    log.warn("❌ Не удалось определить проект из issueContext.")
    # [22]: Exits the script when project is missing
    return
}
# [23]: Blank line for readability

# [24]: Comment describing the next section: getting user roles
// 🔹 Получаем роли пользователя в проекте
# [25]: Retrieves the user's roles in the project
def userRoles = projectRoleManager.getProjectRoles(user, project)*.name
# [26]: Logs the user's roles for debugging
log.debug("✅ Роли пользователя: ${userRoles}")
# [27]: Blank line for readability

# [28]: Comment describing the next section: fetching project issue types
// 🔹 Получаем все доступные типы задач проекта (замена getIssueTypesForProject)
# [29]: Gets all issue types available in the project
def allIssueTypes = project.issueTypes
# [30]: Checks if issue types were retrieved
if (!allIssueTypes || allIssueTypes.isEmpty()) {
    # [31]: Logs a warning if no issue types were found
    log.warn("❌ Не удалось получить Issue Types из проекта.")
    # [32]: Exits when issue types could not be retrieved
    return
}
# [33]: Blank line for readability

# [34]: Logs the available issue types before filtering
log.debug("✅ Доступные Issue Types (до фильтрации): ${allIssueTypes*.name}")
# [35]: Blank line for readability

# [36]: Comment about defining allowed issue types for roles
// 🔹 Определяем доступные Issue Types для разных ролей
# [37]: List of issue types allowed for administrators
def allowedForAdministrators = [
    # [38]: Administrator can create Epic issues
    "Epic",
    # [39]: Administrator can create regular tasks
    "Задача",
    # [40]: Administrator can create regulator tasks
    "Задача от Регулятора (ЦБ,ПП,УП,Мин.Фин.)",
    # [41]: Administrator can create change request tasks
    "Заявка на внесение изменений в АРМах АСБТ",
    # [42]: Administrator can create information request tasks
    "Информационный запрос",
    # [43]: Administrator can create bug issues
    "Ошибка",
    # [44]: Administrator can create sub-tasks
    "Подзадача"
]
# [45]: Blank line for readability

def allowedForOthers = [
    # [46]: Non-admin user can create regulator tasks
    "Задача от Регулятора (ЦБ,ПП,УП,Мин.Фин.)",
    # [47]: Non-admin user can create change requests
    "Заявка на внесение изменений в АРМах АСБТ",
    # [48]: Non-admin user can create information requests
    "Информационный запрос",
    # [49]: Non-admin user can create bugs
    "Ошибка",
    # [50]: Non-admin user can create sub-tasks
    "Подзадача"
]
# [51]: Blank line for readability

# [52]: Comment about checking if user is an administrator
// 🔹 Проверяем, является ли пользователь администратором
# [53]: Determines whether the user belongs to an admin role
def isAdmin = userRoles.any { it in ["Administrators", "Project Administrators", "jira-administrators"] }
# [54]: Blank line for readability

# [55]: Comment about filtering available issue types by role
// 🔹 Фильтруем доступные Issue Types на основе роли
# [56]: Start of conditional expression to filter issue types
def filteredOptions = isAdmin ?
    # [57]: Use admin allowed list when user is admin
    allIssueTypes.findAll { issueType -> allowedForAdministrators.contains(issueType.name) } :
    # [58]: Use non-admin allowed list otherwise
    allIssueTypes.findAll { issueType -> allowedForOthers.contains(issueType.name) }
# [59]: Blank line for readability

# [60]: Logs the filtered issue types for the user
log.debug("✅ Фильтрованные Issue Types для ${user?.displayName}: ${filteredOptions*.name}")
# [61]: Blank line for readability

# [62]: Comment about setting the filtered issue type options
// 🔹 Устанавливаем отфильтрованные Issue Types
# [63]: Applies the filtered issue types to the Issue Type field
issueTypeField.setFieldOptions(filteredOptions.collectEntries { [(it.id): it.name] })
