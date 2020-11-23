package com.ainzzorl.algorithms

interface Heap<N : Node<K, V>, K, V> {
    fun getMin(): N?

    fun extractMin(): N?

    fun insert(key: K, value: V)

    fun decreaseKey(node: Node<K, V>, k: K)
}
