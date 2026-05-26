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
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (partnerUid == null) {
                // UNPAIRED STATE UI
                Column(
                    modifier = Modifier.weight(1f),
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
                        .fillMaxWidth()
                        .weight(1f),
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
                                Text(
                                    text = status.statusType,
                                    fontSize = 48.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                if (status.detail != null) {
                                    Text(
                                        text = status.detail,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 12.dp)
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
                    Box(
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
                            Column {
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
