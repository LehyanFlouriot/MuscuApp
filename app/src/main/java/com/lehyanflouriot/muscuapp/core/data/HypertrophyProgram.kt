package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
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
 * The first program the app came with: a week of bodyweight hypertrophy, five full body sessions
 * and two rest days, with a bar, rings and a few loads. Each session has an exercise of emphasis,
 * which gets the most effort.
 */
object HypertrophyProgram : BuiltInProgram {
    const val ID = "programme-hypertrophie"

    override val id: String = ID

    private val pompesDeficit = Move(
        "ex-pompes-deficit", "Pompes en déficit, pieds surélevés", CHEST, emphasis = true, secondary = listOf(TRICEPS, SHOULDERS),
    )
    private val rowingPiedsSureleves = Move(
        "ex-rowing-australien-pieds-sureleves", "Rowing australien, pieds surélevés", BACK, secondary = listOf(BICEPS),
    )
    private val squatBulgare = Move("ex-squat-bulgare", "Squat bulgare", QUADS, emphasis = true, secondary = listOf(GLUTES))
    private val extensionsTriceps = Move("ex-extensions-triceps-sol", "Extensions triceps au sol", TRICEPS)
    private val relevesJambes = Move("ex-releves-jambes", "Relevés de jambes", ABS)
    private val mollets = Move("ex-mollets-unilateraux", "Mollets unilatéraux", CALVES)
    private val tractionsPronation = Move(
        "ex-tractions-pronation", "Tractions pronation", BACK, emphasis = true, secondary = listOf(BICEPS),
    )
    private val rowingSupination = Move(
        "ex-rowing-australien-supination", "Rowing australien supination", BACK, secondary = listOf(BICEPS),
    )
    private val pompesPseudoPlanche = Move(
        "ex-pompes-pseudo-planche-pike", "Pompes pseudo-planche ou pike", SHOULDERS, secondary = listOf(TRICEPS, CHEST),
    )
    private val souleveDeTerre = Move(
        "ex-souleve-de-terre-roumain-unilateral",
        "Soulevé de terre roumain unilatéral",
        HAMSTRINGS,
        usesWeight = true,
        secondary = listOf(GLUTES),
    )
    private val hollowBody = Move("ex-hollow-body-hold", "Hollow body hold", ABS, measure = Measure.SECONDS)
    private val sissySquat = Move("ex-sissy-squat-shrimp-squat", "Sissy squat ou shrimp squat", QUADS)
    private val pompesArcher = Move("ex-pompes-archer-diamant", "Pompes archer ou diamant", CHEST, secondary = listOf(TRICEPS, SHOULDERS))
    private val rowing = Move("ex-rowing-australien", "Rowing australien", BACK, secondary = listOf(BICEPS))
    private val rollout = Move("ex-rollout-serviette", "Rollout serviette", ABS)
    private val pikePushUps = Move(
        "ex-pike-push-ups-pieds-sureleves", "Pike push-ups, pieds surélevés", SHOULDERS, emphasis = true, secondary = listOf(TRICEPS),
    )
    private val tractionsSupination = Move("ex-tractions-supination", "Tractions supination", BACK, secondary = listOf(BICEPS))
    private val rowingCoudesEcartes = Move("ex-rowing-coudes-ecartes", "Rowing coudes écartés", SHOULDERS, secondary = listOf(BACK))
    private val curlServiette = Move("ex-curl-serviette", "Curl serviette dans une porte", BICEPS)
    private val fentes = Move("ex-fentes-marchees-lentes", "Fentes marchées lentes", QUADS, secondary = listOf(GLUTES))
    private val nordicCurl = Move("ex-nordic-curl", "Nordic curl", HAMSTRINGS, emphasis = true)
    private val pontFessier = Move(
        "ex-pont-fessier-unilateral-sureleve", "Pont fessier unilatéral surélevé", GLUTES, secondary = listOf(HAMSTRINGS),
    )
    private val rowingLeste = Move(
        "ex-rowing-australien-leste", "Rowing australien lesté", BACK, usesWeight = true, secondary = listOf(BICEPS),
    )
    private val dipsPompesDiamant = Move("ex-dips-pompes-diamant", "Dips ou pompes diamant", TRICEPS, secondary = listOf(CHEST))

    private val moves = listOf(
        pompesDeficit, rowingPiedsSureleves, squatBulgare, extensionsTriceps, relevesJambes, mollets,
        tractionsPronation, rowingSupination, pompesPseudoPlanche, souleveDeTerre, hollowBody,
        sissySquat, pompesArcher, rowing, rollout,
        pikePushUps, tractionsSupination, rowingCoudesEcartes, curlServiette, fentes,
        nordicCurl, pontFessier, rowingLeste, dipsPompesDiamant,
    )

    override fun exercises(createdAt: Long): List<Exercise> = moves.toExercises(createdAt)

