package com.junkfood.seal.ui.page.settings.interaction

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.DialogSingleChoiceItem
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.util.NONE
import com.junkfood.seal.util.USE_PREVIOUS_SELECTION

@Composable
fun DownloadTypeCustomizationDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    selectedItem: Int,
    onSelect: (Int) -> Unit,
) {
    SealDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        confirmButton = null,
        title = { Text(text = "Default download type") },
        text = {
            LazyColumn(modifier = Modifier.padding()) {
                item {
                    DialogSingleChoiceItem(
                        text = "Remember previous selection",
                        selected = selectedItem == USE_PREVIOUS_SELECTION,
                        containerColor = Color.Transparent,
                    ) {
                        onSelect(USE_PREVIOUS_SELECTION)
                    }
                }

                item {
                    DialogSingleChoiceItem(
                        text = "No default",
                        selected = selectedItem == NONE,
                        containerColor = Color.Transparent,
                    ) {
                        onSelect(NONE)
                    }
                }
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    DownloadTypeCustomizationDialog(onDismissRequest = {}, selectedItem = NONE) {}
}
