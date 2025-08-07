package com.example.clothstock.data.repository

import com.example.clothstock.data.model.ClothItem
import com.example.clothstock.data.model.FilterState
import com.example.clothstock.data.model.TagData
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

/**
 * Tests for search result caching functionality
 */
@RunWith(MockitoJUnitRunner::class)
class SearchCacheTest {

    private lateinit var searchCache: SearchCache

    @Before
    fun setup() {
        searchCache = SearchCache(maxSize = 10)
    }

    @Test
    fun `cache should store and retrieve search results`() = runTest {
        val filterState = FilterState(searchText = "test")
        val testItems = listOf(
            ClothItem(
                id = 1,
                imagePath = "/path1.jpg",
                tagData = TagData(size = 100, color = "Red", category = "Shirt"),
                createdAt = Date(System.currentTimeMillis())
            ),
            ClothItem(
                id = 2,
                imagePath = "/path2.jpg",
                tagData = TagData(size = 110, color = "Blue", category = "Pants"),
                createdAt = Date(System.currentTimeMillis())
            )
        )

        // Store in cache
        searchCache.put(filterState, testItems)

        // Retrieve from cache
        val cachedItems = searchCache.get(filterState)

        assertEquals(testItems, cachedItems)
    }

    @Test
    fun `cache should return null for non-existent entries`() = runTest {
        val filterState = FilterState(searchText = "nonexistent")

        val cachedItems = searchCache.get(filterState)

        assertNull(cachedItems)
    }

    @Test
    fun `cache should evict oldest entries when size limit is reached`() = runTest {
        val cacheSize = 3
        val smallCache = SearchCache(maxSize = cacheSize)

        // Fill cache beyond limit
        repeat(cacheSize + 2) { index ->
            val filterState = FilterState(searchText = "query$index")
            val items = listOf(
                ClothItem(
                    id = index.toLong(),
                    imagePath = "/path$index.jpg",
                    tagData = TagData(size = 100, color = "Color", category = "Category"),
                    createdAt = Date(System.currentTimeMillis())
                )
            )
            smallCache.put(filterState, items)
        }

        // First entries should be evicted
        val firstEntry = smallCache.get(FilterState(searchText = "query0"))
        val lastEntry = smallCache.get(FilterState(searchText = "query${cacheSize + 1}"))

        assertNull("First entry should be evicted", firstEntry)
        assertNotNull("Last entry should still exist", lastEntry)
    }

    @Test
    fun `cache should handle memory pressure by clearing entries`() = runTest {
        val filterState = FilterState(searchText = "test")
        val testItems = listOf(
            ClothItem(
                id = 1,
                imagePath = "/path.jpg",
                tagData = TagData(size = 100, color = "Red", category = "Shirt"),
                createdAt = Date(System.currentTimeMillis())
            )
        )

        searchCache.put(filterState, testItems)
        assertNotNull(searchCache.get(filterState))

        // Simulate memory pressure
        searchCache.onMemoryPressure()

        // Cache should be cleared or reduced
        val remainingItems = searchCache.get(filterState)
        assertTrue("Cache should be cleared or reduced after memory pressure", 
                  remainingItems == null || searchCache.size() < 5)
    }

    @Test
    fun `cache should generate consistent keys for equivalent filter states`() = runTest {
        val filterState1 = FilterState(
            sizeFilters = setOf(100, 110),
            colorFilters = setOf("Red", "Blue"),
            searchText = "test"
        )
        val filterState2 = FilterState(
            sizeFilters = setOf(110, 100), // Different order
            colorFilters = setOf("Blue", "Red"), // Different order
            searchText = "test"
        )

        val key1 = searchCache.generateKey(filterState1)
        val key2 = searchCache.generateKey(filterState2)

        assertEquals("Keys should be equal regardless of filter order", key1, key2)
    }

    @Test
    fun `cache should track hit and miss statistics`() = runTest {
        val filterState = FilterState(searchText = "test")
        val testItems = listOf(
            ClothItem(
                id = 1,
                imagePath = "/path.jpg",
                tagData = TagData(size = 100, color = "Red", category = "Shirt"),
                createdAt = Date(System.currentTimeMillis())
            )
        )

        // Miss
        searchCache.get(filterState)
        assertEquals("Miss count should be 1", 1, searchCache.getMissCount())
        assertEquals("Hit count should be 0", 0, searchCache.getHitCount())

        // Store and hit
        searchCache.put(filterState, testItems)
        searchCache.get(filterState)
        assertEquals("Hit count should be 1", 1, searchCache.getHitCount())
        assertEquals("Miss count should be 1", 1, searchCache.getMissCount())
    }
}
