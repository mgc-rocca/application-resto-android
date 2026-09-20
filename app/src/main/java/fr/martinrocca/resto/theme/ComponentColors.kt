package fr.martinrocca.resto.theme

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable

@Composable
fun restoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    disabledTextColor = DisabledElement,
    disabledBorderColor = DisabledElement,
    disabledLeadingIconColor = DisabledElement,
    disabledTrailingIconColor = DisabledElement,
    disabledLabelColor = DisabledElement,
    disabledPlaceholderColor = DisabledElement,
)

@Composable
fun restoButtonColors() = ButtonDefaults.buttonColors(
    disabledContainerColor = DisabledElement,
    disabledContentColor = Ink,
)

@Composable
fun restoOutlinedButtonColors() = ButtonDefaults.outlinedButtonColors(
    disabledContentColor = DisabledElement,
)

@Composable
fun restoIconButtonColors() = IconButtonDefaults.iconButtonColors(
    disabledContentColor = DisabledElement,
)

@Composable
fun restoFilledIconButtonColors() = IconButtonDefaults.filledIconButtonColors(
    disabledContainerColor = DisabledElement,
    disabledContentColor = Ink,
)
