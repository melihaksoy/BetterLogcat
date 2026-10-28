package com.melihaksoy.betterlogcat.ui

import com.melihaksoy.betterlogcat.filter.LogFilter
import com.melihaksoy.betterlogcat.toolwindow.AppSwitch
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSwitchTest {

    @Test
    fun `a new tab copies the filter of the selected package mine tab with the new package`() {
        val action = AppSwitch.plan(
            panes = listOf(
                pane(
                    tabIndex = 0,
                    query = "package:mine tag:Other",
                ),
                pane(
                    tabIndex = 1,
                    query = "package:mine -tag=:Chatty level:W",
                ),
            ),
            selectedTab = 1,
            newAppId = NEW_APP,
        )

        assertEquals(
            AppSwitch.Action.Open(
                serial = SERIAL,
                query = "package:$NEW_APP -tag=:Chatty level:W",
            ),
            action,
        )
    }

    @Test
    fun `a tab that shows the new app already gets the focus`() {
        val action = AppSwitch.plan(
            panes = listOf(
                pane(
                    tabIndex = 0,
                    query = "package:mine",
                ),
                pane(
                    tabIndex = 1,
                    query = "package:$NEW_APP -tag=:Chatty",
                ),
            ),
            selectedTab = 0,
            newAppId = NEW_APP,
        )

        assertEquals(AppSwitch.Action.Focus(tabIndex = 1), action)
    }

    @Test
    fun `without a package mine tab the switch changes nothing`() {
        val action = AppSwitch.plan(
            panes = listOf(
                pane(
                    tabIndex = 0,
                    query = "package:com.example.other",
                ),
                pane(
                    tabIndex = 1,
                    query = "tag:OkHttp",
                ),
            ),
            selectedTab = 0,
            newAppId = NEW_APP,
        )

        assertEquals(AppSwitch.Action.None, action)
    }

    private fun pane(
        tabIndex: Int,
        query: String,
    ): AppSwitch.PaneInfo = AppSwitch.PaneInfo(
        tabIndex = tabIndex,
        serial = SERIAL,
        filter = LogFilter().withQueryString(text = query),
    )

    private companion object {
        const val SERIAL = "emulator-5554"
        const val NEW_APP = "com.example.beta.debug"
    }
}
