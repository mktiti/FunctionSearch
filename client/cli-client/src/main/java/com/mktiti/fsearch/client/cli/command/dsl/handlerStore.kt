package com.mktiti.fsearch.client.cli.command.dsl

import com.mktiti.fsearch.client.cli.util.ListIntRangeMap
import com.mktiti.fsearch.client.cli.util.MutableIntRangeMap

interface HandlerStore {

    object NOP : HandlerStore {
        override val default: TransformCommandHandle
            get() = nopTransformCommandHandle

        override fun get(paramCount: Int) = nopTransformCommandHandle
    }

    val default: TransformCommandHandle?

    operator fun get(paramCount: Int): TransformCommandHandle?

}

interface MutableHandlerStore : HandlerStore {

    override var default: TransformCommandHandle?

    operator fun set(range: IntRange, handler: TransformCommandHandle)

}

class DefaultHandlerStore : MutableHandlerStore {

    private val backingStore: MutableIntRangeMap<TransformCommandHandle> = ListIntRangeMap()

    override var default: TransformCommandHandle? = null

    override fun set(range: IntRange, handler: TransformCommandHandle) {
        if (range.last < range.first) {
            default = handler
        } else {
            backingStore[range] = handler
        }
    }

    override fun get(paramCount: Int): TransformCommandHandle? {
        return backingStore[paramCount] ?: default
    }

}
