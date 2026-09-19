package com.copdhealthtracker.ui.resources

/**
 * Plain-language explanations for the ten icons under Resp. Care > Medication Types.
 * General education only; wording should be reviewed by a clinician before release.
 */
object MedicationTypes {

    const val SAFETY_NOTE =
        "This is general information, not medical advice. Take your medicines as prescribed and " +
            "talk to your clinician before starting, stopping or changing any of them."

    data class Info(
        val id: String,
        val title: String,
        val whatItIs: String,
        val howItHelps: String,
        val examples: List<String>
    ) {
        /** The text shown in the pop-up when the icon is tapped. */
        fun message(): String = buildString {
            append("What it is\n").append(whatItIs).append("\n\n")
            append("How it helps\n").append(howItHelps).append("\n\n")
            append("Examples\n")
            examples.forEach { append("• ").append(it).append("\n") }
            append("\n").append(SAFETY_NOTE)
        }
    }

    val ALL: List<Info> = listOf(
        Info(
            id = "bronchodilators",
            title = "Bronchodilators",
            whatItIs = "Inhaled medicines that relax the muscles wrapped around your airways. " +
                "Short-acting ones work within minutes and are used for quick relief (rescue). " +
                "Long-acting ones are taken every day to keep the airways open.",
            howItHelps = "Opens the airways so air moves in and out more easily, easing shortness of " +
                "breath, wheezing and chest tightness.",
            examples = listOf(
                "Short-acting (SABAs): Albuterol, Levalbuterol",
                "Short-acting (SAMAs): Ipratropium",
                "Long-acting (LABAs): Salmeterol, Formoterol, Indacaterol",
                "Long-acting (LAMAs): Tiotropium, Aclidinium, Umeclidinium"
            )
        ),
        Info(
            id = "ics",
            title = "Inhaled Corticosteroids",
            whatItIs = "Steroid medicines breathed straight into the lungs. In COPD they are usually " +
                "prescribed together with a long-acting bronchodilator rather than on their own.",
            howItHelps = "Reduces swelling and irritation in the airways and can lower the number of " +
                "flare-ups. Rinse your mouth after each use to help prevent thrush.",
            examples = listOf("Fluticasone", "Budesonide", "Beclomethasone", "Mometasone")
        ),
        Info(
            id = "combination",
            title = "Combination Inhalers",
            whatItIs = "One inhaler that holds two or three medicines, such as two long-acting " +
                "bronchodilators, or a bronchodilator with an inhaled corticosteroid.",
            howItHelps = "Gives the benefit of several medicines in a single device, which is simpler " +
                "to use each day and can control symptoms better than one medicine alone.",
            examples = listOf(
                "LABA + LAMA: Anoro Ellipta, Stiolto Respimat",
                "LABA + ICS: Advair, Symbicort, Breo Ellipta",
                "Triple Therapy: Trelegy Ellipta, Breztri Aerosphere"
            )
        ),
        Info(
            id = "pde4",
            title = "Phosphodiesterase-4 (PDE4) Inhibitors",
            whatItIs = "A tablet taken once a day that lowers inflammation in the lungs. It is not a " +
                "rescue medicine and does not open the airways right away.",
            howItHelps = "Helps reduce flare-ups in people with severe COPD who have chronic bronchitis " +
                "and frequent exacerbations.",
            examples = listOf(
                "Roflumilast (Daliresp)",
                "Used for severe COPD with chronic bronchitis",
                "Helps reduce exacerbations"
            )
        ),
        Info(
            id = "antibiotics",
            title = "Antibiotics",
            whatItIs = "Medicines that treat infections caused by bacteria. They do not work against " +
                "viruses such as colds or the flu.",
            howItHelps = "Treats bacterial chest infections, a common cause of COPD flare-ups. Some " +
                "people are prescribed a long-term antibiotic to help prevent flare-ups.",
            examples = listOf(
                "Azithromycin (Z-pack)",
                "Amoxicillin-clavulanate (Augmentin)",
                "Doxycycline",
                "Levofloxacin"
            )
        ),
        Info(
            id = "systemic",
            title = "Systemic Corticosteroids",
            whatItIs = "Steroid medicines taken as tablets or given by injection, so they act " +
                "throughout the whole body rather than only in the lungs.",
            howItHelps = "Quickly calms airway inflammation during a flare-up. Usually given as a short " +
                "course of a few days, because long-term use can cause side effects.",
            examples = listOf(
                "Prednisone",
                "Methylprednisolone",
                "Dexamethasone",
                "Used short-term during exacerbations"
            )
        ),
        Info(
            id = "methylxanthines",
            title = "Methylxanthines",
            whatItIs = "An older type of bronchodilator taken by mouth as a tablet, capsule or liquid.",
            howItHelps = "Relaxes the airways and may help the breathing muscles. It is used less often " +
                "today because of side effects and the need for blood tests to check the level.",
            examples = listOf(
                "Theophylline (Theo-24, Elixophyllin)",
                "Older class of bronchodilators",
                "Used less frequently due to side effects"
            )
        ),
        Info(
            id = "mucolytics",
            title = "Mucolytics/Expectorants",
            whatItIs = "Medicines that change the thickness of mucus. Mucolytics thin it, and " +
                "expectorants help you cough it up.",
            howItHelps = "Makes thick, sticky mucus easier to clear from the lungs, which can ease " +
                "coughing and chest congestion.",
            examples = listOf(
                "N-acetylcysteine (NAC)",
                "Carbocysteine",
                "Guaifenesin",
                "Help thin and loosen mucus"
            )
        ),
        Info(
            id = "biologics",
            title = "Biologics Medications for COPD",
            whatItIs = "Newer injected medicines made from antibodies that block specific signals " +
                "driving inflammation. They are for certain people whose COPD involves a high " +
                "level of eosinophils, a type of white blood cell.",
            howItHelps = "Added to regular inhalers, they can lower the number of flare-ups in people " +
                "with this type of COPD. Your clinician uses blood tests to decide if one may help.",
            examples = listOf(
                "Mepolizumab (Nucala) - for eosinophilic COPD",
                "Benralizumab (Fasenra)",
                "Dupilumab (Dupixent)",
                "Newer targeted therapies"
            )
        ),
        Info(
            id = "nebulizer",
            title = "Nebulizer Medications",
            whatItIs = "Liquid medicines that a nebulizer machine turns into a fine mist, breathed in " +
                "through a mouthpiece or mask over several minutes.",
            howItHelps = "Delivers the same kinds of medicine as inhalers. It is useful for people who " +
                "find inhalers hard to use, or during a flare-up when breathing deeply is difficult.",
            examples = listOf(
                "Albuterol nebulizer solution",
                "Ipratropium nebulizer solution",
                "Budesonide (Pulmicort Respules)",
                "Combination: Albuterol + Ipratropium (DuoNeb)"
            )
        )
    )

    fun forId(id: String): Info? = ALL.firstOrNull { it.id == id }
}
