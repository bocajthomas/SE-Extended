package me.rhunk.snapenhance.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun CssBackground() {
    val colorScheme = MaterialTheme.colorScheme

    fun Color.toCssRgba(): String {
        val argb = this.toArgb()
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val a = ((argb shr 24) and 0xFF) / 255f
        return "rgba($r, $g, $b, $a)"
    }

    val c1 = colorScheme.primary.toCssRgba()
    val c2 = colorScheme.secondary.toCssRgba()
    val c3 = colorScheme.tertiary.toCssRgba()

    val htmlData = """
        <html>
        <head>
            <style>
                body { margin: 0; padding: 0; overflow: hidden; background: transparent; }
                .container { position: absolute; inset: 0; overflow: hidden; }
                .container::before {
                    content: "";
                    position: absolute;
                    inset: -145%;
                    rotate: -45deg;
                    background: transparent; 
                    background-image: 
                        radial-gradient(4px 100px at 0px 235px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 235px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 117.5px, $c1 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 252px, $c3, #0000),
                        radial-gradient(4px 100px at 300px 252px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 126px, $c1 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 150px, $c2, #0000),
                        radial-gradient(4px 100px at 300px 150px, $c3, #0000),
                        radial-gradient(1.5px 1.5px at 150px 75px, $c2 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 253px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 253px, $c1, #0000),
                        radial-gradient(1.5px 1.5px at 150px 126.5px, $c2 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 204px, $c3, #0000),
                        radial-gradient(4px 100px at 300px 204px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 102px, $c1 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 134px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 134px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 67px, $c3 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 179px, $c2, #0000),
                        radial-gradient(4px 100px at 300px 179px, $c1, #0000),
                        radial-gradient(1.5px 1.5px at 150px 89.5px, $c2 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 299px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 299px, $c3, #0000),
                        radial-gradient(1.5px 1.5px at 150px 149.5px, $c1 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 215px, $c2, #0000),
                        radial-gradient(4px 100px at 300px 215px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 107.5px, $c3 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 281px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 281px, $c1, #0000),
                        radial-gradient(1.5px 1.5px at 150px 140.5px, $c2 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 158px, $c3, #0000),
                        radial-gradient(4px 100px at 300px 158px, $c2, #0000),
                        radial-gradient(1.5px 1.5px at 150px 79px, $c1 100%, #0000 150%),
                        radial-gradient(4px 100px at 0px 210px, $c1, #0000),
                        radial-gradient(4px 100px at 300px 210px, $c3, #0000),
                        radial-gradient(1.5px 1.5px at 150px 105px, $c2 100%, #0000 150%);
                    background-size:
                        300px 235px, 300px 235px, 300px 235px, 300px 252px, 300px 252px, 300px 252px,
                        300px 150px, 300px 150px, 300px 150px, 300px 253px, 300px 253px, 300px 253px,
                        300px 204px, 300px 204px, 300px 204px, 300px 134px, 300px 134px, 300px 134px,
                        300px 179px, 300px 179px, 300px 179px, 300px 299px, 300px 299px, 300px 299px,
                        300px 215px, 300px 215px, 300px 215px, 300px 281px, 300px 281px, 300px 281px,
                        300px 158px, 300px 158px, 300px 158px, 300px 210px, 300px 210px, 300px 210px;
                    animation: animation 150s linear infinite;
                }
                @keyframes animation {
                    0% { background-position: 0px 220px, 3px 220px, 151.5px 337.5px, 25px 24px, 28px 24px, 176.5px 150px, 50px 16px, 53px 16px, 201.5px 91px, 75px 224px, 78px 224px, 226.5px 350.5px, 100px 19px, 103px 19px, 251.5px 121px, 125px 120px, 128px 120px, 276.5px 187px, 150px 31px, 153px 31px, 301.5px 120.5px, 175px 235px, 178px 235px, 326.5px 384.5px, 200px 121px, 203px 121px, 351.5px 228.5px, 225px 224px, 228px 224px, 376.5px 364.5px, 250px 26px, 253px 26px, 401.5px 105px, 275px 75px, 278px 75px, 426.5px 180px; }
                    to { background-position: 0px 6800px, 3px 6800px, 151.5px 6917.5px, 25px 13632px, 28px 13632px, 176.5px 13758px, 50px 5416px, 53px 5416px, 201.5px 5491px, 75px 17175px, 78px 17175px, 226.5px 17301.5px, 100px 5119px, 103px 5119px, 251.5px 5221px, 125px 8428px, 128px 8428px, 276.5px 8495px, 150px 9876px, 153px 9876px, 301.5px 9965.5px, 175px 13391px, 178px 13391px, 326.5px 13540.5px, 200px 14741px, 203px 14741px, 351.5px 14848.5px, 225px 18770px, 228px 18770px, 376.5px 18910.5px, 250px 5082px, 253px 5082px, 401.5px 5161px, 275px 6375px, 278px 6375px, 426.5px 6480px; }
                }
            </style>
        </head>
        <body>
            <div class="container"></div>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(0)
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                settings.javaScriptEnabled = true
                loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)
        }
    )
}