# iOS Parity P3.F (Login, Sign-up, HIPAA, Paywall) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match Android copy, validation order and flows for Login (`LoginActivity`), Sign-up (`SignUpActivity`), the HIPAA authorization screen (`HipaaAuthorizationActivity` + `hipaa_authorization_text.xml` + `strings.xml`) including its storage keys and the signature-pad Clear bug, and the Paywall (`PaywallActivity` + `activity_paywall.xml` + `BillingManager` messages).

**Architecture:** Four view rewrites (`LoginView`, `SignUpView`, `HipaaAuthorizationView`, `PaywallView`), a key/migration change in `HipaaConsentStorage`, a `clearTrigger` on `SignaturePadView`, a price change in `COPDFuel.storekit`. `AuthService` is untouched (its friendly Cognito messages are kept per spec §1.4).

**Tech Stack:** SwiftUI, Amplify Auth via `AuthService`, StoreKit 2 via `StoreManager`.

**Spec:** `docs/superpowers/specs/2026-09-19-ios-parity-p3-design.md` §1.4 (remove "Forgot password?", keep friendly messages), §1.5 (HIPAA keys), §3 row P3.F. Gap list: `docs/superpowers/specs/2026-09-19-ios-parity-p3/audit-profile-report-auth.md` §C1–C5, §D3–D4, §E1–E12.

## Global Constraints

- Copy verbatim; Android inline error TextViews stay inline on iOS (login/sign-up/HIPAA errors); Android `Toast`s use `ToastCenter` (paywall, HIPAA save/revoke). Sheets that host toasts get `.toastOverlay()`.
- Keys (spec §1.5): `hipaa_consent_signed`, `hipaa_consent_date` (seconds since 1970; ms-vs-s is internal), `hipaa_consent_printed_name`, `hipaa_consent_dob`, `hipaa_consent_expiry_type`, `hipaa_consent_expiry_date`, `hipaa_revoked`, `hipaa_revoked_date`. One-shot migration from `hipaa_consent_revoked`, `hipaa_consent_revoked_date`, `hipaa_consent_name`.
- Depends on P3.0 (`ToastCenter`). `ProfileView` (P3.E) reads `hipaaStorage.getRevokedDate()` for "Revoked on …".
- MemberImportVisibility: keep `import Combine` in `HipaaConsentStorage.swift`.
- Build then one commit at the end; `/usr/bin/git`; commands from the parent repo root.

---

### Task 1: Login (audit D3)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/LoginView.swift`

- [ ] **Step 1: Header** — in `headerSection` delete the `Text("Welcome back")` block (3 modifiers), keep the logo, wordmark and "Sign in to sync your health data".

- [ ] **Step 2: Field hints** — change `TextField("you@example.com", …)` to `TextField("Email", …)`; change both password fields' placeholder `"Enter your password"` to `"Password"`. Delete the two small caption labels `Text("Email")` / `Text("Password")` above the fields (Android has hints only).

- [ ] **Step 3: Button + validation** — replace `.disabled(isSigningIn || email.isEmpty || password.isEmpty)` with `.disabled(isSigningIn)`, and replace `signIn()` with:
```swift
    /// LoginActivity.doSignIn: both empty → "Enter email and password";
    /// failure → message (friendly Cognito mapping kept) or "Sign in failed".
    private func signIn() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        if trimmedEmail.isEmpty || password.isEmpty {
            errorMessage = "Enter email and password"
            return
        }
        errorMessage = nil
        isSigningIn = true
        Task {
            do {
                try await auth.signIn(email: trimmedEmail, password: password)
                await auth.checkSession()
            } catch {
                await MainActor.run {
                    let msg = AuthService.userMessage(for: error)
                    errorMessage = msg.isEmpty ? "Sign in failed" : msg
                }
            }
            await MainActor.run { isSigningIn = false }
        }
    }
```

- [ ] **Step 4: Check** — `grep -n "Welcome back\|Forgot\|you@example.com" COPDFuel/COPDFuel/Views/LoginView.swift` → no output.

---

### Task 2: Sign-up (audit D4)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/SignUpView.swift`

- [ ] **Step 1: Placeholders instead of captions** — delete the four caption `Text(...)` labels ("Name (optional)", "Email", "Password", "Confirm password") and the "Verification code" caption; set the placeholders to `"Name (optional)"`, `"Email"`, `"Password"`, `"Confirm password"`, `"Verification code"`. Drop `.multilineTextAlignment(.center)` / `.font(.title2.monospacedDigit())` on the code field (Android is a plain field).

