package com.varshudev.a1swaara.ui.components.themed

import androidx.annotation.IntRange
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.varshudev.a1swaara.R
import com.varshudev.a1swaara.ui.components.SwaaraCyan
import com.varshudev.a1swaara.ui.components.SwaaraElevatedSurface
import com.varshudev.a1swaara.ui.components.SwaaraGlassContainer
import com.varshudev.a1swaara.ui.components.SwaaraMagenta
import com.varshudev.a1swaara.ui.components.SwaaraModalSurface
import com.varshudev.a1swaara.ui.components.SwaaraViolet
import com.varshudev.a1swaara.utils.center
import com.varshudev.a1swaara.utils.drawCircle
import com.varshudev.a1swaara.utils.medium
import com.varshudev.a1swaara.utils.semiBold
import app.a1swaara.core.ui.LocalAppearance
import app.a1swaara.core.ui.glassBorder
import app.a1swaara.core.ui.secondaryAccent
import app.a1swaara.core.ui.utils.roundedShape
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay

@Composable
fun TextFieldDialog(
    hintText: String,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    doneText: String = stringResource(R.string.done),
    initialTextInput: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    onCancel: () -> Unit = onDismiss,
    isTextInputValid: (String) -> Boolean = { it.isNotEmpty() },
    keyboardOptions: KeyboardOptions = KeyboardOptions()
) = DefaultDialog(
    onDismiss = onDismiss,
    modifier = modifier
) {
    val focusRequester = remember { FocusRequester() }
    val (colorPalette, typography) = LocalAppearance.current

    var value by rememberSaveable(initialTextInput) { mutableStateOf(initialTextInput) }
    val isValid = isTextInputValid(value)

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
            .clip(14.dp.roundedShape)
            .background(
                if (colorPalette.isDefault && colorPalette.isDark) SwaaraElevatedSurface
                else colorPalette.background2.copy(alpha = 0.72f)
            )
            .border(1.dp, colorPalette.glassBorder, 14.dp.roundedShape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        TextField(
            value = value,
            onValueChange = { value = it },
            textStyle = typography.xs.semiBold.center,
            singleLine = singleLine,
            maxLines = maxLines,
            hintText = hintText,
            keyboardActions = KeyboardActions(
                onDone = {
                    if (isValid) {
                        onDismiss()
                        onAccept(value)
                    }
                }
            ),
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
        modifier = Modifier.fillMaxWidth()
    ) {
        DialogTextButton(
            text = cancelText,
            onClick = onCancel
        )

        DialogTextButton(
            primary = true,
            enabled = isValid,
            text = doneText,
            onClick = {
                if (isValid) {
                    onDismiss()
                    onAccept(value)
                }
            }
        )
    }
}

@Composable
fun <T> NumberFieldDialog(
    onDismiss: () -> Unit,
    onAccept: (T) -> Unit,
    initialValue: T,
    defaultValue: T,
    convert: (String) -> T?,
    range: ClosedRange<T>,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    doneText: String = stringResource(R.string.done),
    onCancel: () -> Unit = onDismiss
) where T : Number, T : Comparable<T> = TextFieldDialog(
    hintText = "",
    onDismiss = onDismiss,
    onAccept = { onAccept((convert(it) ?: defaultValue).coerceIn(range)) },
    modifier = modifier,
    cancelText = cancelText,
    doneText = doneText,
    initialTextInput = initialValue.toString(),
    onCancel = onCancel,
    isTextInputValid = { true },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
)

@Composable
fun ConfirmationDialog(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onCancel: () -> Unit = onDismiss
) = DefaultDialog(
    onDismiss = onDismiss,
    modifier = modifier
) {
    ConfirmationDialogBody(
        text = text,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        cancelText = cancelText,
        confirmText = confirmText,
        onCancel = onCancel
    )
}

@Suppress("ModifierMissing", "UnusedReceiverParameter")
@Composable
fun ColumnScope.ConfirmationDialogBody(
    text: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    cancelText: String = stringResource(R.string.cancel),
    confirmText: String = stringResource(R.string.confirm),
    onCancel: () -> Unit = onDismiss
) {
    val (_, typography) = LocalAppearance.current

    BasicText(
        text = text,
        style = typography.xs.medium.center,
        modifier = Modifier.padding(all = 16.dp)
    )

    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        DialogTextButton(
            text = cancelText,
            onClick = {
                onCancel()
                onDismiss()
            }
        )

        DialogTextButton(
            text = confirmText,
            primary = true,
            onClick = {
                onConfirm()
                onDismiss()
            }
        )
    }
}

