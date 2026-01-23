package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import me.rhunk.snapenhance.common.ui.cardShape
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.ui.manager.Routes
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.json.JSONObject

class HomeCredits : Routes.Route() {
    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val creditsJson = remember {
            context.androidContext.assets.open("credits.json").bufferedReader()
                .use { it.readText() }
        }
        val jsonObject = remember { JSONObject(creditsJson) }
        val donators = remember { jsonObject.getJSONArray("donators") }
        val contributors = remember { jsonObject.getJSONArray("contributors") }
        val translators = remember { jsonObject.getJSONArray("translators") }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            item {
                CreditHeader(translation["donators_title"])
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    (0 until donators.length()).forEach { index ->
                        val item = donators.getJSONObject(index)
                        CreditCard(
                            name = item.getString("name"),
                            imageUrl = item.getString("avatar_url"),
                            shape = cardShape(donators.length(), index)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                CreditHeader(translation["contributors_title"])
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    (0 until contributors.length()).forEach { index ->
                        val item = contributors.getJSONObject(index)
                        CreditCard(
                            name = item.getString("name"),
                            imageUrl = item.getString("avatar_url"),
                            redirectUrl = item.getString("commits_url"),
                            subText = "${item.getString("contributions")} " + translation["contributions_desc"],
                            shape = cardShape(contributors.length(), index)
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                CreditHeader(translation["translators_title"])
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    (0 until translators.length()).forEach { index ->
                        val item = translators.getJSONObject(index)
                        CreditCard(
                            name = item.getString("name"),
                            subText = item.getString("language"),
                            imageUrl = item.getString("avatar_url"),
                            shape = cardShape(translators.length(), index)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun CreditHeader(title: String) {
        Text(
            text = title,
            modifier = Modifier.padding(16.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }

    private fun openLink(link: String) {
        kotlin.runCatching {
            context.activity?.startActivity(Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                data = link.toUri()
            })
        }.onFailure {
            context.log.error("Couldn't open link", it)
            context.shortToast("Couldn't open link. Check SE Extended logs for more details.")
        }
    }

    @Composable
    private fun CreditCard(
        name: String,
        imageUrl: String? = null,
        subText: String? = null,
        redirectUrl: String? = null,
        shape: Shape
    ) {
        val context = LocalContext.current
        val imageLoader = remember {
            ImageLoader.Builder(context)
                .okHttpClient {
                    OkHttpClient.Builder()
                        .cache(Cache(context.cacheDir, 10 * 1024 * 1024))
                        .followRedirects(true)
                        .build()
                }
                .crossfade(true)
                .build()
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (redirectUrl != null) {
                            Modifier.clickable { openLink(redirectUrl) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(imageUrl)
                            .addHeader("Accept", "image/png,image/webp,image/*")
                            .addHeader("User-Agent", "Coil/2.0")
                            .build(),
                        imageLoader = imageLoader,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 20.sp
                    )
                    if (subText != null) {
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Light,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}