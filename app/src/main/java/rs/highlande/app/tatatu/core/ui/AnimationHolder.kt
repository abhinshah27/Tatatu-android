package rs.highlande.app.tatatu.core.ui

import androidx.annotation.AnimRes
import rs.highlande.app.tatatu.R

/**
 * Base class holding the information for any screen transition that might occur (both for [androidx.fragment.app.Fragment]s
 * and [androidx.appcompat.app.AppCompatActivity]s.
 */
open class AnimationHolder(@AnimRes val enterAnim: Int, val exitAnim: Int, val popEnterAnim: Int, val popExitAnim: Int)

/**
 * Child class holding a default navigation transition pattern.
 */
class NavigationAnimationHolder : AnimationHolder(R.anim.slide_in_from_right, R.anim.slide_out_to_left, R.anim.slide_in_from_left, R.anim.slide_out_to_right)

/**
 * Child class holding a custom transition pattern for activities.
 */
class ActivityAnimationHolder(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int) : AnimationHolder(enterAnim, exitAnim, 0 , 0)

