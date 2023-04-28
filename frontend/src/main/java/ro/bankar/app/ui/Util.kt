package ro.bankar.app.ui

import androidx.constraintlayout.compose.ConstraintSetScope



fun ConstraintSetScope.createRefsFor(vararg ids: String) = ids.map { createRefFor(it) }