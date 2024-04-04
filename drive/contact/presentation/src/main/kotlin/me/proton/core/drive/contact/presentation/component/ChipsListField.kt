/*
 * Copyright (c) 2024 Proton AG.
 * This file is part of Proton Core.
 *
 * Proton Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Core.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.core.drive.contact.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultSmallNorm

/*
    Composable that displays a ChipsListTextField with a label in a row (useful for forms)
 */
@Composable
fun ChipsListField(
    hint: String,
    value: List<ChipItem>,
    modifier: Modifier = Modifier,
    chipValidator: (String) -> Boolean = { true },
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    focusRequester: FocusRequester? = null,
    focusOnClick: Boolean = true,
    actions: ChipsListField.Actions,
    contactSuggestionState: ContactSuggestionState,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier,
    ) {
        var hintVisible by remember(value) { mutableStateOf(value.isEmpty()) }
        if (hintVisible) {
            Text(
                text = hint,
                modifier = Modifier.align(Alignment.CenterStart),
                color = ProtonTheme.colors.textWeak,
                style = ProtonTheme.typography.defaultSmallNorm
            )
        }
        ChipsListTextField(
            modifier = Modifier
                .thenIf(focusOnClick) {
                    clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { focusRequester?.requestFocus() }
                    )
                }
                .fillMaxSize(),
            chipValidator = chipValidator,
            onListChanged = actions.onListChanged,
            value = value,
            keyboardOptions = keyboardOptions,
            focusRequester = focusRequester,
            actions = ChipsListTextField.Actions(
                onSuggestionTermTyped = { searchTerm ->
                    hintVisible = value.isEmpty() && searchTerm.isEmpty()
                    actions.onSuggestionTermTyped(searchTerm)
                },
                onSuggestionsDismissed = actions.onSuggestionsDismissed
            ),
            contactSuggestionState = contactSuggestionState
        )
    }
}

@Stable
data class ContactSuggestionState(
    val areSuggestionsExpanded: Boolean,
    val suggestionItems: List<SuggestionItem>,
)

object ChipsListField {
    data class Actions(
        val onSuggestionTermTyped: (String) -> Unit,
        val onSuggestionsDismissed: () -> Unit,
        val onListChanged: (List<ChipItem>) -> Unit,
    )
}

fun Modifier.thenIf(condition: Boolean, modifier: Modifier.() -> Modifier): Modifier {
    return if (condition) {
        then(modifier())
    } else {
        this
    }
}