- [ ] **Step 2: Title** — the title `Text` always reads `"Create account"` (Android never changes it; only the subtitle and button change). Keep the subtitle switch and the "Sign up"/"Confirm" button text.

- [ ] **Step 3: Button enabled** — replace the long `.disabled(...)` with `.disabled(isBusy)`.

- [ ] **Step 4: Validation order and messages** — replace `signUp()` and `confirmCode()`:
```swift
    /// SignUpActivity.submit (:50-73) — first failing rule wins.
    private func signUp() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"
        if trimmedEmail.isEmpty { errorMessage = "Email is required."; return }
        if trimmedEmail.range(of: emailRegex, options: .regularExpression) == nil { errorMessage = "Email is not valid."; return }
        if password.isEmpty { errorMessage = "Password is required."; return }
        if password.count < 8 { errorMessage = "Password must be at least 8 characters."; return }
        if password != confirmPassword { errorMessage = "Passwords do not match."; return }
        errorMessage = nil
        isBusy = true
        Task {
            do {
                let nameToPass = name.trimmingCharacters(in: .whitespaces)
                let needsCode = try await auth.signUp(email: trimmedEmail, password: password, name: nameToPass.isEmpty ? nil : nameToPass)
                await MainActor.run {
                    needsConfirmation = needsCode
                    if !needsCode { trySignInAfterSignUp() }
                }
            } catch {
                await MainActor.run {
                    let msg = AuthService.userMessage(for: error)
                    errorMessage = msg.isEmpty ? "Sign up failed" : msg
                }
            }
            await MainActor.run { isBusy = false }
        }
    }

    /// SignUpActivity.confirmCode (:105-130).
    private func confirmCode() {
        let trimmedEmail = email.trimmingCharacters(in: .whitespaces)
        let code = confirmationCode.trimmingCharacters(in: .whitespaces)
        if code.isEmpty { errorMessage = "Verification code is required."; return }
        if code.count != 6 || !code.allSatisfy(\.isNumber) { errorMessage = "Verification code must be 6 digits."; return }
        errorMessage = nil
        isBusy = true
        Task {
            do {
                try await auth.confirmSignUp(email: trimmedEmail, code: code)
                await MainActor.run { trySignInAfterSignUp() }
            } catch {
                await MainActor.run {
                    let msg = AuthService.userMessage(for: error)
                    errorMessage = msg.isEmpty ? "Confirmation failed" : msg
                }
            }
            await MainActor.run { isBusy = false }
        }
    }
```
In `trySignInAfterSignUp`'s catch use `"Sign in failed"` as the empty-message fallback the same way.

- [ ] **Step 5: "Use a different email"** — replace the button action body with (Android :28-42 clears only the code and error; typed values stay):
```swift
                        needsConfirmation = false
                        confirmationCode = ""
                        errorMessage = nil
```

- [ ] **Step 6: Check** — `grep -c "Passwords do not match\.\|Password must be at least 8 characters\.\|Verification code must be 6 digits\." COPDFuel/COPDFuel/Views/SignUpView.swift` → `3`.

---

### Task 3: HIPAA storage keys + migration; signature Clear (audit E10, E11)

**Files:**
- Modify: `COPDFuel/COPDFuel/Services/HipaaConsentStorage.swift` (whole file)
- Modify: `COPDFuel/COPDFuel/Views/SignaturePadView.swift` (add `clearTrigger`)

**Interfaces (produced):**
- `HipaaConsentStorage`: `hasValidConsent()`, `isRevoked()`, `getConsentDate() -> Date?`, `getRevokedDate() -> Date?`, `saveConsent(name:dob:expiryType:expiryDate:)`, `revokeConsent()`, `consentStatus`.
- `SignaturePadView(isEmpty: Binding<Bool>, clearTrigger: Int)`: increments of `clearTrigger` clear the strokes.

- [ ] **Step 1: Replace `HipaaConsentStorage.swift`**

