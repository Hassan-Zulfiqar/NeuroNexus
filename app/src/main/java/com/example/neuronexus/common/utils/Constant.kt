package com.example.neuronexus.common.utils

import com.example.neuronexus.common.models.Symptoms
import com.example.neuronexus.doctor.models.AnalyzeResponse

object Constant {

    var analyzeResponse: AnalyzeResponse? = null
    var isFromNoti = false

    fun getSymptomsData(): ArrayList<Symptoms> {

        val symptomsList = ArrayList<Symptoms>()

        symptomsList.add(
            Symptoms(
                "Do you experience frequent or persistent headaches?",
                "Persistent or progressively worsening headaches are a common symptom of brain tumors, often caused by increased pressure inside the skull.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do your headaches feel worse in the morning?",
                "Headaches that are more severe in the morning may be linked to increased intracranial pressure during sleep.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you experienced seizures recently?",
                "Seizures are one of the most common early signs of brain tumors, occurring due to abnormal electrical activity in the brain.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you have blurred or double vision?",
                "Visual disturbances can occur when tumors affect the optic pathways or increase pressure in the brain.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you experienced sudden or partial vision loss?",
                "Vision loss may indicate compression of the optic nerve or visual centers in the brain.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you feel frequent nausea or vomiting without a clear reason?",
                "Unexplained nausea or vomiting, especially in the morning, can result from increased pressure inside the skull.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you often feel unusually drowsy or extremely fatigued?",
                "Persistent fatigue or drowsiness may occur due to brain dysfunction or pressure effects caused by a tumor.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you noticed problems with memory or confusion?",
                "Brain tumors can affect cognitive functions, leading to memory loss, confusion, or difficulty concentrating.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you have difficulty speaking or understanding speech?",
                "Tumors affecting language areas can cause slurred speech or trouble understanding others.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you experienced weakness or numbness in one side of your body?",
                "Unilateral weakness or numbness may occur when tumors affect motor or sensory areas of the brain.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you have problems with balance or coordination?",
                "Difficulty walking, balance issues, or poor coordination may indicate involvement of the cerebellum or brainstem.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you noticed changes in your personality or behavior?",
                "Personality changes, mood swings, or unusual behavior can occur when tumors affect the frontal lobe.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you have difficulty hearing or ringing in your ears?",
                "Hearing problems or tinnitus may occur if tumors affect auditory nerves or brain regions.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you experienced dizziness or a spinning sensation?",
                "Dizziness or vertigo may result from tumors affecting balance-related areas of the brain.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you have trouble swallowing?",
                "Difficulty swallowing may occur if tumors affect nerves controlling throat muscles.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you experienced frequent unexplained falls?",
                "Frequent falls may indicate problems with coordination, balance, or muscle control.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Do you experience weakness in your face or drooping on one side?",
                "Facial weakness or drooping can occur if tumors affect cranial nerves or motor areas.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        symptomsList.add(
            Symptoms(
                "Have you noticed hormonal changes (e.g., weight gain, irregular periods)?",
                "Tumors in the pituitary gland can disrupt hormone production, leading to noticeable body changes.",
                "Yes", "Yes", "No", "Sometimes"
            )
        )

        return symptomsList
    }
}