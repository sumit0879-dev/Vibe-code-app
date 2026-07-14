package com.vibecode.ide.ui.editor.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibecode.ide.ui.editor.OpenTab
import com.vibecode.ide.ui.theme.AuroraCyan
import com.vibecode.ide.ui.theme.Motion
import com.vibecode.ide.ui.theme.SurfaceDeep
import com.vibecode.ide.ui.theme.SurfaceHighest
import com.vibecode.ide.ui.theme.TextMuted

@Composable
fun EditorTabBar(
    tabs: List<OpenTab>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    LazyRow(
        modifier
            .fillMaxWidth()
            .background(SurfaceDeep),
    ) {
        items(tabs.size) { index ->
            TabItem(
                tab = tabs[index],
                isActive = index == activeIndex,
                onSelect = { onSelect(index) },
                onClose = { onClose(index) },
            )
        }
    }
}

@Composable
private fun TabItem(
    tab: OpenTab,
    isActive: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    val bg by animateColorAsState(
        if (isActive) SurfaceHighest else SurfaceDeep,
        animationSpec = Motion.fast(),
        label = "tabBg",
    )
    Column {
        Row(
            Modifier
                .background(bg)
                .clickable { onSelect() }
                .padding(start = 14.dp, end = 6.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                tab.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isActive) MaterialTheme.colorScheme.onSurface else TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(6.dp))
            if (tab.isDirty) {
                Box(Modifier.size(7.dp).background(AuroraCyan, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            IconButton(onClick = onClose, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Close tab", modifier = Modifier.size(14.dp), tint = TextMuted)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isActive) AuroraCyan else Color.Transparent),
        )
    }
}
