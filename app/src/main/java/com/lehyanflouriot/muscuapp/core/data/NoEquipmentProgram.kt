package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.ABS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.BACK
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.BICEPS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CALVES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.CHEST
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.GLUTES
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.HAMSTRINGS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.QUADS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.SHOULDERS
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup.TRICEPS
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/**
 * The program the app comes with: a week of hypertrophy for a bulk, at body weight and without
 * any equipment, five full body sessions and two rest days on the days of the meal plan. Every
 * muscle gets 10 to 20 sets a week, in simple exercises done close to failure. A door frame to
 * pull on and a chair are all it uses. Each session has an exercise of emphasis, which gets the
 * most effort.
 */
object NoEquipmentProgram : BuiltInProgram {
    const val ID = "programme-sans-materiel"

    override val id: String = ID

    private val pompes = Move("ex-pompes", "Pompes", CHEST, emphasis = true, secondary = listOf(TRICEPS, SHOULDERS))
    private val pompesLarges = Move("ex-pompes-larges", "Pompes larges", CHEST, secondary = listOf(SHOULDERS, TRICEPS))
    private val pompesDiamant = Move("ex-pompes-diamant", "Pompes diamant", TRICEPS, secondary = listOf(CHEST))
    private val pompesPike = Move("ex-pompes-pike", "Pompes pike", SHOULDERS, emphasis = true, secondary = listOf(TRICEPS))
    private val extensionsTriceps = Move("ex-extensions-triceps-sol", "Extensions triceps au sol", TRICEPS)
    private val rowing = Move("ex-rowing-cadre-porte", "Rowing au cadre de porte", BACK, secondary = listOf(BICEPS))
    private val rowingUneMain = Move(
        "ex-rowing-cadre-porte-une-main", "Rowing au cadre de porte, une main", BACK, emphasis = true, secondary = listOf(BICEPS),
    )
    private val backWidow = Move("ex-back-widow", "Back widow", BACK, secondary = listOf(SHOULDERS))
    private val curl = Move("ex-curl-marteau-cadre-porte", "Curl marteau au cadre de porte", BICEPS)
    private val elevationsY = Move("ex-elevations-y-sol", "Élévations Y au sol", SHOULDERS, secondary = listOf(BACK))
    private val squatBulgare = Move("ex-squat-bulgare", "Squat bulgare", QUADS, emphasis = true, secondary = listOf(GLUTES))
    private val sissySquat = Move("ex-sissy-squat-tenu", "Sissy squat tenu", QUADS)
    private val fenteArriere = Move("ex-fente-arriere", "Fente arrière", QUADS, secondary = listOf(GLUTES))
    private val pontFessier = Move("ex-pont-fessier-une-jambe", "Pont fessier une jambe", GLUTES, secondary = listOf(HAMSTRINGS))
    private val legCurl = Move("ex-leg-curl-glisse", "Leg curl glissé", HAMSTRINGS, emphasis = true)
    private val mollets = Move("ex-mollets-unilateraux", "Mollets unilatéraux", CALVES)
    private val crunchInverse = Move("ex-crunch-inverse", "Crunch inversé", ABS)
    private val crunch = Move("ex-crunch", "Crunch", ABS)

    private val moves = listOf(
        pompes, pompesLarges, pompesDiamant, pompesPike, extensionsTriceps,
        rowing, rowingUneMain, backWidow, curl, elevationsY,
        squatBulgare, sissySquat, fenteArriere, pontFessier, legCurl, mollets,
        crunchInverse, crunch,
    )

    override fun exercises(createdAt: Long): List<Exercise> = moves.toExercises(createdAt)

