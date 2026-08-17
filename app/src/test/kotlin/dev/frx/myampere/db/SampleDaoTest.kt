package dev.frx.myampere.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SampleDaoTest {
    private lateinit var db: AppDb

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
            .allowMainThreadQueries().build()
    }
    @After fun teardown() = db.close()

    @Test fun `insert puis range`() = runTest {
        db.sampleDao().insertAll(listOf(SampleEntity(1L, -100, 80), SampleEntity(2L, -110, 80), SampleEntity(99L, 500, 81)))
        assertEquals(2, db.sampleDao().range(0L, 50L).size)
    }

    @Test fun `delete by timestamps`() = runTest {
        db.sampleDao().insertAll(listOf(SampleEntity(1L, -100, 80), SampleEntity(2L, -110, 80)))
        db.sampleDao().deleteByTimestamps(listOf(1L))
        assertEquals(listOf(2L), db.sampleDao().timestampsBetween(0L, 100L))
    }

    @Test fun `timestampsBetween borne la fenetre haute et basse`() = runTest {
        db.sampleDao().insertAll(listOf(SampleEntity(1L, -100, 80), SampleEntity(50L, -110, 80), SampleEntity(200L, 500, 81)))
        assertEquals(listOf(50L), db.sampleDao().timestampsBetween(10L, 100L))
    }
}
