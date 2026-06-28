package com.example.data

object DocumentTemplates {
    val templates = listOf(
        Template(
            name = "Personal Journal",
            iconName = "menu_book",
            title = "Personal Journal",
            content = """
                # 📝 Personal Journal Entry
                
                **Date:** ${java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                **Mood:** 🌟 Gratitude and Focus
                
                ---
                
                ## 💭 Daily Reflection
                > Write down what's on your mind today, your current feelings, and thoughts.
                
                - **What went exceptionally well today:**
                  1. Completed the immersive UI transition beautifully.
                  2. 
                - **What I'm grateful for:**
                  - 
                
                ## 🎯 Goals & Focus
                - [ ] Keep typing in the distraction-free focus mode
                - [ ] Review document versions in the history pane
                
                ---
                *Created using the Personal Journal Template.*
            """.trimIndent()
        ),
        Template(
            name = "Blog Post Draft",
            iconName = "edit",
            title = "My New Blog Post",
            content = """
                # ✍️ Blog Post Title: The Art of Distraction-Free Flow
                
                **Author:** Creative Mind
                **Status:** Draft
                **Tags:** #writing #markdown #mindfulness
                
                ---
                
                ## Introduction
                Grab your reader's attention here with a strong opening paragraph. Explain what you'll be sharing.
                
                ## 💡 Core Philosophy
                > "The interface should be a shadow that only appears when summoned."
                
                Writing in **Markdown** allows you to stay in the zone. You don't have to navigate heavy menus to format your text.
                
                ### Tips for Better Writing:
                1. Start in **Focus Mode** to block out noise.
                2. Use rich headers to structure your ideas.
                3. Export to PDF or HTML when done!
                
                ---
                *Created using the Blog Post Template.*
            """.trimIndent()
        ),
        Template(
            name = "Meeting Minutes",
            iconName = "groups",
            title = "Meeting Minutes - Sync",
            content = """
                # 👥 Meeting Minutes
                
                **Topic:** Weekly Alignment Sync
                **Date:** ${java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.getDefault()).format(java.util.Date())}
                **Attendees:** Alice, Bob, Charlie, Diana
                
                ---
                
                ## 📌 Agenda
                1. Review feature progress
                2. Address blockers and server synchronization
                3. New template library design
                
                ## 📝 Discussion Notes
                - **Feature Rollout:** Version control and offline persistence are complete.
                - **Export Engine:** Native PDF printing layout is fully integrated.
                - **UI Theme:** Immersive dark design matches user specifications.
                
                ## 🎯 Action Items
                - [ ] **Alice**: Roll out new update and check compiler build
                - [ ] **Bob**: Confirm local storage schemas
                
                ---
                *Created using the Meeting Minutes Template.*
            """.trimIndent()
        )
    )

    data class Template(val name: String, val iconName: String, val title: String, val content: String)
}