    override fun create(createdAt: Long): TrainingProgram = TrainingProgram(
        id = ID,
        name = "Prise de masse sans matériel",
        info = "5 séances full body · 2 jours de repos · 45 à 60 min par séance. Rien à acheter : le sol, un cadre de " +
            "porte et une chaise suffisent. L'exercice d'emphase de chaque séance est celui qui reçoit le plus d'effort.",
        days = listOf(
            session(
                MONDAY, "Pectoraux", "Poussée",
                line(
                    pompes, 4, 8, 20, rir = 1, emphasis = true,
                    note = "Descente en 3 s, poitrine au ras du sol. Plus de 20 : pieds sur une chaise",
                ),
                line(rowing, 4, 8, 15, rir = 1, note = "Pieds près du cadre, corps gainé : plus tu te penches en arrière, plus c'est dur"),
                line(squatBulgare, 3, 10, 15, rir = 2, per = "jambe", note = "Pied arrière sur une chaise ou le canapé"),
                line(pompesLarges, 3, 8, 20, rir = 1),
                line(extensionsTriceps, 3, 8, 15, rir = 1, note = "Sur les genoux pour commencer"),
                line(crunchInverse, 4, 10, 20, rir = 1),
            ),
            session(
                TUESDAY, "Dos", "Tirage",
                line(
                    rowingUneMain, 4, 8, 15, rir = 1, per = "bras", emphasis = true,
                    note = "Descente en 3 s. Trop facile : pieds plus près du cadre",
                ),
                line(backWidow, 3, 10, 15, rir = 1, note = "Sur le dos, pousse les coudes dans le sol pour soulever le haut du dos. Tiens 2 s"),
                line(pompesPike, 3, 6, 15, rir = 2),
                line(curl, 3, 8, 15, rir = 1, per = "bras", note = "Coude fixe à hauteur d'épaule : ramène le corps vers ta main"),
                line(pontFessier, 3, 10, 20, rir = 1, per = "jambe", note = "Pause 2 s en haut"),
                line(mollets, 3, 12, 20, rir = 1, per = "jambe", note = "Sur une marche si tu peux. Pause 2 s en haut"),
            ),
            session(
                WEDNESDAY, "Jambes", "Séance la plus dure",
                line(squatBulgare, 4, 8, 15, rir = 1, per = "jambe", emphasis = true, note = "Descente en 3 s, genou arrière au ras du sol"),
                line(sissySquat, 3, 8, 20, rir = 1, note = "Une main au cadre de porte, talons levés, genoux vers l'avant"),
                line(legCurl, 3, 6, 15, rir = 1, note = "En chaussettes sur un sol lisse. Trop dur : ne fais que la descente"),
                line(mollets, 4, 12, 20, rir = 1, per = "jambe"),
                line(pompesDiamant, 3, 6, 15, rir = 1),
                line(crunch, 3, 12, 25, rir = 1),
            ),
            restDay(THURSDAY, "Placé juste après la séance la plus dure. Marche, mobilité, sommeil."),
            session(
                FRIDAY, "Épaules & bras", "Poussée verticale",
                line(
                    pompesPike, 4, 6, 15, rir = 1, emphasis = true,
                    note = "Hanches hautes, la tête descend devant les mains. Plus de 15 : pieds sur une chaise",
                ),
                line(rowing, 3, 8, 15, rir = 1),
                line(pompes, 3, 8, 20, rir = 1),
                line(curl, 3, 8, 15, rir = 1, per = "bras"),
                line(extensionsTriceps, 3, 8, 15, rir = 1),
                line(elevationsY, 3, 10, 20, rir = 1, note = "À plat ventre, bras en Y, pouces vers le haut. Tiens 2 s"),
                line(crunchInverse, 3, 10, 20, rir = 1),
            ),
            session(
                SATURDAY, "Fessiers & ischios", "Chaîne postérieure",
                line(legCurl, 4, 6, 15, rir = 1, emphasis = true, note = "Descente en 4 s. Trop facile : une jambe"),
                line(pontFessier, 4, 10, 20, rir = 1, per = "jambe", note = "Pause 2 s en haut"),
                line(fenteArriere, 3, 10, 15, rir = 2, per = "jambe"),
                line(pompesLarges, 3, 8, 20, rir = 1),
                line(rowingUneMain, 3, 8, 15, rir = 1, per = "bras"),
                line(mollets, 3, 12, 20, rir = 1, per = "jambe"),
            ),
            restDay(SUNDAY, "C'est au repos que la masse se construit : mange bien et dors."),
        ),
        notes = listOf(
            PlanNote(
                "note-materiel",
                "Sans matériel",
                "Le sol, un cadre de porte solide pour tirer, et une chaise ou le canapé pour le squat bulgare. Rien d'autre.",
            ),
            PlanNote(
                "note-echauffement",
                "Échauffement",
                "5 min de mobilité, puis 2 séries légères de l'exercice d'emphase.",
            ),
            PlanNote(
                "note-rir",
                "Près de l'échec",
                "Sans charge, tout se joue sur l'effort : finis chaque série en gardant 1 ou 2 reps en réserve (le RIR), " +
                    "pas plus. Une série facile ne fait pas grossir.",
            ),
            PlanNote(
                "note-tempo",
                "Tempo",
                "Descente en 3 s sur chaque rep, sans élan : le muscle travaille plus longtemps à chaque série.",
            ),
            PlanNote(
                "note-repos",
                "Repos entre les séries",
                "3 min sur l'exercice d'emphase, 1 min 30 sur les autres.",
            ),
            PlanNote(
                "note-progression",
                "Progression",
                "+1 ou 2 reps par série chaque semaine. Haut de la fourchette atteint ? Rends l'exercice plus dur : " +
                    "descente en 4 s, pause de 2 s en bas, puis la variante plus dure de la consigne.",
            ),
            PlanNote(
                "note-decharge",
                "Décharge",
                "Toutes les 6 à 8 semaines : une semaine avec moitié moins de séries, en RIR 3.",
            ),
        ),
        createdAt = createdAt,
    )
}
