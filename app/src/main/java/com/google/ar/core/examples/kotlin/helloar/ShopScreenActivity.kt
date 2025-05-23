package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalContext
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import com.google.ar.core.examples.kotlin.helloar.data.Skin
import com.google.ar.core.examples.kotlin.helloar.data.UserSkins
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import kotlinx.coroutines.launch
import com.google.ar.core.examples.kotlin.helloar.data.ApiService
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory


@Composable
fun ShopScreen(
    onMenu: () -> Unit = {} // Callback pour le bouton retour/menu
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val uuid = prefs.uuid
    val font = DisketFont

    // Récupération correcte des préférences de skins équipés
    val arrowPref = prefs.arrowUsed
    val moonPref = prefs.moonUsed
    val planetPref = prefs.planetUsed

    var money by remember { mutableStateOf<Int?>(0) }
    var bestScore by remember { mutableStateOf<Int?>(0) }

    //état pour afficher un indicateur de chargement d'argent
    var isLoadingMoney by remember { mutableStateOf(false) }

    // Variables d'état pour les items et les prix par catégorie
    val itemsByCategory = remember { mutableStateOf<List<List<Skin>>>(emptyList()) }
    val pricesByCategory = remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var selectedCategory by remember { mutableIntStateOf(0) } // Catégorie sélectionnée

    // Remplacer la variable selectedItem unique par un tableau d'indices par catégorie
    var selectedItems by remember { mutableStateOf(listOf(-1, -1, -1)) }

    // État pour l'item équipé/sélectionné dans chaque catégorie
    var equippedItems by remember {
        mutableStateOf(
            listOf(
                arrowPref?.toIntOrNull()?.takeIf { it >= 0 } ?: 0,   // flèche
                moonPref?.toIntOrNull()?.takeIf { it >= 0 } ?: 0,    // lune
                planetPref?.toIntOrNull()?.takeIf { it >= 0 } ?: 0   // planète
            )
        )
    }

    var itemIsEquipped by remember {
        mutableStateOf(
            listOf(
                arrowPref != null && arrowPref.isNotEmpty(),
                moonPref != null && moonPref.isNotEmpty(),
                planetPref != null && planetPref.isNotEmpty()
            )
        )
    }

    // Variables d'état pour l'API
    val allSkins = remember { mutableStateOf<List<Skin>>(emptyList()) }
    val userSkins = remember { mutableStateOf<List<UserSkins>>(emptyList()) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()


    // Initialisation de l'API
    val api = remember {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl(ApiService.BASE_URL)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(
                MoshiConverterFactory.create(moshi).asLenient()
            )
            .build()
            .create(ApiService::class.java)
    }

    //récupération des informations non stocké dans les préférences
    fun getUserInfo() {
        Log.e("moi", "getUserInfo enter")
        isLoadingMoney = true

        coroutineScope.launch {
            //recuperation de l'argent
            try {
                val responseMoney = api.getMoney(uuid)
                if (responseMoney.isSuccessful) {
                    val userData = responseMoney.body()
                    // Validation et assignation sécurisée
                    money = userData?.money.takeIf { it!! >= 0 } ?: 0
                } else {
                    money = money ?: 0  // Conserver la valeur ou utiliser 0
                    errorMessage.value = "Erreur serveur: ${responseMoney.code()}"
                }
            } catch (e: Exception) {
                money = money ?: 0
                errorMessage.value = "Erreur réseau: ${e.message}"
            } finally {
                isLoadingMoney = false
            }
            //recuperation du meilleur score
            try {
                Log.e("moi", "getUserInfo score enter")
                val responseScore = api.getBestScores(uuid)
                Log.e("moi", "data : ${responseScore}")

                if (responseScore.isSuccessful) {
                    val userData = responseScore.body()
                    bestScore = userData?.bestScore.takeIf { it!! >= 0 } ?: 0
                    Log.e("moi", "Score mis à jour: $bestScore")
                } else {
                    bestScore = bestScore ?: 0
                    Log.e("moi", "getUserScore echoue")
                    errorMessage.value = "Erreur serveur: ${responseScore.code()}"
                    Log.e("moi", "Erreur serveur: ${responseScore.code()}")
                }
            } catch (e: Exception) {
                bestScore = bestScore ?: 0
                errorMessage.value = "Erreur réseau: ${e.message}"
                Log.e("moi", "Erreur réseau: ${e.message}")
            }

        }
    }


    fun getSkin() {
        coroutineScope.launch {
            val skinsResponse = api.getSkins()
            if (skinsResponse.isSuccessful) {
                try {
                    //Protection contre les réponses null
                    val body = skinsResponse.body()
                    if (body != null) {
                        allSkins.value = body
                    } else {
                        allSkins.value = emptyList()
                        errorMessage.value = "Réponse de l'API vide"
                    }

                    // Vérifier si les skins sont valides avant de les traiter
                    if (allSkins.value.isNotEmpty()) {
                        try {
                            // Organiser les skins par catégorie (basée sur type)
                            val arrow = allSkins.value.filter { it.type == 0 }
                            val moon = allSkins.value.filter { it.type == 1 }
                            val planet = allSkins.value.filter { it.type == 2 }

                            itemsByCategory.value = listOf(arrow, moon, planet)

                            pricesByCategory.value = listOf(
                                arrow.map { it.price },
                                moon.map { it.price },
                                planet.map { it.price }
                            )
                        } catch (e: Exception) {
                            // Initialiser avec des listes vides en cas d'erreur
                            itemsByCategory.value = listOf(emptyList(), emptyList(), emptyList())
                            pricesByCategory.value = listOf(emptyList(), emptyList(), emptyList())
                        }
                    }
                } catch (e: Exception) {
                    // Gérer les erreurs de parsing JSON
                    errorMessage.value = "Erreur de traitement des données: ${e.message}"
                    itemsByCategory.value = listOf(emptyList(), emptyList(), emptyList())
                    pricesByCategory.value = listOf(emptyList(), emptyList(), emptyList())
                }
            } else {
                errorMessage.value = "Erreur de chargement des skins: ${skinsResponse.code()}"
            }
        }

    }

    fun getUserSkin() {
        coroutineScope.launch {
            if (prefs.uuid != null) {
                try {
                    val userSkinsResponse = api.getUserSkins(prefs.uuid)
                    if (userSkinsResponse.isSuccessful) {
                        userSkins.value = userSkinsResponse.body() ?: emptyList()
                    } else {
                        errorMessage.value =
                            "Erreur de chargement des skins utilisateur: ${userSkinsResponse.code()}"
                        userSkins.value = emptyList()
                    }
                } catch (e: Exception) {
                    errorMessage.value =
                        "Erreur lors de la récupération des skins utilisateur: ${e.message}"
                    userSkins.value = emptyList()
                }
            }
        }
    }


    // Fonction pour sauvegarder les préférences d'items équipés
    fun saveEquippedItem(categoryIndex: Int, itemIndex: Int) {

        when (categoryIndex) {
            0 -> {
                prefs.arrowUsed = itemIndex.toString()
            }

            1 -> {
                prefs.moonUsed = itemIndex.toString()
            }

            2 -> {
                prefs.planetUsed = itemIndex.toString()
            }
        }
    }

    //État pour le dialogue d'achat
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedSkinId by remember { mutableStateOf<Int?>(null) }
    var selectedSkinPrice by remember { mutableStateOf(0) }

    // Fonction pour vérifier si un index d'item est valide
    fun isItemValid(categoryIndex: Int, itemIndex: Int): Boolean {
        if (categoryIndex < 0 || categoryIndex >= 3) return false
        if (itemIndex < 0) return false
        return true
    }

    // Fonction pour l'achat d'un skin
    fun buySkin(skinId: Int) {
        val uuid = prefs.uuid

        // Afficher un indicateur de chargement
        isLoading.value = true

        coroutineScope.launch {
            try {
                // Appeler l'API et ATTENDRE sa réponse
                val response = api.buySkin(uuid, skinId)

                // Vérifier si l'API a bien répondu
                if (response.isSuccessful) {

                    AudioManager.playSfx("purchase")
                    val currentSelectedItem = selectedItems[selectedCategory]

                    // Équiper l'item si nécessaire (avec vérification)
                    val categoryEquipped = if (selectedCategory in itemIsEquipped.indices)
                        itemIsEquipped[selectedCategory] else false

                    if (!categoryEquipped && isItemValid(selectedCategory, currentSelectedItem)) {
                        val newEquippedItems = equippedItems.toMutableList()
                        val newItemIsEquipped = itemIsEquipped.toMutableList()

                        // Vérification des indices avant modification
                        if (selectedCategory < newEquippedItems.size) {
                            newEquippedItems[selectedCategory] = currentSelectedItem
                        }

                        if (selectedCategory < newItemIsEquipped.size) {
                            newItemIsEquipped[selectedCategory] = true
                        }

                        equippedItems = newEquippedItems
                        itemIsEquipped = newItemIsEquipped

                        // Sauvegarde locale de l'item équipé
                        saveEquippedItem(selectedCategory, currentSelectedItem)

                    }
                    getUserSkin()//update de la boutique
                    getUserInfo()//update de l'argent
                } else {
                    // Gérer l'échec de l'API
                    errorMessage.value = "Échec de l'achat: ${response.code()}"
                    AudioManager.playSfx("error")
                }
            } catch (e: Exception) {
                // Gérer les erreurs de connexion
                errorMessage.value = "Erreur de connexion: ${e.message}"
                AudioManager.playSfx("error")
            } finally {
                isLoading.value = false
                // Fermer le dialogue
                showPurchaseDialog = false
            }
        }
    }

    // Fonction pour équiper un item
    fun equipItem(categoryIndex: Int, itemIndex: Int) {
        // Vérification des index
        if (isItemValid(categoryIndex, itemIndex)) {
            // Mettre à jour l'item équipé pour la catégorie
            val newEquippedItems = equippedItems.toMutableList()
            val newItemIsEquipped = itemIsEquipped.toMutableList()

            if (categoryIndex < newEquippedItems.size) {
                newEquippedItems[categoryIndex] = itemIndex
            }

            if (categoryIndex < newItemIsEquipped.size) {
                newItemIsEquipped[categoryIndex] = true
            }

            equippedItems = newEquippedItems
            itemIsEquipped = newItemIsEquipped

            // Sauvegarder l'item équipé dans les préférences utilisateur
            saveEquippedItem(categoryIndex, itemIndex)


        }
    }


    @Composable
    fun OneSkin(
        skin: Skin,
        isSelected: Boolean,
        isPurchased: Boolean,
        isEquipped: Boolean,
        itemIndex: Int,
        onClick: (Int) -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .clickable {
                    onClick(itemIndex)
                    AudioManager.playSfx("tap")

                    // Si l'item n'est pas encore acheté, montrer le dialogue d'achat
                    if (!isPurchased) {
                        selectedSkinId = skin.id
                        selectedSkinPrice = skin.price
                        showPurchaseDialog = true
                    }
                },
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isSelected -> Color(0xFF4A8CFF)
                    isPurchased -> Color(0xFF2A3A5F)
                    else -> Color(0xFF1C2B4F)
                }
            ),
            shape = RoundedCornerShape(16.dp),
            border = if (isEquipped)
                BorderStroke(2.dp, Color(0xFF90CAF9))
            else
                BorderStroke(1.dp, Color(0xFF4D76CF).copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {

            // Informations de l'item
            Column(
                modifier = Modifier
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                //todo : remplacer cette box par l'image du skin
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF2A3A5F), CircleShape),
                    contentAlignment = Alignment.Center

                ) {
                    Text(
                        text = "${skin.id}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = font
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Afficher le prix ou le statut (acheté/équipé)
                if (!isPurchased) {
                    Text(
                        text = "Prix: ${skin.price} 💰",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = font
                    )
                } else {
                    Text(
                        text = if (isEquipped) "Équipé" else "Acheté",
                        color = if (isEquipped) Color(0xFF90CAF9) else Color(0xFF81C784),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = font
                    )

                    // Bouton pour équiper l'item acheté
                    Button(
                        onClick = {
                            equipItem(selectedCategory, itemIndex)
                            AudioManager.playSfx("equip")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEquipped) Color(0xFF4A8CFF) else Color(0xFF1C2B4F)
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = !isEquipped, // Désactiver si déjà équipé
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "SÉLECTIONNER",
                            fontSize = 10.sp,
                            fontFamily = font,
                            color = Color.White
                        )
                    }
                }

                // Score minimum requis
                Text(
                    text = "Score min: ${skin.minimalScore}",
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontFamily = font
                )
            }
        }
    }

    //Affichage de la liste des skins par pairs
    @Composable
    fun ListSkins(skins: List<Skin>, userSkins: MutableState<List<UserSkins>>, selectedItem: Int) {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(skins.size / 2 + skins.size % 2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Premier élément de la paire
                    val firstIndex = rowIndex * 2
                    val isPurchased = userSkins.value.any { it.skinId == skins[firstIndex].id }
                    val isSelected = selectedItem == firstIndex

                    Box(modifier = Modifier.weight(1f)) {
                        OneSkin(
                            skin = skins[firstIndex],
                            itemIndex = firstIndex,
                            isSelected = isSelected,
                            isPurchased = isPurchased,
                            isEquipped = equippedItems[selectedCategory] == firstIndex &&
                                    itemIsEquipped[selectedCategory],
                            onClick = { index ->
                                // Mise à jour du tableau selectedItems au lieu de la variable unique
                                val newSelectedItems = selectedItems.toMutableList()
                                newSelectedItems[selectedCategory] = index
                                selectedItems = newSelectedItems
                            }
                        )
                    }

                    // Deuxième élément de la paire
                    val secondIndex = firstIndex + 1
                    if (secondIndex < skins.size) {
                        val isPurchased2 =
                            userSkins.value.any { it.skinId == skins[secondIndex].id }
                        val isSelected2 = selectedItem == secondIndex

                        Box(modifier = Modifier.weight(1f)) {
                            OneSkin(
                                skin = skins[secondIndex],
                                itemIndex = secondIndex,
                                isSelected = isSelected2,
                                isPurchased = isPurchased2,
                                isEquipped = equippedItems[selectedCategory] == secondIndex &&
                                        itemIsEquipped[selectedCategory],
                                onClick = { index ->
                                    // Mise à jour du tableau selectedItems au lieu de la variable unique
                                    val newSelectedItems = selectedItems.toMutableList()
                                    newSelectedItems[selectedCategory] = index
                                    selectedItems = newSelectedItems
                                }
                            )
                        }
                    } else if (skins.size % 2 != 0) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    @Composable
    fun ListSkinPerType(
        skins: List<Skin>,
        type: Int,
        userSkins: MutableState<List<UserSkins>>,
        selectedItem: Int
    ) {
        var newListSkin = skins.filter { it.type == type }
        ListSkins(newListSkin, userSkins, selectedItem)

    }

    //Dialogue de confirmation d'achat
    @Composable
    fun PurchaseDialog() {
        if (showPurchaseDialog && selectedSkinId != null) {
            // Find the selected skin to get its minimal score requirement
            val selectedSkin = allSkins.value.find { it.id == selectedSkinId }
            val minimalScoreRequired = selectedSkin?.minimalScore ?: 0
            val hasEnoughMoney = (money ?: 0) >= selectedSkinPrice
            val hasEnoughScore = (bestScore ?: 0) >= minimalScoreRequired
            val canPurchase = hasEnoughMoney && hasEnoughScore

            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showPurchaseDialog = false
                },
                title = {
                    Text(
                        "Confirmer l'achat",
                        color = Color.White,
                        fontFamily = font,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            "Voulez-vous acheter cet item pour $selectedSkinPrice 💰?",
                            color = Color.White,
                            fontFamily = font
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (!hasEnoughMoney) {
                            Text(
                                "Solde insuffisant!",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontFamily = font
                            )
                        }
                        
                        if (!hasEnoughScore) {
                            Text(
                                "Score minimal requis: $minimalScoreRequired\nVotre meilleur score: ${bestScore ?: 0}",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                fontFamily = font
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedSkinId?.let { buySkin(it) }
                        },
                        enabled = canPurchase,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A8CFF),
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acheter", fontFamily = font)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showPurchaseDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C2B4F)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Annuler", fontFamily = font)
                    }
                },
                containerColor = Color(0xFF192542),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1A36))
    ) {
        // Add parallax background
        ParallaxBackground()

        // Content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with home button and title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home button
                Image(
                    painter = painterResource(id = R.drawable.ui_home),
                    contentDescription = "Menu Button",
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { onMenu() }
                )

                // Title
                Box(
                    modifier = Modifier
                        .weight(3f)
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Background title image
                    Image(
                        painter = painterResource(id = R.drawable.page_title),
                        contentDescription = "Shop Title Background",
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(y = (13.dp))
                    )

                    // Title text
                    Text(
                        text = "SHOP",
                        style = TextStyle(
                            fontFamily = font,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f)
                    )
                }
            }

            // Main content container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1E3B70))
                    .border(
                        width = 2.dp,
                        color = Color(0xFF4D76CF),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Money display card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF142C57))
                            .border(
                                width = 1.dp,
                                color = Color(0xFFFFB74D).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            //Afficher un indicateur de chargement ou le solde
                            if (isLoadingMoney) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chargement du solde...",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontFamily = font
                                    )
                                }
                            } else {
                                Column {
                                    Text(
                                        text = "SOLDE",
                                        fontFamily = font,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = "${money} 💰",
                                    fontFamily = font,
                                    fontSize = 20.sp,
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // Category selection buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            "FLÈCHES",
                            "LUNES",
                            "PLANÈTES"
                        ).forEachIndexed { index, text ->
                            Button(
                                onClick = {
                                    selectedCategory = index
                                    AudioManager.playSfx("tap")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedCategory == index)
                                        Color(0xFF4A8CFF) else Color(0xFF142C57)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selectedCategory == index)
                                        Color(0xFF90CAF9) else Color(0xFF4D76CF).copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = text,
                                    color = Color.White,
                                    fontFamily = font,
                                    fontSize = 14.sp,
                                    maxLines = 1
                                )
                            }

                            if (index < 2) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    // Skins grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF142C57))
                            .border(
                                width = 1.dp,
                                color = Color(0xFF4D76CF).copy(alpha = 0.5f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp)
                    ) {
                        ListSkinPerType(
                            allSkins.value,
                            selectedCategory,
                            userSkins,
                            selectedItems[selectedCategory]
                        )
                    }
                }
            }
        }

        // Afficher le dialogue d'achat si nécessaire
        PurchaseDialog()
    }


    // Chargement des données au démarrage
    LaunchedEffect(Unit) {
        isLoading.value = true
        try {
            // Récupérer d'abord les données de l'utilisateur
            val userJob = launch { getUserInfo() }

            // Puis charger les autres données
            val skinjob = launch { getSkin() }
            val userSkinJob = launch { getUserSkin() }
            skinjob.join()
            userSkinJob.join()
            userJob.join()
            Log.e("moi", "tout est finis")


            // Vérification cruciale: s'assurer que les données sont valides
            if (itemsByCategory.value.isEmpty() || itemsByCategory.value.any { it.isEmpty() }) {
                // Réinitialiser avec des listes vides mais correctement structurées
                itemsByCategory.value = listOf(listOf(), listOf(), listOf())
                pricesByCategory.value = listOf(listOf(), listOf(), listOf())
            }

        } catch (e: Exception) {
            errorMessage.value = "Erreur de connexion: ${e.message}"
            // Initialisation sécurisée en cas d'erreur
            itemsByCategory.value = listOf(listOf(), listOf(), listOf())
            pricesByCategory.value = listOf(listOf(), listOf(), listOf())
        } finally {
            isLoading.value = false
        }
    }

}


class ShopScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OrbitVectorARTheme {
                ShopScreen(
                    onMenu = {
                        AudioManager.playSfx("tap") // Joue un son lors du retour
                        startActivity(
                            Intent(
                                this,
                                MenuScreenActivity::class.java
                            )
                        ) // Retour au menu
                        finish()
                    }
                )
            }
        }
    }
}
