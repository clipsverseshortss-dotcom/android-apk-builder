package com.yourpackage.arrowescape

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
    val startingLives: Int = 3,
    val startingHints: Int = 2
)

data class GameState(
    val level: Level,
    val remainingArrows: List<Arrow>,
    val lives: Int,
    val hintsRemaining: Int,
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