```swift
//
//  HipaaConsentStorage.swift
//  COPDFuel
//
//  HIPAA consent + revocation in UserDefaults on Android's key names
//  (utils/HipaaConsentStorage.kt). Dates are stored as seconds since 1970
//  (Android stores ms; internal only).
//

import Combine
import Foundation

class HipaaConsentStorage: ObservableObject {
    static let shared = HipaaConsentStorage()

    private let signedKey = "hipaa_consent_signed"
    private let dateKey = "hipaa_consent_date"
    private let printedNameKey = "hipaa_consent_printed_name"
    private let dobKey = "hipaa_consent_dob"
    private let expiryTypeKey = "hipaa_consent_expiry_type"
    private let expiryDateKey = "hipaa_consent_expiry_date"
    private let revokedKey = "hipaa_revoked"
    private let revokedDateKey = "hipaa_revoked_date"

    @Published var consentStatus: ConsentStatus = .notSigned

    enum ConsentStatus: Equatable {
        case notSigned
        case signed(date: Date)
        case revoked
    }

    private init() {
        migrateLegacyKeysIfNeeded()
        refreshStatus()
    }

    /// Pre-P3 iOS keys → Android names (one shot).
    private func migrateLegacyKeysIfNeeded() {
        let d = UserDefaults.standard
        if d.object(forKey: revokedKey) == nil, d.object(forKey: "hipaa_consent_revoked") != nil {
            let wasRevoked = d.bool(forKey: "hipaa_consent_revoked")
            d.set(wasRevoked, forKey: revokedKey)
            if wasRevoked {
                // Android keeps signed=true after a revoke.
                d.set(true, forKey: signedKey)
                let rd = d.double(forKey: "hipaa_consent_revoked_date")
                if rd > 0 { d.set(rd, forKey: revokedDateKey) }
            }
        }
        if d.object(forKey: printedNameKey) == nil, let name = d.string(forKey: "hipaa_consent_name") {
            d.set(name, forKey: printedNameKey)
        }
        d.removeObject(forKey: "hipaa_consent_revoked")
        d.removeObject(forKey: "hipaa_consent_revoked_date")
        d.removeObject(forKey: "hipaa_consent_name")
    }

    func refreshStatus() {
        let d = UserDefaults.standard
        let signed = d.bool(forKey: signedKey)
        let revoked = d.bool(forKey: revokedKey)
        if signed && !revoked {
            let seconds = d.double(forKey: dateKey)
            consentStatus = .signed(date: seconds > 0 ? Date(timeIntervalSince1970: seconds) : Date())
        } else if revoked {
            consentStatus = .revoked
        } else {
            consentStatus = .notSigned
        }
    }

    /// Android: signed && !revoked.
    func hasValidConsent() -> Bool {
        if case .signed = consentStatus { return true }
        return false
    }

    func isRevoked() -> Bool {
        if case .revoked = consentStatus { return true }
        return false
    }

    func getConsentDate() -> Date? {
        if case .signed(let date) = consentStatus { return date }
        return nil
    }

    func getRevokedDate() -> Date? {
        guard isRevoked() else { return nil }
        let seconds = UserDefaults.standard.double(forKey: revokedDateKey)
        return seconds > 0 ? Date(timeIntervalSince1970: seconds) : nil
    }

    func getPrintedName() -> String { UserDefaults.standard.string(forKey: printedNameKey) ?? "" }

    /// Android saveConsent: signed=true, date=now, fields, revoked=false, revoked_date removed.
    func saveConsent(name: String, dob: String, expiryType: String, expiryDate: Date?) {
        let d = UserDefaults.standard
        d.set(true, forKey: signedKey)
        d.set(Date().timeIntervalSince1970, forKey: dateKey)
        d.set(name.trimmingCharacters(in: .whitespaces), forKey: printedNameKey)
        d.set(dob.trimmingCharacters(in: .whitespaces), forKey: dobKey)
        d.set(expiryType, forKey: expiryTypeKey)
        d.set(expiryDate?.timeIntervalSince1970 ?? 0, forKey: expiryDateKey)
        d.set(false, forKey: revokedKey)
        d.removeObject(forKey: revokedDateKey)
        refreshStatus()
    }

    /// Android revokeConsent: revoked=true + date; signed stays true.
    func revokeConsent() {
        let d = UserDefaults.standard
        d.set(true, forKey: revokedKey)
        d.set(Date().timeIntervalSince1970, forKey: revokedDateKey)
        refreshStatus()
    }
}
```

- [ ] **Step 2: `SignaturePadView` clear trigger**

