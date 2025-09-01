package com.luutran.mycookingapp.ui.drawer

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.luutran.mycookingapp.R
import com.luutran.mycookingapp.ui.auth.AuthViewModel
import com.luutran.mycookingapp.ui.editprofile.ProfileLoadState
import com.luutran.mycookingapp.ui.editprofile.ProfileViewModel

@Composable
fun AppDrawerContent(
    currentRoute: String,
    onNavigate: (DrawerItem) -> Unit,
    closeDrawer: () -> Unit,
    onEditProfileIconClick: () -> Unit,
    authViewModel: AuthViewModel,
    profileViewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val profileState by profileViewModel.profileLoadState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        Log.d("AppDrawerContent", "LaunchedEffect: Requesting profile refresh.")
        profileViewModel.refreshUserProfile()
    }

    ModalDrawerSheet(modifier = modifier.fillMaxWidth(0.8f)) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Or adjust as needed
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Display Profile Image using Coil
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (profileState is ProfileLoadState.Success) {
                                    (profileState as ProfileLoadState.Success).userProfile.photoUrl
                                } else {
                                    null
                                }
                            )
                            .crossfade(true)
                            .error(R.drawable.ic_launcher_foreground)
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .build(),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display User Name
                    Text(
                        text = if (profileState is ProfileLoadState.Success) {
                            (profileState as ProfileLoadState.Success).userProfile.displayName ?: "User"
                        } else {
                            ""
                        },
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                IconButton(
                    onClick = {
                        onEditProfileIconClick()
                        closeDrawer()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Profile",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Items
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(drawerItems) { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            onNavigate(item)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }

            // --- SIGN OUT SECTION ---
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            NavigationDrawerItem(
                icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign Out") },
                label = { Text("Sign Out") },
                selected = false,
                onClick = {
                    authViewModel.signOut()
                    closeDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            Text(
                text = "App Version 1.0.0",
                style = TextStyle(fontSize = 12.sp, color = Color.Gray),
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}