    override fun create(createdAt: Long): TrainingProgram = TrainingProgram(
        id = ID,
        name = "Hypertrophie au poids du corps",
        info = "5 séances full body · 2 jours de repos · 45 à 60 min par séance. L'exercice d'emphase de chaque " +
            "séance est celui qui reçoit le plus d'effort.",
        days = listOf(
            session(
                MONDAY, "Pectoraux", "Poussée horizontale",
                line(pompesDeficit, 4, 6, 12, rir = 1, emphasis = true, note = "Mains sur 2 livres. Dernière série : + 3-5 partielles étirées"),
                line(rowingPiedsSureleves, 3, 8, 12, rir = 2),
                line(squatBulgare, 2, 10, 15, rir = 3, per = "jambe"),
                line(extensionsTriceps, 3, 8, 15, rir = 1),
                line(relevesJambes, 3, 10, 15, rir = 1),
                line(mollets, 3, 12, 20, rir = 1, note = "Pause 2 s en bas"),
            ),
            session(
                TUESDAY, "Dos", "Tirage vertical",
                line(tractionsPronation, 4, 4, 10, rir = 1, emphasis = true, note = "Moins de 4 reps : négatives de 5 s"),
                line(rowingSupination, 3, 8, 12, rir = 1),
                line(pompesPseudoPlanche, 3, 6, 12, rir = 2),
                line(souleveDeTerre, 3, 8, 12, rir = 2, note = "Sac à dos ou bidon en main"),
                line(hollowBody, 3, 30, 45),
            ),
            session(
                WEDNESDAY, "Quadriceps", "Séance la plus dure",
                line(squatBulgare, 4, 8, 15, rir = 1, per = "jambe", emphasis = true, note = "Excentrique 3 s, amplitude complète"),
                line(sissySquat, 3, 8, 12, rir = 1),
                line(pompesArcher, 3, 8, 12, rir = 2),
                line(rowing, 3, 10, 15, rir = 2),
                line(mollets, 3, 10, 15, rir = 1),
                line(rollout, 2, 8, 12, rir = 1),
            ),
            restDay(
                THURSDAY,
                "Placé juste après la séance quadriceps, la plus riche en excentrique. Marche, mobilité, sommeil. " +
                    "Pas de séance de rattrapage.",
            ),
            session(
                FRIDAY, "Épaules", "Poussée verticale",
                line(pikePushUps, 4, 6, 12, rir = 1, emphasis = true, note = "Ou handstand push-ups au mur"),
                line(tractionsSupination, 4, 6, 12, rir = 1),
                line(rowingCoudesEcartes, 3, 12, 20, rir = 1, note = "Deltoïdes postérieurs"),
                line(curlServiette, 3, 10, 15, rir = 1),
                line(fentes, 3, 12, rir = 3, per = "jambe"),
            ),
            session(
                SATURDAY, "Ischios & fessiers", "Chaîne postérieure",
                line(nordicCurl, 4, 4, 8, rir = 1, emphasis = true, note = "Assisté, ou descente en 5 s"),
                line(pontFessier, 3, 10, 15, rir = 1),
                line(rowingLeste, 3, 8, 12, rir = 1, note = "Sac à dos sur la poitrine"),
                line(dipsPompesDiamant, 3, 8, 12, rir = 1),
                line(mollets, 3, 12, 20, rir = 1),
            ),
            restDay(
                SUNDAY,
                "Après la séance ischios, la plus traumatisante pour les fibres. C'est ici que la masse se " +
                    "construit réellement.",
            ),
        ),
        notes = listOf(
            PlanNote(
                "note-echauffement",
                "Échauffement",
                "5 min de mobilité, puis 2 séries légères de l'exercice d'emphase. L'exercice d'emphase se termine " +
                    "par 3-5 partielles en position étirée.",
            ),
            PlanNote(
                "note-rir",
                "Le RIR",
                "Le nombre de reps qu'il te reste en réserve à la fin de la série. RIR 1 : tu aurais pu en faire une " +
                    "de plus. C'est le levier principal au poids du corps.",
            ),
            PlanNote(
                "note-repos",
                "Repos entre les séries",
                "2 à 3 min sur l'exercice d'emphase, 60 à 90 s sur les autres. Le repos long augmente mesurablement " +
                    "l'hypertrophie.",
            ),
            PlanNote(
                "note-progression",
                "Progression",
                "+1 rep par série chaque semaine. Haut de la fourchette atteint partout ? Variante plus dure, et on " +
                    "repart en bas.",
            ),
            PlanNote(
                "note-durcir",
                "Durcir un exercice, dans l'ordre",
                "Amplitude complète → excentrique de 3 s → pause en position étirée → unilatéral ou décliné → lest.",
            ),
            PlanNote(
                "note-decharge",
                "Décharge",
                "Toutes les 6 à 8 semaines : une semaine avec moitié moins de séries, en RIR 3-4.",
            ),
        ),
        createdAt = createdAt,
    )
}