Replace the struct's stored properties/init/`updateUIView`/`clear()` so that:
```swift
struct SignaturePadView: UIViewRepresentable {
    @Binding var isEmpty: Bool
    /// Bump this value to clear the strokes (SwiftUI cannot call into the UIView directly).
    var clearTrigger: Int = 0

    init(isEmpty: Binding<Bool>, clearTrigger: Int = 0) {
        _isEmpty = isEmpty
        self.clearTrigger = clearTrigger
    }

    func makeUIView(context: Context) -> SignaturePadUIView {
        let padView = SignaturePadUIView()
        padView.delegate = context.coordinator
        padView.backgroundColor = .secondarySystemBackground
        padView.layer.cornerRadius = 8
        padView.layer.borderWidth = 1
        padView.layer.borderColor = UIColor.systemGray4.cgColor
        return padView
    }

    func updateUIView(_ uiView: SignaturePadUIView, context: Context) {
        if context.coordinator.lastClearTrigger != clearTrigger {
            context.coordinator.lastClearTrigger = clearTrigger
            uiView.clear()
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }

    class Coordinator: NSObject, SignaturePadUIViewDelegate {
        var parent: SignaturePadView
        var lastClearTrigger = 0
        init(parent: SignaturePadView) { self.parent = parent }
        func signaturePadDidChange(isEmpty: Bool) {
            DispatchQueue.main.async { self.parent.isEmpty = isEmpty }
        }
    }
}
```
(Delete the old `private let padView`, `onClear`, and `func clear()`. `SignaturePadUIView` is unchanged.) Also, in `Coordinator`, `parent` must be updated on each `updateUIView`: add `context.coordinator.parent = self` as the first line of `updateUIView` so the `isEmpty` binding stays current.

---

### Task 4: HIPAA authorization screen (audit E1–E9)

**Files:**
- Rewrite: `COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift`

- [ ] **Step 1: Replace the file**

