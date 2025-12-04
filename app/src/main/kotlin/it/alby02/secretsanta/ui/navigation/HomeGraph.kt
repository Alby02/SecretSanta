/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import it.alby02.secretsanta.ui.features.groupdetail.GroupDetailRoute
import it.alby02.secretsanta.ui.features.home.HomeRoute

fun EntryProviderScope<NavKey>.homeGraph(navigator: Navigator) {
    entry<Home> {
        HomeRoute(
            onNavigateToGroup = { groupId -> navigator.navigate(GroupDetail(groupId)) },
            onLogout = { navigator.navigate(Login) }
        )
    }
    entry<GroupDetail> { key ->
        GroupDetailRoute(
            groupId = key.groupId,
            onNavigateBack = { navigator.goBack() }
        )
    }
}
