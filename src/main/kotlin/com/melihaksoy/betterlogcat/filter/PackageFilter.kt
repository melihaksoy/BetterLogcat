package com.melihaksoy.betterlogcat.filter

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/** The package part of a filter, as the picker shows it. Stored as `package:` terms in [LogFilter.terms]. */
@Stable
sealed interface PackageFilter {

    /** No package term. */
    @Immutable
    data object All : PackageFilter

    /** `package:mine`: application ids of the open project. Acts like [All] while no id is known. */
    @Immutable
    data object ProjectApps : PackageFilter

    @Immutable
    data class Package(val name: String) : PackageFilter

    /** More than one package term. */
    @Immutable
    data class Several(val names: List<String>) : PackageFilter
}