```swift
//
//  HipaaAuthorizationView.swift
//  COPDFuel
//
//  HIPAA Authorization for PHI Disclosure. Order, copy, validation and
//  states mirror Android HipaaAuthorizationActivity + activity_hipaa_authorization.xml
//  + hipaa_authorization_text.xml + strings.xml.
//

import SwiftUI

enum HipaaCopy {
    static let title = "HIPAA Authorization for PHI Disclosure"

    /// hipaa_authorization_text.xml `hipaa_full_text` (verbatim; "\n" → newlines).
    static let fullText = """
    Digital HIPAA Authorization for Disclosure of Personal Health Information (PHI)

    By checking the box and electronically signing below, I provide my express written authorization for COPD Fuel ("Company") to use and disclose my Personal Health Information (PHI) as described in this Authorization.

    1. Persons Authorized to Receive Information
    I authorize COPD Fuel to disclose my PHI to the individual(s) I designate, which may include:
    - My physician or other health care provider
    - A family member or relative
    - A caregiver or other person I choose
    I may identify specific individuals within the app or through a separate written designation.

    2. Information Authorized for Disclosure
    I authorize COPD Fuel to disclose the following categories of my PHI, including related records, data, communications, or documentation (check all that apply):
    - Diagnoses, medical conditions, and symptom tracking data
    - Medication and prescription records
    - Laboratory, diagnostic, and test results
    - Clinical notes, treatment plans, and care coordination documentation
    - Diet, nutrition plans, dietary logs, and recommendations
    - Medical equipment usage data (oxygen therapy, inhalers, respiratory devices, etc.)

    3. Purpose of Disclosure
    This Authorization permits COPD Fuel to disclose my PHI for care coordination, communication with individuals I designate, and any other purpose I specify within the app or in writing.

    4. Individual Rights and Acknowledgments
    I understand that:
    - This Authorization is voluntary. I am not required to sign it.
    - I may refuse to sign this Authorization.
    - I may revoke this Authorization at any time through the COPD Fuel platform or by emailing support@copdfuel.com. Revocation will not apply to uses or disclosures made before the effective date of revocation.
    - COPD Fuel may rely on this Authorization until a valid revocation is received.
    - Once my PHI is disclosed to the person(s) I designate, it may no longer be protected under the HIPAA Privacy Rule and may be subject to redisclosure.
    - I have the right to receive a copy of this signed Authorization in electronic form.

    5. Expiration
    This Authorization remains valid for as long as my COPD Fuel account is active, unless I revoke it earlier. It will terminate upon my written revocation or upon closure or deactivation of my account.

    6. Account Security
    I am responsible for maintaining the confidentiality of my account credentials. COPD Fuel will not be liable for unauthorized access resulting from my sharing or failing to safeguard my login credentials.

    7. Electronic Consent
    By checking the box and electronically signing, I confirm that I have read and understand this Authorization, I agree to the disclosure of my PHI as described above, and I understand my rights as outlined herein.
    """

    static let consentTitle = "Health Information Consent"

    /// strings.xml `health_info_consent_body` (verbatim).
    static let consentBody = """
    By signing, you allow COPD Fuel, owned by Ingenious Medical Solutions, LLC, to share your medical records with the person or organization you choose.

    What information is shared?

    Your complete medical record.

    Any other specific health details you choose to list.

    Why is it being shared?

    To help with your medical treatment.

    Because you requested it.

    Important Things to Know:

    It's Voluntary: You do not have to sign this to receive medical care.

    You Can Change Your Mind: You can cancel this permission in writing at any time.

    Expiration: This permission will end on the date you select below (or until you withdraw).

    Privacy Note: Once your records are shared, they may no longer be protected by federal privacy rules.
    """

    static let agree = "I have read and agree to the disclosure of my PHI as described in this Authorization."
    static let nameHint = "Patient Signature (full name)"
    static let dobHint = "Date of Birth"
    static let signatureLabel = "Sign with your finger or stylus"
    static let confirm = "By tapping \"Agree & Sign\", I confirm I am the patient and that the date below is correct."
    static let agreeSign = "Agree & Sign"
    static let revoke = "Revoke authorization"
    static let revokeConfirm = "Revoke your HIPAA authorization? You can sign again later if needed."
    static let dobRequired = "Date of birth is required."
    static let signatureRequired = "Please draw your signature above."
    static let expiryRequired = "Please enter an expiry date."
}

struct HipaaAuthorizationView: View {
    @Environment(\.dismiss) private var dismiss
    @ObservedObject private var consentStorage = HipaaConsentStorage.shared

    @State private var agreeChecked = false
    @State private var printedName = ""
    @State private var dobText = ""
    @State private var dobDate = Date()
    @State private var showDobPicker = false
    @State private var expiryType = "1_year"
    @State private var expiryText = ""
    @State private var expiryDate = Calendar.current.date(byAdding: .year, value: 1, to: Date()) ?? Date()
    @State private var showExpiryPicker = false
    @State private var signatureIsEmpty = true
    @State private var clearTrigger = 0
    @State private var errorMessage: String?
    @State private var showRevokeConfirm = false

    private static let minimumDob: Date = {
        Calendar.current.date(from: DateComponents(year: 1900, month: 1, day: 1)) ?? Date(timeIntervalSince1970: 0)
    }()
    private let longDate: DateFormatter = { let f = DateFormatter(); f.dateFormat = "MMMM d, yyyy"; return f }()
    private let slashDate: DateFormatter = {
        let f = DateFormatter(); f.locale = Locale(identifier: "en_US_POSIX"); f.dateFormat = "MM/dd/yyyy"; return f
    }()

    private var isSigned: Bool { consentStorage.hasValidConsent() }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if !isSigned {
                        Text(HipaaCopy.fullText)
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: "1f2937"))
                        Text(HipaaCopy.consentTitle)
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(Color(hex: "1f2937"))
                        Text(HipaaCopy.consentBody)
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "1f2937"))
                        agreeRow
                        TextField(HipaaCopy.nameHint, text: $printedName)
                            .textFieldStyle(.roundedBorder)
                        Button(action: { showDobPicker = true }) {
                            HStack {
                                Text(dobText.isEmpty ? HipaaCopy.dobHint : dobText)
                                    .foregroundColor(dobText.isEmpty ? Color(hex: "9ca3af") : Color(hex: "1f2937"))
                                Spacer()
                            }
                            .padding(8)
                            .background(Color.white)
                            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(hex: "d1d5db"), lineWidth: 1))
                        }
                        .buttonStyle(.plain)
                        Text(HipaaCopy.signatureLabel)
                            .font(.system(size: 14))
                            .foregroundColor(Color(hex: "1f2937"))
                        SignaturePadView(isEmpty: $signatureIsEmpty, clearTrigger: clearTrigger)
                            .frame(height: 160)
                        Button("Clear") { clearTrigger += 1 }
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(Color(hex: "2563eb"))
                        expirySection
                        Text(HipaaCopy.confirm)
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: "4b5563"))
                    }

                    HStack {
                        Text("Date").font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
                        Spacer()
                        Text(dateValueText).font(.system(size: 14)).foregroundColor(Color(hex: "1f2937"))
                    }

                    if let errorMessage {
                        Text(errorMessage)
                            .font(.system(size: 13))
                            .foregroundColor(Color(hex: "dc2626"))
                    }

                    if isSigned {
                        Button(action: { showRevokeConfirm = true }) {
                            Text(HipaaCopy.revoke)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color(hex: "dc2626"))
                                .cornerRadius(8)
                        }
                    } else {
                        Button(action: submit) {
                            Text(HipaaCopy.agreeSign)
                                .font(.system(size: 16, weight: .semibold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 14)
                                .background(Color(hex: "2563eb"))
                                .cornerRadius(8)
                        }
                    }
                }
                .padding(20)
            }
            .navigationTitle(HipaaCopy.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Close") { dismiss() } }
            }
            .sheet(isPresented: $showDobPicker) { dobPickerSheet }
            .sheet(isPresented: $showExpiryPicker) { expiryPickerSheet }
            .alert(HipaaCopy.revoke, isPresented: $showRevokeConfirm) {
                Button("OK") {
                    consentStorage.revokeConsent()
                    ToastCenter.shared.show("Authorization revoked.")
                    dismiss()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text(HipaaCopy.revokeConfirm)
            }
        }
        .toastOverlay()
    }

    /// Form / revoked: today; signed: "Signed on MMMM d, yyyy".
    private var dateValueText: String {
        if isSigned, let d = consentStorage.getConsentDate() { return "Signed on \(longDate.string(from: d))" }
        return longDate.string(from: Date())
    }

    private var agreeRow: some View {
        Button(action: { agreeChecked.toggle() }) {
            HStack(alignment: .top, spacing: 10) {
                Image(systemName: agreeChecked ? "checkmark.square.fill" : "square")
                    .font(.system(size: 22))
                    .foregroundColor(agreeChecked ? Color(hex: "2563eb") : Color(hex: "6b7280"))
                Text(HipaaCopy.agree)
                    .font(.system(size: 14))
                    .foregroundColor(Color(hex: "1f2937"))
                    .multilineTextAlignment(.leading)
            }
        }
        .buttonStyle(.plain)
    }

    private var expirySection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Expires:").font(.system(size: 14, weight: .bold)).foregroundColor(Color(hex: "1f2937"))
            radio("1 Year from Today", "1_year")
            HStack(spacing: 10) {
                radio("On", "on_date")
                if expiryType == "on_date" {
                    Button(action: { showExpiryPicker = true }) {
                        Text(expiryText.isEmpty ? "MM / DD / YYYY" : expiryText)
                            .foregroundColor(expiryText.isEmpty ? Color(hex: "9ca3af") : Color(hex: "1f2937"))
                            .padding(8)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .background(Color.white)
                            .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color(hex: "d1d5db"), lineWidth: 1))
                    }
                    .buttonStyle(.plain)
                }
            }
            radio("Until Withdrawn", "until_withdrawn")
        }
    }

    private func radio(_ label: String, _ value: String) -> some View {
        Button(action: { expiryType = value }) {
            HStack(spacing: 8) {
                Image(systemName: expiryType == value ? "largecircle.fill.circle" : "circle")
                    .foregroundColor(Color(hex: "2563eb"))
                Text(label).font(.system(size: 14)).foregroundColor(Color(hex: "1f2937"))
            }
        }
        .buttonStyle(.plain)
    }

    private var dobPickerSheet: some View {
        NavigationStack {
            DatePicker(HipaaCopy.dobHint, selection: $dobDate, in: Self.minimumDob...Date(), displayedComponents: .date)
                .datePickerStyle(.wheel)
                .labelsHidden()
                .padding()
                .navigationTitle(HipaaCopy.dobHint)
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showDobPicker = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("OK") { dobText = slashDate.string(from: dobDate); showDobPicker = false }
                    }
                }
        }
        .presentationDetents([.medium])
    }

    private var expiryPickerSheet: some View {
        NavigationStack {
            DatePicker("Expiry", selection: $expiryDate, in: Date()..., displayedComponents: .date)
                .datePickerStyle(.wheel)
                .labelsHidden()
                .padding()
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { showExpiryPicker = false } }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("OK") { expiryText = slashDate.string(from: expiryDate); showExpiryPicker = false }
                    }
                }
        }
        .presentationDetents([.medium])
    }

    /// HipaaAuthorizationActivity.submitConsent (:255-304).
    private func submit() {
        errorMessage = nil
        if !agreeChecked { errorMessage = HipaaCopy.agree; return }
        let name = printedName.trimmingCharacters(in: .whitespaces)
        if name.isEmpty { errorMessage = HipaaCopy.nameHint; return }
        if dobText.isEmpty { errorMessage = HipaaCopy.dobRequired; return }
        if signatureIsEmpty { errorMessage = HipaaCopy.signatureRequired; return }

        var expiry: Date? = nil
        if expiryType == "on_date" {
            guard !expiryText.isEmpty, let parsed = slashDate.date(from: expiryText) else {
                errorMessage = HipaaCopy.expiryRequired
                return
            }
            expiry = parsed
        } else if expiryType == "1_year" {
            expiry = Calendar.current.date(byAdding: .year, value: 1, to: Date())
        }
        consentStorage.saveConsent(name: name, dob: dobText, expiryType: expiryType, expiryDate: expiry)
        ToastCenter.shared.show("Authorization saved.")
        dismiss()
    }
}
```
Note: the `Text("Date")` row and error are shown in every state, matching the Android layout (`hipaaDateLabel`/`hipaaDateValue` always visible). The revoked state renders the same form as unsigned (no banner).

