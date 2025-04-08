package com.vishal2376.miniplayerexperiment

import android.os.Bundle
import android.util.TypedValue
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private var isNotestFullScreen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnChangeMode = findViewById<Button>(R.id.changeMode)
        val rootLayout = findViewById<ConstraintLayout>(R.id.root)

        btnChangeMode.setOnClickListener {
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootLayout)

            if (!isNotestFullScreen) {
                // Mini-player mode dimensions
                val miniHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 140f, resources.displayMetrics
                ).toInt()
                val miniWidth = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 200f, resources.displayMetrics
                ).toInt()

                constraintSet.constrainHeight(R.id.player_view, miniHeight)
                constraintSet.constrainWidth(R.id.player_view, miniWidth)

                // Position bottom-right
                constraintSet.clear(R.id.player_view, ConstraintSet.TOP)
                constraintSet.connect(
                    R.id.player_view,
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END,
                    16
                )
                constraintSet.connect(
                    R.id.player_view,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    16
                )

                // dummyNotes takes full height
                constraintSet.connect(
                    R.id.dummyNotes, ConstraintSet.TOP, R.id.debugView, ConstraintSet.BOTTOM, 0
                )
                constraintSet.connect(
                    R.id.dummyNotes,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    0
                )
                constraintSet.constrainHeight(R.id.dummyNotes, 0)

                isNotestFullScreen = true

            } else {
                // Fullscreen player
                val fullHeight = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 300f, resources.displayMetrics
                ).toInt()

                constraintSet.constrainHeight(R.id.player_view, fullHeight)
                constraintSet.constrainWidth(R.id.player_view, ConstraintSet.MATCH_CONSTRAINT)

                constraintSet.connect(
                    R.id.player_view, ConstraintSet.TOP, R.id.debugView, ConstraintSet.BOTTOM, 0
                )
                constraintSet.clear(R.id.player_view, ConstraintSet.BOTTOM)
                constraintSet.clear(R.id.player_view, ConstraintSet.END)

                // dummyNotes below player_view
                constraintSet.connect(
                    R.id.dummyNotes, ConstraintSet.TOP, R.id.player_view, ConstraintSet.BOTTOM, 0
                )
                constraintSet.connect(
                    R.id.dummyNotes,
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM,
                    0
                )
                constraintSet.constrainHeight(R.id.dummyNotes, 0)

                isNotestFullScreen = false
            }

            constraintSet.applyTo(rootLayout)
        }
    }
}
