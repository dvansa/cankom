package dvansa.cankom.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun MainScreen(
    onEditButton: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold() { paddingValues ->
        Column(modifier = modifier.padding(paddingValues)) {
            Text(text = "Main screen")
            Button(onClick = {
                onEditButton();
            }) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
        }
    }
}