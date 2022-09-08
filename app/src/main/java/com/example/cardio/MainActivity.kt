package com.example.cardio

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cardio.ui.theme.CardioTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.RuntimeException

class MainActivity : ComponentActivity() {

    var techListsArray: Array<Array<String>>? = null
    var nfcAdapter: NfcAdapter? = null
    var pendingIntent: PendingIntent? = null
    var intentFiltersArray: Array<IntentFilter>? = null

    //var tagId: MutableLiveData<String> = MutableLiveData("")
    var tagId = MutableStateFlow("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardioTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    GameStartingScreen(tagId)
                }
            }
        }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_MUTABLE
            )

        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }
        }

        intentFiltersArray = arrayOf(ndef)
        techListsArray = arrayOf(arrayOf<String>(Ndef::class.java.name))

    }

    public override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    public override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(
            this,
            pendingIntent,
            intentFiltersArray,
            techListsArray
        )
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tagFromIntent: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        //do something with tagFromIntent
        tagId.value = tagFromIntent?.id?.joinToString(":") { "%02x".format(it) }.toString()
        println(tagId.value)
    }
}

@Composable
fun GameStartingScreen(
    isTagId: MutableStateFlow<String>,
    mainViewModel: MainViewModel = viewModel()
) {
    var playerName by remember { mutableStateOf("") }
    var playerBalance by remember { mutableStateOf("") }
    var openDialog by remember { mutableStateOf(false) }
    val tagId by isTagId.collectAsState()
    var playerList by remember { mutableStateOf(mainViewModel.players.size) }

    val focusManager = LocalFocusManager.current
    Column(
        verticalArrangement = Arrangement.spacedBy(50.dp, Alignment.Top),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
                onClick = {
                    focusManager.clearFocus()
                }
            )
            .fillMaxSize()
            .padding(start = 32.dp, end = 32.dp, top = 32.dp)
    ) {
        Text("Veuillez entrer votre solde initial")
        Text("Liste des joueurs : (${mainViewModel.players.size}/${mainViewModel.maxPlayer})")
        LazyRow() {
            items(mainViewModel.players) { player ->
                Text(player.toString())
            }
        }

        OutlinedTextField(
            value = playerName,
            label = { Text("Nom du joueur") },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            ),
            singleLine = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null
                )
            },
            keyboardOptions = KeyboardOptions(
                KeyboardCapitalization.Sentences,
                false,
                imeAction = ImeAction.Next
            ),
            onValueChange = { playerName = it }
        )
        OutlinedTextField(
            enabled = playerName.isNotEmpty(),
            value = playerBalance,
            label = { Text("Solde") },
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = MaterialTheme.colors.surface
            ),
            singleLine = true,
            trailingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_baseline_account_balance_wallet_24),
                    contentDescription = null
                )
            },
            onValueChange = { playerBalance = it },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = {
                //I should pass the onClick function
                focusManager.clearFocus()
                openDialog = true
                isTagId.value = ""
            })
        )
        Button(
            enabled = playerName.isNotEmpty(),
            onClick = {
                openDialog = true
                isTagId.value = ""
            }) {
            Text("Valider")
        }
        if (openDialog) {
            AlertDialog(
                onDismissRequest = { openDialog = false },
                title = {
                    val text = if (tagId.isEmpty()) "En attente d'une carte" else "Carte trouvée"
                    Text(text)
                },
                text = {
                    Text("Joueur: $playerName\n\nSolde initial: $playerBalance\n\nTag: $tagId")
                },
                confirmButton = {
                    TextButton(
                        enabled = tagId != "",
                        onClick = {
                            mainViewModel.addPlayer(
                                Player(
                                    playerName,
                                    playerBalance.toInt(),
                                    tagId
                                )
                            )
                            openDialog = false
                            playerName = ""
                        }
                    ) {
                        Text("Ajouter")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            isTagId.value = ""
                            openDialog = false
                        }
                    ) {
                        Text("Annuler")
                    }
                }

            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CardioTheme {
        GameStartingScreen(MutableStateFlow(""))
    }
}