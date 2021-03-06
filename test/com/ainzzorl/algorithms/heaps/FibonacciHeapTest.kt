package com.ainzzorl.algorithms.heaps

import org.junit.Test

class FibonacciHeapTest {
    @Test
    fun testBasicScenario() {
        HeapTest.testBasicScenario(FibonacciHeap())
    }

    @Test
    fun testInsertExtract() {
        HeapTest.testInsertExtract(FibonacciHeap())
    }
}
