package com.melihaksoy.betterlogcat.ui.logcat.pane

/**
 * Split and close actions of one pane. [canClose] is Compose state, so the close button
 * appears as soon as a tab has a second pane.
 */
interface PaneActions {
    val canClose: Boolean

    fun splitRight()

    fun splitDown()

    fun close()

    object None : PaneActions {
        override val canClose: Boolean = false

        override fun splitRight() = Unit

        override fun splitDown() = Unit

        override fun close() = Unit
    }
}
