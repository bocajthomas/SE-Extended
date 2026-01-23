@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
package me.rhunk.snapenhance.ui.manager.pages.social

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.rhunk.snapenhance.common.data.FriendStreaks
import me.rhunk.snapenhance.common.data.MessagingFriendInfo
import me.rhunk.snapenhance.common.data.MessagingGroupInfo
import me.rhunk.snapenhance.common.data.MessagingRuleType
import me.rhunk.snapenhance.common.data.SocialScope
import me.rhunk.snapenhance.common.ui.*
import me.rhunk.snapenhance.common.util.snap.BitmojiSelfie
import me.rhunk.snapenhance.storage.*
import me.rhunk.snapenhance.ui.manager.Routes
import me.rhunk.snapenhance.ui.util.BottomSheets
import me.rhunk.snapenhance.ui.util.coil.BitmojiImage
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ManageScope: Routes.Route() {
    private val bottomSheets by lazy { BottomSheets(context.translation) }

    private fun deleteScope(scope: SocialScope, id: String, coroutineScope: CoroutineScope) {
        when (scope) {
            SocialScope.FRIEND -> context.database.deleteFriend(id)
            SocialScope.GROUP -> context.database.deleteGroup(id)
        }
        context.database.executeAsync {
            coroutineScope.launch {
                routes.navController.popBackStack()
            }
        }
    }

    override val topBarActions: @Composable (RowScope.() -> Unit) = topBarActions@{
        val navBackStackEntry by routes.navController.currentBackStackEntryAsState()
        var deleteConfirmBottomSheet by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        if (deleteConfirmBottomSheet) {
            val scope =
                navBackStackEntry?.arguments?.getString("scope")?.let { SocialScope.getByName(it) }
                    ?: return@topBarActions
            val id = navBackStackEntry?.arguments?.getString("id")!!

            ModalBottomSheet(
                onDismissRequest = { deleteConfirmBottomSheet = false },
                sheetState = sheetState,
                sheetGesturesEnabled = false,
                shape = bottomSheetShape
            ) {
                LazyColumn(
                    modifier = Modifier
                        .padding(10.dp, 10.dp, 10.dp, 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = translation.format("delete_scope_confirm_dialog_title", "scope" to context.translation["scopes.${scope.key}"]),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )
                            HorizontalDivider(
                                modifier = Modifier
                                    .padding(top = 15.dp, bottom = 10.dp)
                                    .fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { deleteConfirmBottomSheet = false },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(context.translation["button.cancel"])
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        deleteScope(scope, id, coroutineScope); deleteConfirmBottomSheet = false
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(context.translation["button.ok"])
                                }
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = { deleteConfirmBottomSheet = true },
            shapes = IconButtonDefaults.shapes()
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null
            )
        }
    }

    override val content: @Composable (NavBackStackEntry) -> Unit = content@{ navBackStackEntry ->
        val scope = SocialScope.getByName(navBackStackEntry.arguments?.getString("scope")!!)
        val id = navBackStackEntry.arguments?.getString("id")!!

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxSize()
                .screenContentPadding(staticVertical = 10.dp)
        ) {
            var bottomComposable by remember {
                mutableStateOf(null as (@Composable () -> Unit)?)
            }
            var hasScope by remember {
                mutableStateOf(null as Boolean?)
            }
            when (scope) {
                SocialScope.FRIEND -> {
                    var streaks by remember { mutableStateOf(null as FriendStreaks?) }
                    val friend by rememberAsyncMutableState(null) {
                        context.database.getFriendInfo(id)?.also {
                            streaks = context.database.getFriendStreaks(id)
                        }.also {
                            hasScope = it != null
                        }
                    }
                    friend?.let {
                        Friend(id, it, streaks) { bottomComposable = it }
                    }
                }
                SocialScope.GROUP -> {
                    val group by rememberAsyncMutableState(null) {
                        context.database.getGroupInfo(id).also {
                            hasScope = it != null
                        }
                    }
                    group?.let {
                        Group(it) { bottomComposable = it }
                    }
                }
            }
            if (hasScope == true) {
                if (context.config.root.experimental.friendNotes.get()) {
                    NotesCard(id)
                }
                RulesCard(id)
            }
            bottomComposable?.invoke()
            if (hasScope == false) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = translation["not_found"],
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun NotesCard(
        id: String
    ) {
        val coroutineScope = rememberCoroutineScope { Dispatchers.IO }
        var scopeNotes by rememberAsyncMutableState(null) {
            context.database.getScopeNotes(id)
        }

        AutoClearKeyboardFocus()

        EditNoteTextField(
            modifier = Modifier.padding(8.dp),
            primaryColor = Color.White,
            translation = context.translation,
            content = scopeNotes,
            setContent = { scopeNotes = it }
        )

        DisposableEffect(Unit) {
            onDispose {
                coroutineScope.launch {
                    context.database.setScopeNotes(id, scopeNotes)
                }
            }
        }
    }

    @Composable
    private fun RulesCard(
        id: String
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val rules = rememberAsyncMutableStateList(listOf()) {
            context.database.getRules(id)
        }

        SectionTitle(translation["rules_title"])

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MessagingRuleType.entries.forEachIndexed { index, ruleType ->
                var ruleEnabled by remember(rules.size) {
                    mutableStateOf(rules.any { it.key == ruleType.key })
                }
                val ruleState = context.config.root.rules.getRuleState(ruleType)
                val cardShape = cardShape(MessagingRuleType.entries.size, index)
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth()
                        .clip(cardShape)
                        .clickable {
                            if (ruleType.listMode && ruleState == null) return@clickable
                            val newCheckedState = !ruleEnabled
                            context.database.setRule(id, ruleType.key, newCheckedState)
                            ruleEnabled = newCheckedState
                        }
                        .then(
                            if (ruleType.listMode && ruleState == null) {
                                Modifier.graphicsLayer(alpha = 0.5f)
                            } else Modifier
                        ),
                    shape = cardShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = if (ruleType.listMode && ruleState != null) {
                                context.translation["rules.properties.${ruleType.key}.options.${ruleState.key}"]
                            } else context.translation["rules.properties.${ruleType.key}.name"],
                            modifier = Modifier
                                .weight(1f)
                        )
                        Switch(
                            checked = ruleEnabled,
                            enabled = if (ruleType.listMode) ruleState != null else true,
                            onCheckedChange = {
                                context.database.setRule(id, ruleType.key, it)
                                ruleEnabled = it
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ContentCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth(),
            shape = cardShapeSingle
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth()
                    .then(modifier)
            ) {
                content()
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            maxLines = 1,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .offset(x = 20.dp)
                .padding(bottom = 10.dp)
        )
    }

    private fun computeStreakETA(timestamp: Long): String? {
        val now = System.currentTimeMillis()
        val stringBuilder = StringBuilder()
        val diff = timestamp - now
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        if (days > 0) {
            stringBuilder.append("$days day ")
            return stringBuilder.toString()
        }
        if (hours > 0) {
            stringBuilder.append("$hours hours ")
            return stringBuilder.toString()
        }
        if (minutes > 0) {
            stringBuilder.append("$minutes minutes ")
            return stringBuilder.toString()
        }
        if (seconds > 0) {
            stringBuilder.append("$seconds seconds ")
            return stringBuilder.toString()
        }
        return null
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Composable
    private fun Friend(
        id: String,
        friend: MessagingFriendInfo,
        streaks: FriendStreaks?,
        setBottomComposable: ((@Composable () -> Unit)?) -> Unit = {}
    ) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(Unit) {
            setBottomComposable {
                Spacer(modifier = Modifier.height(16.dp))

                if (context.config.root.experimental.e2eEncryption.globalState == true) {
                    SectionTitle(translation["e2ee_title"])
                    var hasSecretKey by rememberAsyncMutableState(defaultValue = false) {
                        context.e2eeImplementation.friendKeyExists(friend.userId)
                    }
                    var importBottomSheet by remember { mutableStateOf(false) }

                    if (importBottomSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { importBottomSheet = false },
                            sheetState = sheetState,
                            sheetGesturesEnabled = false,
                            shape = bottomSheetShape
                        ) {
                            bottomSheets.RawInputBottomSheet(
                                onDismiss = { importBottomSheet = false  }, onConfirm = { newKey ->
                                    importBottomSheet = false
                                runCatching {
                                    val key = Base64.decode(newKey)
                                    if (key.size != 32) {
                                        context.longToast("Invalid key size (must be 32 bytes)")
                                        return@runCatching
                                    }

                                    context.coroutineScope.launch {
                                        context.e2eeImplementation.storeSharedSecretKey(friend.userId, key)
                                        context.longToast("Successfully imported key")
                                    }

                                    hasSecretKey = true
                                }.onFailure {
                                    context.longToast("Failed to import key: ${it.message}")
                                    context.log.error("Failed to import key", it)
                                }
                            })
                        }
                    }

                    ContentCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hasSecretKey) {
                                OutlinedButton(
                                    onClick = {
                                        context.coroutineScope.launch {
                                            val secretKey = Base64.encode(context.e2eeImplementation.getSharedSecretKey(friend.userId) ?: return@launch)
                                            //TODO: fingerprint auth
                                            context.activity!!.startActivity(Intent.createChooser(Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, secretKey)
                                                type = "text/plain" },"").apply {
                                                    putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(
                                                        Intent().apply {
                                                            putExtra(Intent.EXTRA_TEXT, secretKey)
                                                            putExtra(Intent.EXTRA_SUBJECT, secretKey)
                                                        })
                                                    )
                                                }
                                            )
                                        }
                                    },
                                    shapes = ButtonDefaults.shapes()
                                ) {
                                    Text(
                                        text = "Export Base64",
                                        maxLines = 1
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { importBottomSheet = true },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(
                                    text = "Import Base64",
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .padding(5.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val bitmojiUrl = BitmojiSelfie.getBitmojiSelfie(
                friend.selfieId, friend.bitmojiId, BitmojiSelfie.BitmojiSelfieType.NEW_THREE_D
            )
            BitmojiImage(context = context, url = bitmojiUrl, size = 120)
            Text(
                text = friend.displayName ?: friend.mutableUsername,
                maxLines = 1,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = friend.mutableUsername,
                maxLines = 1,
                fontSize = 12.sp,
                fontWeight = FontWeight.Light
            )
        }

        if (context.config.root.experimental.storyLogger.get()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            ) {
                Button(
                    onClick = {
                        routes.loggedStories.navigate {
                            put("id", id)
                        }
                    },
                    shapes = ButtonDefaults.shapes()
                ) {
                    Text(translation["logged_stories_button"])
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // streaks
        streaks?.let {
            var shouldNotify by remember { mutableStateOf(it.notify) }
            SectionTitle(translation["streaks_title"])

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ){
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeGroupedTop
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(all = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = translation.format(
                                "streaks_length_text", "length" to streaks.length.toString()
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = computeStreakETA(streaks.expirationTimestamp)?.let {
                                translation.format(
                                    "streaks_expiration_text", "eta" to it
                                )
                            } ?: translation["streaks_expiration_text_expired"],
                            maxLines = 1
                        )
                    }
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cardShapeGroupedBottom
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(15.dp, 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = translation["reminder_button"],
                            maxLines = 1,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Switch(
                            checked = shouldNotify,
                            onCheckedChange = {
                                context.database.setFriendStreaksNotify(id, it)
                                shouldNotify = it
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun Group(
        group: MessagingGroupInfo,
        setBottomComposable: ((@Composable () -> Unit)?) -> Unit = {}
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = group.name, maxLines = 1, fontSize = 20.sp, fontWeight = FontWeight.Bold
            )
            Text(
                text = translation.format(
                    "participants_text", "count" to group.participantsCount.toString()
                ), maxLines = 1, fontSize = 12.sp, fontWeight = FontWeight.Light
            )
        }
    }
}