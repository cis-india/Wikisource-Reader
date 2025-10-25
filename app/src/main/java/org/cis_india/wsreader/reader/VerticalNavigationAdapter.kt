/*
 * Copyright (c) [2022 - Present] Stɑrry Shivɑm
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cis_india.wsreader.reader

import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.input.DragEvent
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.Key
import org.readium.r2.navigator.input.KeyEvent
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import kotlin.math.abs

/**
 * An [InputListener] that turns pages when tapping or swiping vertically instead of horizontally.
 * This adapter intercepts tap events and uses vertical positioning to determine page navigation.
 *
 * Based on Readium's DirectionalNavigationAdapter but configured for vertical navigation.
 */
@ExperimentalReadiumApi
class VerticalNavigationAdapter(
    private val navigator: OverflowableNavigator,
    private val handleTapsWhileScrolling: Boolean = false,
    private val minimumVerticalEdgeSize: Double = 80.0,
    private val verticalEdgeThresholdPercent: Double? = 0.3,
    private val animatedTransition: Boolean = true
) : InputListener {

    // Track if we've already handled the current drag gesture
    private var dragHandled = false

    override fun onTap(event: TapEvent): Boolean {
        // Don't handle taps while scrolling if configured
        if (navigator.overflow.value.scroll && !handleTapsWhileScrolling) {
            return false
        }

        val height = navigator.publicationView.height.toDouble()

        val verticalEdgeSize = verticalEdgeThresholdPercent?.let {
            maxOf(minimumVerticalEdgeSize, it * height)
        } ?: minimumVerticalEdgeSize

        val topRange = 0.0..verticalEdgeSize
        val bottomRange = (height - verticalEdgeSize)..height

        return when {
            event.point.y in topRange -> {
                // Top edge - go forward (next page)
                navigator.goForward(animated = animatedTransition)
            }
            event.point.y in bottomRange -> {
                // Bottom edge - go backward (previous page)
                navigator.goBackward(animated = animatedTransition)
            }
            else -> {
                // Middle area - don't handle
                false
            }
        }
    }

    override fun onDrag(event: DragEvent): Boolean {
        // Don't handle drags while scrolling if configured
        if (navigator.overflow.value.scroll && !handleTapsWhileScrolling) {
            return false
        }

        val verticalDistance = event.offset.y
        val horizontalDistance = event.offset.x

        // Only handle swipes that are primarily vertical
        if (abs(verticalDistance) <= abs(horizontalDistance)) {
            dragHandled = false
            return false
        }

        // If we've already handled this drag gesture, don't handle again
        if (dragHandled) {
            return true
        }

        // Minimum swipe distance threshold (in pixels) - reduced for better responsiveness
        val minSwipeDistance = 80

        // Only trigger on continuous drag, not just any movement
        if (abs(verticalDistance) < 20) {
            return false
        }

        // Swipe up (negative Y offset) = next page (forward)
        // Swipe down (positive Y offset) = previous page (backward)
        val handled = when {
            verticalDistance < -minSwipeDistance -> {
                // Swipe up - go forward (next page)
                dragHandled = true
                navigator.goForward(animated = animatedTransition)
            }
            verticalDistance > minSwipeDistance -> {
                // Swipe down - go backward (previous page)
                dragHandled = true
                navigator.goBackward(animated = animatedTransition)
            }
            else -> {
                // Still in progress, claim the event to prevent other handlers
                abs(verticalDistance) > abs(horizontalDistance)
            }
        }

        // Reset the flag when drag distance is small (finger lifted or reversed direction)
        if (abs(verticalDistance) < 30) {
            dragHandled = false
        }

        return handled
    }

    override fun onKey(event: KeyEvent): Boolean {
        if (event.type != KeyEvent.Type.Down || event.modifiers.isNotEmpty()) {
            return false
        }

        // Map arrow keys to vertical navigation
        return when (event.key) {
            Key.ArrowUp -> navigator.goBackward(animated = animatedTransition)
            Key.ArrowDown, Key.Space -> navigator.goForward(animated = animatedTransition)
            Key.ArrowLeft -> navigator.goBackward(animated = animatedTransition)
            Key.ArrowRight -> navigator.goForward(animated = animatedTransition)
            else -> false
        }
    }
}
