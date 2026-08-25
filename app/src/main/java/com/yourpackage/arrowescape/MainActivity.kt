package com.yourpackage.arrowescape

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
enum class Difficulty { EASY, NORMAL, HARD, EXPERT }

data class Arrow(
    val id: Int,
    val direction: Direction,
    val segments: List<ArrowSegment>,
    val exitPoint: Point,
    val pathWidth: Float = 0.05f
)

data class Level(
    val id: Int,
    val difficulty: Difficulty,
    val arrows: List<Arrow>,
    val startingLives: Int = 3
)

data class GameState(
    val level: Level,
    val remainingArrows: List<Arrow>,
    val lives: Int,
    val isComplete: Boolean = false,
    val isGameOver: Boolean = false
)

class CollisionEngine {
    fun isArrowFree(target: Arrow, others: List<Arrow>): Boolean {
        val otherSegments = others.filter { it.id != target.id }.flatMap { it.segments }
        val (minX, maxX, minY, maxY) = when (target.direction) {
            Direction.UP -> listOf(target.exitPoint.x - target.pathWidth, target.exitPoint.x + target.pathWidth, 0f, target.exitPoint.y)
            Direction.DOWN -> listOf(target.exitPoint.x - target.pathWidth, target.exitPoint.x + target.pathWidth, target.exitPoint.y, 1f)
            Direction.LEFT -> listOf(0f, target.exitPoint.x, target.exitPoint.y - target.pathWidth, target.exitPoint.y + target.pathWidth)
            Direction.RIGHT -> listOf(target.exitPoint.x, 1f, target.exitPoint.y - target.pathWidth, target.exitPoint.y + target.pathWidth)
        }
        return otherSegments.none { segment ->
            val segMinX = min(segment.start.x, segment.end.x)
            val segMaxX = max(segment.start.x, segment.end.x)
            val segMinY = min(segment.start.y, segment.end.y)
            val segMaxY = max(segment.start.y, segment.end.y)
            segMinX <= maxX && segMaxX >= minX && segMinY <= maxY && segMaxY >= minY
        }
    }
}

class GameViewModel : ViewModel() {
    private val collisionEngine = CollisionEngine()
    private val sampleLevel = Level(
        1, Difficulty.EASY, listOf(
            Arrow(1, Direction.RIGHT, listOf(ArrowSegment(Point(0.2f, 0.4f), Point(0.5f, 0.4f))), Point(0.5f, 0.4f)),
            Arrow(2, Direction.UP, listOf(ArrowSegment(Point(0.7f, 0.8f), Point(0.7f, 0.3f))), Point(0.7f, 0.3f)),
            Arrow(3, Direction.LEFT, listOf(ArrowSegment(Point(0.8f, 0.6f), Point(0.3f, 0.6f))), Point(0.3f, 0.6f))
        )
    )
    private val _state = MutableStateFlow(GameState(sampleLevel, sampleLevel.arrows, 3))
    val state = _state.asStateFlow()

    fun onArrowTapped(arrowId: Int) {
        val cur = _state.value
        if (cur.isGameOver || cur.isComplete) return
        val target = cur.remainingArrows.find { it.id == arrowId } ?: return
        if (collisionEngine.isArrowFree(target, cur.remainingArrows)) {
            val updated = cur.remainingArrows.filter { it.id != arrowId }
            _state.update { it.copy(remainingArrows = updated, isComplete = updated.isEmpty()) }
        } else {
            val lives = cur.lives - 1
            _state.update { it.copy(lives = lives, isGameOver = lives <= 0) }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsState()
            Column(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("ARROW ESCAPE", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B2A49))
                Text("Lives: ${state.lives}  |  Remaining: ${state.remainingArrows.size}")
                Spacer(Modifier.height(32.dp))
                Canvas(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.White)
                        .pointerInput(state.remainingArrows) {
                            detectTapGestures { offset ->
                                val nx = offset.x / size.width
                                val ny = offset.y / size.height
                                state.remainingArrows.find {
                                    val dx = it.exitPoint.x - nx
                                    val dy = it.exitPoint.y - ny
                                    (dx * dx + dy * dy) < 0.02f
                                }?.let { vm.onArrowTapped(it.id) }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    state.remainingArrows.forEach { arrow ->
                        val path = Path()
                        arrow.segments.forEachIndexed { i, seg ->
                            if (i == 0) path.moveTo(seg.start.x * w, seg.start.y * h)
                            else path.lineTo(seg.start.x * w, seg.start.y * h)
                            path.lineTo(seg.end.x * w, seg.end.y * h)
                        }
                        drawPath(path, Color(0xFF1B2A49), style = Stroke(arrow.pathWidth * w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                if (state.isComplete) {
                    Spacer(Modifier.height(24.dp))
                    Text("Level Cleared! 🎉", fontSize = 22.sp, color = Color(0xFF0096FF), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
