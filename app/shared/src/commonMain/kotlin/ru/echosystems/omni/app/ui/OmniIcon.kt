package ru.echosystems.omni.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class OmniIconKind { Inventory, Person, Lock, Visibility, VisibilityOff, Login }

@Composable
fun OmniIcon(kind: OmniIconKind, tint: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val color = if (tint == Color.Unspecified) androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant else tint
    Canvas(modifier.size(24.dp)) {
        val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            OmniIconKind.Person -> {
                drawCircle(color, radius = size.minDimension * .18f, center = Offset(size.width / 2, size.height * .30f), style = stroke)
                drawArc(color, 205f, 130f, false, Offset(size.width * .22f, size.height * .46f), Size(size.width * .56f, size.height * .42f), style = stroke)
            }
            OmniIconKind.Lock -> {
                drawRoundRect(color, Offset(size.width * .22f, size.height * .43f), Size(size.width * .56f, size.height * .40f), size.width * .08f, size.width * .08f, style = stroke)
                drawArc(color, 180f, 180f, false, Offset(size.width * .32f, size.height * .18f), Size(size.width * .36f, size.height * .45f), style = stroke)
            }
            OmniIconKind.Inventory -> {
                drawRoundRect(color, Offset(size.width * .16f, size.height * .25f), Size(size.width * .68f, size.height * .56f), size.width * .08f, size.width * .08f, style = stroke)
                drawLine(color, Offset(size.width * .16f, size.height * .43f), Offset(size.width * .84f, size.height * .43f), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * .40f, size.height * .58f), Offset(size.width * .60f, size.height * .58f), strokeWidth = stroke.width)
            }
            OmniIconKind.Visibility, OmniIconKind.VisibilityOff -> {
                drawOval(color, Offset(size.width * .10f, size.height * .26f), Size(size.width * .80f, size.height * .48f), style = stroke)
                drawCircle(color, size.width * .13f, Offset(size.width / 2, size.height / 2))
                if (kind == OmniIconKind.VisibilityOff) drawLine(color, Offset(size.width * .18f, size.height * .18f), Offset(size.width * .82f, size.height * .82f), strokeWidth = stroke.width)
            }
            OmniIconKind.Login -> {
                drawRoundRect(color, Offset(size.width * .18f, size.height * .16f), Size(size.width * .48f, size.height * .68f), size.width * .08f, size.width * .08f, style = stroke)
                drawLine(color, Offset(size.width * .45f, size.height / 2), Offset(size.width * .86f, size.height / 2), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * .68f, size.height * .30f), Offset(size.width * .86f, size.height / 2), strokeWidth = stroke.width)
                drawLine(color, Offset(size.width * .68f, size.height * .70f), Offset(size.width * .86f, size.height / 2), strokeWidth = stroke.width)
            }
        }
    }
}
