package com.androtask.app

import com.androtask.app.data.ResumeRepository
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ResumeRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun saveGetDeleteRoundTrip() {
        val repo = ResumeRepository(tmp.newFolder("r"))
        val r = SampleData.resume()
        repo.save(r)
        assertEquals(r, repo.get(r.id))
        assertEquals(1, repo.all().size)
        repo.delete(r.id)
        assertNull(repo.get(r.id))
    }

    @Test fun duplicateCreatesNewIdAndTitle() {
        val repo = ResumeRepository(tmp.newFolder("r"))
        val r = SampleData.resume(); repo.save(r)
        val copy = repo.duplicate(r.id)!!
        assertNotEquals(r.id, copy.id)
        assertEquals("Sample (copy)", copy.title)
        assertEquals(r.experience, copy.experience)
        assertEquals(2, repo.all().size)
    }

    @Test fun backupRestoresIntoEmptyStore() {
        val a = ResumeRepository(tmp.newFolder("a"))
        a.save(SampleData.resume()); a.save(SampleData.resume())
        val b = ResumeRepository(tmp.newFolder("b"))
        assertEquals(2, b.importBackup(a.exportBackup()))
        assertEquals(a.all().map { it.id }.toSet(), b.all().map { it.id }.toSet())
    }

    @Test fun concurrentSavesOfSameResumeNeverCorruptIt() {
        val repo = ResumeRepository(tmp.newFolder("r"))
        val base = SampleData.resume()
        val threads = (1..8).map { n ->
            Thread { repeat(50) { i -> repo.save(base.copy(title = "t$n-$i")) } }
        }
        threads.forEach { it.start() }; threads.forEach { it.join() }
        val all = repo.all()
        assertEquals(1, all.size)
        assertTrue(all.single().title.startsWith("t"))
        assertTrue(tmp.root.walk().none { it.name.endsWith(".tmp") })
    }

    @Test fun corruptFilesAreSkippedAndUnknownFieldsIgnored() {
        val dir = tmp.newFolder("r")
        File(dir, "bad.json").writeText("{not json")
        File(dir, "future.json").writeText("""{"id":"x","title":"From the future","newField":42}""")
        val all = ResumeRepository(dir).all()
        assertEquals(listOf("From the future"), all.map { it.title })
    }
}
