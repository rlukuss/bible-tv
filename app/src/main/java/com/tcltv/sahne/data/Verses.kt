package com.tcltv.sahne.data

import kotlin.random.Random

data class Verse(val text: String, val reference: String)

/** Short verses from the Louis Segond 1910 (public domain), kept short enough to sit on one line of the top bar. */
object Verses {
    val ALL = listOf(
        Verse("L'Éternel est mon berger : je ne manquerai de rien.", "Psaume 23:1"),
        Verse("Je puis tout par celui qui me fortifie.", "Philippiens 4:13"),
        Verse("Celui qui n'aime pas n'a pas connu Dieu, car Dieu est amour.", "1 Jean 4:8"),
        Verse("Ta parole est une lampe à mes pieds, et une lumière sur mon sentier.", "Psaume 119:105"),
        Verse("Recommande ton sort à l'Éternel, mets en lui ta confiance, et il agira.", "Psaume 37:5"),
        Verse("Venez à moi, vous tous qui êtes fatigués et chargés, et je vous donnerai du repos.", "Matthieu 11:28"),
        Verse("Je suis le chemin, la vérité, et la vie. Nul ne vient au Père que par moi.", "Jean 14:6"),
        Verse("Réjouissez-vous toujours dans le Seigneur ; je le répète, réjouissez-vous.", "Philippiens 4:4"),
        Verse("Priez sans cesse.", "1 Thessaloniciens 5:17"),
        Verse("L'Éternel est ma lumière et mon salut : de qui aurais-je crainte ?", "Psaume 27:1"),
        Verse("Dieu est pour nous un refuge et un appui, un secours qui ne manque jamais dans la détresse.", "Psaume 46:2"),
        Verse("Confie-toi en l'Éternel de tout ton cœur, et ne t'appuie pas sur ta sagesse.", "Proverbes 3:5"),
        Verse("Car rien n'est impossible à Dieu.", "Luc 1:37"),
        Verse("Je vous laisse la paix, je vous donne ma paix.", "Jean 14:27"),
        Verse("Aimez-vous les uns les autres ; comme je vous ai aimés, vous aussi, aimez-vous les uns les autres.", "Jean 13:34"),
        Verse("Ne crains rien, car je suis avec toi.", "Ésaïe 41:10"),
        Verse("Le Seigneur est fidèle, il vous affermira et vous préservera du malin.", "2 Thessaloniciens 3:3"),
        Verse("Que tout ce que vous faites se fasse avec charité !", "1 Corinthiens 16:14"),
        Verse("Heureux ceux qui procurent la paix, car ils seront appelés fils de Dieu !", "Matthieu 5:9"),
        Verse("C'est ici la journée que l'Éternel a faite : qu'elle soit pour nous un sujet d'allégresse et de joie !", "Psaume 118:24"),
        Verse("Sentez et voyez combien l'Éternel est bon ! Heureux l'homme qui cherche en lui son refuge !", "Psaume 34:9"),
        Verse("La charité est patiente, elle est pleine de bonté.", "1 Corinthiens 13:4"),
        Verse("Rendez grâces en toutes choses.", "1 Thessaloniciens 5:18"),
        Verse("Cherchez premièrement le royaume et la justice de Dieu ; et toutes ces choses vous seront données par-dessus.", "Matthieu 6:33"),
        Verse("Laissez venir à moi les petits enfants, et ne les en empêchez pas.", "Marc 10:14"),
        Verse("Honore ton père et ta mère.", "Exode 20:12"),
        Verse("Je t'aime d'un amour éternel.", "Jérémie 31:3"),
        Verse("L'Éternel te gardera de tout mal, il gardera ton âme.", "Psaume 121:7"),
        Verse("Le secours me vient de l'Éternel, qui a fait les cieux et la terre.", "Psaume 121:2"),
        Verse("Soyez bons les uns envers les autres, compatissants, vous pardonnant réciproquement.", "Éphésiens 4:32"),
        Verse("Celui qui demeure sous l'abri du Très-Haut repose à l'ombre du Tout-Puissant.", "Psaume 91:1"),
        Verse("Je suis la lumière du monde.", "Jean 8:12"),
        Verse("La joie de l'Éternel sera votre force.", "Néhémie 8:10"),
        Verse("Mon âme, bénis l'Éternel, et n'oublie aucun de ses bienfaits !", "Psaume 103:2"),
    )

    /** A random verse index other than [previous], so the bar never seems stuck on one verse. */
    fun next(previous: Int, random: Random = Random): Int {
        if (ALL.size < 2) return 0
        val pick = random.nextInt(ALL.size - 1)
        return if (pick >= previous && previous >= 0) pick + 1 else pick
    }
}