---

### Task 5: Paywall copy, error/restore toasts, StoreKit test price (audit C2–C4)

**Files:**
- Modify: `COPDFuel/COPDFuel/Views/PaywallView.swift`
- Modify: `COPDFuel/COPDFuel/COPDFuel.storekit` (yearly `"displayPrice" : "99.99"` → `"99.00"`)

- [ ] **Step 1: Subscribe buttons and copy** — replace `subscribeButton(title:subtitle:product:)` and the two call sites:
```swift
                                if let product = store.productMonthly {
                                    subscribeButton(title: "1 week free, then \(product.displayPrice)/month", product: product, filled: true)
                                } else {
                                    unavailableButton("1 week free, then $9.99/month", filled: true)
                                }
                                if let product = store.productYearly {
                                    subscribeButton(title: "1 week free, then \(product.displayPrice)/year (save more)", product: product, filled: false)
                                } else {
                                    unavailableButton("1 week free, then $99/year (save more)", filled: false)
                                }
```
```swift
    private func subscribeButton(title: String, product: Product, filled: Bool) -> some View {
        Button {
            Task { _ = await store.purchase(product) }
        } label: {
            Group {
                if store.isLoading { ProgressView().tint(filled ? .white : Color(hex: "2563eb")) }
                else { Text(title).font(.system(size: 15, weight: .semibold)) }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
        }
        .buttonStyle(.borderedProminent)
        .tint(filled ? Color(hex: "2563eb") : Color.white)
        .foregroundColor(filled ? .white : Color(hex: "2563eb"))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "2563eb"), lineWidth: filled ? 0 : 1))
        .disabled(store.isLoading)
    }

    /// BillingManager: "Subscription not available. Try again later." when the product is missing.
    private func unavailableButton(_ title: String, filled: Bool) -> some View {
        Button { ToastCenter.shared.show("Subscription not available. Try again later.") } label: {
            Text(title).font(.system(size: 15, weight: .semibold)).frame(maxWidth: .infinity).padding(.vertical, 14)
        }
        .buttonStyle(.borderedProminent)
        .tint(filled ? Color(hex: "2563eb") : Color.white)
        .foregroundColor(filled ? .white : Color(hex: "2563eb"))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color(hex: "2563eb"), lineWidth: filled ? 0 : 1))
    }
```
Replace `headerSubtitle` with the Android template using live prices:
```swift
    private var headerSubtitle: String {
        let monthly = store.productMonthly?.displayPrice ?? "$9.99"
        let yearly = store.productYearly?.displayPrice ?? "$99"
        return "1 week free, then \(monthly)/month or \(yearly)/year. Unlock full health report sharing and support COPD Fuel."
    }
```
Delete `priceSubtitle(for:period:)` and `humanReadable(period:)`. Render the legal links as `Text("Terms of Use") | Text("Privacy Policy")` in one `HStack(spacing: 8)` with a `Text("|")` between the two `Link`s.

