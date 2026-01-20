package com.example.dozo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween // <-- FIX 1: Import 'tween' (not coreTween)
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// --- EFFECT 1: FLIPPABLE CARD CONTAINER ---
@Composable
fun FlippableCardContainer(
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFlipped by remember { mutableStateOf(false) }

    // Animate rotation 0 -> 180
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600), // <-- FIX 2: Use 'tween'
        label = "flip_rotation"
    )

    // Animate elevation (lift up slightly while flipping)
    val zAxisDistance by animateFloatAsState(
        targetValue = if (rotation > 5f && rotation < 175f) 30f else 0f,
        label = "lift_animation"
    )

    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { isFlipped = !isFlipped }
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
                shadowElevation = zAxisDistance // <-- FIX 3: Use 'shadowElevation' instead of 'translationZ'
            }
    ) {
        if (rotation <= 90f) {
            // Front Content
            Box(Modifier.fillMaxSize()) {
                front()
            }
        } else {
            // Back Content (Must rotate back 180 so it's not mirrored)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = 180f
                    }
            ) {
                back()
            }
        }
    }
}

// --- EFFECT 2: 3D PRESS TILT MODIFIER ---
@Composable
fun ThreeDTiltCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Rename variables to avoid conflict with graphicsLayer properties
    val animatedScale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")
    val animatedRotationX by animateFloatAsState(if (isPressed) 5f else 0f, label = "rotX")

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                rotationX = animatedRotationX
                cameraDistance = 10f * density
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val up = waitForUpOrCancellation()
                        isPressed = false
                        if (up != null) {
                            onClick()
                        }
                    }
                }
            }
    ) {
        content()
    }
}