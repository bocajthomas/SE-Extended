package me.rhunk.snapenhance.ui.manager.pages.home

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QuestionMark
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.navigation.NavBackStackEntry
import me.rhunk.snapenhance.R
import me.rhunk.snapenhance.common.ui.cardShapeGroupedBottom
import me.rhunk.snapenhance.common.ui.cardShapeGroupedMiddle
import me.rhunk.snapenhance.common.ui.cardShapeGroupedTop
import me.rhunk.snapenhance.common.ui.lazyColumnContentPadding
import me.rhunk.snapenhance.ui.manager.Routes

class HomeAbout: Routes.Route() {
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

    override val content: @Composable (NavBackStackEntry) -> Unit = {
        val packageName = context.androidContext.packageName
        val clipboardManager = LocalClipboardManager.current
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = lazyColumnContentPadding(staticVertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedTop,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboardManager.setText(
                                        AnnotatedString(packageName)
                                    )
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["package_name_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = packageName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://t.me/SE_Extended")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_telegram),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["telegram_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "https://t.me/SE_Extended",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://www.reddit.com/r/SEExtendedApp/")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_reddit),
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["reddit_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "https://www.reddit.com/r/SEExtendedApp/",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://github.com/bocajthomas/SE-Extended/wiki")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.QuestionMark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["wiki_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "https://github.com/bocajthomas/SE-Extended/wiki",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://ko-fi.com/seextended")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AttachMoney, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["donate_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "https://ko-fi.com/seextended",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://hosted.weblate.org/projects/se-extended/se-extended/")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Translate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["help_translate_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["help_translate_description"],
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://github.com/bocajthomas/SE-Extended?tab=GPL-3.0-1-ov-file")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.WorkspacePremium,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["licence_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "GNU General Public Licence v3.0",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedMiddle,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    openLink("https://github.com/bocajthomas/SE-Extended")
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.Code,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["source_code_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = "https://github.com/bocajthomas/SE-Extended",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Light,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                    // Third Party Libraries
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = cardShapeGroupedBottom,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    routes.homeThirdPartyLibraries.navigate()
                                }
                                .padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.LibraryBooks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = translation["third_party_libraries_title"],
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp
                                )
                                Text(
                                    text = translation["third_party_libraries_description"],
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
    }
}