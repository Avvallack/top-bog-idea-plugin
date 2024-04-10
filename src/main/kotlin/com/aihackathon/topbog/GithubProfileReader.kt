import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class GithubProfileReader {
    private fun getUsername(): String {
        val processBuilder = ProcessBuilder()
        processBuilder.command("bash", "-c", "ssh git@github.com")
        var output: String? = null
        try {
            val process = processBuilder.start()
            var line: String?

            // Error Output
            val stdErrorReader = BufferedReader(InputStreamReader(process.errorStream))
            val stringBuilderError = StringBuilder()
            while (stdErrorReader.readLine().also { line = it } != null) {
                stringBuilderError.append(line + "\n")
            }

            val exitVal = process.waitFor()
            if (exitVal == 1) {
                //println("Success!")
                output = stringBuilderError.toString()
                //println("Output: $output")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        //val splitOutput = output?.split("/n")?.get(1)?.split(" ")
        //var githubUsername = splitOutput?.get(1)?.removeSuffix("!")
        val parts = output?.split(" ")
        if (parts != null) {
            for (part in parts) {
                if (part.endsWith("!")) {
                    val githubUser = part.dropLast(1)
                    return githubUser
                }

            }
        }
        return "unknownUser"
    }

    private fun parseGithubProfile(username: String): String {
        if (username != "unknownUser") {
            val githubLink = "https://github.com/$username"
            return githubLink
        }
        return "unknownUser"
    }

    private fun loadGithubProfile(githubUrl: String): String{
        if (githubUrl != "unknownUser") {
            val url = URL(githubUrl)
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()
            val content = inputStream.bufferedReader().use(BufferedReader::readText)
            return content
        }
        else {
            return " "
        }

    }

    fun getGithubProfile(): String{
        val profileReader = GithubProfileReader()
        val username = profileReader.getUsername()
        val githubProfile = profileReader.parseGithubProfile(username)
        val githubProfileContent = profileReader.loadGithubProfile(githubProfile)
        return githubProfileContent
    }
}