package fake.fixture

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

private fun Activity.column(build: LinearLayout.() -> Unit) = setContentView(
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        fitsSystemWindows = true
        setBackgroundColor(Color.WHITE)
        build()
    },
)

private fun LinearLayout.label(text: String, viewId: Int = View.NO_ID) = addView(
    TextView(context).apply {
        this.text = text
        id = viewId
        textSize = 22f
        setPadding(32, 32, 32, 32)
    },
)

private fun LinearLayout.button(text: String, viewId: Int, onClick: () -> Unit) = addView(
    Button(context).apply {
        this.text = text
        id = viewId
        setOnClickListener { onClick() }
    },
)

class HomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        column {
            id = R.id.tab_bar
            label("Home feed", R.id.feed_tab)
            label("1,204 likes", R.id.row_feed_textview_likes)
            button("Open chat", R.id.open_thread) { startActivity(Intent(this@HomeActivity, ThreadActivity::class.java)) }
            button("Open a reel", R.id.open_reel) { startActivity(Intent(this@HomeActivity, ViewerActivity::class.java)) }
        }
    }
}

class ThreadActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        column {
            label("5 likes", R.id.direct_thread_message)
            addView(EditText(context).apply { id = R.id.row_thread_composer_edittext; hint = "Message" })
            button("Sent reel", R.id.open_sent_reel) { startActivity(Intent(this@ThreadActivity, ViewerActivity::class.java)) }
        }
    }
}

class ViewerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ScrollView(this).apply {
                id = R.id.clips_viewer_view_pager
                isFillViewport = true
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.VERTICAL
                        repeat(4) { index ->
                            addView(
                                FrameLayout(context).apply {
                                    setBackgroundColor(if (index % 2 == 0) Color.DKGRAY else Color.GRAY)
                                    addView(
                                        TextView(context).apply {
                                            text = "Reel $index"
                                            textSize = 30f
                                            setTextColor(Color.WHITE)
                                            gravity = Gravity.CENTER
                                        },
                                        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                                    )
                                },
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, resources.displayMetrics.heightPixels),
                            )
                        }
                    },
                )
            },
        )
    }
}
