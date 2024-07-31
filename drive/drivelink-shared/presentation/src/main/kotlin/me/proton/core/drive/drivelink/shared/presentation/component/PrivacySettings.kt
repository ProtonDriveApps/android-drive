/*
 * Copyright (c) 2022-2023 Proton AG.
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
package me.proton.core.drive.drivelink.shared.presentation.component

import android.app.DatePickerDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.proton.core.compose.theme.ProtonDimens.DefaultSpacing
import me.proton.core.compose.theme.ProtonDimens.MediumSpacing
import me.proton.core.compose.theme.ProtonDimens.SmallSpacing
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.default
import me.proton.core.compose.theme.defaultHint
import me.proton.core.compose.theme.defaultSmallStrong
import me.proton.core.compose.theme.headlineSmall
import me.proton.core.drive.base.presentation.component.protonOutlineTextFieldColors
import me.proton.core.drive.drivelink.shared.presentation.viewevent.PrivacySettingsViewEvent
import me.proton.core.drive.drivelink.shared.presentation.viewstate.PrivacySettingsViewState
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import me.proton.core.drive.i18n.R as I18N
import me.proton.core.presentation.R as CorePresentation

@Composable
internal fun PrivacySettings(
    viewState: PrivacySettingsViewState,
    viewEvent: PrivacySettingsViewEvent,
    title: String = stringResource(id = I18N.string.shared_link_privacy_settings),
    modifier: Modifier = Modifier,
) {
    val localContext = LocalContext.current
    var password by rememberSaveable(viewState.password) {
        mutableStateOf(viewState.password)
    }
    Column(
        modifier = modifier
            .padding(horizontal = DefaultSpacing)
    ) {
        Text(
            text = title,
            style = ProtonTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = MediumSpacing),
        )
        Text(
            text = stringResource(id = I18N.string.shared_link_password_protection),
            style = ProtonTheme.typography.defaultSmallStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = SmallSpacing),
        )
        var passwordVisible by rememberSaveable { mutableStateOf(false) }
        val iconRes = if (passwordVisible) {
            CorePresentation.drawable.ic_proton_eye_slash
        } else {
            CorePresentation.drawable.ic_proton_eye
        }
        val focusManager = LocalFocusManager.current
        val passwordSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = MediumSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = password.orEmpty(),
                enabled = viewState.enabled,
                onValueChange = { text ->
                    password = text
                    viewEvent.onPasswordChanged(text)
                },
                colors = TextFieldDefaults.protonOutlineTextFieldColors(),
                singleLine = true,
                maxLines = 1,
                interactionSource = passwordSource,
                trailingIcon = {
                    IconButton(
                        enabled = true,
                        onClick = {
                            passwordVisible = !passwordVisible
                        },
                    ) {
                        Icon(
                            painter = painterResource(id = iconRes),
                            tint = ProtonTheme.colors.iconNorm,
                            contentDescription = null,
                        )
                    }
                },
                placeholder = {
                    Text(
                        text = stringResource(id = I18N.string.shared_link_hint_password_field),
                        style = ProtonTheme.typography.defaultHint,
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                textStyle = ProtonTheme.typography.default,
                modifier = Modifier
                    .padding(end = TextFieldSwitchSpacing)
                    .weight(1f)
                    .testTag(PrivacySettingsTestTag.passwordTextField),
            )
            Switch(
                checked = viewState.passwordChecked,
                enabled = viewState.enabled,
                onCheckedChange = { enabled ->
                    viewEvent.onPasswordEnabledChanged(enabled)
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .testTag(PrivacySettingsTestTag.passwordSwitch),
            )
        }
        if (passwordSource.collectIsPressedAsState().value) {
            viewEvent.onPasswordEnabledChanged(true)
        }
        Text(
            text = stringResource(id = I18N.string.shared_link_expiration_date),
            style = ProtonTheme.typography.defaultSmallStrong,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = SmallSpacing),
        )
        var expirationDate by remember(viewState.expirationDate) {
            val text = viewState.expirationDate?.let {
                DateFormat.getDateInstance(DateFormat.MEDIUM).format(it)
            } ?: ""
            mutableStateOf(TextFieldValue(text = text))
        }
        val expirationDateSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        )  {
            OutlinedTextField(
                value = expirationDate,
                enabled = viewState.enabled,
                onValueChange = { textField ->
                    expirationDate = textField
                },
                colors = TextFieldDefaults.protonOutlineTextFieldColors(),
                singleLine = true,
                interactionSource = expirationDateSource,
                readOnly = true,
                maxLines = 1,
                placeholder = {
                    Text(
                        text = stringResource(id = I18N.string.shared_link_hint_expiration_date_field),
                        style = ProtonTheme.typography.defaultHint,
                    )
                },
                textStyle = ProtonTheme.typography.default,
                modifier = Modifier
                    .padding(end = TextFieldSwitchSpacing)
                    .weight(1f)
                    .testTag(PrivacySettingsTestTag.expirationDateTextField),
            )
            Switch(
                checked = viewState.expirationDateChecked,
                enabled = viewState.enabled,
                onCheckedChange = { enabled ->
                    viewEvent.onExpirationDateEnabledChanged(enabled)
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .testTag(PrivacySettingsTestTag.expirationDateSwitch),
            )
        }
        if (expirationDateSource.collectIsPressedAsState().value) {
            viewEvent.onExpirationDateEnabledChanged(true)
            val expiration = viewState.expirationDate ?: Date()
            DatePickerDialog(
                localContext,
                { _, y, m, d -> viewEvent.onExpirationDateChanged(y, m ,d) },
                expiration.asYear,
                expiration.asMonth,
                expiration.asDayOfMonth,
            )
                .apply {
                    datePicker.minDate = viewState.minDatePickerDate
                    datePicker.maxDate = viewState.maxDatePickerDate
                }
                .show()
        }
    }
}

val Date.asYear: Int get() = Calendar.getInstance()
    .apply { time = this@asYear }
    .get(Calendar.YEAR)

val Date.asMonth: Int get() = Calendar.getInstance()
    .apply { time = this@asMonth }
    .get(Calendar.MONTH)

val Date.asDayOfMonth: Int get() = Calendar.getInstance()
    .apply { time = this@asDayOfMonth }
    .get(Calendar.DAY_OF_MONTH)

private val TextFieldSwitchSpacing = 18.dp

object PrivacySettingsTestTag {
    const val passwordTextField = "password text field"
    const val passwordSwitch = "password switch"
    const val expirationDateTextField = "expiration date text field"
    const val expirationDateSwitch = "expiration date switch"
}

@Preview
@Composable
private fun PrivacySettingsPreview() {
    ProtonTheme {
        PrivacySettings(
            viewState = PrivacySettingsViewState(
                enabled = false,
                password = null,
                passwordChecked = false,
                expirationDate = null,
                expirationDateChecked = false,
                minDatePickerDate = 0,
                maxDatePickerDate = 0
            ),
            viewEvent = object : PrivacySettingsViewEvent {
                override val onPasswordChanged:(String) -> Unit = {}
                override val onPasswordEnabledChanged: (Boolean) -> Unit = {}
                override val onExpirationDateChanged: (Int, Int, Int) -> Unit = {_, _, _ ->}
                override val onExpirationDateEnabledChanged: (Boolean) -> Unit = {}
            }
        )
    }
}
