package com.aihackathon.topbog

import ai.grazie.api.gateway.client.SuspendableAPIGatewayClient
import ai.grazie.client.common.SuspendableHTTPClient
import ai.grazie.client.ktor.GrazieKtorHTTPClient
import ai.grazie.model.auth.v5.AuthData
import ai.grazie.model.cloud.AuthType
import ai.grazie.model.llm.profile.GoogleProfileIDs
import ai.grazie.model.llm.prompt.LLMPromptID

class ChatSender {
    suspend fun callToChat(githubProfileContent: String,
                           projectContent: String): String {
        val appToken = System.getenv("Grazie-Auth-Application-JWT")
        val chatProfile = GoogleProfileIDs.Chat.GeminiPro1_5
        val systemPrompt = """You are an intelligent assistant integrated within JetBrains IDEs, 
            |equipped with deep knowledge in social profiling and software development insights. 
            |Your expertise allows you to analyze a developer's GitHub profile comprehensively, 
            |understanding their programming languages proficiency, level of experience, 
            |and personal characteristics. Your role is to offer tailored advice on using 
            |JetBrains IDEs more effectively, improving coding practices, and recommending project-specific tips. 
            |Your insights are based on each developer's unique profile, 
            |aiming to enhance their development experience by leveraging your understanding of their skills, 
            |preferences, and professional background.""".trimMargin()
        val userProfilePrompt = "Analyze the GitHub profile details of a developer provided below: $githubProfileContent. " +
                "\nExamine their public repositories, predominant programming languages, and overall experience to " +
                "construct a comprehensive social and professional profile. Consider the following aspects: " +
                "\na) Programming Language Proficiency: Identify the languages the developer is familiar with. " +
                "Determine their area of specialization and the primary language they use. " +
                "\nb) Professional Experience Level: Assess their years of programming experience, " +
                "contributions to projects, and the complexity of their work. " +
                "Place them on a scale from novice to expert, noting any particular areas of expertise. " +
                "\nc) JetBrains IDE Usage: Evaluate the likelihood of their familiarity with JetBrains IDEs. " +
                "If applicable, gauge their proficiency level with these tools. " +
                "\nd) Personal Insights: Infer personal traits, hobbies, or unique characteristics from their profile. " +
                "This could include coding styles, project themes, or any personal hobbies mentioned. " +
                "\nThis analysis will form the basis for customized IDE tips, coding advice, and project recommendations " +
                "tailored to the developer's specific skills and preferences."
        val projectPrompt = """
                Based on the developer's social profile gleaned from their GitHub analysis and the specifics of the project code below, generate four personalized tips. Aim to blend practical advice with elements that reflect the developer's personality and preferences. The tips should cover the following areas:
                
                - Two or three tips should be highly specific to the project, focusing on enhancing coding practices, improving efficiency, or leveraging advanced features of JetBrains IDEs relevant to the project's technology stack.
                - One or two tips should be more personal or fun, related to plugins, IDE tricks, or features that match the developer's hobbies, interests, or unique coding style.
                
                Keep the following in mind:
                
                - Refrain from basic IDE suggestions for developers familiar with JetBrains tools.
                - Avoid language-specific advice for developers who are experts in the project's main programming language.
                - For those new to development or exhibiting a foundational skill level, suggest JetBrains Academy's free courses as a valuable resource to start or enhance their coding journey.
                - For fun and personal tips, consider the developer's likely personal interests or humor as inferred from their GitHub profile, suggesting plugins or IDE features that add a unique touch to their coding experience.
                
                Message style: Tips should be formatted as messages you'd see in the interface of a JetBrains IDE. Start each tip with "Tip n.", where n is the number of the tip. Place "**" before and after tip number. Maintain a focus on utility, delivering each tip in a concise, direct manner without introductory words. Use a tone of voice that's typical for IDE interfaces, ensuring clarity and relevance. 
                
                Guidelines for Crafting Tips:
                1. Begin each tip with a specific directive or suggestion that directly addresses one of the outlined areas.
                2. Ensure project-specific tips are closely related to the project's technology stack, coding patterns, or efficiency optimizations.
                3. For personal or fun tips, focus on aspects of the JetBrains IDEs that can bring joy, creativity, or personalization to the coding process. This could include suggesting a little-known plugin that aligns with their interests or a coding style enhancement that reflects their personality.
                
                The aim is to deliver a balanced set of advice that not only helps the developer with their current project but also makes their development experience more enjoyable and aligned with their personal interests.
                
                Project code: $projectContent
                """
        val client = SuspendableAPIGatewayClient(
            serverUrl = "https://api.app.stgn.grazie.aws.intellij.net",
            authType = AuthType.Application,
            httpClient = SuspendableHTTPClient.WithV5(
                GrazieKtorHTTPClient.Client.Default,
                authData = AuthData(appToken, originalApplicationToken = appToken)
            ),
        )
        //val fullPrompt = systemPrompt + "\n" + userProfilePrompt + "\n" + projectPrompt
        val profileResponseStream = client.llm().v6().chat {
            prompt = LLMPromptID("userProfile")
            profile = chatProfile
            messages {
                user("$systemPrompt/n$userProfilePrompt")
            }
        }

        val userProfileBuilder = StringBuilder()

        profileResponseStream.collect {
            userProfileBuilder.append(it.content)
        }
        val userProfile = ".Here is user profile from github: $userProfileBuilder /n"
        val tipsResponseStream = client.llm().v6().chat {
            prompt = LLMPromptID("userProfile")
            profile = chatProfile
            messages {
                user("$systemPrompt/n$userProfilePrompt$projectPrompt")
            }
        }
        val projectTipsBuilder = StringBuilder()

        tipsResponseStream.collect {
            projectTipsBuilder.append(it.content)
        }
        return projectTipsBuilder.toString()
    }
}