package com.aihackathon.topbog

import java.io.File

class ProjectFilesReader {
    private fun getSourceFiles(
        projectPath: String?
    ): String {
        if (projectPath == null) {
            return " "
        }
        val finalList = File(projectPath).walk()
            .filter { it.isFile }
            .map { it.absolutePath }
            .toList()

        val fileDelimiter = " <EOF> "
        var projectContext = ""
        finalList.forEach {
            try {
                val content = File(it).readText()
                projectContext += "FILE NAME: $it\\n"
                projectContext += content + fileDelimiter
            } catch (ex: Exception) {
                // Ignore the exception and continue
            }
        }

        return projectContext
    }
    fun getProjectFiles(
        paths: List<String>
    ): String {
        val allFilesContent = mutableListOf<String>()
        paths.forEach { path ->
            val projectFilesContent = getSourceFiles(path)
            allFilesContent.add(projectFilesContent)
        }
        return allFilesContent.joinToString(separator = "\nNext Source Path\n")
    }
}


