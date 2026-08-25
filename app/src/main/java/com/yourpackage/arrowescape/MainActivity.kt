package com.yourpackage.arrowescape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.*
import kotlin.math.max
import kotlin.math.min

data class Point(val x: Float, val y: Float)
data class ArrowSegment(val start: Point, val end: Point)
enum class Direction { UP, DOWN, LEFT, RIGHT }

data class Arrow(
    val id: Int,
    val direction: Direction,
    val segments: List<ArrowSegment>,
    val exitPoint: Point,
    val color: Color,
    val pathWidth: Float = 0.07f
)

data class Level(
    val id: Int,
    val arrows: List<Arrow>
)

data class GameState(
    val currentLevelIndex: Int,
    val level: Level,
    val remainingArrows: List<Arrow>,
    val lives: Int,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false
)

class GameViewModel : ViewModel() {
    private val levels = listOf(
        // Level 1
        Level(1, listOf(
            Arrow(1, Direction.RIGHT, listOf(ArrowSegment(Point(0.2f, 0.4f), Point(0.5f, 0.4f))), Point(0.5f, 0.4f), Color(0xFFE53935)),
            Arrow(2, Direction.UP, listOf(ArrowSegment(Point(0.6f, 0.7f), Point(0.6f, 0.3f))), Point(0.6f, 0.3f), Color(0xFF1E88E5))
        )),
        // Level 2
        Level(2, listOf(
            Arrow(1, Direction.RIGHT, listOf(ArrowSegment(Point(0.2f, 0.3f), Point(0.7f, 0.3f))), Point(0.7f, 0.3f), Color(0xFFE53935)),
            Arrow(2, Direction.DOWN, listOf(ArrowSegment(Point(0.4f, 0.2f), Point(0.4f, 0.7f))), Point(0.4f, 0.7f), Color(0xFF43A047)),
            Arrow(3, Direction.LEFT, listOf(ArrowSegment(Point(0.8f, 0.6f), Point(0.3f, 0.6f))), Point(0.3f, 0.6f), Color(0xFFFB8C00))
        ))
    )

    private val _state = MutableStateFlow(
        GameState(currentLevelIndex = 0, level = levels[0], remainingArrows = levels[0].arrows, lives = 3)
    )
    val state = _state.asStateFlow()

    fun onArrowTapped(arrowId: Int) {
        val cur = _state.value
        if (cur.isGameOver || cur.isComplete) return
        val target = cur.remainingArrows.find { it.id == arrowId } ?: return

        // Check if path is clear from other remaining arrows
        val others = cur.remainingArrows.filter { it.id != arrowId }
        val isBlocked = others.any { other -> 
            doesIntersect(target, other) 
        }

        if (!isBlocked) {
            val updated = others
            val isFinished = updated.isEmpty()
            if (isFinished && cur.currentLevelIndex + 1 < levels.size) {
                // Next level
                val nextIdx = cur.currentLevelIndex + 1
                _state.update { 
                    it.copy(
                        currentLevelIndex = nextIdx, 
                        level = levels[nextIdx], 
                        remainingArrows = levels[nextIdx].arrows,
                        isComplete = false
                    ) 
                }
            } else {
                _state.update { it.copy(remainingArrows = updated, isComplete = isFinished) }
            }
        } else {
            // Penalty for hitting blocked arrow
            val lives = cur.lives - 1
            _state.update { it.copy(lives = lives, isGameOver = lives <= 0) }
        }
    }

    private fun doesIntersect(a: Arrow, b: Arrow): Boolean {
        // Simple bounding box collision check between arrows
        val aMinX = a.segments.minOf { min(it.start.x, it.end.x) }
        val aMaxX = a.segments.maxOf { max(it.start.x, it.end.x) }
        val aMinY = a.segments.minOf { min(it.start.y, it.end.y) }
        val aMaxY = a.segments.maxOf { max(it.start.y, it.end.y) }

        val bMinX = b.segments.minOf { min(it.start.x, it.end.x) }
        val bMaxX = b.segments.maxOf { max(it.start.x, it.end.x) }
        val bMinY = b.segments.minOf { min(it.start.y, it.end.y) }
        val bMaxY = b.segments.maxOf { max(it.start.y, it.end.y) }

        return aMinX < bMaxX && aMaxX > bMinX && aMinY < bMaxY && aMaxY > bMinY
    }

    fun restartGame() {
        _state.value = GameState(currentLevelIndex = 0, level = levels[0], remainingArrows = levels[0].arrows, lives = 3)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsState()

            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFF121212)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                Text("ARROW ESCAPE", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Level ${state.currentLevelIndex + 1}  |  Lives: ${state.lives}  |  Left: ${state.remainingArrows.size}", color = Color.LightGray, fontSize = 16.sp)
                Spacer(Modifier.height(32.dp))

                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E))) {
                    Canvas(
                        modifier = Modifier.fillMaxSize().pointerInput(state.remainingArrows) {
                            detectTapGestures { offset ->
                                val nx = offset.x / size.width
                                val ny = offset.y / size.height
                                state.remainingArrows.find {
                                    val startX = it.segments.first().start.x
                                    val startY = it.segments.first().start.y
                                    kotlin.math.abs(startX - nx) < 0.15f && kotlin.math.abs(startY - ny) < 0.15f
                                }?.let { vm.onArrowTapped(it.id) }
                            }
                        }
                    ) {
                        val w = size.width
                        val h = size.height

                        // Grid background
                        val grid = 6
                        for (i in 1 until grid) {
                            drawLine(Color(0xFF2A2A2A), Offset(w * i / grid, 0f), Offset(w * i / grid, h), strokeWidth = 2f)
                            drawLine(Color(0xFF2A2A2A), Offset(0f, h * i / grid), Offset(w, h * i / grid), strokeWidth = 2f)
                        }

                        // Render Arrows
                        state.remainingArrows.forEach { arrow ->
                            val path = Path()
                            val startX = arrow.segments.first().start.x * w
                            val startY = arrow.segments.first().start.y * h
                            val endX = arrow.exitPoint.x * w
                            val endY = arrow.exitPoint.y * h

                            path.moveTo(startX, startY)
                            path.lineTo(endX, endY)

                            // Arrow Head
                            val hs = 35f
                            when (arrow.direction) {
                                Direction.UP -> { path.moveTo(endX - hs/2, endY + hs); path.lineTo(endX, endY); path.lineTo(endX + hs/2, endY + hs) }
                                Direction.DOWN -> { path.moveTo(endX - hs/2, endY - hs); path.lineTo(endX, endY); path.lineTo(endX + hs/2, endY - hs) }
                                Direction.LEFT -> { path.moveTo(endX + hs, endY - hs/2); path.lineTo(endX, endY); path.lineTo(endX + hs, endY + hs/2) }
                                Direction.RIGHT -> { path.moveTo(endX - hs, endY - hs/2); path.lineTo(endX, endY); path.lineTo(endX - hs, endY + hs/2) }
                            }

                            drawPath(path, arrow.color, style = Stroke(arrow.pathWidth * w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                if (state.isGameOver) {
                    Text("GAME OVER ❌", fontSize = 24.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))) {
                        Text("Try Again", color = Color.White, fontSize = 16.sp)
                    }
                } else if (state.isComplete && state.currentLevelIndex >= 1) {
                    Text("YOU WIN! 🎉", fontSize = 24.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.restartGame() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) {
                        Text("Play Again", color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
