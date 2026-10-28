package com.lehyanflouriot.muscuapp.core.data

import com.lehyanflouriot.muscuapp.core.model.Exercise
import com.lehyanflouriot.muscuapp.core.model.Measure
import com.lehyanflouriot.muscuapp.core.model.MuscleGroup
import com.lehyanflouriot.muscuapp.core.model.PlanNote
import com.lehyanflouriot.muscuapp.core.model.SessionExercise
import com.lehyanflouriot.muscuapp.core.model.SetTarget
import com.lehyanflouriot.muscuapp.core.model.TrainingDay
import com.lehyanflouriot.muscuapp.core.model.TrainingProgram
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY

/**
 * The program the app comes with: a week of bodyweight hypertrophy, five full body sessions and
 * two rest days. Each session has an exercise of emphasis, which gets the most effort.
 */
object HypertrophyProgram {
    const val ID = "programme-hypertrophie"

    /** Rests of the exercise of emphasis, and of the others. */
    const val EMPHASIS_REST = 180
    const val REST = 90

    private class Move(
        val id: String,
        val name: String,
        val group: MuscleGroup,
        val usesWeight: Boolean = false,
        val measure: Measure = Measure.REPS,
        val emphasis: Boolean = false,
    )

    private val pompesDeficit = Move("ex-pompes-deficit", "Pompes en déficit, pieds surélevés", MuscleGroup.CHEST, emphasis = true)
    private val rowingPiedsSureleves = Move("ex-rowing-australien-pieds-sureleves", "Rowing australien, pieds surélevés", MuscleGroup.BACK)
    private val squatBulgare = Move("ex-squat-bulgare", "Squat bulgare", MuscleGroup.LEGS, emphasis = true)
    private val extensionsTriceps = Move("ex-extensions-triceps-sol", "Extensions triceps au sol", MuscleGroup.TRICEPS)
    private val relevesJambes = Move("ex-releves-jambes", "Relevés de jambes", MuscleGroup.ABS)
    private val mollets = Move("ex-mollets-unilateraux", "Mollets unilatéraux", MuscleGroup.LEGS)
    private val tractionsPronation = Move("ex-tractions-pronation", "Tractions pronation", MuscleGroup.BACK, emphasis = true)
    private val rowingSupination = Move("ex-rowing-australien-supination", "Rowing australien supination", MuscleGroup.BACK)
    private val pompesPseudoPlanche = Move("ex-pompes-pseudo-planche-pike", "Pompes pseudo-planche ou pike", MuscleGroup.SHOULDERS)
    private val souleveDeTerre = Move(
        "ex-souleve-de-terre-roumain-unilateral",
        "Soulevé de terre roumain unilatéral",
        MuscleGroup.LEGS,
        usesWeight = true,
    )
    private val hollowBody = Move("ex-hollow-body-hold", "Hollow body hold", MuscleGroup.ABS, measure = Measure.SECONDS)
    private val sissySquat = Move("ex-sissy-squat-shrimp-squat", "Sissy squat ou shrimp squat", MuscleGroup.LEGS)
    private val pompesArcher = Move("ex-pompes-archer-diamant", "Pompes archer ou diamant", MuscleGroup.CHEST)
    private val rowing = Move("ex-rowing-australien", "Rowing australien", MuscleGroup.BACK)
    private val rollout = Move("ex-rollout-serviette", "Rollout serviette", MuscleGroup.ABS)
    private val pikePushUps = Move("ex-pike-push-ups-pieds-sureleves", "Pike push-ups, pieds surélevés", MuscleGroup.SHOULDERS, emphasis = true)
    private val tractionsSupination = Move("ex-tractions-supination", "Tractions supination", MuscleGroup.BACK)
    private val rowingCoudesEcartes = Move("ex-rowing-coudes-ecartes", "Rowing coudes écartés", MuscleGroup.SHOULDERS)
    private val curlServiette = Move("ex-curl-serviette", "Curl serviette dans une porte", MuscleGroup.BICEPS)
    private val fentes = Move("ex-fentes-marchees-lentes", "Fentes marchées lentes", MuscleGroup.LEGS)
    private val nordicCurl = Move("ex-nordic-curl", "Nordic curl", MuscleGroup.LEGS, emphasis = true)
    private val pontFessier = Move("ex-pont-fessier-unilateral-sureleve", "Pont fessier unilatéral surélevé", MuscleGroup.GLUTES)
    private val rowingLeste = Move("ex-rowing-australien-leste", "Rowing australien lesté", MuscleGroup.BACK, usesWeight = true)
    private val dipsPompesDiamant = Move("ex-dips-pompes-diamant", "Dips ou pompes diamant", MuscleGroup.TRICEPS)

    private val moves = listOf(
        pompesDeficit, rowingPiedsSureleves, squatBulgare, extensionsTriceps, relevesJambes, mollets,
        tractionsPronation, rowingSupination, pompesPseudoPlanche, souleveDeTerre, hollowBody,
        sissySquat, pompesArcher, rowing, rollout,
        pikePushUps, tractionsSupination, rowingCoudesEcartes, curlServiette, fentes,
        nordicCurl, pontFessier, rowingLeste, dipsPompesDiamant,
    )

    /** The exercises of the program, as they are added to the app. */
    fun exercises(createdAt: Long = 0L): List<Exercise> = moves.map { move ->
        Exercise(
            id = move.id,
            name = move.name,
            muscleGroup = move.group,
            usesWeight = move.usesWeight,
            measure = move.measure,
            restSeconds = if (move.emphasis) EMPHASIS_REST else REST,
            createdAt = createdAt,
        )
    }

    private class Line(val move: Move, val target: SetTarget, val emphasis: Boolean = false, val note: String = "")

    private fun line(
        move: Move,
        sets: Int,
        min: Int,
        max: Int = min,
        rir: Int? = null,
        per: String = "",
        emphasis: Boolean = false,
        note: String = "",
    ) = Line(move, SetTarget(sets, min, max, per, rir), emphasis, note)

    private fun session(day: DayOfWeek, title: String, focus: String, vararg lines: Line): TrainingDay {
        val code = day.name.take(3).lowercase()
        return TrainingDay(
            day = day,
            title = title,
            focus = focus,
            exercises = lines.mapIndexed { index, line ->
                SessionExercise(
                    id = "$code-${index + 1}",
                    exerciseId = line.move.id,
                    target = line.target,
                    emphasis = line.emphasis,
                    note = line.note,
                    restSeconds = if (line.emphasis) EMPHASIS_REST else REST,
                )
            },
        )
    }

    private fun rest(day: DayOfWeek, note: String) = TrainingDay(day, "Repos", "Récup.", note = note)

    /** The program, made of the exercises of [exercises]. */
    fun create(createdAt: Long = 0L): TrainingProgram = TrainingProgram(
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
            rest(
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
            rest(
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
