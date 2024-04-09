package com.aihackathon.topbog

import GithubProfileReader
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.application.ApplicationManager
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.exists

class OpenMarkdownOnStartup : StartupActivity {
    private fun writeStringToMdFile(content: String, filePath: String) {
        try {
            val file = File(filePath)
            file.writeText(content)
        } catch (e: Exception) {
            println("Exception occurred while writing to file: $e")
        }
    }

    private fun waitForFile(filePath: String, timeout: Long = 5000000) {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout)
        while (System.nanoTime() < deadline) {
            if (Path(filePath).exists()) return
            Thread.sleep(50)
        }
        throw IllegalStateException("File $filePath didn't become available within ${timeout}ms")
    }
    private fun getGithubProfile(): String{
        val githubLoader = GithubProfileReader()
        return githubLoader.getGithubProfile()
    }

    private fun getProjectFiles(project: Project): String {
        val manager = ModuleManager.getInstance(project)
        val modules = manager.modules
        val fileLists = ArrayList<String>()
        for (module in modules) {
            val root = ModuleRootManager.getInstance(module)
            for (file in root.sourceRoots) {
                fileLists.add(file.path)
            }
        }
        val projectReader = ProjectFilesReader()
        val projectContext = projectReader.getProjectFiles(fileLists)
        return projectContext
    }
    override fun runActivity(project: Project) {
        val fileName = "Start with the project tips.md"
        val filePath = "${project.basePath}/$fileName"
        writeStringToMdFile("", filePath)
        val githubContext = getGithubProfile()

        val projectContext = getProjectFiles(project)

        val chat = ChatSender()

        runBlocking {
            val projectTips = chat.callToChat(githubContext, projectContext)
            val tipsContent = "# Your Personalized Tips for Project\n\n$projectTips"
            writeStringToMdFile(tipsContent, filePath)
            waitForFile(filePath)
        }

        try {
            val file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(filePath))
            if (file != null) {
                ApplicationManager.getApplication().invokeLater {
                    FileEditorManager.getInstance(project)?.openFile(file, true)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}