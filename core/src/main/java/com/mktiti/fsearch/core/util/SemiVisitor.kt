package com.mktiti.fsearch.core.util

import com.mktiti.fsearch.core.repo.TypeResolver
import com.mktiti.fsearch.core.type.CompleteMinInfo
import com.mktiti.fsearch.core.type.SemiType

class SemiVisitor(
        private val typeResolver: TypeResolver
) {

    private fun visitInner(root: SemiType, code: (node: SemiType, depth: Int, hasMore: Boolean) -> Unit, depth: Int, hasMoreSiblings: Boolean) {
        code(root, depth, hasMoreSiblings)

        val childCount = root.superTypes.size
        root.superTypes.forEachIndexed { i, node ->
            val resolved = typeResolver.getAny(node.info) ?: return
            visitInner(resolved, code, depth + 1, i < childCount - 1)
        }
    }

    fun visitSupersDf(root: CompleteMinInfo<*>, code: (node: SemiType, depth: Int, hasMore: Boolean) -> Unit) {
        val resolved = typeResolver.getAny(root) ?: return
        visitInner(resolved, code, 0, false)
    }

    fun visitSupersDf(root: SemiType, code: (node: SemiType, depth: Int, hasMore: Boolean) -> Unit) {
        visitInner(root, code, 0, false)
    }

}