- [ ] **Step 2: Error + restore toasts** — delete `RestoreResultMessage`, the `restoreResultMessage` state and its `.alert(item:)`; replace `restore()`:
```swift
    /// PaywallActivity restore: "Premium restored." + close / "No subscription found.".
    private func restore() {
        Task {
            let nowPremium = await store.restorePurchases()
            if nowPremium {
                ToastCenter.shared.show("Premium restored.")
                dismiss()
            } else {
                ToastCenter.shared.show("No subscription found.")
            }
        }
    }
```
Add to the outer `NavigationStack` modifiers:
```swift
        .onChange(of: store.errorMessage) { _, message in
            if let message, !message.isEmpty { ToastCenter.shared.show(message) }
        }
        .toastOverlay()
```
(StoreKit purchase failures surface via `StoreManager.errorMessage`, which was set but never shown — audit C3.)

- [ ] **Step 3: `.storekit` yearly price** — edit `COPDFuel/COPDFuel/COPDFuel.storekit`: the `"displayPrice" : "99.99"` under `copdfuel_premium_yearly` becomes `"displayPrice" : "99.00"`.

- [ ] **Step 4: Check** — `grep -c "1 week free, then\|Restore purchases\|Subscriptions automatically renew" COPDFuel/COPDFuel/Views/PaywallView.swift` ≥ `3`; `grep -n '"99.00"' COPDFuel/COPDFuel/COPDFuel.storekit` → one hit.

