package com.deep.sideglance.ui.screens.dashboard

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.draw.scale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Send
import com.deep.sideglance.data.ServiceLocator
import com.deep.sideglance.domain.model.User
import com.deep.sideglance.domain.model.UserStatus
import com.deep.sideglance.util.LocationHelper
import com.deep.sideglance.util.NotificationHelper
import com.deep.sideglance.ui.widget.WidgetStateUpdateHelper
import com.deep.sideglance.ui.widget.StatusExpiredWorker
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    user: User,
    onNavigateToPairing: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Status repositories
    val statusRepo = ServiceLocator.statusRepository
    val partnerRepo = ServiceLocator.partnerRepository
    val authRepo = ServiceLocator.authRepository

    val sharedPrefs = remember { context.getSharedPreferences("sideglance_prefs", android.content.Context.MODE_PRIVATE) }
    var isLocationOptIn by remember {
        mutableStateOf(sharedPrefs.getBoolean("location_opt_in", true))
    }

    // State bindings
    var myStatus by remember { mutableStateOf<UserStatus?>(null) }
    var partnerUser by remember { mutableStateOf<User?>(null) }
    var partnerStatus by remember { mutableStateOf<UserStatus?>(null) }
    
    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(LocationHelper.hasLocationPermission(context))
    }
    
    // Coordinates
    var myLat by remember { mutableStateOf<Double?>(null) }
    var myLng by remember { mutableStateOf<Double?>(null) }

    // Sheets & Dialogs
    var showBottomSheet by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            if (isLocationOptIn) {
                scope.launch {
                    val loc = LocationHelper.getCurrentLocation(context)
                    if (loc != null) {
                        myLat = loc.first
                        myLng = loc.second
                        statusRepo.updateLocation(loc.first, loc.second)
                        triggerWidgetUpdate(context, partnerUser, partnerStatus, loc.first, loc.second, true)
                    }
                }
            } else {
                scope.launch {
                    statusRepo.updateLocation(null, null)
                    myLat = null
                    myLng = null
                    triggerWidgetUpdate(context, partnerUser, partnerStatus, null, null, false)
                }
            }
        } else {
            scope.launch {
                statusRepo.updateLocation(null, null)
                myLat = null
                myLng = null
                triggerWidgetUpdate(context, partnerUser, partnerStatus, null, null, false)
            }
        }
    }

    // Lifecycle observer to recheck location permissions when app resumes (e.g. from Settings)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val granted = LocationHelper.hasLocationPermission(context)
                if (granted != hasLocationPermission) {
                    hasLocationPermission = granted
                    scope.launch {
                        if (granted && isLocationOptIn) {
                            val loc = LocationHelper.getCurrentLocation(context)
                            if (loc != null) {
                                myLat = loc.first
                                myLng = loc.second
                                statusRepo.updateLocation(loc.first, loc.second)
                                triggerWidgetUpdate(context, partnerUser, partnerStatus, loc.first, loc.second, true)
                            }
                        } else {
                            statusRepo.updateLocation(null, null)
                            myLat = null
                            myLng = null
                            triggerWidgetUpdate(context, partnerUser, partnerStatus, null, null, false)
                        }
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load user own status initially
    LaunchedEffect(user.uid) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (!hasLocationPermission) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Also request notification permission if not granted on Tiramisu+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }

            if (isLocationOptIn) {
                val loc = LocationHelper.getCurrentLocation(context)
                if (loc != null) {
                    myLat = loc.first
                    myLng = loc.second
                    statusRepo.updateLocation(loc.first, loc.second)
                    triggerWidgetUpdate(context, partnerUser, partnerStatus, loc.first, loc.second, hasLocationPermission)
                }
            } else {
                myLat = null
                myLng = null
                statusRepo.updateLocation(null, null)
                triggerWidgetUpdate(context, partnerUser, partnerStatus, null, null, false)
            }
        }
    }

    // Observe partner status & user info if exists
    val partnerUid = user.partnerUid
    LaunchedEffect(partnerUid) {
        if (partnerUid != null) {
            launch {
                statusRepo.observePartnerUser(partnerUid).collect { pUser ->
                    partnerUser = pUser
                    triggerWidgetUpdate(context, pUser, partnerStatus, myLat, myLng, hasLocationPermission && isLocationOptIn)
                }
            }
            launch {
                statusRepo.observePartnerStatus(partnerUid).collect { pStatus ->
                    partnerStatus = pStatus
                    triggerWidgetUpdate(context, partnerUser, pStatus, myLat, myLng, hasLocationPermission && isLocationOptIn)
                }
            }
        } else {
            // Unpaired - reset widget state to default
            WidgetStateUpdateHelper.updateWidgetState(
                context = context,
                name = "Partner",
                statusType = "Offline",
                detail = "",
                time = 0L,
                expiry = 0L,
                distance = "infinity"
            )
        }
    }

    // Observe own status in real-time to retain it across app opens and handle expiration worker
    LaunchedEffect(user.uid) {
        statusRepo.observePartnerStatus(user.uid).collect { status ->
            myStatus = status
            
            // Handle StatusExpiredWorker scheduling
            if (status != null && !status.isExpired && status.expirationTime > 0L) {
                val delay = status.expirationTime - System.currentTimeMillis()
                if (delay > 0) {
                    val workRequest = androidx.work.OneTimeWorkRequestBuilder<StatusExpiredWorker>()
                        .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build()
                    androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                        "StatusExpiredWork",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        workRequest
                    )
                }
            } else {
                androidx.work.WorkManager.getInstance(context).cancelUniqueWork("StatusExpiredWork")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hi, ${user.displayName.split(" ").firstOrNull() ?: "there"} 👋",
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsMenu = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    DropdownMenu(
                        expanded = showSettingsMenu,
                        onDismissRequest = { showSettingsMenu = false },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .width(200.dp)
                    ) {
                        if (partnerUid != null) {
                            DropdownMenuItem(
                                text = { Text("Disconnect Partner", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Disconnect Icon",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    scope.launch {
                                        partnerRepo.unpairPartner()
                                        showSettingsMenu = false
                                    }
                                }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                        }
                        DropdownMenuItem(
                            text = { Text("Sign Out", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                    contentDescription = "Sign Out Icon",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            },
                            onClick = {
                                scope.launch {
                                    authRepo.signOut()
                                    onSignOut()
                                    showSettingsMenu = false
                                }
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (partnerUid == null) {
                // UNPAIRED STATE UI
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("👋", fontSize = 48.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Connect with your partner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Sideglance works best when linked. You will be able to share your presence, distance, and see each other's status on home screen widgets.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = onNavigateToPairing,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Link with Partner", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                // PAIRED STATE UI
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Partner status display card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = partnerUser?.displayName ?: "Partner",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Big status type representation
                            val status = partnerStatus
                            if (status != null && !status.isExpired) {
                                val (emoji, title) = splitStatusType(status.statusType)
                                if (emoji.isNotEmpty()) {
                                    Text(
                                        text = emoji,
                                        fontSize = 48.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                if (title.isNotEmpty()) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }

                                if (status.detail != null) {
                                    Text(
                                        text = status.detail,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                    )
                                }

                                // Expiration and formatting
                                val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                                val timeText = sdf.format(Date(status.timestamp))
                                val expiryText = if (status.expirationTime == 0L) {
                                    "forever"
                                } else {
                                    "until ${sdf.format(Date(status.expirationTime))}"
                                }
                                
                                Text(
                                    text = "Updated $timeText • Active $expiryText",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )

                                // Quick Reactions
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )

                                Text(
                                    text = "React to status",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    val emojis = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")
                                    emojis.forEach { emoji ->
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    statusRepo.addReactionToStatus(
                                                        partnerUid,
                                                        com.deep.sideglance.domain.model.StatusReaction(
                                                            senderUid = user.uid,
                                                            emoji = emoji
                                                        )
                                                    )
                                                }
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Text(emoji, fontSize = 20.sp)
                                        }
                                    }
                                }

                                // Messages & Reactions List
                                val messages = status.messages
                                val reactions = status.reactions
                                val combinedInteractions = remember(messages, reactions) {
                                    val items = mutableListOf<Any>()
                                    items.addAll(messages)
                                    items.addAll(reactions)
                                    items.sortBy {
                                        when (it) {
                                            is com.deep.sideglance.domain.model.StatusMessage -> it.timestamp
                                            is com.deep.sideglance.domain.model.StatusReaction -> it.timestamp
                                            else -> 0L
                                        }
                                    }
                                    items
                                }

                                if (combinedInteractions.isNotEmpty()) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                    )

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 180.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        combinedInteractions.forEach { interaction ->
                                            val isMe = when (interaction) {
                                                is com.deep.sideglance.domain.model.StatusMessage -> interaction.senderUid == user.uid
                                                is com.deep.sideglance.domain.model.StatusReaction -> interaction.senderUid == user.uid
                                                else -> false
                                            }
                                            val senderName = if (isMe) "You" else (partnerUser?.displayName?.split(" ")?.firstOrNull() ?: "Partner")

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                        Text(
                                                            text = senderName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        when (interaction) {
                                                            is com.deep.sideglance.domain.model.StatusMessage -> {
                                                                Text(
                                                                    text = interaction.text,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                                )
                                                            }
                                                            is com.deep.sideglance.domain.model.StatusReaction -> {
                                                                Text(
                                                                    text = "Reacted: ${interaction.emoji}",
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                )

                                // Chat Message Input
                                var chatInputText by remember { mutableStateOf("") }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = chatInputText,
                                        onValueChange = { chatInputText = it },
                                        placeholder = { Text("Send a message...", style = MaterialTheme.typography.bodyMedium) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (chatInputText.isNotBlank()) {
                                                val textToSend = chatInputText
                                                chatInputText = ""
                                                scope.launch {
                                                    statusRepo.addMessageToStatus(
                                                        partnerUid,
                                                        com.deep.sideglance.domain.model.StatusMessage(
                                                            senderUid = user.uid,
                                                            text = textToSend
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send Message",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "😴 Offline",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                                Text(
                                    text = "No status updated recently",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    // Distance display card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Distance between you",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val distanceStr = LocationHelper.formatDistance(
                                    myLat, myLng,
                                    partnerUser?.latitude, partnerUser?.longitude,
                                    hasLocationPermission && isLocationOptIn
                                )
                                
                                Text(
                                    text = if (distanceStr == "infinity") "infinity (∞)" else distanceStr,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (!hasLocationPermission) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            val permissionsToRequest = mutableListOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                        }
                                    ) {
                                        Text("Grant", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", context.packageName, null)
                                            ).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                    ) {
                                        Text("Settings", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isLocationOptIn) "Sharing" else "Hidden",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLocationOptIn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                    Switch(
                                        checked = isLocationOptIn,
                                        onCheckedChange = { checked ->
                                            isLocationOptIn = checked
                                            sharedPrefs.edit().putBoolean("location_opt_in", checked).apply()
                                            scope.launch {
                                                if (checked) {
                                                    val loc = LocationHelper.getCurrentLocation(context)
                                                    if (loc != null) {
                                                        myLat = loc.first
                                                        myLng = loc.second
                                                        statusRepo.updateLocation(loc.first, loc.second)
                                                        triggerWidgetUpdate(context, partnerUser, partnerStatus, loc.first, loc.second, true)
                                                    }
                                                } else {
                                                    myLat = null
                                                    myLng = null
                                                    statusRepo.updateLocation(null, null)
                                                    triggerWidgetUpdate(context, partnerUser, partnerStatus, null, null, false)
                                                }
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.scale(0.8f)
                                    )
                                }
                            }
                        }
                    }

                    // Personal own status summary card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 16.dp)
                            ) {
                                Text(
                                    text = "Your current status",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = myStatus?.let {
                                        if (it.isExpired) "Expired" else "${it.statusType}${it.detail?.let { d -> " ($d)" } ?: ""}"
                                    } ?: "No status set",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Button(
                                onClick = { showBottomSheet = true },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update")
                            }
                        }

                        val myMessages = myStatus?.messages ?: emptyList()
                        val myReactions = myStatus?.reactions ?: emptyList()
                        val myCombined = remember(myMessages, myReactions) {
                            val items = mutableListOf<Any>()
                            items.addAll(myMessages)
                            items.addAll(myReactions)
                            items.sortBy {
                                when (it) {
                                    is com.deep.sideglance.domain.model.StatusMessage -> it.timestamp
                                    is com.deep.sideglance.domain.model.StatusReaction -> it.timestamp
                                    else -> 0L
                                }
                            }
                            items
                        }

                        if (myCombined.isNotEmpty()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                            
                            Text(
                                text = "Reactions & Messages",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 120.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                myCombined.forEach { interaction ->
                                    val isMe = when (interaction) {
                                        is com.deep.sideglance.domain.model.StatusMessage -> interaction.senderUid == user.uid
                                        is com.deep.sideglance.domain.model.StatusReaction -> interaction.senderUid == user.uid
                                        else -> false
                                    }
                                    val senderName = if (isMe) "You" else (partnerUser?.displayName?.split(" ")?.firstOrNull() ?: "Partner")

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                            modifier = Modifier.padding(horizontal = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                                Text(
                                                    text = senderName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                                )
                                                Spacer(modifier = Modifier.height(1.dp))
                                                when (interaction) {
                                                    is com.deep.sideglance.domain.model.StatusMessage -> {
                                                        Text(
                                                            text = interaction.text,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                    is com.deep.sideglance.domain.model.StatusReaction -> {
                                                        Text(
                                                            text = "Reacted: ${interaction.emoji}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Status Update bottom sheet trigger
            if (showBottomSheet) {
                StatusUpdateBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    onStatusSelected = { status ->
                        myStatus = status
                        scope.launch {
                            statusRepo.updateStatus(status)
                            showBottomSheet = false
                        }
                    }
                )
            }
        }
    }
}

private suspend fun triggerWidgetUpdate(
    context: android.content.Context,
    partnerUser: User?,
    partnerStatus: UserStatus?,
    myLat: Double?,
    myLng: Double?,
    hasLocationPermission: Boolean
) {
    val distanceStr = LocationHelper.formatDistance(
        myLat, myLng,
        partnerUser?.latitude, partnerUser?.longitude,
        hasLocationPermission
    )
    WidgetStateUpdateHelper.updateWidgetState(
        context = context,
        name = partnerUser?.displayName ?: "Partner",
        statusType = partnerStatus?.statusType ?: "Offline",
        detail = if (partnerStatus != null) (partnerStatus.detail ?: "") else null,
        time = partnerStatus?.timestamp ?: 0L,
        expiry = partnerStatus?.expirationTime ?: 0L,
        distance = distanceStr
    )
}

private fun splitStatusType(statusType: String): Pair<String, String> {
    val trimmed = statusType.trim()
    if (trimmed.isEmpty()) return Pair("", "")
    val parts = trimmed.split(" ", limit = 2)
    return if (parts.size == 2) {
        Pair(parts[0], parts[1])
    } else {
        Pair("", parts[0])
    }
}

