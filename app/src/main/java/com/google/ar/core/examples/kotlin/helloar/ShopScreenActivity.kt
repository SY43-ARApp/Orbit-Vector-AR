package com.google.ar.core.examples.kotlin.helloar

import android.content.Intent
import android.os.Bundle
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
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateOf
import com.google.ar.core.examples.kotlin.helloar.data.Skin
import com.google.ar.core.examples.kotlin.helloar.data.UserSkins
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.tooling.preview.Preview
import com.google.ar.core.examples.kotlin.helloar.data.ApiService
import com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import kotlin.collections.set


@Composable
fun ShopScreen(
    onMenu: () -> Unit = {} // Callback pour le bouton retour/menu
) {
    val context = LocalContext.current
    val font = DisketFont // Police personnalisée
    val prefs = remember { UserPreferences(context) } // Préférences utilisateur
    val uuid = prefs.uuid // Identifiant utilisateur
    
    // TODO: Récupération correcte des préférences de skins équipés
    val arrowPref = prefs.arrowUsed
    val moonPref = prefs.moonUsed
    val planetPref = prefs.planetUsed
    
    // État pour l'argent
    var money by remember { mutableStateOf<Int?>(1000) }
    var bestScore by remember { mutableStateOf<Int?>(0) }

    // Variables d'état pour les items et les prix par catégorie
    val itemsByCategory = remember { mutableStateOf<List<List<Skin>>>(emptyList()) }
    val pricesByCategory = remember { mutableStateOf<List<List<Int>>>(emptyList()) }
    var selectedCategory by remember { mutableIntStateOf(0) } // Catégorie sélectionnée
    
    // Remplacer la variable selectedItem unique par un tableau d'indices par catégorie
    var selectedItems by remember { mutableStateOf(listOf(-1, -1, -1)) }


    var purchasedItems by remember { mutableStateOf(mutableListOf<String>()) }

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

    // TODO: Initialisation correcte de itemIsEquipped basée sur les préférences
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

    fun getUser() {
        //charger l'argent depuis l'api
        coroutineScope.launch {
            try {
                val response = api.getUser(uuid)
                if (response.isSuccessful) {
                    money = response.body()?.money
                    bestScore = response.body()?.bestScore
                } else {
                    errorMessage.value = "Erreur de chargement de l'argent: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage.value = "Erreur de connexion: ${e.message}"
            }
        }
    }

    fun saveMoney() {
        //envoyer la nouvelle valeur d'argent à l'api
        coroutineScope.launch {
            try {
                api.updateMoney(
                    uuid = uuid,
                    money = money
                )
            } catch (_: Exception) {
            }
        }
    }


    // Fonction pour sauvegarder les préférences d'items équipés
    fun saveEquippedItem(categoryIndex: Int, itemIndex: Int) {
        // TODO: Afficher des logs pour le débogage des préférences
        println("Saving equipped item: Category $categoryIndex, Item $itemIndex")
        
        when (categoryIndex) {
            0 -> {
                prefs.arrowUsed = itemIndex.toString()
                println("Arrow preference saved: ${prefs.arrowUsed}")
            }
            1 -> {
                prefs.moonUsed = itemIndex.toString()
                println("Moon preference saved: ${prefs.moonUsed}")
            }
            2 -> {
                prefs.planetUsed = itemIndex.toString()
                println("Planet preference saved: ${prefs.planetUsed}")
            }
        }
    }

    // TODO: État pour le dialogue d'achat
    var showPurchaseDialog by remember { mutableStateOf(false) }
    var selectedSkinId by remember { mutableStateOf<Int?>(null) }
    var selectedSkinPrice by remember { mutableStateOf(0) }

    // Fonction pour vérifier si un index d'item est valide
    fun isItemValid(categoryIndex: Int, itemIndex: Int): Boolean {
        if (categoryIndex < 0 || categoryIndex >= 3) return false
        if (itemIndex < 0) return false
        return true
    }

    // Fonction pour l'achat d'un skin (version corrigée)
    fun buySkin(skinId: Int) {
        val uuid = prefs.uuid
        val skinPrice = allSkins.value.find { it.id == skinId }?.price ?: 0

        // Vérifier si l'utilisateur a assez d'argent
        money?.let {
            if (it < skinPrice) {
                AudioManager.playSfx("error")
                return
            }
        }

        // Afficher un indicateur de chargement
        isLoading.value = true

        coroutineScope.launch {
            try {
                // Appeler l'API et ATTENDRE sa réponse
                val response = api.sendUserSkins(uuid, skinId)

                // Vérifier si l'API a bien répondu
                if (response.isSuccessful) {
                    // Mettre à jour les skins possédés par l'utilisateur
                    val updatedUserSkins = userSkins.value.toMutableList()
                    if (!userSkins.value.any { it.skinId == skinId }) {
                        updatedUserSkins.add(UserSkins(skinId))
                        userSkins.value = updatedUserSkins
                    }

                    money = money?.minus(skinPrice)
                    saveMoney()

                    AudioManager.playSfx("purchase")
                    
                    // Utiliser selectedItems[selectedCategory] au lieu de selectedItem
                    val currentSelectedItem = selectedItems[selectedCategory]
                    
                    // TODO: Correction de la méthode getOrDefault qui n'existe pas pour les listes
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
                // Toujours désactiver l'indicateur de chargement
                isLoading.value = false
                // Fermer le dialogue
                showPurchaseDialog = false
            }
        }
    }

    // Fonction pour équiper un item
    fun equipItem(categoryIndex: Int, itemIndex: Int, skinId: Int) {
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
            
            // TODO: Vérification de la persistance
            when (categoryIndex) {
                0 -> println("Après sauvegarde, Arrow preference: ${prefs.arrowUsed}")
                1 -> println("Après sauvegarde, Moon preference: ${prefs.moonUsed}")
                2 -> println("Après sauvegarde, Planet preference: ${prefs.planetUsed}")
            }

            // Mise à jour sur le serveur si nécessaire
            coroutineScope.launch {
                try {
                    // Vous pouvez implémenter ici un appel API pour sauvegarder
                    // l'équipement sur le serveur si nécessaire
                    // api.updateEquippedItem(uuid, categoryIndex, skinId)
                } catch (e: Exception) {
                    errorMessage.value = "Erreur lors de la mise à jour: ${e.message}"
                }
            }
        }
    }
    
    
    @Composable
    fun OneSkin(
        skin: Skin,
        isSelected: Boolean,
        isPurchased: Boolean,
        isEquipped: Boolean,
        itemIndex :Int,
        onClick : (Int) -> Unit
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
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
            border = if (isEquipped) BorderStroke(2.dp, Color.Yellow) else null
        ) {

            // Informations de l'item
            Column(
                modifier = Modifier
                    .padding(16.dp),
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
                        text = "${skin.id}, ${skin.type}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Afficher le prix ou le statut (acheté/équipé)
                if (!isPurchased) {
                    Text(
                        text = "Prix: ${skin.price} 💰",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = if (isEquipped) "Équipé" else "Acheté",
                        color = if (isEquipped) Color.Yellow else Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Bouton pour équiper l'item acheté
                    Button(
                        onClick = {
                            equipItem(selectedCategory, itemIndex, skin.id)
                            AudioManager.playSfx("equip")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEquipped) Color(0xFF4A8CFF) else Color(0xFF1C2B4F)
                        ),
                        modifier = Modifier.padding(top = 8.dp),
                        enabled = !isEquipped // Désactiver si déjà équipé
                    ) {
                        Text(
                            text = "SÉLECTIONNER",
                            fontSize = 10.sp, // Taille de texte réduite
                            color = Color.White
                        )
                    }
                }

                // Score minimum requis
                Text(
                    text = "Score minimum: ${skin.minimalScore}",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }


    }

    @Composable
    fun ListSkins(skins: List<Skin>, userSkins: MutableState<List<UserSkins>>, selectedItem: Int) {
        LazyColumn(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(skins.size / 2 + skins.size % 2) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                        val isPurchased2 = userSkins.value.any { it.skinId == skins[secondIndex].id }
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
    fun ListSkinPerType(skins: List<Skin>, type: Int, userSkins: MutableState<List<UserSkins>>,selectedItem :Int ) {
        var newListSkin = skins.filter { it.type == type }
        ListSkins(newListSkin, userSkins, selectedItem)

    }

    // TODO: Dialogue de confirmation d'achat
    @Composable
    fun PurchaseDialog() {
        if (showPurchaseDialog && selectedSkinId != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { 
                    showPurchaseDialog = false 
                },
                title = { 
                    Text("Confirmer l'achat", color = Color.White) 
                },
                text = { 
                    Column {
                        Text(
                            "Voulez-vous acheter cet item pour $selectedSkinPrice 💰?",
                            color = Color.White
                        )
                        
                        if ((money ?: 0) < selectedSkinPrice) {
                            Text(
                                "Solde insuffisant!",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedSkinId?.let { buySkin(it) }
                        },
                        enabled = (money ?: 0) >= selectedSkinPrice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A8CFF),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text("Acheter")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showPurchaseDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1C2B4F)
                        )
                    ) {
                        Text("Annuler")
                    }
                },
                containerColor = Color(0xFF192542)
            )
        }
    }

    Column {
        //Carte de l'argent
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF192542)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solde: ${money} 💰",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF2A3A5F)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(
                    "Flèches",
                    "Lunes",
                    "Planètes"
                ).forEachIndexed { index, text ->
                    Button(
                        onClick = {
                            selectedCategory = index
                            // Ne pas réinitialiser la sélection quand on change de catégorie
                            AudioManager.playSfx("tap")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedCategory == index)
                                Color(0xFF4A8CFF) else Color(0xFF1C2B4F)
                        )
                    ) {
                        Text(text = text, color = Color.White)
                    }
                }
            }
        }
        ListSkinPerType(allSkins.value, selectedCategory, userSkins,selectedItems[selectedCategory])

        // Afficher le dialogue d'achat si nécessaire
        PurchaseDialog()
    }

    fun getSkin() {
        coroutineScope.launch {
            val skinsResponse = api.getSkins()
            if (skinsResponse.isSuccessful) {
                try {
                    // TODO: Protection contre les réponses null
                    val body = skinsResponse.body()
                    if (body != null) {
                        allSkins.value = body
                    } else {
                        allSkins.value = emptyList()
                        errorMessage.value = "Réponse de l'API vide"
                    }

                    // Vérifier si les skins sont valides avant de les traiter
                    if (allSkins.value.isNotEmpty()) {
                        // TODO: Utilisation de try-catch pour sécuriser le filtrage des items
                        try {
                            // Organiser les skins par catégorie (basée sur type)
                            val arrow = allSkins.value.filter { it.type == 0 }
                            val moon = allSkins.value.filter { it.type == 1 }
                            val planet = allSkins.value.filter { it.type == 2 }

                            // S'assurer que chaque catégorie a au moins une liste vide
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
                    // TODO: Initialisation de secours en cas d'erreur
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
                // TODO: Protection contre les exceptions lors de la récupération des skins utilisateur
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


    // Chargement des données au démarrage
    LaunchedEffect(Unit) {
        isLoading.value = true
        try {
            val skinjob = launch { getSkin() }
            val userSkinJob = launch { getUserSkin() }
            val userJob = launch { getUser() }
            skinjob.join()
            userSkinJob.join()
            userJob.join()

            // Vérification cruciale: s'assurer que les données sont valides
            if (itemsByCategory.value.isEmpty() || itemsByCategory.value.any { it.isEmpty() }) {
                // Réinitialiser avec des listes vides mais correctement structurées
                itemsByCategory.value = listOf(listOf(), listOf(), listOf())
                pricesByCategory.value = listOf(listOf(), listOf(), listOf())
            }

            // TODO: Vérification et log des préférences au démarrage
            println("Préférences au démarrage:")
            println("Arrow: ${prefs.arrowUsed}")
            println("Moon: ${prefs.moonUsed}")
            println("Planet: ${prefs.planetUsed}")

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
