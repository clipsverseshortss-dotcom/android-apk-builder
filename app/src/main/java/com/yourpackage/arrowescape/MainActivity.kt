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
enum class Difficulty { EASY, NORMAL, HARD, EXPERT }

data class Arrow(
    val id: Int,
    val direction: Direction,
    val segments: List<ArrowSegment>,
    val exitPoint: Point,
    val color: Color,
    val pathWidth: Float = 0.06f
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
        // Simplified collision logic
        return others.none { it.id != target.id && intersects(target, it) }
    }
    
    private fun intersects(a: Arrow, b: Arrow): Boolean {
        // Mock intersection for prototype
        return false 
    }
}

class GameViewModel : ViewModel() {
    private val collisionEngine = CollisionEngine()
    private val sampleLevel = Level(
        1, Difficulty.EASY, listOf(
            Arrow(1, Direction.RIGHT, listOf(ArrowSegment(Point(0.2f, 0.4f), Point(0.6f, 0.4f))), Point(0.6f, 0.4f), Color(0xFFE53935)),
            Arrow(2, Direction.UP, listOf(ArrowSegment(Point(0.6f, 0.7f), Point(0.6f, 0.3f))), Point(0.6f, 0.3f), Color(0xFF1E88E5)),
            Arrow(3, Direction.LEFT, listOf(ArrowSegment(Point(0.8f, 0.6f), Point(0.3f, 0.6f))), Point(0.3f, 0.6f), Color(0xFF43A047)),
            Arrow(4, Direction.DOWN, listOf(ArrowSegment(Point(0.4f, 0.2f), Point(0.4f, 0.7f))), Point(0.4f, 0.7f), Color(0xFFFB8C00))
        )
    )
    private val _state = MutableStateFlow(GameState(sampleLevel, sampleLevel.arrows, 3))
    val state = _state.asStateFlow()

    fun onArrowTapped(arrowId: Int) {
        val cur = _state.value
        if (cur.isGameOver || cur.isComplete) return
        val target = cur.remainingArrows.find { it.id == arrowId } ?: return
        
        val updated = cur.remainingArrows.filter { it.id != arrowId }
        _state.update { it.copy(remainingArrows = updated, isComplete = updated.isEmpty()) }
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
                Spacer(Modifier.height(32.dp))
                Text("ARROW ESCAPE", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text("Lives: ${state.lives}  |  Remaining: ${state.remainingArrows.size}", color = Color.LightGray, fontSize = 18.sp)
                Spacer(Modifier.height(48.dp))
                
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(0xFF1E1E1E))) {
                    Canvas(
                        modifier = Modifier.fillMaxSize().pointerInput(state.remainingArrows) {
                            detectTapGestures { offset ->
                                val nx = offset.x / size.width
                                val ny = offset.y / size.height
                                state.remainingArrows.find {
                                    val dx = it.segments.first().start.x - nx
                                    val dy = it.segments.first().start.y - ny
                                    (dx * dx + dy * dy) < 0.05f || 
                                    (it.exitPoint.x - nx)*(it.exitPoint.x - nx) + (it.exitPoint.y - ny)*(it.exitPoint.y - ny) < 0.05f
                                }?.let { vm.onArrowTapped(it.id) }
                            }
                        }
                    ) {
                        val w = size.width
                        val h = size.height
                        
                        // Draw grid
                        val gridSize = 5
                        for (i in 1 until gridSize) {
                            drawLine(Color(0xFF333333), Offset(w * i / gridSize, 0f), Offset(w * i / gridSize, h), strokeWidth = 2f)
                            drawLine(Color(0xFF333333), Offset(0f, h * i / gridSize), Offset(w, h * i / gridSize), strokeWidth = 2f)
                        }

                        // Draw arrows
                        state.remainingArrows.forEach { arrow ->
                            val path = Path()
                            val startX = arrow.segments.first().start.x * w
                            val startY = arrow.segments.first().start.y * h
                            val endX = arrow.exitPoint.x * w
                            val endY = arrow.exitPoint.y * h
                            
                            path.moveTo(startX, startY)
                            path.lineTo(endX, endY)
                            
                            // Draw the arrow head
                            val headSize = 40f
                            when (arrow.direction) {
                                Direction.UP -> {
                                    path.moveTo(endX - headSize/2, endY + headSize)
                                    path.lineTo(endX, endY)
                                    path.lineTo(endX + headSize/2, endY + headSize)
                                }
                                Direction.DOWN -> {
                                    path.moveTo(endX - headSize/2, endY - headSize)
                                    path.lineTo(endX, endY)
                                    path.lineTo(endX + headSize/2, endY - headSize)
                                }
                                Direction.LEFT -> {
                                    path.moveTo(endX + headSize, endY - headSize/2)
                                    path.lineTo(endX, endY)
                                    path.lineTo(endX + headSize, endY + headSize/2)
                                }
                                Direction.RIGHT -> {
                                    path.moveTo(endX - headSize, endY - headSize/2)
                                    path.lineTo(endX, endY)
                                    path.lineTo(endX - headSize, endY + headSize/2)
                                }
                            }
                            drawPath(path, arrow.color, style = Stroke(arrow.pathWidth * w, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
                
                if (state.isComplete) {
                    Spacer(Modifier.height(32.dp))
                    Text("LEVEL CLEARED! 🚀", fontSize = 28.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
