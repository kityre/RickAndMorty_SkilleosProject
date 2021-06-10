package com.example.rickandmortyskilleoshugod

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_character.view.*
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private lateinit var adapter: OperationAdapter
    var listCharacter = listOf<Character>()

    override fun onCreate(savedInstanceState: Bundle?) = runBlocking<Unit> {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        listCharacter = getListCharacters()
        adapter = OperationAdapter(listCharacter)
        myRecyclerView.adapter = adapter
    }

    private suspend fun getListCharacters(): List<Character> {
        val listCharacter: MutableList<Character> = mutableListOf<Character>()
        var haveNextPage = true
        var nextPage = "1"                                                                      // premiere page

        while (haveNextPage) {
            val response = ApiClient.apiService.getAllCharactersFromPage(nextPage)              // On fait une requete API avec le numèro de page en parametre (https://rickandmortyapi.com/api/character/?page=$nextpage)
            if (response.isSuccessful && response.body() != null) {                             // La requete a marché et on a du contenue

                val listCharacterJsonByPage =                                                   // On crée une liste en Json qui possède tous les personnages
                    (Response(Gson().toJson(response.body()))["results"] as JSONArray)

                for (characterPlaceInJsonList in 0 until listCharacterJsonByPage.length()) {
                    val characterInJson =
                        listCharacterJsonByPage.getJSONObject(characterPlaceInJsonList)         // Récupère pour chaque ligne du tableau Json son personnage quon transforme en ObjetJson
                    listCharacter.add(createCharacterElement(characterInJson))                  // On crée l'objet 'Character' via l'ObjetJson qu'on ajoute ensuite a un liste comportant tous les personnages de la class 'Character'
                }

                val infoRequestJson =                                                           // On récupère l'info de la requete APi, pour savoir si il y a une page suivante
                    (Response(Gson().toJson(response.body()))["info"] as JSONObject).keys()

                haveNextPage = false
                infoRequestJson.forEach {
                    if (it == "next") {
                        haveNextPage = true                                                     // Si il y a une page suivante, on récupère le numero de page qu'on sauvegarde alors dans la variable $nextPage
                        nextPage = getNextPageNumberFromUrl((Response(Gson().toJson(response.body()))["info"] as JSONObject)["next"].toString())
                    }
                }
            } else haveNextPage = false
        }
        return listCharacter
    }

    private fun getNextPageNumberFromUrl(url: String): String{
        val urlSplit = url.split("=")                    //  On sépare l'url en deux à partir du '=' (ex:  ../api/character/?page=3)
        return Integer.parseInt(urlSplit[1]).toString()             //  On récupere alors tous les caractères au format int qu'on retourne alors en format String
    }

    private fun createCharacterElement(characterInJson: JSONObject): Character {
        return Character(
            characterInJson.get("name").toString(),
            characterInJson.get("status").toString(),
            characterInJson.get("species").toString(),
            characterInJson.get("gender").toString(),
            characterInJson.get("image").toString(),
            (characterInJson.get("origin") as JSONObject)["name"].toString()
        )
    }

    class OperationAdapter(private val list: List<Character>) :
        RecyclerView.Adapter<OperationAdapter.OperationViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OperationViewHolder {
            return OperationViewHolder(LayoutInflater.from(parent.context), parent)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: OperationViewHolder, position: Int) {
            holder.bind(list[position])
        }

        class OperationViewHolder(inflater: LayoutInflater, viewGroup: ViewGroup) :
            RecyclerView.ViewHolder(inflater.inflate(R.layout.item_character, viewGroup, false)) {

            @SuppressLint("SetTextI18n")
            fun bind(character: Character) {
                val statusColor: Int = when (character.status) {
                    "Alive" -> Color.GREEN
                    "Dead" -> Color.RED
                    else -> Color.GRAY
                }

                Picasso.get().load(character.image).into(itemView.image_perso)
                itemView.name_perso.text = character.name
                itemView.icon_status_perso.setTextColor(statusColor)
                itemView.status_perso.text = character.status + " - " + character.species
                itemView.gender_perso.text =
                    Html.fromHtml(
                        "Gender: <b>" + character.gender + "</b>",
                        Html.FROM_HTML_MODE_COMPACT
                    )
                itemView.origin_perso.text =
                    Html.fromHtml(
                        "origin: <b>" + character.origin + "</b>",
                        Html.FROM_HTML_MODE_COMPACT
                    )

            }
        }
    }

    //  méthode récupère sur internet
    class Response(json: String) : JSONObject(json) {
        val data = this.optJSONArray("data")
            ?.let {
                0.until(it.length()).map { i -> it.optJSONObject(i) }
            }

    }

    data class Character(
        var name: String = "",
        var status: String = "",
        var species: String = "",
        var gender: String = "",
        var image: String = "",
        var origin: String = ""
    )
}
