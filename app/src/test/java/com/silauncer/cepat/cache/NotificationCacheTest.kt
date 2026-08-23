package com.silauncer.cepat.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NotificationCacheTest {
    
    @Before
    fun setUp() {
        NotificationCache.clear()
    }

    @Test
    fun testConcurrentAddAndRemove() {
        val executor = Executors.newFixedThreadPool(10)
        val latch = CountDownLatch(100)
        val pkg = "com.test.pkg"
        
        for (i in 0 until 50) {
            executor.submit {
                NotificationCache.addNotificationKey(pkg, "key_$i")
                latch.countDown()
            }
        }
        
        for (i in 50 until 100) {
            executor.submit {
                NotificationCache.addNotificationKey(pkg, "key_$i")
                NotificationCache.removeNotificationKey(pkg, "key_$i")
                latch.countDown()
            }
        }
        
        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()
        
        val keys = NotificationCache.getKeysForPackage(pkg)
        assertEquals(50, keys.size)
        for (i in 0 until 50) {
            assertTrue(keys.contains("key_$i"))
        }
    }

    @Test
    fun testReplaceAllIsAtomicAndCorrect() {
        NotificationCache.addNotificationKey("pkg1", "old_key")
        
        val newMap = mapOf(
            "pkg1" to setOf("new_key1"),
            "pkg2" to setOf("new_key2", "new_key3")
        )
        NotificationCache.replaceAll(newMap)
        
        val all = NotificationCache.getAll()
        assertEquals(2, all.size)
        assertEquals(1, all["pkg1"]?.size)
        assertTrue(all["pkg1"]?.contains("new_key1") == true)
        
        assertEquals(2, all["pkg2"]?.size)
        assertTrue(all["pkg2"]?.contains("new_key2") == true)
    }
}