@Composable
private fun SwaaraDialogCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) = Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
) {
    val colorPalette = LocalAppearance.current.colorPalette
    val surfaceColor = if (colorPalette.isDefault && colorPalette.isDark) {
        SwaaraModalSurface
    } else {
        colorPalette.background1
    }
    val borderBrush = if (colorPalette.isDefault) {
        Brush.linearGradient(
            listOf(
                SwaaraMagenta.copy(alpha = 0.50f),
                SwaaraViolet.copy(alpha = 0.28f),
                SwaaraCyan.copy(alpha = 0.45f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                colorPalette.accent.copy(alpha = 0.45f),
                colorPalette.glassBorder
            )
        )
    }

    Box(
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        SwaaraGlassContainer(
            shape = 24.dp.roundedShape,
            backgroundColor = surfaceColor,
            borderBrush = borderBrush,
            borderWidth = 1.2.dp,
            shadowElevation = 24.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
fun DefaultDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    horizontalPadding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit
) = SwaaraDialogCard(
    onDismiss = onDismiss,
    modifier = modifier
) {
    Column(
        horizontalAlignment = horizontalAlignment,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = horizontalPadding,
                vertical = 20.dp
            ),
        content = content
    )
}

@Composable
fun <T> ValueSelectorDialog(
    onDismiss: () -> Unit,
    title: String,
    selectedValue: T,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    valueText: @Composable (T) -> String = { it.toString() }
) = SwaaraDialogCard(
    onDismiss = onDismiss,
    modifier = modifier
) {
    ValueSelectorDialogBody(
        onDismiss = onDismiss,
        title = title,
        selectedValue = selectedValue,
        values = values,
        onValueSelect = onValueSelect,
        modifier = Modifier.padding(vertical = 20.dp),
        valueText = valueText
    )
}

@Composable
fun <T> ValueSelectorDialogBody(
    onDismiss: () -> Unit,
    title: String,
    selectedValue: T?,
    values: ImmutableList<T>,
    onValueSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    valueText: @Composable (T) -> String = { it.toString() }
) = Column(modifier = modifier.fillMaxWidth()) {
    val (colorPalette, typography) = LocalAppearance.current

    BasicText(
        text = title,
        style = typography.s.semiBold,
        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp, start = 24.dp, end = 24.dp)
    )

    Column(
        modifier = Modifier
            .heightIn(max = 360.dp)
            .verticalScroll(rememberScrollState())
    ) {
        values.forEach { value ->
            val isSelected = selectedValue == value
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .fillMaxWidth()
                    .clip(14.dp.roundedShape)
                    .background(if (isSelected) colorPalette.accent.copy(alpha = 0.14f) else Color.Transparent)
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) colorPalette.accent.copy(alpha = 0.35f) else Color.Transparent,
                        shape = 14.dp.roundedShape
                    )
                    .clickable(
                        onClick = {
                            onDismiss()
                            onValueSelect(value)
                        }
                    )
                    .padding(vertical = 12.dp, horizontal = 14.dp)
            ) {
                if (isSelected) Canvas(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = colorPalette.accent,
                            shape = CircleShape
                        )
                ) {
                    drawCircle(
                        color = colorPalette.onAccent,
                        radius = 4.5.dp.toPx(),
                        center = size.center,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.35f),
                            blurRadius = 4.dp.toPx(),
                            offset = Offset(x = 0f, y = 1.dp.toPx())
                        )
                    )
                } else Spacer(
                    modifier = Modifier
                        .size(20.dp)
                        .border(
                            width = 1.5.dp,
                            color = colorPalette.textDisabled.copy(alpha = 0.65f),
                            shape = CircleShape
                        )
                )

                BasicText(
                    text = valueText(value),
                    style = typography.xs.medium
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = 10.dp, end = 20.dp)
    ) {
        DialogTextButton(
            text = stringResource(R.string.cancel),
            onClick = onDismiss
        )
    }
}

@Suppress("ModifierMissing") // intentional, I guess
@Composable
fun ColumnScope.SliderDialogBody(
    provideState: @Composable () -> MutableState<Float>,
    onSlideComplete: (newState: Float) -> Unit,
    min: Float,
    max: Float,
    toDisplay: @Composable (Float) -> String = { it.toString() },
    @IntRange(from = 0) steps: Int = 0,
    label: String? = null
) {
    val (colorPalette, typography) = LocalAppearance.current
    var state by provideState()

    if (label != null) BasicText(
        text = label,
        style = typography.xs.semiBold,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
    )

    Slider(
        state = state,
        setState = { state = it },
        onSlideComplete = { onSlideComplete(state) },
        range = min..max,
        steps = steps,
        modifier = Modifier
            .height(36.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    )

    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(vertical = 8.dp)
            .clip(12.dp.roundedShape)
            .background(colorPalette.background2.copy(alpha = 0.8f))
            .border(1.dp, colorPalette.glassBorder, 12.dp.roundedShape)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = toDisplay(state),
            style = typography.s.semiBold.copy(color = colorPalette.secondaryAccent)
        )
    }
}

@Composable
fun SliderDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = { }
) = SwaaraDialogCard(
    onDismiss = onDismiss,
    modifier = modifier
) {
    val typography = LocalAppearance.current.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp)
    ) {
        BasicText(
            text = title,
            style = typography.s.semiBold,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp)
        )

        content()

        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp, end = 20.dp)
        ) {
            DialogTextButton(
                primary = true,
                text = stringResource(R.string.confirm),
                onClick = onDismiss
            )
        }
    }
}