---

### Task 6: Build, checks, commit

- [ ] **Step 1: Build**

```bash
xcodebuild -project "COPDFuel/COPDFuel.xcodeproj" -scheme COPDFuel -destination 'generic/platform=iOS Simulator' -derivedDataPath "$HOME/Library/Developer/Xcode/DerivedData/COPDFuel-cli" -quiet build 2>&1 | grep -E "error:" | head -20; echo "exit=${PIPESTATUS[0]}"
```
Expected `exit=0`. Likely first-pass issues: other callers of `SignaturePadView(isEmpty:onClear:)` (grep `SignaturePadView(`); `HipaaConsentStorage.getConsentDate()` callers in `RootView`/`ProfileView` are unchanged.

- [ ] **Step 2: Copy checks**

```bash
for s in "Enter email and password" "Sign in to sync your health data" "Use your email and a password to sign up." "Password must be at least 8 characters." "Verification code must be 6 digits." "Use a different email" "HIPAA Authorization for PHI Disclosure" "Patient Signature (full name)" "Sign with your finger or stylus" "1 Year from Today" "Until Withdrawn" "Agree & Sign" "Revoke your HIPAA authorization? You can sign again later if needed." "Please draw your signature above." "Authorization saved." "This Authorization is voluntary. I am not required to sign it." "Your complete medical record." "1 week free, then" "Restore purchases" "Premium restored." "No subscription found." "Subscription not available. Try again later."; do
  a=$(grep -rl -F "$s" android/app/src/main/java/com/copdhealthtracker android/app/src/main/res | wc -l | tr -d ' ')
  i=$(grep -rl -F "$s" COPDFuel/COPDFuel/Views/LoginView.swift COPDFuel/COPDFuel/Views/SignUpView.swift COPDFuel/COPDFuel/Views/HipaaAuthorizationView.swift COPDFuel/COPDFuel/Views/PaywallView.swift | wc -l | tr -d ' ')
  echo "$a android / $i ios  <- $s"
done
```
Expected: non-zero on both sides for every row (Android's "Agree &amp; Sign" matches because the `&amp;` entity only appears in the XML; if that row shows 0 on the Android side, re-run with `Agree &amp; Sign`).

- [ ] **Step 3: Removal checks**

```bash
grep -rn "Welcome back\|owned by Ingenious Medical Solutions, LLC, to use and disclose\|hipaa_consent_revoked\|hipaa_consent_name\|Submit Authorization\|Subscribe — " COPDFuel/COPDFuel --include='*.swift' | grep -v "migrateLegacyKeysIfNeeded\|removeObject"
```
Expected: no output.

- [ ] **Step 4: Commit**

```bash
cd COPDFuel && /usr/bin/git add -A && /usr/bin/git commit -q -m "iOS parity P3.F: Android login/sign-up copy + validation, HIPAA screen order/copy/expiry/revoke with Android keys and working Clear, paywall copy + toasts

Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>" && /usr/bin/git log --oneline -1
```

- [ ] **Step 5: Manual smoke (simulator)**

Login: empty fields + Sign in → inline "Enter email and password". Sign-up: "abc" email → "Email is not valid."; short password → "Password must be at least 8 characters."; mismatched → "Passwords do not match.". HIPAA: legal text first, then consent title/body, checkbox, name/DOB, pad + Clear (strokes disappear), Expires radios with the "On" date field appearing, "Date" row with today's date, "Agree & Sign" errors in order, save toast; reopening shows "Date  Signed on …" + "Revoke authorization"; revoking toasts and reopening shows the form again. Paywall: two buttons with live prices, Restore toasts.
