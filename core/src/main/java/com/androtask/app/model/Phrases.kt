package com.androtask.app.model

/**
 * Offline writing help: ready-made summaries and achievement bullets per role.
 * Users tap a phrase to insert it, then edit the bracketed placeholders.
 */
data class RolePhrases(val role: String, val summaries: List<String>, val bullets: List<String>, val skills: List<String>)

object Phrases {
    val actionVerbs = listOf(
        "Achieved", "Built", "Coordinated", "Delivered", "Designed", "Drove", "Improved", "Increased",
        "Launched", "Led", "Managed", "Negotiated", "Optimised", "Reduced", "Resolved", "Streamlined",
        "Trained", "Automated", "Mentored", "Implemented",
    )

    val roles = listOf(
        RolePhrases(
            "Software Developer",
            listOf(
                "Software developer with [X] years of experience building reliable web and mobile applications. Skilled in [languages] and passionate about clean, well-tested code that solves real user problems.",
                "Detail-oriented engineer who ships features end to end, from design to deployment, and works closely with product and design teams to deliver measurable impact.",
            ),
            listOf(
                "Built and shipped [feature] used by [N] users, increasing engagement by [X]%",
                "Reduced page load time by [X]% by optimising queries and caching",
                "Automated deployment with CI/CD, cutting release time from [X] hours to [Y] minutes",
                "Wrote unit and integration tests raising coverage from [X]% to [Y]%",
                "Mentored [N] junior developers through code reviews and pair programming",
            ),
            listOf("Kotlin", "Java", "JavaScript", "SQL", "Git", "REST APIs", "Unit testing", "Agile"),
        ),
        RolePhrases(
            "Sales / Retail",
            listOf(
                "Results-driven sales professional with a track record of exceeding targets and building lasting customer relationships.",
                "Friendly, energetic retail associate experienced in customer service, merchandising and cash handling.",
            ),
            listOf(
                "Exceeded monthly sales targets by [X]% for [N] consecutive months",
                "Grew a portfolio of [N] accounts worth $[X] in annual revenue",
                "Handled [N]+ customer enquiries per day with a [X]% satisfaction score",
                "Trained [N] new team members on products, POS and service standards",
                "Kept stock accurate by running weekly inventory counts and restocking",
            ),
            listOf("Customer service", "Upselling", "CRM", "POS systems", "Negotiation", "Cash handling"),
        ),
        RolePhrases(
            "Customer Support",
            listOf(
                "Patient, solution-focused support specialist skilled at resolving issues quickly over phone, chat and email.",
            ),
            listOf(
                "Resolved an average of [N] tickets per day while keeping CSAT above [X]%",
                "Cut average response time by [X]% by creating saved replies and help articles",
                "Escalated and tracked complex issues with engineering until resolution",
                "Wrote [N] knowledge-base articles that reduced repeat tickets by [X]%",
            ),
            listOf("Zendesk", "Live chat", "Conflict resolution", "Troubleshooting", "Empathy", "Typing 60+ WPM"),
        ),
        RolePhrases(
            "Nurse / Healthcare",
            listOf(
                "Compassionate registered nurse with [X] years of experience in [ward/speciality], committed to safe, patient-centred care.",
            ),
            listOf(
                "Provided care for up to [N] patients per shift in a fast-paced [unit] environment",
                "Administered medications and monitored vital signs following safety protocols",
                "Educated patients and families on care plans and discharge instructions",
                "Collaborated with doctors and therapists to coordinate treatment plans",
            ),
            listOf("Patient assessment", "Medication administration", "EHR / EMR", "BLS / ACLS", "Infection control", "Teamwork"),
        ),
        RolePhrases(
            "Teacher / Education",
            listOf(
                "Dedicated teacher who creates engaging, inclusive lessons and helps every student reach their potential.",
            ),
            listOf(
                "Planned and delivered lessons for [N] students in [subject] across grades [X–Y]",
                "Raised average test scores by [X]% through differentiated instruction",
                "Introduced [tool/approach] to increase classroom participation",
                "Communicated regularly with parents on progress and goals",
            ),
            listOf("Lesson planning", "Classroom management", "Differentiated instruction", "Google Classroom", "Assessment"),
        ),
        RolePhrases(
            "Driver / Logistics",
            listOf(
                "Safe, reliable driver with a clean licence and [X] years of on-time delivery experience.",
            ),
            listOf(
                "Completed [N]+ deliveries per day with a [X]% on-time rate",
                "Maintained a clean driving record over [X] years and [N] miles",
                "Performed daily vehicle inspections and logged hours in compliance with regulations",
                "Planned efficient routes, reducing fuel costs by [X]%",
            ),
            listOf("Route planning", "Vehicle inspection", "Customer service", "Forklift", "Time management", "Logbooks / ELD"),
        ),
        RolePhrases(
            "Administrative / Office",
            listOf(
                "Organised administrative professional who keeps offices running smoothly through excellent scheduling, communication and record keeping.",
            ),
            listOf(
                "Managed calendars and travel for [N] managers, scheduling [N]+ meetings per week",
                "Processed invoices and expense reports with 100% accuracy",
                "Introduced a digital filing system that cut document search time by [X]%",
                "Served as first point of contact for visitors and phone enquiries",
            ),
            listOf("Microsoft Office", "Google Workspace", "Scheduling", "Data entry", "Bookkeeping", "Communication"),
        ),
        RolePhrases(
            "Student / First Job",
            listOf(
                "Motivated [degree] student eager to apply strong [skills] and a fast learning attitude in a [role] position.",
                "Hard-working recent graduate with hands-on project experience and a passion for [field].",
            ),
            listOf(
                "Led a team of [N] students to deliver [project] ahead of deadline",
                "Volunteered [N] hours at [organisation], helping [task]",
                "Balanced part-time work of [N] hours per week with full-time studies",
                "Earned [award/grade] for [course or project]",
            ),
            listOf("Teamwork", "Communication", "Problem solving", "Microsoft Office", "Time management", "Fast learner"),
        ),
    )
}
