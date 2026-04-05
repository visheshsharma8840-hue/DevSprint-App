package com.example.myapplication

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var selectedMood = "chaotic"
    private var genCount = 0
    private var insultCount = 0
    private var wisdomCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnChaotic = findViewById<Button>(R.id.btnChaotic)
        val btnWise    = findViewById<Button>(R.id.btnWise)
        val btnBrave   = findViewById<Button>(R.id.btnBrave)
        val btnMonday  = findViewById<Button>(R.id.btnMonday)
        val allMoods   = listOf(btnChaotic, btnWise, btnBrave, btnMonday)

        fun selectMood(mood: String, selected: Button) {
            selectedMood = mood
            allMoods.forEach { it.alpha = 0.45f }
            selected.alpha = 1f
        }

        btnChaotic.setOnClickListener { selectMood("chaotic", btnChaotic) }
        btnWise.setOnClickListener    { selectMood("wise",    btnWise)    }
        btnBrave.setOnClickListener   { selectMood("brave",   btnBrave)   }
        btnMonday.setOnClickListener  { selectMood("monday",  btnMonday)  }

        allMoods.forEach { it.alpha = 0.45f }
        btnChaotic.alpha = 1f

        findViewById<Button>(R.id.btnGenerate).setOnClickListener {
            generateNonsense()
        }
    }

    private fun generateNonsense() {
        val btn      = findViewById<Button>(R.id.btnGenerate)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val scroll   = findViewById<LinearLayout>(R.id.resultScroll)

        btn.isEnabled = false
        btn.text = "Loading..."
        progress.visibility = View.VISIBLE
        scroll.visibility = View.GONE

        val fetchYesNo  = true
        val fetchKanye  = selectedMood in listOf("chaotic", "wise", "monday")
        val fetchChuck  = selectedMood in listOf("chaotic", "brave", "monday")
        val fetchInsult = selectedMood in listOf("chaotic", "brave", "monday")
        val fetchAdvice = selectedMood in listOf("wise", "monday")

        CoroutineScope(Dispatchers.IO).launch {
            val results = mutableMapOf<String, String>()

            try {
                val calls = mutableListOf<Deferred<Unit>>()

                if (fetchYesNo) calls.add(async {
                    val obj = JSONObject(URL("https://yesno.wtf/api").readText())
                    results["yesno_answer"] = obj.getString("answer")
                    results["yesno_image"]  = obj.getString("image")
                })
                if (fetchKanye) calls.add(async {
                    results["kanye"] = JSONObject(
                        URL("https://api.kanye.rest").readText()
                    ).getString("quote")
                })
                if (fetchChuck) calls.add(async {
                    results["chuck"] = JSONObject(
                        URL("https://api.chucknorris.io/jokes/random").readText()
                    ).getString("value")
                })
                if (fetchInsult) calls.add(async {
                    results["insult"] = JSONObject(
                        URL("https://evilinsult.com/generate_insult.php?lang=en&type=json")
                            .readText()
                    ).getString("insult")
                })
                if (fetchAdvice) calls.add(async {
                    results["advice"] = JSONObject(
                        URL("https://api.adviceslip.com/advice").readText()
                    ).getJSONObject("slip").getString("advice")
                })

                calls.awaitAll()

            } catch (e: Exception) {
                results["error"] = "Could not reach the nonsense servers. Check your internet!"
            }

            genCount++
            if (fetchInsult) insultCount++
            if (fetchAdvice) wisdomCount++

            withContext(Dispatchers.Main) {
                findViewById<TextView>(R.id.tvGenCount).text    = genCount.toString()
                findViewById<TextView>(R.id.tvInsultCount).text = insultCount.toString()
                findViewById<TextView>(R.id.tvWisdomCount).text = wisdomCount.toString()

                if (results.containsKey("error")) {
                    findViewById<TextView>(R.id.tvError).apply {
                        text = results["error"]
                        visibility = View.VISIBLE
                    }
                } else {
                    findViewById<TextView>(R.id.tvError).visibility = View.GONE

                    // Kanye card
                    val kanyeCard = findViewById<LinearLayout>(R.id.cardKanye)
                    if (results.containsKey("kanye")) {
                        kanyeCard.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvKanye).text = "\"${results["kanye"]}\""
                    } else kanyeCard.visibility = View.GONE

                    // Chuck card
                    val chuckCard = findViewById<LinearLayout>(R.id.cardChuck)
                    if (results.containsKey("chuck")) {
                        chuckCard.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvChuck).text = results["chuck"]
                    } else chuckCard.visibility = View.GONE

                    // Insult card
                    val insultCard = findViewById<LinearLayout>(R.id.cardInsult)
                    if (results.containsKey("insult")) {
                        insultCard.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvInsult).text = results["insult"]
                    } else insultCard.visibility = View.GONE

                    // Advice card
                    val adviceCard = findViewById<LinearLayout>(R.id.cardAdvice)
                    if (results.containsKey("advice")) {
                        adviceCard.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvAdvice).text = results["advice"]
                    } else adviceCard.visibility = View.GONE

                    // Verdict
                    val verdicts = mapOf(
                        "chaotic" to "Pure chaos achieved 🔥",
                        "wise"    to "Accidentally enlightened 🧠",
                        "brave"   to "You survived. Barely. 💀",
                        "monday"  to "Monday fully loaded 😵"
                    )
                    findViewById<TextView>(R.id.tvVerdict).text = verdicts[selectedMood]

                    // GIF card
                    val gifCard = findViewById<LinearLayout>(R.id.cardGif)
                    if (results.containsKey("yesno_image")) {
                        gifCard.visibility = View.VISIBLE
                        findViewById<TextView>(R.id.tvYesNo).text =
                            results["yesno_answer"]?.uppercase()

                        val imageView = findViewById<ImageView>(R.id.ivYesNo)

                        imageView.post {
                            val imageLoader = ImageLoader.Builder(this@MainActivity)
                                .components {
                                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                                        add(ImageDecoderDecoder.Factory())
                                    } else {
                                        add(GifDecoder.Factory())
                                    }
                                }
                                .build()

                            val request = ImageRequest.Builder(this@MainActivity)
                                .data(results["yesno_image"])
                                .target(imageView)
                                .crossfade(false)
                                .build()

                            imageLoader.enqueue(request)
                        }

                    } else gifCard.visibility = View.GONE
                }

                progress.visibility = View.GONE
                scroll.visibility   = View.VISIBLE
                btn.isEnabled       = true
                btn.text            = "Generate again"
            }
        }
    }
}