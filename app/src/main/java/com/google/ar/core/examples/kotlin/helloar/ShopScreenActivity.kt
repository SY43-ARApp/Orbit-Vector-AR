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


@Composable
fun ShopScreen(
    onMenu: () -> Unit = {} // Callback pour le bouton retour/menu
) {
    val context = LocalContext.current
    val font = DisketFont // Police personnalisée
    val prefs = remember { UserPreferences(context) } // Préférences utilisateur
    val uuid = prefs.uuid // Identifiant utilisateur
    var moon = prefs.moonUsed // Lune utilisée
    var arrow = prefs.arrowUsed // Flèche utilisée
    var planet = prefs.planetUsed

    // Variables d'état pour les items et les prix par catégorie
    val itemsByCategory = remember { mutableStateOf<List<List<Skin>>>(emptyList()) }
    val pricesByCategory = remember { mutableStateOf<List<List<Int>>>(emptyList()) }


    // État pour l'argent
    var money by remember { mutableStateOf<Int?>(1000) }
    var bestScore by remember { mutableStateOf<Int?>(0) }


    var purchasedItems by remember { mutableStateOf(mutableListOf<String>()) }

    // État pour l'item équipé/sélectionné dans chaque catégorie
    var equippedItems by remember {
        mutableStateOf(
            listOf(
                arrow?.toInt()?.takeIf { it >= 0 } ?: 0,   // flèche
                moon?.toIntOrNull()?.takeIf { it >= 0 } ?: 0,    // lune
                planet?.toIntOrNull()?.takeIf { it >= 0 } ?: 0   // planète
            )
        )
    }

    // TODO: Ajout d'un état pour suivre si les items sont équipés
    var itemIsEquipped by remember { mutableStateOf(listOf(false, false, false)) }

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
        // Utilise la fonction de vérification
        when (categoryIndex) {
            0 -> prefs.arrowUsed = itemIndex.toString()
            1 -> prefs.moonUsed = itemIndex.toString()
            2 -> prefs.planetUsed = itemIndex.toString()
        }
    }


    // Fonction pour l'achat d'un skin
//    fun buySkin(skinId: Int) {
//        val uuid = prefs.uuid
//        val skinPrice = allSkins.value.find { it.id == skinId }?.price ?: 0
//
//        // Vérifier si l'utilisateur a assez d'argent
//        money?.let {
//            if (it < skinPrice) {
//                AudioManager.playSfx("error")
//                return
//            }
//        }
//
//        // Afficher un indicateur de chargement
//        isLoading.value = true
//
//        coroutineScope.launch {
//            try {
//                // Appeler l'API et ATTENDRE sa réponse
//                val response = api.sendUserSkins(uuid, skinId)
//
//                // Vérifier si l'API a bien répondu
//                if (response.isSuccessful) {
//                    // Mettre à jour les skins possédés par l'utilisateur
//                    val updatedUserSkins = userSkins.value.toMutableList()
//                    if (!userSkins.value.any { it.skinId == skinId }) {
//                        updatedUserSkins.add(UserSkins(skinId))
//                        userSkins.value = updatedUserSkins
//                    }
//
//
//                    money = money?.minus(skinPrice)
//                    saveMoney()
//
//
//                    AudioManager.playSfx("purchase")
//                    selectedItem = 0
//
//                    // Équiper l'item si nécessaire (avec vérification)
//                    val categoryEquipped = itemIsEquipped.getOrDefault(selectedCategory, false)
//                    if (!categoryEquipped && isItemValid(selectedCategory, selectedItem)) {
//                        val newEquippedItems = equippedItems.toMutableList()
//                        val newItemIsEquipped = itemIsEquipped.toMutableList()
//
//                        // Vérification des indices avant modification
//                        if (selectedCategory < newEquippedItems.size) {
//                            newEquippedItems[selectedCategory] = selectedItem
//                        }
//
//                        if (selectedCategory < newItemIsEquipped.size) {
//                            newItemIsEquipped[selectedCategory] = true
//                        }
//
//                        equippedItems = newEquippedItems
//                        itemIsEquipped = newItemIsEquipped
//
//                        // Sauvegarde locale de l'item équipé
//                        saveEquippedItem(selectedCategory, selectedItem)
//                    }
//
//                } else {
//                    // Gérer l'échec de l'API
//                    errorMessage.value = "Échec de l'achat: ${response.code()}"
//                    AudioManager.playSfx("error")
//                }
//            } catch (e: Exception) {
//                // Gérer les erreurs de connexion
//                errorMessage.value = "Erreur de connexion: ${e.message}"
//                AudioManager.playSfx("error")
//            } finally {
//                // Toujours désactiver l'indicateur de chargement
//                isLoading.value = false
//            }
//        }
//    }

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

            // Vérification des indices équipés avec la nouvelle fonction


        } catch (e: Exception) {
            errorMessage.value = "Erreur de connexion: ${e.message}"
            // Initialisation sécurisée en cas d'erreur
            itemsByCategory.value = listOf(listOf(), listOf(), listOf())
            pricesByCategory.value = listOf(listOf(), listOf(), listOf())
        } finally {
            isLoading.value = false
        }
    }
    if (isLoading.value) {

    } else {
        ShopView(allSkins.value, userSkins)
    }


}

@Composable
fun ShopView(skins: List<Skin>, userSkins: MutableState<List<UserSkins>>) {
    var selectedCategory by remember { mutableIntStateOf(0) }
    var money by remember { mutableStateOf<Int?>(1000) }
    var selectedItem by remember { mutableIntStateOf(0) }



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
                            selectedItem = 0  // Réinitialiser l'item sélectionné
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
        ListSkinPerType(skins, selectedCategory, userSkins)


    }


}


@Composable
fun ListSkinPerType(skins: List<Skin>, type: Int, userSkins: MutableState<List<UserSkins>>) {
    var newListSkin = skins.filter { it.type == type }
    ListSkins(newListSkin, userSkins)

}


@Composable
fun ListSkins(skins: List<Skin>, userSkins: MutableState<List<UserSkins>>) {
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
                if (firstIndex < skins.size) {
                    val isPurchased = userSkins.value.any {
                        it.skinId == skins[firstIndex].id
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OneSkin(
                        skin = skins[firstIndex],
                        isSelected = false,
                        isPurchased = true,
                        isEquipped = false
                    )
                }


                // Deuxième élément de la paire
                val secondIndex = firstIndex + 1
                if (secondIndex < skins.size) {
                    Box(modifier = Modifier.weight(1f)) {
                        OneSkin(
                            skin = skins[secondIndex],
                            isSelected = false,
                            isPurchased = true,
                            isEquipped = false
                        )
                    }
                } else if (skins.size % 2 != 0) {
                    // Espace vide pour maintenir l'alignement
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun OneSkin(
    skin: Skin,
    isSelected: Boolean,
    isPurchased: Boolean,
    isEquipped: Boolean
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .clickable(enabled = !isPurchased || !isEquipped) {
                if (!isPurchased) {
                    //selectedItem = if (isSelected) 0 else itemIndex
                    AudioManager.playSfx("tap")
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

val skins = listOf(
    Skin(1, 100, 0, 0),
    Skin(2, 200, 10, 1),
    Skin(3, 300, 20, 2),
    Skin(4, 400, 30, 0),
    Skin(5, 500, 40, 1),
    Skin(6, 600, 50, 1),
)


