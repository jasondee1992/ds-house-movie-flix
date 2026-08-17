package com.jasond.homeflix.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

enum class TvIcon { HOME, SEARCH, MOVIES, BOOKMARK, SETTINGS, PLAY, INFO, BACK }

@Composable
fun TvVectorIcon(icon: TvIcon, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val stroke = Stroke(size.minDimension * .09f)
        when (icon) {
            TvIcon.HOME -> {
                val path = Path().apply {
                    moveTo(size.width * .14f, size.height * .48f)
                    lineTo(size.width * .5f, size.height * .16f)
                    lineTo(size.width * .86f, size.height * .48f)
                    lineTo(size.width * .78f, size.height * .48f)
                    lineTo(size.width * .78f, size.height * .84f)
                    lineTo(size.width * .22f, size.height * .84f)
                    lineTo(size.width * .22f, size.height * .48f)
                    close()
                }
                drawPath(path, color, style = stroke)
            }
            TvIcon.SEARCH -> {
                drawCircle(color, size.minDimension * .25f, Offset(size.width * .43f, size.height * .42f), style = stroke)
                drawLine(color, Offset(size.width * .62f, size.height * .61f), Offset(size.width * .84f, size.height * .83f), stroke.width)
            }
            TvIcon.MOVIES -> {
                drawRoundRect(color, Offset(size.width * .12f, size.height * .2f), Size(size.width * .76f, size.height * .64f), style = stroke)
                drawLine(color, Offset(size.width * .12f, size.height * .39f), Offset(size.width * .88f, size.height * .39f), stroke.width)
                repeat(3) { index ->
                    val x = size.width * (.25f + index * .25f)
                    drawLine(color, Offset(x, size.height * .2f), Offset(x - size.width * .08f, size.height * .39f), stroke.width)
                }
            }
            TvIcon.BOOKMARK -> drawPath(Path().apply {
                moveTo(size.width * .25f, size.height * .14f); lineTo(size.width * .75f, size.height * .14f)
                lineTo(size.width * .75f, size.height * .86f); lineTo(size.width * .5f, size.height * .69f)
                lineTo(size.width * .25f, size.height * .86f); close()
            }, color, style = stroke)
            TvIcon.SETTINGS -> {
                drawCircle(color, size.minDimension * .31f, style = stroke)
                drawCircle(color, size.minDimension * .09f, style = stroke)
            }
            TvIcon.PLAY -> drawPath(Path().apply {
                moveTo(size.width * .3f, size.height * .18f); lineTo(size.width * .8f, size.height * .5f)
                lineTo(size.width * .3f, size.height * .82f); close()
            }, color)
            TvIcon.INFO -> {
                drawCircle(color, size.minDimension * .36f, style = stroke)
                drawCircle(color, size.minDimension * .045f, Offset(size.width * .5f, size.height * .32f))
                drawLine(color, Offset(size.width * .5f, size.height * .44f), Offset(size.width * .5f, size.height * .7f), stroke.width)
            }
            TvIcon.BACK -> {
                drawLine(color, Offset(size.width * .78f, size.height * .5f), Offset(size.width * .2f, size.height * .5f), stroke.width)
                drawLine(color, Offset(size.width * .2f, size.height * .5f), Offset(size.width * .45f, size.height * .25f), stroke.width)
                drawLine(color, Offset(size.width * .2f, size.height * .5f), Offset(size.width * .45f, size.height * .75f), stroke.width)
            }
        }
    }
}
