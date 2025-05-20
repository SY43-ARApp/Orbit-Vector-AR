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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import com.google.ar.core.examples.kotlin.helloar.data.UserPreferences
import com.google.ar.core.examples.kotlin.helloar.ui.theme.DisketFont
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.mutableStateOf

@Composable
fun ShopScreen(
    onMenu : () -> Unit = {} // Callback pour le bouton retour/menu
) {
    val context = LocalContext.current
    val font = DisketFont // Police personnalisée (non utilisée ici)
    val prefs = remember { UserPreferences(context) } // Préférences utilisateur
    val uuid = prefs.uuid // Identifiant utilisateur (non utilisé ici)
    val moone = prefs.mooneUsed // Lune utilisée
    val arrow = prefs.arrowUsed // Flèche utilisée
    val planet = prefs.planetUsed
    
    // Accès aux SharedPreferences pour stocker les achats
    val sharedPrefs = remember { context.getSharedPreferences("shop_prefs", ComponentActivity.MODE_PRIVATE) }
    
    // État pour l'argent
    var money by remember { mutableIntStateOf(sharedPrefs.getInt("user_money", 1000)) }
    var selectedCategory by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableIntStateOf(-1) }

    //TODO : changer cette variable qui utilisera l'API pour récupérer les possessions

    // Ensemble pour stocker les IDs des items déjà achetés
    val purchasedItems = remember { 
        mutableSetOf<String>().apply {
            // Charger les achats depuis les préférences
            val savedPurchases = sharedPrefs.getStringSet("purchased_items", emptySet()) ?: emptySet()
            addAll(savedPurchases)
        }
    }
    
    // Fonction pour générer l'ID unique d'un item
    fun getItemId(categoryIndex: Int, itemIndex: Int): String {
        return "cat${categoryIndex}_item${itemIndex}"
    }
    
    // Fonction pour sauvegarder l'argent
    fun saveMoney() {
        sharedPrefs.edit().putInt("user_money", money).apply()
    }
    
    // Fonction pour sauvegarder les achats
    fun savePurchases() {
        sharedPrefs.edit().putStringSet("purchased_items", purchasedItems).apply()
    }
    
    // État pour l'item équipé/sélectionné dans chaque catégorie
    var equippedItems by remember {
        mutableStateOf(listOf(
            arrow?.toIntOrNull() ?: -1,   // flèche
            moone?.toIntOrNull() ?: -1,   // lune
            planet?.toIntOrNull() ?: -1   // planète
        ))
    }
    
    /// Fonction pour sauvegarder les préférences d'items équipés
    fun saveEquippedItem(categoryIndex: Int, itemIndex: Int) {
        when (categoryIndex) {
            0 -> prefs.arrowUsed = itemIndex.toString()
            1 -> prefs.mooneUsed = itemIndex.toString()
            2 -> prefs.planetUsed = itemIndex.toString()
        }
    }
    
    // Définition des items par catégorie
    // TODO: valeur en dur
    val itemsByCategory = listOf(
        listOf("Flèche 1", "Flèche 2", "Flèche 3", "Flèche 4"),
        listOf("Lune 1", "Lune 2", "Lune 3", "Lune 4"),
        listOf("Planète 1", "Planète 2", "Planète 3", "Planète 4")
    )
    
    // Prix des items par catégorie
    // TODO: valeur en dur
    val pricesByCategory = listOf(
        listOf(50, 100, 150, 200),
        listOf(100, 200, 300, 400),
        listOf(150, 300, 450, 600)
    )
    
    Scaffold(
        
        topBar = {
            // Barre supérieure avec bouton retour et titre
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1C2B4F)), // Nouveau background bleu foncé
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onMenu() }) { // Bouton retour
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }

                Text(
                    text = "SHOP", // Titre de la page
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(Color(0xFF0A142A))) { // Background général de l'écran
            // Nouveau Box avec le style spécifié pour remplacer la Card principale
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp, bottom = 90.dp)
                    .fillMaxWidth(0.96f)
                    .background(Color(0xFF101B3A), shape = RoundedCornerShape(28.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    // Carte pour le solde d'argent uniquement
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF192542) // Bleu foncé légèrement plus clair
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { money += 100 }, // Ajoute 100 au solde
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add money",
                                    tint = Color(0xFF4A8CFF) // Bleu plus clair pour l'icône
                                )
                            }

                            Text(
                                text = "$money", // Affiche le solde
                                modifier = Modifier.padding(horizontal = 12.dp),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White // Texte blanc pour contraste
                            )

                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = CircleShape,
                                color = Color(0xFF4A8CFF) // Bleu clair pour le symbole dollar
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Nouvelle carte dédiée aux boutons de catégorie
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF2A3A5F) // Bleu moyen
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { selectedCategory = 0 },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "Flèche",
                                    color = if (selectedCategory == 0) Color(0xFF4A8CFF) else Color.LightGray, // Accent bleu pour sélection
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            TextButton(
                                onClick = { selectedCategory = 1 },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "Lune",
                                    color = if (selectedCategory == 1) Color(0xFF4A8CFF) else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            TextButton(
                                onClick = { selectedCategory = 2 },
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Text(
                                    text = "Planète",
                                    color = if (selectedCategory == 2) Color(0xFF4A8CFF) else Color.LightGray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }

                    // Liste des items selon la catégorie sélectionnée
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .weight(1f)
                    ) {
                        items((itemsByCategory[selectedCategory].size + 1) / 2) { rowIndex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                            ) {
                                for (col in 0 until 2) {
                                    val itemIndex = rowIndex * 2 + col
                                    val items = itemsByCategory[selectedCategory]
                                    val prices = pricesByCategory[selectedCategory]
                                    if (itemIndex < items.size) {
                                        val isSelected = selectedItem == itemIndex
                                        val isEquipped = equippedItems[selectedCategory] == itemIndex   
                                        val isPurchased = purchasedItems.contains(getItemId(selectedCategory, itemIndex))
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(8.dp)
                                                .aspectRatio(1f)
                                                .clickable(enabled = !isPurchased || !isEquipped) { 
                                                    if (!isPurchased) {
                                                        selectedItem = if (isSelected) -1 else itemIndex
                                                        AudioManager.playSfx("tap")
                                                    }
                                                },
                                            elevation = CardDefaults.cardElevation(
                                                defaultElevation = if (isSelected || isEquipped) 8.dp else 4.dp
                                            ),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when {
                                                    isEquipped -> Color(0xFF1E5633) // Vert foncé pour l'item équipé
                                                    isPurchased -> Color(0xFF1E3356) // Couleur différente pour les items achetés
                                                    isSelected -> Color(0xFF2A3A5F)
                                                    else -> Color(0xFF192542)
                                                }
                                            ),
                                            border = when {
                                                isEquipped -> BorderStroke(2.dp, Color(0xFF4AFF8C)) // Bordure vert vif pour item équipé
                                                isPurchased -> BorderStroke(2.dp, Color(0xFF2A9E47)) // Bordure verte pour les items achetés
                                                isSelected -> BorderStroke(2.dp, Color(0xFF4A8CFF))
                                                else -> null
                                            }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.SpaceBetween,
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .fillMaxWidth(),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = items[itemIndex],
                                                        color = Color.White,
                                                        fontWeight = if (isSelected || isPurchased || isEquipped) 
                                                            FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }

                                                Surface(
                                                    color = when {
                                                        isEquipped -> Color(0xFF4AFF8C) // Vert vif pour item équipé
                                                        isPurchased -> Color(0xFF2A9E47) // Couleur verte pour les items achetés
                                                        isSelected -> Color(0xFF5D9CFF)
                                                        else -> Color(0xFF4A8CFF)
                                                    },
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = when {
                                                            isEquipped -> "ÉQUIPÉ"
                                                            isPurchased -> "ACHETÉ"
                                                            else -> "${prices[itemIndex]} $"
                                                        },
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White,
                                                        modifier = Modifier.padding(
                                                            horizontal = 8.dp,
                                                            vertical = 4.dp
                                                        )
                                                    )
                                                }
                                                
                                                // Bouton de sélection pour les items achetés
                                                if (isPurchased && !isEquipped) {
                                                    Button(
                                                        onClick = {
                                                            // Mettre à jour l'item équipé pour cette catégorie
                                                            val newEquippedItems = equippedItems.toMutableList()
                                                            newEquippedItems[selectedCategory] = itemIndex
                                                            equippedItems = newEquippedItems
                                                            
                                                            // Sauvegarder selon la catégorie
                                                            saveEquippedItem(selectedCategory, itemIndex)
                                                            
                                                            AudioManager.playSfx("select")
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 4.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = Color(0xFF4AFF8C)
                                                        )
                                                    ) {
                                                        Text(
                                                            text = "SÉLECTIONNER",
                                                            // TODO: valeur en dur
                                                            fontSize = 10.sp, // Taille de texte réduite de 12.sp à 10.sp
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.Black
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Espace vide si pas d'item
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                    
                    // Bouton d'achat qui apparaît quand un item est sélectionné
                    AnimatedVisibility(
                        visible = selectedItem >= 0,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val items = itemsByCategory[selectedCategory]
                        val prices = pricesByCategory[selectedCategory]
                        val itemId = if (selectedItem >= 0) getItemId(selectedCategory, selectedItem) else ""
                        val canBuy = selectedItem >= 0 && 
                                    selectedItem < prices.size &&
                                    money >= prices[selectedItem] &&
                                    !purchasedItems.contains(itemId)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF2A3A5F)
                            )
                        ) {
                            Button(
                                onClick = { 
                                    if (canBuy) {
                                        // Déduire l'argent
                                        money -= prices[selectedItem]
                                        saveMoney()
                                        
                                        // Ajouter l'item à la liste des items achetés
                                        purchasedItems.add(itemId)
                                        savePurchases()
                                        
                                        // Si c'est le premier item acheté dans cette catégorie,
                                        // l'équiper automatiquement
                                        if (equippedItems[selectedCategory] == -1) {
                                            val newEquippedItems = equippedItems.toMutableList()
                                            newEquippedItems[selectedCategory] = selectedItem
                                            equippedItems = newEquippedItems
                                            
                                            // Sauvegarder selon la catégorie
                                            saveEquippedItem(selectedCategory, selectedItem)
                                        }
                                        
                                        AudioManager.playSfx("purchase")
                                        selectedItem = -1
                                    } else {
                                        AudioManager.playSfx("error")
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                enabled = canBuy,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canBuy) Color(0xFF4A8CFF) else Color.Gray,
                                    disabledContainerColor = Color.Gray
                                )
                            ) {
                                Text(
                                    text = if (canBuy) 
                                        "ACHETER" 
                                    else if (purchasedItems.contains(itemId))
                                        "DÉJÀ ACHETÉ"
                                    else 
                                        "FONDS INSUFFISANTS",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

class ShopScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            com.google.ar.core.examples.kotlin.helloar.ui.theme.OrbitVectorARTheme {
                ShopScreen(
                    onMenu = {
                        AudioManager.playSfx("tap") // Joue un son lors du retour
                        startActivity(Intent(this, MenuScreenActivity::class.java)) // Retour au menu
                        finish()
                    }
                )
            }
        }
    }
}
