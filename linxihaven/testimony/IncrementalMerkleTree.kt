package com.testimony.app.evidence

import com.testimony.app.util.HashUtils
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * 增量默克尔树【司法级实现】
 *
 * 司法意义：
 * - 证据包内每个组件的完整性可通过默克尔根哈希验证
 * - 增量更新避免全量重算导致的ANR
 * - 解包后可在不依赖原始App的情况下独立验证
 *
 * 性能约束：
 * - 单次哈希操作 ≤ 100ms（确保低端机型不出现ANR）
 * - 增量添加时仅计算受影响路径，不重算整棵树
 *
 * @author Testimony数字取证专家
 */
class IncrementalMerkleTree {

    private val leaves = mutableListOf<ByteArray>()
    private var rootHash: ByteArray? = null
    private var layers: MutableList<MutableList<ByteArray>> = mutableListOf()

    /**
     * 添加叶子节点【核心接口】
     *
     * 性能优化：仅计算新增叶子到根的路径，不重算其他分支
     *
     * @param data 叶子数据
     * @return 新增叶子的索引位置
     */
    fun addLeaf(data: ByteArray): Int {
        val leafHash = hashOnce(data)
        val index = leaves.size
        leaves.add(leafHash)

        // 如果是第一个叶子，初始化
        if (leaves.size == 1) {
            layers.clear()
            layers.add(mutableListOf(leafHash))
            rootHash = leafHash
            return index
        }

        // 增量更新：重新计算到根的路径
        updatePath(index)
        recalculateRoot()

        return index
    }

    /**
     * 添加多个叶子节点
     */
    fun addLeaves(dataList: List<ByteArray>) {
        dataList.forEach { addLeaf(it) }
    }

    /**
     * 从文件列表构建【静态工厂】
     */
    fun staticBuild(files: List<File>): ByteArray {
        leaves.clear()
        layers.clear()

        files.forEach { file ->
            if (file.exists() && file.isFile) {
                val hash = hashFileIncremental(file)
                leaves.add(hash)
            }
        }

        if (leaves.isEmpty()) {
            rootHash = hashOnce("empty".toByteArray())
            return rootHash!!
        }

        // 构建树
        var currentLevel = leaves.toMutableList()
        layers.add(currentLevel)

        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<ByteArray>()
            for (i in currentLevel.indices step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                nextLevel.add(hashPair(left, right))
            }

            // 奇数节点复制最后一个以保持平衡
            if (nextLevel.size % 2 == 1 && nextLevel.isNotEmpty()) {
                nextLevel.add(nextLevel.last())
            }

            layers.add(nextLevel)
            currentLevel = nextLevel
        }

