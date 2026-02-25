# BAAs: Where They Live and What to Do (No Submission Required)

You do **not** submit BAAs to the government or any central registry. You obtain them, sign them, and keep them on file.

---

## 1. AWS BAA (you and AWS)

**What it is:** A Business Associate Agreement between you (or the account owner) and Amazon Web Services. AWS acts as your business associate when you process PHI in their HIPAA-eligible services.

**Do you submit it?** **No.** AWS already has the BAA. You do not send anything to AWS or to the government.

**What you do:**

1. Log into the **AWS account** used for the COPD app.
2. Open **AWS Artifact** (in the console search, type “Artifact”).
3. In Artifact, find the **HIPAA Eligibility and BAA** (or equivalent) offering.
4. **Accept** or **sign** the BAA for that account (follow the on-screen steps).
5. **Download and save** a copy of the accepted BAA in a secure place (e.g. your or your client’s compliance/legal folder).

**Note:** The BAA is tied to the AWS account. If you use a different account later, you must accept the BAA for that account too.

---

## 2. Practice BAAs (client and each medical practice)

**What it is:** A Business Associate Agreement between the **covered entity** (the medical practice) and the **business associate** (the entity operating the app—usually your client’s company). It allows the app operator to receive and process PHI for that practice’s patients.

**Do you submit it?** **No.** There is no place to “submit” a BAA to HIPAA, HHS, or any regulator. Signed BAAs are **contracts you keep**, not forms you file.

**What the client does:**

1. **Before** any PHI from that practice’s patients is used in the app, the client (or her company) gets a signed BAA with that practice.
2. Either: (a) use the practice’s own BAA template if they have one, or (b) use a draft like **docs/BAA_TEMPLATE_PRACTICE_TO_BA.md** (after a lawyer reviews it), or (c) have the client’s attorney draft one.
3. Both parties sign (practice = Covered Entity, app operator = Business Associate).
4. **Store** the signed BAA securely (e.g. in the client’s legal or compliance files). Keep it for the duration of the relationship and for a reasonable period after (e.g. 6 years, or as your attorney advises).
5. If the relationship ends, the BAA’s return/destruction and survival clauses still apply; the client does not “submit” anything when that happens.

**Your role:** You don’t sign practice BAAs unless you are the business associate (e.g. your company is the app operator). If the client’s company is the business associate, she signs and stores them. You can remind her that each practice needs a BAA before go-live and that there’s a draft template in the repo for her lawyer to review.

---

## Summary

| BAA | Submit somewhere? | What to do |
|-----|-------------------|------------|
| **AWS BAA** | No | Accept/sign in AWS Artifact; download and keep a copy. |
| **Practice BAAs** | No | Get signed with each practice; store securely. No filing with the government. |

HIPAA does not require BAAs to be filed with HHS or anyone else. Regulators may ask to see them during an audit or investigation, which is why you keep signed copies in a secure, organized way.
