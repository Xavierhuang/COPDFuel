package com.copdhealthtracker.ui.resources

/**
 * Patient-education guides for the ten icons in Resources > Resp. Care > Medication Types.
 * General information only; wording must be reviewed by a clinician before release.
 */
object MedicationTypes {

    const val DISCLAIMER =
        "This guide is general education, not medical advice. It does not list every use, side " +
            "effect, warning or interaction, and brand names are examples only. Always follow your own " +
            "prescription and the leaflet that comes with your medicine. Ask your doctor or pharmacist " +
            "before starting, stopping or changing any medicine. If you have severe trouble breathing, " +
            "chest pain, or swelling of the face, lips or throat, call 911."

    /** Starts every example line; the pop-up gives these lines a hanging indent. */
    const val BULLET = "• "

    data class Info(
        val id: String,
        val title: String,
        val examples: List<String>,
        val whatItIs: String,
        val whatItsFor: String,
        val howItWorks: String,
        val commonForms: String,
        val howToUse: String,
        val commonSideEffects: String,
        val warnings: String,
        val interactions: String
    ) {
        /** Heading and body of each part of the guide, in reading order. */
        fun sections(): List<Pair<String, String>> = listOf(
            "Examples" to examples.joinToString("\n") { BULLET + it },
            "What it is" to whatItIs,
            "What it's for" to whatItsFor,
            "How it works" to howItWorks,
            "Common forms" to commonForms,
            "How to use" to howToUse,
            "Common side effects" to commonSideEffects,
            "Warnings" to warnings,
            "Interactions" to interactions,
            "Important" to DISCLAIMER
        )

        /** The whole guide as plain text. */
        fun message(): String = sections().joinToString("\n\n") { (heading, body) -> "$heading\n$body" }
    }

