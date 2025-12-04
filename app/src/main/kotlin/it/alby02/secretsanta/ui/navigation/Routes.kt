/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package it.alby02.secretsanta.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface AppRoute : NavKey

@Serializable
data object Login : AppRoute

@Serializable
data object SignUp : AppRoute

@Serializable
data object Home : AppRoute

@Serializable
data class GroupDetail(val groupId: String) : AppRoute
