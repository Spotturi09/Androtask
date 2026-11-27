package com.androtask.app

import com.androtask.app.ai.AiException
import com.androtask.app.ai.DocumentText
import com.androtask.app.ai.GeminiClient
import com.androtask.app.ai.ResumeAi
import com.androtask.app.model.ResumeStyle
import com.androtask.app.model.SectionType
import com.androtask.app.model.TemplateId
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Tests the Gemini integration against a local fake of the generateContent endpoint. */
class ResumeAiTest {
    private var status = 200
    private var reply = ""
    private val requests = mutableListOf<Pair<String, String>>() // path+headers summary, body

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { ex ->
            val body = ex.requestBody.readBytes().decodeToString()
            requests += "${ex.requestURI.path}|key=${ex.requestHeaders.getFirst("x-goog-api-key")}" to body
            val bytes = reply.toByteArray()
            ex.sendResponseHeaders(status, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        start()
    }

    @After fun stop() = server.stop(0)

    private fun client(key: String = "test-key") =
        GeminiClient(key, model = "test-model", baseUrl = "http://127.0.0.1:${server.address.port}/v1beta")

    private fun geminiAnswer(text: String) = buildJsonObject {
        put("candidates", buildJsonArray {
            add(buildJsonObject {
                put("finishReason", "STOP")
                put("content", buildJsonObject {
                    put("parts", buildJsonArray { add(buildJsonObject { put("text", text) }) })
                })
            })
        })
    }.toString()

    private val importedJson = """
        {"fullName":"Maria Lopez","jobTitle":"Registered Nurse","email":"maria@example.com","phone":"555-0100",
         "location":"Phoenix, AZ","summary":"Caring RN.",
         "experience":[{"role":"RN","company":"St. Mary's","startDate":"2020","current":true,"endDate":"x",
                        "bullets":["• Cared for 6 patients per shift","Trained new nurses"]},
                       {"role":"","company":"","bullets":["ignored empty job"]}],
         "education":[{"degree":"BSN","school":"ASU","endDate":"2019","details":["Honours"]}],
         "skills":[{"name":"BLS","level":9},{"name":"","level":1}],
         "languages":[{"name":"Spanish","level":5}],
         "otherSectionTitle":"Volunteering","otherSectionContent":["Red Cross"],
         "unknownField":true}
    """.trimIndent()

    @Test fun importSendsPdfInlineAndMapsTheAnswer() {
        reply = geminiAnswer("```json\n$importedJson\n```")
        val pdf = "%PDF-1.4 fake".toByteArray()
        val r = ResumeAi(client()).importResume(pdf, "application/pdf", "Maria CV 2026.pdf")

        val (meta, body) = requests.single()
        assertEquals("/v1beta/models/test-model:generateContent|key=test-key", meta)
        val parts = Json.parseToJsonElement(body).jsonObject["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray
        val inline = parts[1].jsonObject["inline_data"]!!.jsonObject
        assertEquals("application/pdf", inline["mime_type"]!!.jsonPrimitive.content)
        assertEquals(java.util.Base64.getEncoder().encodeToString(pdf), inline["data"]!!.jsonPrimitive.content)
        assertTrue(body.contains("\"responseMimeType\":\"application/json\""))

        assertEquals("Maria CV 2026", r.title)
        assertEquals("Maria Lopez", r.personal.fullName)
        assertEquals(1, r.experience.size)
        assertTrue(r.experience[0].current)
        assertEquals("", r.experience[0].endDate)
        assertEquals("Cared for 6 patients per shift\nTrained new nurses", r.experience[0].description)
        assertEquals(listOf("BLS"), r.skills.map { it.name })
        assertEquals(5, r.skills[0].level) // clamped
        assertEquals("Volunteering", r.customTitle)
        assertTrue(SectionType.CUSTOM in r.visibleSections)
        assertTrue(r.completeness() > 50)
    }

    @Test fun docxIsSentAsExtractedText() {
        reply = geminiAnswer(importedJson)
        val docx = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { z ->
                z.putNextEntry(ZipEntry("word/document.xml"))
                z.write("<w:document><w:body><w:p><w:r><w:t>Maria Lopez</w:t></w:r></w:p><w:p><w:r><w:t>Nurse &amp; carer</w:t></w:r></w:p></w:body></w:document>".toByteArray())
                z.closeEntry()
            }
        }.toByteArray()
        assertEquals("Maria Lopez\nNurse & carer", DocumentText.fromDocx(docx))
        ResumeAi(client()).importResume(docx, "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "cv.docx")
        assertTrue(requests.single().second.contains("Maria Lopez\\nNurse & carer"))
        assertFalse(requests.single().second.contains("inline_data"))
    }

    @Test fun improveKeepsIdentityStylePhotoAndOrder() {
        reply = geminiAnswer(importedJson)
        val original = SampleData.resume(TemplateId.SIDEBAR).copy(
            style = ResumeStyle(template = TemplateId.SIDEBAR, accent = 0xFF0F766E),
            hidden = setOf(SectionType.REFERENCES),
        ).let { it.copy(personal = it.personal.copy(photoPath = "/photos/p.jpg")) }.moveSection(SectionType.SKILLS, -3)
        val improved = ResumeAi(client()).improveResume(original, jobDescription = "ICU nurse wanted")

        assertNotEquals(original.id, improved.id)
        assertEquals("Sample (Tailored)", improved.title)
        assertEquals(original.style, improved.style)
        assertEquals(original.sectionOrder, improved.sectionOrder)
        assertEquals(original.hidden, improved.hidden)
        assertEquals("/photos/p.jpg", improved.personal.photoPath)
        assertEquals("Maria Lopez", improved.personal.fullName)
        val body = requests.single().second
        assertTrue(body.contains("ICU nurse wanted"))
        assertTrue(body.contains("Priya Sharma")) // current resume was sent
        assertFalse(body.contains("/photos/p.jpg")) // photo path never leaves the device
    }

    @Test fun improveBulletsReturnsCleanLines() {
        reply = geminiAnswer("- Led triage for [N] patients\n\n• Cut wait times by [X]%\n")
        val job = SampleData.resume().experience.first()
        assertEquals("Led triage for [N] patients\nCut wait times by [X]%", ResumeAi(client()).improveBullets(job, "Nurse"))
    }

    @Test fun errorsAreMappedToFriendlyMessages() {
        fun kindFor(code: Int, body: String): AiException.Kind {
            status = code; reply = body
            return assertThrows(AiException::class.java) { ResumeAi(client()).improveSummary(SampleData.resume()) }.kind
        }
        assertEquals(AiException.Kind.BAD_KEY, kindFor(400, """{"error":{"message":"API key not valid. Please pass a valid API key."}}"""))
        assertEquals(AiException.Kind.QUOTA, kindFor(429, """{"error":{"message":"Resource exhausted"}}"""))
        assertEquals(AiException.Kind.BAD_MODEL, kindFor(404, """{"error":{"message":"models/test-model is not found"}}"""))
        assertEquals(AiException.Kind.BLOCKED, kindFor(200, """{"promptFeedback":{"blockReason":"SAFETY"}}"""))
        assertEquals(AiException.Kind.BAD_RESPONSE, kindFor(200, """{"candidates":[]}"""))
    }

    @Test fun missingKeyFailsWithoutNetworkCall() {
        val e = assertThrows(AiException::class.java) { ResumeAi(client(key = " ")).improveSummary(SampleData.resume()) }
        assertEquals(AiException.Kind.NO_KEY, e.kind)
        assertTrue(requests.isEmpty())
    }

    @Test fun nonResumeAnswerIsRejected() {
        reply = geminiAnswer("{\"fullName\":\"\"}")
        val e = assertThrows(AiException::class.java) {
            ResumeAi(client()).importResume("hello".toByteArray(), "text/plain", "note.txt")
        }
        assertTrue(e.message!!.contains("couldn't find a resume"))
        reply = geminiAnswer("Sorry, I can't help with that.")
        assertThrows(AiException::class.java) { ResumeAi(client()).importResume("x".toByteArray(), "text/plain", "a.txt") }
    }

    @Test fun oversizedAndUnsupportedFilesAreRejectedLocally() {
        val ai = ResumeAi(client())
        assertEquals(AiException.Kind.TOO_LARGE,
            assertThrows(AiException::class.java) { ai.importResume(ByteArray(ResumeAi.MAX_FILE_BYTES + 1), "application/pdf", "a.pdf") }.kind)
        assertThrows(AiException::class.java) { ai.importResume(ByteArray(10), "application/zip", "a.zip") }
        assertTrue(requests.isEmpty())
    }


    @Test fun checkConnectionHitsModelEndpoint() {
        reply = """{"name":"models/test-model"}"""
        client().checkConnection()
        assertEquals("/v1beta/models/test-model|key=test-key", requests.single().first)
        status = 404; reply = """{"error":{"message":"not found"}}"""
        assertEquals(AiException.Kind.BAD_MODEL, assertThrows(AiException::class.java) { client().checkConnection() }.kind)
    }

    @Test fun networkFailuresNameTheRealCause() {
        val offline = GeminiClient("k", baseUrl = "http://no-such-host.invalid/v1beta")
        val e1 = assertThrows(AiException::class.java) { offline.checkConnection() }
        assertEquals(AiException.Kind.NETWORK, e1.kind)
        assertTrue(e1.message, e1.message!!.contains("offline") && e1.message!!.contains("UnknownHostException"))

        val refused = GeminiClient("k", baseUrl = "http://127.0.0.1:1/v1beta")
        val e2 = assertThrows(AiException::class.java) { refused.checkConnection() }
        assertTrue(e2.message, e2.message!!.contains("ConnectException"))
    }
}