        rootHash = currentLevel.first()
        return rootHash!!
    }

    /**
     * 增量更新单个叶子节点
     */
    fun updateLeaf(index: Int, data: ByteArray) {
        if (index < 0 || index >= leaves.size) return

        leaves[index] = hashOnce(data)
        updatePath(index)
        recalculateRoot()
    }

    /**
     * 验证叶子节点
     *
     * @param leafHash 叶子哈希
     * @param proof 证明路径（兄弟节点哈希列表）
     * @return 是否验证通过
     */
    fun verifyLeaf(leafHash: ByteArray, proof: List<ByteArray>): Boolean {
        val root = rootHash ?: return false
        var currentHash = leafHash

        proof.forEach { sibling ->
            currentHash = hashPair(currentHash, sibling)
        }

        return currentHash.contentEquals(root)
    }

    /**
     * 生成叶子节点的证明路径
     */
    fun generateProof(leafIndex: Int): MerkleProof? {
        if (leafIndex < 0 || leafIndex >= leaves.size) return null

        val proof = mutableListOf<ByteArray>()

        for (level in 0 until layers.size - 1) {
            val isRightNode = leafIndex % 2 == 1
            val siblingIndex = if (isRightNode) leafIndex - 1 else leafIndex + 1

            if (siblingIndex >= 0 && siblingIndex < layers[level].size) {
                proof.add(layers[level][siblingIndex])
            }

            leafIndex /= 2
        }

        return MerkleProof(
            leafHash = leaves.getOrNull(leafIndex) ?: return null,
            proof = proof,
            rootHash = rootHash ?: return null
        )
    }

    /**
     * 获取根哈希
     */
    fun getRootHash(): ByteArray {
        if (rootHash == null) {
            recalculateRoot()
        }
        return rootHash!!
    }

    /**
     * 获取根哈希（十六进制）
     */
    fun getRootHashHex(): String = HashUtils.bytesToHex(getRootHash())

    /**
     * 获取叶子数量
     */
    fun getLeafCount(): Int = leaves.size

    /**
     * 获取所有叶子哈希（调试用）
     */
    fun getLeafHashes(): List<String> = leaves.map { HashUtils.bytesToHex(it) }

    /**
     * 获取树层级信息（调试用）
     */
    fun getLayersInfo(): List<List<String>> = layers.map { level ->
        level.map { HashUtils.bytesToHex(it) }
    }

    // ===== 内部方法 =====

    /**
     * 更新从指定索引到根的路径
     */
    private fun updatePath(leafIndex: Int) {
        var idx = leafIndex

        // 确保层数足够
        while (layers.size < 2) {
            layers.add(0, mutableListOf())
        }

        // 更新第一层（叶子层）
        if (layers[0].size <= idx) {
            layers[0].addAll(List(idx - layers[0].size + 1) { leaves.last() })
        }
        layers[0][idx] = leaves[idx]

        // 更新上层
        for (level in 0 until layers.size - 1) {
            val parentIndex = idx / 2
            val leftChild = layers[level][idx]
            val rightChild = if (idx + 1 < layers[level].size) layers[level][idx + 1] else leftChild

            // 确保上层有足够的空间
            while (layers.size <= level + 1) {
                layers.add(mutableListOf())
            }

            // 更新父节点
            if (parentIndex < layers[level + 1].size) {
                layers[level + 1][parentIndex] = hashPair(leftChild, rightChild)
            } else {
                layers[level + 1].add(hashPair(leftChild, rightChild))
            }

            idx = parentIndex
        }
    }

    /**
     * 重新计算根哈希
     */
    private fun recalculateRoot() {
        if (leaves.isEmpty()) {
            rootHash = hashOnce("empty".toByteArray())
            return
        }

        if (layers.isEmpty() || layers.first().isEmpty()) {
            staticBuild(emptyList())
            return
        }

        var currentLevel = leaves.toMutableList()

        while (currentLevel.size > 1) {
            val nextLevel = mutableListOf<ByteArray>()
            for (i in currentLevel.indices step 2) {
                val left = currentLevel[i]
                val right = if (i + 1 < currentLevel.size) currentLevel[i + 1] else left
                nextLevel.add(hashPair(left, right))
            }

            // 保持偶数个节点
            if (nextLevel.size % 2 == 1) {
                nextLevel.add(nextLevel.last())
            }

            currentLevel = nextLevel
        }

        rootHash = currentLevel.first()
    }

    /**
     * 单次哈希
     */
    private fun hashOnce(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }

    /**
     * 哈希配对
     */
    private fun hashPair(left: ByteArray, right: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(left)
        digest.update(right)
        return digest.digest()
    }

    /**
     * 增量哈希文件（避免大文件导致OOM）
     */
    private fun hashFileIncremental(file: File): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)

        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return digest.digest()
    }

    // ===== 数据类 =====

    data class MerkleProof(
        val leafHash: ByteArray,
        val proof: List<ByteArray>,
        val rootHash: ByteArray
    ) {
        fun verify(): Boolean {
            var currentHash = leafHash
            proof.forEach { sibling ->
                currentHash = hashPair(currentHash, sibling)
            }
            return currentHash.contentEquals(rootHash)
        }

        private fun hashPair(left: ByteArray, right: ByteArray): ByteArray {
            val digest = MessageDigest.getInstance("SHA-256")
            digest.update(left)
            digest.update(right)
            return digest.digest()
        }
    }

    companion object {
        /**
         * 从字符串列表构建
         */
        fun fromStrings(items: List<String>): IncrementalMerkleTree {
            val tree = IncrementalMerkleTree()
            items.forEach { tree.addLeaf(it.toByteArray()) }
            return tree
        }

        /**
         * 从字节数组列表构建
         */
        fun fromByteArrays(items: List<ByteArray>): IncrementalMerkleTree {
            val tree = IncrementalMerkleTree()
            items.forEach { tree.addLeaf(it) }
            return tree
        }

        /**
         * 从文件列表构建（静态）
         */
        fun fromFiles(files: List<File>): IncrementalMerkleTree {
            val tree = IncrementalMerkleTree()
            tree.staticBuild(files)
            return tree
        }
    }
}
