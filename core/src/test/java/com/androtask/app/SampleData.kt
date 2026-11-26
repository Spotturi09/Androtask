package com.androtask.app

import com.androtask.app.model.*

object SampleData {
    fun resume(template: TemplateId = TemplateId.MODERN, photo: Boolean = false) = Resume(
        title = "Sample",
        personal = PersonalInfo(
            fullName = "Priya Sharma", jobTitle = "Senior Android Developer",
            email = "priya.sharma@example.com", phone = "+1 555 010 2030", location = "Austin, TX",
            linkedin = "linkedin.com/in/priyasharma", website = "priya.dev",
            photoPath = if (photo) "/tmp/none.jpg" else null,
        ),
        summary = "Android developer with 7 years of experience shipping consumer apps used by millions. " +
            "Specialised in Kotlin, Jetpack Compose and offline-first architecture, with a track record of " +
            "improving performance, crash-free rates and store ratings.",
        experience = listOf(
            Experience(role = "Senior Android Developer", company = "Brightpath Health", location = "Austin, TX",
                startDate = "Mar 2022", current = true,
                description = "Led the rewrite of the patient app in Jetpack Compose, raising the Play rating from 3.9 to 4.7\n" +
                    "Cut cold-start time by 42% through baseline profiles and lazy initialisation\n" +
                    "Mentored 4 developers and introduced code review guidelines adopted by 3 teams"),
            Experience(role = "Android Developer", company = "Kiwi Commerce", location = "Remote",
                startDate = "Jun 2019", endDate = "Feb 2022",
                description = "Built checkout and payments flows processing \$12M per year\n" +
                    "Raised crash-free sessions from 97.1% to 99.6%\n" +
                    "Automated releases with GitHub Actions and Fastlane"),
            Experience(role = "Junior Developer", company = "Appworks Studio", startDate = "2017", endDate = "2019",
                description = "Delivered 9 client apps end to end\nWrote unit tests for core modules"),
        ),
        education = listOf(Education(degree = "B.Sc. Computer Science", school = "University of Texas at Austin",
            startDate = "2013", endDate = "2017", details = "GPA 3.8 · Dean's List")),
        skills = listOf("Kotlin", "Jetpack Compose", "Coroutines", "Room", "Retrofit", "CI/CD", "Unit testing")
            .mapIndexed { i, s -> Skill(name = s, level = 5 - i % 3) },
        languages = listOf(Skill(name = "English", level = 5), Skill(name = "Hindi", level = 5), Skill(name = "Spanish", level = 3)),
        projects = listOf(Project(name = "OpenTrails", link = "github.com/priya/opentrails",
            description = "Open-source hiking app with offline maps, 50K downloads")),
        certifications = listOf(Certification(name = "Associate Android Developer", issuer = "Google", date = "2020")),
        references = listOf(Reference(name = "Daniel Ortiz", position = "Engineering Manager, Brightpath", contact = "daniel@example.com")),
        customTitle = "Interests", customContent = "Trail running, open-source, mentoring at Women Who Code",
        style = ResumeStyle(template = template),
    )
}