    val ALL: List<Info> = listOf(
        Info(
            id = "bronchodilators",
            title = "Bronchodilators",
            examples = listOf(
                "Short-acting (SABAs): Albuterol, Levalbuterol",
                "Short-acting (SAMAs): Ipratropium",
                "Long-acting (LABAs): Salmeterol, Formoterol, Indacaterol",
                "Long-acting (LAMAs): Tiotropium, Aclidinium, Umeclidinium"
            ),
            whatItIs = "Inhaled medicines that relax the muscles wrapped around your airways. They are " +
                "the main treatment for COPD symptoms. Short-acting types work within minutes; long-acting " +
                "types last 12 to 24 hours.",
            whatItsFor = "Short-acting (rescue) inhalers relieve sudden shortness of breath, wheezing and " +
                "chest tightness. Long-acting (maintenance) inhalers are taken every day to keep symptoms " +
                "under control and lower the chance of flare-ups.",
            howItWorks = "Beta-agonists (SABAs and LABAs) switch on receptors that tell airway muscle to " +
                "relax. Anticholinergics (SAMAs and LAMAs) block the nerve signals that tell it to tighten. " +
                "Either way the airways widen and air moves more easily.",
            commonForms = "Metered-dose inhalers (often used with a spacer), dry powder inhalers, soft mist " +
                "inhalers, and liquid for a nebulizer.",
            howToUse = "Use your rescue inhaler when you need it, as prescribed, and keep it with you. Take " +
                "long-acting inhalers at the same time every day, even when you feel well; they are not for " +
                "sudden symptoms. Ask your pharmacist or nurse to check your inhaler technique.",
            commonSideEffects = "Beta-agonists: shakiness, a fast or pounding heartbeat, nervousness, " +
                "headache, muscle cramps. Anticholinergics: dry mouth, cough, constipation, trouble passing urine.",
            warnings = "Tell your clinician if you need your rescue inhaler more often than usual or it is " +
                "not helping; this can be an early sign of a flare-up. Get urgent help for chest pain, a very " +
                "fast or irregular heartbeat, breathing that gets worse right after a dose, eye pain or blurred " +
                "vision, or being unable to pass urine.",
            interactions = "Beta-blockers (including some glaucoma eye drops) can reduce the effect. Some " +
                "antidepressants (MAOIs, tricyclics) and stimulants can add to heart effects. Water pills " +
                "(diuretics) may lower potassium further. Other anticholinergic medicines can add to dry mouth " +
                "and urinary problems."
        ),
        Info(
            id = "ics",
            title = "Inhaled Corticosteroids",
            examples = listOf("Fluticasone", "Budesonide", "Beclomethasone", "Mometasone"),
            whatItIs = "Steroid (anti-inflammatory) medicines that are breathed straight into the lungs. " +
                "They are different from the anabolic steroids misused in sport.",
            whatItsFor = "Lowering the number of flare-ups in people who have them often, who have a raised " +
                "eosinophil count, or who also have asthma. In COPD they are prescribed together with a " +
                "long-acting bronchodilator, not on their own, and they are not for quick relief.",
            howItWorks = "They calm inflammation and swelling in the lining of the airways. The benefit " +
                "builds up over days to weeks of regular use.",
            commonForms = "Metered-dose inhalers, dry powder inhalers, and budesonide liquid for a nebulizer. " +
                "In COPD they most often come inside a combination inhaler.",
            howToUse = "Take every day as prescribed, even when you feel well. Rinse your mouth, gargle and " +
                "spit after every dose. Use a spacer with a metered-dose inhaler if you have one. Do not stop " +
                "suddenly without talking to your clinician.",
            commonSideEffects = "Hoarse voice, sore throat, cough, and oral thrush (white patches in the " +
                "mouth). Rinsing after each dose makes these less likely.",
            warnings = "Inhaled steroids raise the risk of pneumonia in people with COPD: report fever, more " +
                "or discoloured mucus, or worse breathlessness. High doses over a long time can thin the bones, " +
                "raise the risk of cataracts and glaucoma, and cause easy bruising. Keep up with eye and bone " +
                "checks if your clinician advises them.",
            interactions = "Some medicines raise steroid levels in the body, including ritonavir and " +
                "cobicistat (HIV medicines), ketoconazole and itraconazole (antifungals) and clarithromycin. " +
                "Tell your clinician and pharmacist if you take any of these."
        ),
        Info(
            id = "combination",
            title = "Combination Inhalers",
            examples = listOf(
                "LABA + LAMA: Anoro Ellipta, Stiolto Respimat",
                "LABA + ICS: Advair, Symbicort, Breo Ellipta",
                "Triple Therapy: Trelegy Ellipta, Breztri Aerosphere"
            ),
            whatItIs = "One inhaler that holds two or three maintenance medicines: two long-acting " +
                "bronchodilators, a bronchodilator with an inhaled steroid, or all three.",
            whatItsFor = "Daily, long-term control when a single medicine is not enough, and to reduce " +
                "flare-ups. Having one device instead of several makes the routine simpler.",
            howItWorks = "A LABA and a LAMA relax the airway muscles in two different ways, so together they " +
                "open the airways more than either alone. An inhaled steroid adds protection against flare-ups " +
                "by reducing airway inflammation.",
            commonForms = "Dry powder inhalers (Ellipta, Diskus), metered-dose inhalers (Symbicort, Breztri, " +
                "Bevespi) and soft mist inhalers (Respimat).",
            howToUse = "Take at the same time every day, once or twice daily depending on the product. They " +
                "are not for sudden symptoms, so keep your rescue inhaler with you. Rinse your mouth and spit if " +
                "yours contains a steroid. Do not take extra doses.",
            commonSideEffects = "The side effects of the medicines inside: shakiness, fast heartbeat, " +
                "headache, dry mouth, hoarse voice, throat irritation and oral thrush.",
            warnings = "Do not use a second inhaler containing the same type of medicine (another LABA or " +
                "LAMA) unless your clinician tells you to. Products with a steroid carry the pneumonia risk " +
                "described under Inhaled Corticosteroids. Get help for chest pain, an irregular heartbeat, eye " +
                "pain, trouble passing urine, or breathing that worsens right after a dose.",
            interactions = "As for the individual medicines: beta-blockers, some antidepressants (MAOIs, " +
                "tricyclics), water pills, other anticholinergic medicines, and, for steroid-containing " +
                "inhalers, ritonavir, cobicistat, ketoconazole, itraconazole and clarithromycin."
        ),
        Info(
            id = "pde4",
            title = "Phosphodiesterase-4 (PDE4) Inhibitors",
            examples = listOf(
                "Roflumilast (Daliresp) - a once-daily tablet",
                "Ensifentrine (Ohtuvayre) - a nebulized PDE3 and PDE4 inhibitor"
            ),
            whatItIs = "Non-steroid medicines that reduce inflammation in the lungs by blocking an enzyme " +
                "called phosphodiesterase-4. They are add-on treatments, not rescue medicines.",
            whatItsFor = "Roflumilast is used for severe COPD with chronic bronchitis (daily cough and mucus) " +
                "and a history of flare-ups, to help reduce further flare-ups. Ensifentrine is a maintenance " +
                "treatment for COPD symptoms.",
            howItWorks = "Blocking PDE4 inside inflammatory cells lowers the release of substances that " +
                "drive swelling and mucus in the airways. Ensifentrine also blocks PDE3, which helps relax the " +
                "airways.",
            commonForms = "Roflumilast is a tablet. Ensifentrine is a liquid used in a standard jet " +
                "nebulizer.",
            howToUse = "Take roflumilast once a day, with or without food; some people start on a lower dose " +
                "for the first weeks. Use ensifentrine as prescribed through your nebulizer. Keep taking your " +
                "other COPD inhalers unless told otherwise.",
            commonSideEffects = "Roflumilast: diarrhea, nausea, reduced appetite, weight loss, headache, " +
                "trouble sleeping, dizziness and back pain. Stomach effects often ease after the first weeks.",
            warnings = "Tell your clinician straight away about new or worse anxiety, depression, trouble " +
                "sleeping, or thoughts of self-harm. Weigh yourself regularly and report unplanned weight loss. " +
                "Roflumilast is not suitable for people with moderate or severe liver disease.",
            interactions = "Rifampin, phenobarbital, carbamazepine and phenytoin can make roflumilast less " +
                "effective. Erythromycin, ketoconazole, fluvoxamine, cimetidine and some birth control pills can " +
                "raise its level and side effects."
        ),
        Info(
            id = "antibiotics",
            title = "Antibiotics",
            examples = listOf(
                "Azithromycin (Z-pack)",
                "Amoxicillin-clavulanate (Augmentin)",
                "Doxycycline",
                "Levofloxacin"
            ),
            whatItIs = "Medicines that treat infections caused by bacteria. They do not work against " +
                "viruses such as colds or the flu.",
            whatItsFor = "Treating bacterial chest infections and flare-ups, often signalled by more mucus, a " +
                "change in its colour, and more breathlessness. Some people with frequent flare-ups are " +
                "prescribed long-term azithromycin to help prevent them.",
            howItWorks = "They kill bacteria or stop them multiplying, giving your body the chance to clear " +
                "the infection. Azithromycin also has a mild anti-inflammatory effect in the airways.",
            commonForms = "Tablets, capsules and liquids taken by mouth. In hospital they may be given " +
                "through a vein.",
            howToUse = "Take exactly as prescribed and finish the course unless your clinician tells you to " +
                "stop. Space doses evenly. Do not save leftovers or share them. Take doxycycline with a full glass " +
                "of water while upright, and keep it 2 hours apart from dairy, antacids and iron.",
            commonSideEffects = "Nausea, diarrhea, stomach upset, rash and yeast infections. Doxycycline can " +
                "make your skin burn more easily in the sun.",
            warnings = "Hives, swelling of the face or throat, or trouble breathing is an allergic emergency: " +
                "call 911. Report severe or bloody diarrhea, even weeks later. Azithromycin and levofloxacin can " +
                "affect heart rhythm. Stop levofloxacin and call your clinician for tendon pain, numbness or " +
                "tingling, or sudden severe chest, back or belly pain.",
            interactions = "Many antibiotics increase the effect of warfarin. Antacids, calcium, iron and " +
                "magnesium block absorption of doxycycline and levofloxacin. Some should not be combined with " +
                "other medicines that affect heart rhythm. Ciprofloxacin, clarithromycin and erythromycin raise " +
                "theophylline levels."
        ),
        Info(
            id = "systemic",
            title = "Systemic Corticosteroids",
            examples = listOf(
                "Prednisone",
                "Methylprednisolone",
                "Dexamethasone",
                "Used short-term during exacerbations"
            ),
            whatItIs = "Steroid medicines taken as tablets or liquid, or given by injection, so they act " +
                "throughout the whole body rather than only in the lungs.",
            whatItsFor = "Treating COPD flare-ups. A short course, usually around five days, can speed " +
                "recovery and improve breathing. They are generally not used long term for COPD because of " +
                "side effects.",
            howItWorks = "They strongly reduce inflammation everywhere in the body, including the swollen " +
                "airways that make a flare-up so hard to breathe through.",
            commonForms = "Tablets and liquids by mouth; injections or a drip in hospital.",
            howToUse = "Take in the morning with food, exactly as directed, and follow your action plan. If " +
                "you have taken steroids for more than a few weeks, do not stop suddenly; your clinician will " +
                "reduce the dose gradually.",
            commonSideEffects = "Bigger appetite, trouble sleeping, mood changes, stomach upset, fluid " +
                "retention and higher blood sugar.",
            warnings = "Repeated or long courses can cause thin bones, muscle weakness, cataracts, thin skin, " +
                "high blood pressure, diabetes and a higher risk of infection. Call your clinician for black or " +
                "bloody stools, severe mood changes, signs of infection or changes in vision. If you have " +
                "diabetes, check your blood sugar more often.",
            interactions = "Anti-inflammatory painkillers such as ibuprofen and naproxen raise the risk of " +
                "stomach bleeding. Diabetes medicines may need adjusting. They can affect warfarin, lower " +
                "potassium further with water pills, and live vaccines may need to be delayed."
        ),
        Info(
            id = "methylxanthines",
            title = "Methylxanthines",
            examples = listOf(
                "Theophylline (Theo-24, Elixophyllin)",
                "Older class of bronchodilators",
                "Used less frequently due to side effects"
            ),
            whatItIs = "An older type of bronchodilator taken by mouth. It is chemically related to " +
                "caffeine.",
            whatItsFor = "An add-on for people whose symptoms are not controlled by inhalers, or who cannot " +
                "use them. It is prescribed much less often today.",
            howItWorks = "It relaxes the airway muscles, may strengthen the breathing muscles, and has a " +
                "mild anti-inflammatory effect.",
            commonForms = "Extended-release tablets and capsules, and liquid. A related medicine, " +
                "aminophylline, can be given through a vein in hospital.",
            howToUse = "Take at the same times each day and in the same way with respect to food. Swallow " +
                "extended-release forms whole. Have blood tests when asked, and do not switch brands without " +
                "advice. Limit coffee, tea, cola and energy drinks.",
            commonSideEffects = "Nausea, stomach upset, heartburn, headache, trouble sleeping, feeling " +
                "jittery, and passing more urine.",
            warnings = "The helpful dose is close to the harmful dose. Get urgent help for repeated vomiting, " +
                "a fast or irregular heartbeat, confusion or a seizure. Tell your clinician if you stop or start " +
                "smoking, have a fever, or develop heart or liver problems, because these change the level in " +
                "your blood.",
            interactions = "Many medicines change theophylline levels. Ciprofloxacin, erythromycin, " +
                "clarithromycin, cimetidine, fluvoxamine and allopurinol raise it. Rifampin, carbamazepine, " +
                "phenytoin, phenobarbital, St John's wort and smoking lower it. Caffeine adds to side effects."
        ),
        Info(
            id = "mucolytics",
            title = "Mucolytics/Expectorants",
            examples = listOf(
                "N-acetylcysteine (NAC)",
                "Carbocysteine",
                "Guaifenesin",
                "Help thin and loosen mucus"
            ),
            whatItIs = "Medicines that change mucus. Mucolytics make it thinner and less sticky; " +
                "expectorants add water to it so it is easier to cough up.",
            whatItsFor = "Thick, sticky mucus that is hard to clear, especially with chronic bronchitis. " +
                "Taken regularly, some mucolytics may slightly lower the number of flare-ups in certain people.",
            howItWorks = "Mucolytics break the chemical bonds that make mucus thick. Expectorants increase " +
                "the fluid in airway secretions so your airways' natural clearing action and coughing can move it.",
            commonForms = "Tablets, capsules, effervescent tablets and liquids. Acetylcysteine also comes " +
                "as a solution for a nebulizer.",
            howToUse = "Take as directed and drink plenty of fluids unless you have been told to limit them. " +
                "They work best alongside airway clearance techniques or devices. With nebulized acetylcysteine " +
                "you may be told to use a bronchodilator first.",
            commonSideEffects = "Nausea, stomach upset, vomiting and diarrhea. Nebulized acetylcysteine can " +
                "cause cough, throat irritation, a runny nose and an unpleasant smell.",
            warnings = "Nebulized acetylcysteine can tighten the airways in some people; stop and use your " +
                "rescue inhaler if breathing worsens. Tell your clinician if you have had a stomach ulcer. Many " +
                "cough and cold products mix guaifenesin with decongestants that can raise blood pressure, so " +
                "read labels.",
            interactions = "Acetylcysteine taken with nitroglycerin can cause low blood pressure and " +
                "headache. Cough suppressants can work against these medicines by stopping you clearing mucus. " +
                "Check combination cold products with your pharmacist."
        ),
        Info(
            id = "biologics",
            title = "Biologics",
            examples = listOf(
                "Dupilumab (Dupixent) (taken once every 2 weeks)",
                "Mepolizumab (Nucala) (taken once every 4 weeks) - for eosinophilic COPD",
                "Newer targeted therapies"
            ),
            whatItIs = "Injected medicines made from antibodies that block one specific part of the immune " +
                "system. They are a newer option for a particular type of COPD.",
            whatItsFor = "An add-on for adults whose COPD is not controlled by inhaled triple therapy and who " +
                "have a raised eosinophil count (a type of white blood cell). Your clinician uses blood tests to " +
                "see whether one may help.",
            howItWorks = "Dupilumab blocks the signals of two messengers, interleukin-4 and interleukin-13. " +
                "Mepolizumab blocks interleukin-5, which eosinophils depend on. Both reduce the kind of " +
                "inflammation that drives flare-ups in this type of COPD.",
            commonForms = "A prefilled pen or syringe injected under the skin, every 2 or 4 weeks depending " +
                "on the medicine. It can be given at a clinic, or at home after training.",
            howToUse = "Store in the refrigerator and follow the leaflet for warming and injecting. Change " +
                "the injection site each time. Keep taking your inhalers, and do not stop steroid medicines " +
                "suddenly. They do not treat sudden breathing problems.",
            commonSideEffects = "Redness, swelling or pain where injected, headache, back or joint pain, " +
                "cold-like symptoms, and with dupilumab, red or irritated eyes.",
            warnings = "Serious allergic reactions can happen: get emergency help for hives, swelling of the " +
                "face or throat, faintness or trouble breathing. Report new or worsening eye problems. " +
                "Existing parasitic (worm) infections should be treated first. Shingles has been reported with " +
                "mepolizumab.",
            interactions = "No major interactions with other medicines are known. Live vaccines should be " +
                "avoided while taking dupilumab. Tell every clinician and pharmacist that you are on a biologic."
        ),
        Info(
            id = "nebulizer",
            title = "Nebulizer Medications",
            examples = listOf(
                "Albuterol nebulizer solution",
                "Ipratropium nebulizer solution",
                "Budesonide (Pulmicort Respules)",
                "Combination: Albuterol + Ipratropium (DuoNeb)"
            ),
            whatItIs = "Liquid medicines that a nebulizer machine turns into a fine mist, breathed in " +
                "through a mouthpiece or mask over several minutes.",
            whatItsFor = "People who find inhalers hard to use because of weak breath, arthritis, poor " +
                "coordination or memory problems, and for treatment during a flare-up when taking a deep breath " +
                "is difficult.",
            howItWorks = "The machine breaks the liquid into tiny droplets that reach the lungs with normal, " +
                "relaxed breathing. The medicine then works just as its inhaler version does.",
            commonForms = "Single-dose plastic vials used with a jet, mesh or ultrasonic nebulizer, with a " +
                "mouthpiece or a face mask.",
            howToUse = "Wash your hands, sit upright, and breathe normally until the mist stops, usually 5 to " +
                "15 minutes. Rinse your mouth after a steroid. Wash and air-dry the parts after each use, " +
                "disinfect them regularly, and replace parts and filters as the maker advises. Only mix " +
                "medicines if your pharmacist says it is safe.",
            commonSideEffects = "The same as the medicine in inhaler form: shakiness and a fast heartbeat " +
                "with albuterol, dry mouth with ipratropium, hoarse voice and thrush with budesonide.",
            warnings = "A dirty nebulizer can cause lung infections, so clean it as instructed. Mist from " +
                "ipratropium leaking into the eyes can cause blurred vision or trigger glaucoma; use a mouthpiece " +
                "or a well-fitting mask. Tell your clinician if you need treatments more often than usual.",
            interactions = "The same as for the inhaled forms. Do not double up with an inhaler containing " +
                "the same type of medicine unless told to, and check with your pharmacist before mixing two " +
                "solutions in one cup."
        )
    )

    fun forId(id: String): Info? = ALL.firstOrNull { it.id == id }
}
