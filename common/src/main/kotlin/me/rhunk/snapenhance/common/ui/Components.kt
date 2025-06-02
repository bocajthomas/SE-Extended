package me.rhunk.snapenhance.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rhunk.snapenhance.common.bridge.wrapper.LocaleWrapper


@Composable
fun EditNoteTextField(
    modifier: Modifier = Modifier,
    primaryColor: Color,
    translation: LocaleWrapper,
    content: String?,
    setContent: (String) -> Unit
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 180.dp)
            .then(modifier),
        value = content ?: "",
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = primaryColor
        ),
        onValueChange = {
            setContent(it)
        },
        shape = MaterialTheme.shapes.medium,
        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, color = primaryColor),
        placeholder = { Text(text = translation["manager.sections.manage_scope.notes_placeholder"], fontSize = 12.sp) }
    )
}

@Composable
fun TopBarActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit = {}
) {
    ElevatedButton(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(icon, contentDescription = null)
            Text(text = text, overflow = TextOverflow.Ellipsis)
        }
    }
}